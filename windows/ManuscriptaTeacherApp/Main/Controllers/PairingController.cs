using Microsoft.AspNetCore.Mvc;

using Main.Models.Dtos;
using Main.Services;

namespace Main.Controllers;

/// <summary>
/// Controller for device pairing operations.
/// Implements API Contract.md §2.4 and Pairing Process.md §2(3)(b).
/// </summary>
[ApiController]
[Route("api/v1")]
public class PairingController : ControllerBase
{
    private readonly IDeviceRegistryService _deviceRegistry;
    private readonly ILogger<PairingController> _logger;

    public PairingController(
        IDeviceRegistryService deviceRegistry,
        ILogger<PairingController> logger)
    {
        _deviceRegistry = deviceRegistry ?? throw new ArgumentNullException(nameof(deviceRegistry));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <summary>
    /// Registers a device for pairing.
    /// Per API Contract.md §2.4 and Pairing Process.md §2(3)(b).
    /// </summary>
    /// <param name="request">The pairing request containing the device ID.</param>
    /// <returns>201 Created on success, 409 Conflict if already paired, 400 Bad Request if invalid.</returns>
    [HttpPost("pair")]
    [ProducesResponseType(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status409Conflict)]
    public async Task<IActionResult> Pair([FromBody] PairRequest request)
    {
        if (request == null || string.IsNullOrWhiteSpace(request.DeviceId))
        {
            _logger.LogWarning("Received pairing request with missing device ID");
            return BadRequest(new { error = "DeviceId is required" });
        }

        if (!Guid.TryParse(request.DeviceId, out var deviceId))
        {
            _logger.LogWarning("Received pairing request with invalid device ID format: {DeviceId}", request.DeviceId);
            return BadRequest(new { error = "DeviceId must be a valid GUID" });
        }

        _logger.LogInformation("Received HTTP pairing request for device {DeviceId}", deviceId);

        try
        {
            // Register the device - per Pairing Process §2(4)
            // RegisterDeviceAsync handles the check-and-register atomically at the database level
            // using a try-catch for DbUpdateException to handle race conditions
            var isNewDevice = await _deviceRegistry.RegisterDeviceAsync(deviceId);
            
            if (!isNewDevice)
            {
                // Device already existed - per API Contract §2.4, return 409 Conflict
                _logger.LogInformation("Device {DeviceId} is already paired", deviceId);
                return Conflict(new { error = "Device is already paired" });
            }
            
            _logger.LogInformation("Device {DeviceId} successfully paired via HTTP", deviceId);

            // Per API Contract §2.4: Return 201 Created with empty body
            return StatusCode(StatusCodes.Status201Created, new { });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing pairing request for device {DeviceId}", deviceId);
            return StatusCode(StatusCodes.Status500InternalServerError, new { error = "Internal server error" });
        }
    }
}
