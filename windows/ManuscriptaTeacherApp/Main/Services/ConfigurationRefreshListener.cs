using Microsoft.Extensions.Logging;
using Main.Models.Entities;
using Main.Services.Network;

namespace Main.Services;

/// <summary>
/// Singleton listener that subscribes to <see cref="IDeviceRegistryService.DevicePaired"/>
/// and triggers a config refresh per ConfigurationManagementSpecification §3(1)(a).
/// 
/// Separated from <see cref="ConfigurationService"/> (which is scoped) to avoid
/// a memory leak from scoped instances subscribing to a singleton event.
/// </summary>
public class ConfigurationRefreshListener
{
    private readonly ITcpPairingService _tcpPairingService;
    private readonly ILogger<ConfigurationRefreshListener> _logger;

    public ConfigurationRefreshListener(
        IDeviceRegistryService deviceRegistryService,
        ITcpPairingService tcpPairingService,
        ILogger<ConfigurationRefreshListener> logger)
    {
        ArgumentNullException.ThrowIfNull(deviceRegistryService);
        _tcpPairingService = tcpPairingService ?? throw new ArgumentNullException(nameof(tcpPairingService));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));

        // §3(1)(a): When a device is first paired, request a config refresh to that device.
        deviceRegistryService.DevicePaired += OnDevicePaired;
    }

    private void OnDevicePaired(object? sender, PairedDeviceEntity device)
    {
        _ = RefreshDeviceAsync(device.DeviceId);
    }

    private async Task RefreshDeviceAsync(Guid deviceId)
    {
        try
        {
            await _tcpPairingService.SendRefreshConfigAsync(deviceId.ToString());
            _logger.LogInformation("Config refresh sent to newly paired device {DeviceId}", deviceId);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to send config refresh to newly paired device {DeviceId}", deviceId);
        }
    }
}
