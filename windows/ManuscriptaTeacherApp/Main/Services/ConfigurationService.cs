using Microsoft.Extensions.Logging;
using Main.Models.Entities;
using Main.Models.Enums;
using Main.Services.Network;
using Main.Services.Repositories;

namespace Main.Services;

/// <summary>
/// Service for managing configuration with validation and config refresh triggers.
/// Implements the two-tier model per ConfigurationManagementSpecification:
/// - Defaults: long-term persisted via <see cref="IDefaultConfigurationRepository"/>
/// - Per-device overrides: short-term persisted via <see cref="IConfigurationOverrideRepository"/>
/// Triggers config refresh per ConfigurationManagementSpecification §3.
/// </summary>
public class ConfigurationService : IConfigurationService
{
    private readonly IDefaultConfigurationRepository _defaultsRepository;
    private readonly IConfigurationOverrideRepository _overrideRepository;
    private readonly ITcpPairingService _tcpPairingService;
    private readonly IDeviceRegistryService _deviceRegistryService;
    private readonly ILogger<ConfigurationService> _logger;

    public ConfigurationService(
        IDefaultConfigurationRepository defaultsRepository,
        IConfigurationOverrideRepository overrideRepository,
        ITcpPairingService tcpPairingService,
        IDeviceRegistryService deviceRegistryService,
        ILogger<ConfigurationService> logger)
    {
        _defaultsRepository = defaultsRepository ?? throw new ArgumentNullException(nameof(defaultsRepository));
        _overrideRepository = overrideRepository ?? throw new ArgumentNullException(nameof(overrideRepository));
        _tcpPairingService = tcpPairingService ?? throw new ArgumentNullException(nameof(tcpPairingService));
        _deviceRegistryService = deviceRegistryService ?? throw new ArgumentNullException(nameof(deviceRegistryService));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));

        // §3(1)(a): When a device is first paired, request a config refresh to that device.
        _deviceRegistryService.DevicePaired += OnDevicePaired;
    }

    /// <inheritdoc />
    public async Task<ConfigurationEntity> GetDefaultsAsync()
    {
        return await _defaultsRepository.GetAsync();
    }

    /// <inheritdoc />
    public async Task<ConfigurationEntity> UpdateDefaultsAsync(ConfigurationEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        ValidateConfiguration(entity);
        await _defaultsRepository.UpdateAsync(entity);

        // §3(1)(b): When a default value is set or changed, refresh all devices.
        await RefreshAllDevicesAsync();

        return entity;
    }

    /// <inheritdoc />
    public ConfigurationOverride? GetOverride(Guid deviceId)
    {
        return _overrideRepository.GetByDeviceId(deviceId);
    }

    /// <inheritdoc />
    public void SetOverride(Guid deviceId, ConfigurationOverride overrides)
    {
        if (overrides == null)
            throw new ArgumentNullException(nameof(overrides));

        ValidateOverride(overrides);
        _overrideRepository.Set(deviceId, overrides);

        // §3(1)(c): When an override value is set or changed, refresh that device.
        _ = RefreshDeviceAsync(deviceId);
    }

    /// <inheritdoc />
    public void RemoveOverride(Guid deviceId)
    {
        _overrideRepository.Remove(deviceId);

        // §3(1)(c): Removing overrides changes the effective config, trigger refresh.
        _ = RefreshDeviceAsync(deviceId);
    }

    /// <inheritdoc />
    public async Task<ConfigurationEntity> CompileConfigAsync(Guid deviceId)
    {
        var defaults = await _defaultsRepository.GetAsync();
        var overrides = _overrideRepository.GetByDeviceId(deviceId);

        if (overrides == null || overrides.IsEmpty)
            return defaults;

        // Merge: override fields win when non-null, per §2(2)(b)
        return new ConfigurationEntity(
            id: defaults.Id,
            textSize: overrides.TextSize ?? defaults.TextSize,
            feedbackStyle: overrides.FeedbackStyle ?? defaults.FeedbackStyle,
            ttsEnabled: overrides.TtsEnabled ?? defaults.TtsEnabled,
            aiScaffoldingEnabled: overrides.AiScaffoldingEnabled ?? defaults.AiScaffoldingEnabled,
            summarisationEnabled: overrides.SummarisationEnabled ?? defaults.SummarisationEnabled,
            mascotSelection: overrides.MascotSelection ?? defaults.MascotSelection);
    }

    #region §3 Config Refresh Triggers

    /// <summary>
    /// Handles device paired event per §3(1)(a).
    /// </summary>
    private void OnDevicePaired(object? sender, PairedDeviceEntity device)
    {
        _ = RefreshDeviceAsync(device.DeviceId);
    }

    /// <summary>
    /// Sends config refresh to a specific device.
    /// </summary>
    private async Task RefreshDeviceAsync(Guid deviceId)
    {
        try
        {
            await _tcpPairingService.SendRefreshConfigAsync(deviceId.ToString());
            _logger.LogInformation("Config refresh sent to device {DeviceId}", deviceId);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to send config refresh to device {DeviceId}", deviceId);
        }
    }

    /// <summary>
    /// Sends config refresh to all paired devices per §3(1)(b).
    /// </summary>
    private async Task RefreshAllDevicesAsync()
    {
        var devices = await _deviceRegistryService.GetAllAsync();
        foreach (var device in devices)
        {
            await RefreshDeviceAsync(device.DeviceId);
        }
    }

    #endregion

    #region Validation per §2G

    /// <summary>
    /// Validates a full configuration entity per Validation Rules §2G.
    /// </summary>
    internal static void ValidateConfiguration(ConfigurationEntity entity)
    {
        // §2G(1)(a): TextSize must be between 5 and 50 (inclusive)
        if (entity.TextSize < 5 || entity.TextSize > 50)
            throw new ArgumentException(
                $"TextSize must be between 5 and 50 (inclusive). Got: {entity.TextSize}",
                nameof(entity));

        // §2G(2)(a): FeedbackStyle must match one of the allowed values
        if (!Enum.IsDefined(typeof(FeedbackStyle), entity.FeedbackStyle))
            throw new ArgumentException(
                $"FeedbackStyle must be a valid value. Got: {entity.FeedbackStyle}",
                nameof(entity));

        // §2G(2)(a): MascotSelection must match one of the allowed values
        if (!Enum.IsDefined(typeof(MascotSelection), entity.MascotSelection))
            throw new ArgumentException(
                $"MascotSelection must be a valid value. Got: {entity.MascotSelection}",
                nameof(entity));
    }

    /// <summary>
    /// Validates override values per Validation Rules §2G.
    /// Only non-null fields are validated.
    /// </summary>
    internal static void ValidateOverride(ConfigurationOverride overrides)
    {
        if (overrides.TextSize.HasValue && (overrides.TextSize.Value < 5 || overrides.TextSize.Value > 50))
            throw new ArgumentException(
                $"TextSize must be between 5 and 50 (inclusive). Got: {overrides.TextSize.Value}",
                nameof(overrides));

        if (overrides.FeedbackStyle.HasValue && !Enum.IsDefined(typeof(FeedbackStyle), overrides.FeedbackStyle.Value))
            throw new ArgumentException(
                $"FeedbackStyle must be a valid value. Got: {overrides.FeedbackStyle.Value}",
                nameof(overrides));

        if (overrides.MascotSelection.HasValue && !Enum.IsDefined(typeof(MascotSelection), overrides.MascotSelection.Value))
            throw new ArgumentException(
                $"MascotSelection must be a valid value. Got: {overrides.MascotSelection.Value}",
                nameof(overrides));
    }

    #endregion
}
