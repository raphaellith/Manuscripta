using Microsoft.AspNetCore.Mvc;

using Main.Models.Dtos;
using Main.Services;

namespace Main.Controllers;

/// <summary>
/// Controller for material distribution operations.
/// Implements API Contract.md §2.5.
/// </summary>
[ApiController]
[Route("api/v1")]
public class DistributionController : ControllerBase
{
    private readonly IDistributionService _distributionService;
    private readonly ILogger<DistributionController> _logger;

    public DistributionController(
        IDistributionService distributionService,
        ILogger<DistributionController> logger)
    {
        _distributionService = distributionService ?? throw new ArgumentNullException(nameof(distributionService));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <summary>
    /// Gets the distribution bundle for a specific device.
    /// Per API Contract.md §2.5: GET /distribution/{deviceId}
    /// </summary>
    /// <param name="deviceId">The device ID to get materials for.</param>
    /// <returns>200 OK with materials and questions, or 404 Not Found.</returns>
    [HttpGet("distribution/{deviceId}")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetDistribution(string deviceId)
    {
        if (string.IsNullOrWhiteSpace(deviceId))
        {
            _logger.LogWarning("Distribution request with missing device ID");
            return BadRequest(new { error = "DeviceId is required" });
        }

        if (!Guid.TryParse(deviceId, out var parsedDeviceId))
        {
            _logger.LogWarning("Distribution request with invalid device ID format: {DeviceId}", deviceId);
            return NotFound(new { error = "No materials available for this device" });
        }

        _logger.LogInformation("Distribution request for device {DeviceId}", parsedDeviceId);

        try
        {
            var bundle = await _distributionService.GetDistributionBundleAsync(parsedDeviceId);

            if (bundle == null)
            {
                // Per API Contract §2.5: 404 Not Found if no materials available
                _logger.LogInformation("No materials available for device {DeviceId}", parsedDeviceId);
                return NotFound(new { error = "No materials available for this device" });
            }

            _logger.LogInformation(
                "Returning {MaterialCount} materials and {QuestionCount} questions for device {DeviceId}",
                bundle.Materials.Count(),
                bundle.Questions.Count(),
                parsedDeviceId);

            // Per API Contract §2.5: Response format
            // Per AdditionalValidationRules s1A(2): Use Android DTOs to exclude Windows-only fields
            // Per AdditionalValidationRules s1A(3): Flat composition-like structure
            return Ok(new
            {
                materials = bundle.Materials.Select(AndroidMaterialDto.FromEntity),
                questions = bundle.Questions.Select(AndroidQuestionDto.FromEntity)
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing distribution request for device {DeviceId}", parsedDeviceId);
            return StatusCode(StatusCodes.Status500InternalServerError, new { error = "Internal server error" });
        }
    }
}
