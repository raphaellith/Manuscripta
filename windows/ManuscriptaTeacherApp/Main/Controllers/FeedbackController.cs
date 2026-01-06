using Microsoft.AspNetCore.Mvc;
using Main.Services.Repositories;

namespace Main.Controllers;

/// <summary>
/// Controller for feedback retrieval operations.
/// Implements API Contract.md §2.6 and Session Interaction.md §7.
/// </summary>
[ApiController]
[Route("api/v1")]
public class FeedbackController : ControllerBase
{
    private readonly IFeedbackRepository _feedbackRepository;
    private readonly IResponseRepository _responseRepository;
    private readonly ILogger<FeedbackController> _logger;

    public FeedbackController(
        IFeedbackRepository feedbackRepository,
        IResponseRepository responseRepository,
        ILogger<FeedbackController> logger)
    {
        _feedbackRepository = feedbackRepository ?? throw new ArgumentNullException(nameof(feedbackRepository));
        _responseRepository = responseRepository ?? throw new ArgumentNullException(nameof(responseRepository));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <summary>
    /// Gets all feedback for responses submitted by a specific device.
    /// Per API Contract.md §2.6: GET /feedback/{deviceId}
    /// </summary>
    /// <param name="deviceId">The device ID to get feedback for.</param>
    /// <returns>200 OK with feedback array, or 404 Not Found.</returns>
    [HttpGet("feedback/{deviceId}")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetFeedback(string deviceId)
    {
        if (string.IsNullOrWhiteSpace(deviceId))
        {
            _logger.LogWarning("Feedback request with missing device ID");
            return BadRequest(new { error = "DeviceId is required" });
        }

        if (!Guid.TryParse(deviceId, out var parsedDeviceId))
        {
            _logger.LogWarning("Feedback request with invalid device ID format: {DeviceId}", deviceId);
            return BadRequest(new { error = "DeviceId must be a valid GUID" });
        }

        _logger.LogInformation("Feedback request for device {DeviceId}", parsedDeviceId);

        try
        {
            // Get all feedback for responses submitted by this device
            var allFeedback = await _feedbackRepository.GetAllAsync();
            
            // Filter to feedback for responses from this device
            // We need to check response ownership by device
            var deviceFeedback = new List<Main.Models.Entities.FeedbackEntity>();
            
            foreach (var feedback in allFeedback)
            {
                var response = await _responseRepository.GetByIdAsync(feedback.ResponseId);
                if (response != null && response.DeviceId == parsedDeviceId)
                {
                    deviceFeedback.Add(feedback);
                }
            }

            if (!deviceFeedback.Any())
            {
                // Per API Contract §2.6: 404 Not Found if no feedback available
                _logger.LogInformation("No feedback available for device {DeviceId}", parsedDeviceId);
                return NotFound(new { error = "No feedback available for this device" });
            }

            _logger.LogInformation(
                "Returning {FeedbackCount} feedback items for device {DeviceId}",
                deviceFeedback.Count,
                parsedDeviceId);

            // Per API Contract §2.6: Response format
            return Ok(new
            {
                feedback = deviceFeedback
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing feedback request for device {DeviceId}", parsedDeviceId);
            return StatusCode(StatusCodes.Status500InternalServerError, new { error = "Internal server error" });
        }
    }
}
