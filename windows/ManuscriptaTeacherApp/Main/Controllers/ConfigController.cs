using Microsoft.AspNetCore.Mvc;
using Main.Services.Network;

namespace Main.Controllers;

/// <summary>
/// Controller for device configuration.
/// Implements GET /config/{deviceId} per API Contract.md §2.2.
/// </summary>
[ApiController]
[Route("api/v1")]
public class ConfigController : ControllerBase
{
    private readonly ILogger<ConfigController> _logger;
    private readonly IRefreshConfigTracker _refreshTracker;

    public ConfigController(
        ILogger<ConfigController> logger,
        IRefreshConfigTracker refreshTracker)
    {
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
        _refreshTracker = refreshTracker ?? throw new ArgumentNullException(nameof(refreshTracker));
    }

    /// <summary>
    /// Gets the configuration for the device.
    /// Per API Contract.md §2.2: GET /config/{deviceId}
    /// Also serves as implicit ACK for REFRESH_CONFIG per Session Interaction.md §6(3).
    /// </summary>
    /// <param name="deviceId">The device requesting configuration.</param>
    /// <returns>200 OK with configuration.</returns>
    [HttpGet("config/{deviceId}")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public IActionResult GetConfig(string deviceId)
    {
        _logger.LogInformation("Configuration requested by {DeviceId}", deviceId);

        // Signal that this device has fetched config (ACK for REFRESH_CONFIG)
        _refreshTracker.MarkConfigReceived(deviceId);

        // Dummy configuration as stated in the provisional API contract
        return Ok(new
        {
            KioskMode = true,
            TextSize = "medium"
        });
    }
}

