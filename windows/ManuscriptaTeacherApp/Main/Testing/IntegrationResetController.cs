using Microsoft.AspNetCore.Mvc;
using Main.Models.Entities;
using Main.Models.Entities.Responses;
using Main.Models.Enums;
using Main.Services;
using Main.Services.Network;
using Main.Services.Repositories;

namespace Main.Testing;

/// <summary>
/// Resets in-memory server state between integration test runs.
/// Only operational in the Integration environment; returns 404 otherwise.
/// </summary>
[ApiController]
[Route("api/v1")]
public class IntegrationResetController : ControllerBase
{
    // Same well-known IDs as IntegrationSeedService
    private static readonly Guid TestDeviceId =
        Guid.Parse("00000001-0000-0000-0000-000000000001");
    private static readonly Guid TestMaterialId =
        Guid.Parse("00000001-0000-0000-0000-000000000003");
    private static readonly Guid TestFeedbackId =
        Guid.Parse("00000001-0000-0000-0000-000000000005");
    private static readonly Guid TestResponseId =
        Guid.Parse("00000001-0000-0000-0000-000000000006");
    private static readonly Guid FeedbackQuestionId =
        Guid.Parse("00000001-0000-0000-0000-000000000007");

    private readonly IHostEnvironment _env;
    private readonly IDeviceRegistryService _deviceRegistry;
    private readonly IResponseRepository _responseRepo;
    private readonly IFeedbackRepository _feedbackRepo;
    private readonly IDistributionService _distributionService;
    private readonly ITcpPairingService _tcpPairingService;
    private readonly ILogger<IntegrationResetController> _logger;

    /// <summary>
    /// Initialises the reset controller with required services.
    /// </summary>
    public IntegrationResetController(
        IHostEnvironment env,
        IDeviceRegistryService deviceRegistry,
        IResponseRepository responseRepo,
        IFeedbackRepository feedbackRepo,
        IDistributionService distributionService,
        ITcpPairingService tcpPairingService,
        ILogger<IntegrationResetController> logger)
    {
        _env = env;
        _deviceRegistry = deviceRegistry;
        _responseRepo = responseRepo;
        _feedbackRepo = feedbackRepo;
        _distributionService = distributionService;
        _tcpPairingService = tcpPairingService;
        _logger = logger;
    }

    /// <summary>
    /// Clears all in-memory state and re-seeds data required by tests.
    /// </summary>
    /// <returns>204 No Content on success; 404 if not in Integration mode.</returns>
    [HttpPost("integration/reset")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Reset()
    {
        if (!_env.IsEnvironment("Integration"))
        {
            return NotFound();
        }

        _logger.LogInformation("Resetting integration test state...");

        // 0. Disconnect all TCP clients to ensure clean connection state
        _tcpPairingService.DisconnectAllClients();

        // 1. Unregister all devices
        var devices = await _deviceRegistry.GetAllAsync();
        foreach (var device in devices)
        {
            await _deviceRegistry.UnregisterDeviceAsync(device.DeviceId);
        }

        // 2. Delete all responses
        var responses = await _responseRepo.GetAllAsync();
        foreach (var response in responses)
        {
            await _responseRepo.DeleteAsync(response.Id);
        }

        // 3. Delete all feedback
        var feedback = await _feedbackRepo.GetAllAsync();
        foreach (var fb in feedback)
        {
            await _feedbackRepo.DeleteAsync(fb.Id);
        }

        // 4. Clear distribution assignments for the test device
        await _distributionService.ClearDeviceAssignmentsAsync(TestDeviceId);

        // 5. Re-seed response + feedback for the feedback test path
        var seededResponse = new WrittenAnswerResponseEntity(
            TestResponseId,
            FeedbackQuestionId,
            TestDeviceId,
            "Integration test answer",
            DateTime.UtcNow,
            true);
        await _responseRepo.AddAsync(seededResponse);

        var seededFeedback = new FeedbackEntity(
            TestFeedbackId,
            TestResponseId,
            "Good answer!",
            5);
        seededFeedback.Status = FeedbackStatus.READY;
        await _feedbackRepo.AddAsync(seededFeedback);

        // 6. Re-assign materials to the test device
        await _distributionService.AssignMaterialsToDeviceAsync(
            TestDeviceId, new[] { TestMaterialId });

        _logger.LogInformation("Integration test state reset complete.");
        return NoContent();
    }
}
