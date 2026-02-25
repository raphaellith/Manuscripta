using Microsoft.AspNetCore.Mvc;
using Main.Services;
using Main.Services.Network;

namespace Main.Controllers;

/// <summary>
/// Controller for device configuration.
/// Implements GET /config/{deviceId} per API Contract.md §2.2.
/// Per ConfigurationManagementSpecification §2(2)(b): compiles defaults + overrides.
/// </summary>
[ApiController]
[Route("api/v1")]
public class ConfigController : ControllerBase
{
    private readonly ILogger<ConfigController> _logger;
    private readonly IRefreshConfigTracker _refreshTracker;
    private readonly IConfigurationService _configurationService;

    public ConfigController(
        ILogger<ConfigController> logger,
        IRefreshConfigTracker refreshTracker,
        IConfigurationService configurationService)
    {
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
        _refreshTracker = refreshTracker ?? throw new ArgumentNullException(nameof(refreshTracker));
        _configurationService = configurationService ?? throw new ArgumentNullException(nameof(configurationService));
    }

    /// <summary>
    /// Gets the compiled configuration for the device (defaults merged with per-device overrides).
    /// Per API Contract.md §2.2: GET /config/{deviceId}
    /// Per ConfigurationManagementSpecification §2(2)(b).
    /// Also serves as implicit ACK for REFRESH_CONFIG per Session Interaction.md §6(3).
    /// Configuration is only applicable to Android devices per ConfigurationManagementSpecification.
    /// </summary>
    /// <param name="deviceId">The device requesting configuration.</param>
    /// <returns>200 OK with compiled configuration; 403 Forbidden for non-Android devices.</returns>
    [HttpGet("config/{deviceId}")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status403Forbidden)]
    public async Task<IActionResult> GetConfig(string deviceId)
    {
        _logger.LogInformation("Configuration requested by {DeviceId}", deviceId);

        if (!Guid.TryParse(deviceId, out var deviceGuid))
        {
            _logger.LogWarning("Invalid device ID format: {DeviceId}", deviceId);
            return BadRequest("Invalid device ID format.");
        }

        // Validate device is Android using positive detection (checks if paired in Android registry)
        try
        {
            await _configurationService.ValidateAndroidDeviceAsync(deviceGuid);
        }
        catch (ArgumentException ex)
        {
            _logger.LogInformation("Configuration request from non-Android device {DeviceId}: {Reason}", deviceId, ex.Message);
            return StatusCode(StatusCodes.Status403Forbidden, "Device is not a valid Android device for configuration management.");
        }

        // Signal that this device has fetched config (ACK for REFRESH_CONFIG)
        _refreshTracker.MarkConfigReceived(deviceId);

        var config = await _configurationService.CompileConfigAsync(deviceGuid);
        return Ok(config);
    }
}
