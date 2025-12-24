using Microsoft.AspNetCore.Mvc;

namespace Main.Controllers;

/// <summary>
/// Controller for device configuration.
/// Implements dummy endpoint for /config.
/// </summary>
[ApiController]
[Route("api/v1")]
public class ConfigController : ControllerBase
{
    private readonly ILogger<ConfigController> _logger;

    public ConfigController(ILogger<ConfigController> logger)
    {
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <summary>
    /// Gets the configuration for the device.
    /// Per API Contract.md §2.2: GET /config
    /// </summary>
    /// <returns>200 OK with configuration.</returns>
    [HttpGet("config")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public IActionResult GetConfig()
    {
        _logger.LogInformation("Configuration requested");

        // Dummy configuration as stated in the provisional API contract
        return Ok(new
        {
            KioskMode = true,
            TextSize = "medium"
        });
    }
}
