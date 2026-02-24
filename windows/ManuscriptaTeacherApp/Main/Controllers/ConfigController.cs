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
    private readonly IDeviceRegistryService _deviceRegistryService;

    public ConfigController(
        ILogger<ConfigController> logger,
        IRefreshConfigTracker refreshTracker,
        IConfigurationService configurationService,
        IDeviceRegistryService deviceRegistryService)
    {
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
        _refreshTracker = refreshTracker ?? throw new ArgumentNullException(nameof(refreshTracker));
        _configurationService = configurationService ?? throw new ArgumentNullException(nameof(configurationService));
        _deviceRegistryService = deviceRegistryService ?? throw new ArgumentNullException(nameof(deviceRegistryService));
    }

    /// <summary>
    /// Gets the compiled configuration for the device (defaults merged with per-device overrides).
    /// Per API Contract.md §2.2: GET /config/{deviceId}
    /// Per ConfigurationManagementSpecification §2(2)(b).
    /// Also serves as implicit ACK for REFRESH_CONFIG per Session Interaction.md §6(3).
    /// </summary>
    /// <param name="deviceId">The device requesting configuration.</param>
    /// <returns>200 OK with compiled configuration.</returns>
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

        // Reject requests from unpaired devices per Pairing Process §1(3)
        if (!await _deviceRegistryService.IsDevicePairedAsync(deviceGuid))
        {
            _logger.LogInformation("Configuration request from unpaired device {DeviceId}", deviceId);
            return StatusCode(StatusCodes.Status403Forbidden, "Device is not paired.");
        }

        // Signal that this device has fetched config (ACK for REFRESH_CONFIG)
        _refreshTracker.MarkConfigReceived(deviceId);

        var config = await _configurationService.CompileConfigAsync(deviceGuid);
        return Ok(config);
    }
}
