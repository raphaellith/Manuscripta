using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.SignalR;
using Main.Models.Enums;
using Main.Models.Entities;
using Main.Services;
using Main.Services.Hubs;
using Main.Services.Network;
using Main.Services.Repositories;
using System.Text.Json;

namespace Main.Testing.Simulation;

/// <summary>
/// Controller for simulating backend behaviour during testing.
/// Existing endpoints support frontend UI simulation.
/// §5 endpoints (stage-material, stage-feedback, send-command, stage-attachment) enable
/// deterministic end-to-end integration testing per IntegrationTestSpecification §5.
/// </summary>
[ApiController]
[Route("api/simulation")]
public class SimulationController : ControllerBase
{
    private readonly IHubContext<TeacherPortalHub> _hubContext;
    private readonly IDeviceRegistryService _registryService;
    private readonly IDeviceStatusCacheService _statusCache;
    private readonly ITcpPairingService _tcpPairingService;
    private readonly IDistributionService _distributionService;
    private readonly IServiceProvider _serviceProvider;
    private readonly ILogger<SimulationController> _logger;

    public SimulationController(
        IHubContext<TeacherPortalHub> hubContext,
        IDeviceRegistryService registryService,
        IDeviceStatusCacheService statusCache,
        ITcpPairingService tcpPairingService,
        IDistributionService distributionService,
        IServiceProvider serviceProvider,
        ILogger<SimulationController> logger)
    {
        _hubContext = hubContext;
        _registryService = registryService;
        _statusCache = statusCache;
        _tcpPairingService = tcpPairingService;
        _distributionService = distributionService;
        _serviceProvider = serviceProvider;
        _logger = logger;
    }

    [HttpPost("add-device")]
    public async Task<IActionResult> AddDevice([FromQuery] int count = 1)
    {
        var devices = new List<PairedDeviceEntity>();
        
        for (int i = 0; i < count; i++)
        {
            var id = Guid.NewGuid();
            var name = $"Simulated Grid Device {Random.Shared.Next(1000, 9999)}";
            
            // RegisterDeviceAsync creates and returns the entity
            var device = await _registryService.RegisterDeviceAsync(id, name);
            
            if (device == null) continue; // Should not happen with new GUID
            
            var status = new DeviceStatusEntity(
                device.DeviceId,
                DeviceStatus.ON_TASK,
                100,
                Guid.Empty,
                "Home",
                DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
            );
            _statusCache.UpdateStatus(status);
            
            // Notify frontend
            await _hubContext.Clients.All.SendAsync("DevicePaired", device);

            devices.Add(device);
        }

        return Ok(new { Count = count, Devices = devices });
    }

    [HttpPost("update-status")]
    public async Task<IActionResult> UpdateStatus([FromBody] UpdateStatusRequest request)
    {
        if (request.Status == DeviceStatus.DISCONNECTED)
        {
             // Disconnected logic usually implies removed from active session, but here we just update status
        }

        var statusEntity = new DeviceStatusEntity(
            request.DeviceId,
            request.Status,
            100,
            Guid.Empty,
            "Simulation",
            DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
        );

        _statusCache.UpdateStatus(statusEntity);
        
        // We already have statusEntity, so we can send it directly
        await _hubContext.Clients.All.SendAsync("UpdateDeviceStatus", statusEntity);

        return Ok(new { Message = "Status updated" });
    }

    [HttpPost("raise-hand")]
    public async Task<IActionResult> RaiseHand([FromBody] DeviceIdRequest request)
    {
        _logger.LogInformation("Simulating Hand Raise for {DeviceId}", request.DeviceId);
        
        // Broadcast Event ONLY - Do not change status
        await _hubContext.Clients.All.SendAsync("HandRaised", request.DeviceId.ToString());

        return Ok(new { Message = "Hand raised event broadcasted" });
    }

    [HttpPost("disconnect")]
    public async Task<IActionResult> Disconnect([FromBody] DeviceIdRequest request)
    {
        var statusEntity = new DeviceStatusEntity(
            request.DeviceId,
            DeviceStatus.DISCONNECTED,
            0,
            Guid.Empty,
            "Disconnected",
            DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
        );

        _statusCache.UpdateStatus(statusEntity);
        
        await _hubContext.Clients.All.SendAsync("UpdateDeviceStatus", statusEntity);

        return Ok(new { Message = "Device disconnected" });
    }

    // ========================================================================
    // Integration Test Endpoints — IntegrationTestSpecification §5(2)
    // ========================================================================

    /// <summary>
    /// Stages a material bundle for a device and triggers DISTRIBUTE_MATERIAL TCP signal.
    /// Per IntegrationTestSpecification §5(2)(a).
    /// </summary>
    [HttpPost("stage-material")]
    public async Task<IActionResult> StageMaterial([FromBody] StageDeviceRequest request)
    {
        _logger.LogInformation("Staging material distribution for device {DeviceId}", request.DeviceId);

        // Get the distribution bundle to find assigned material IDs
        var bundle = await _distributionService.GetDistributionBundleAsync(request.DeviceId);
        if (bundle == null)
        {
            return BadRequest(new { error = "No materials assigned to this device" });
        }

        var materialIds = bundle.Materials.Select(m => m.Id).ToList();

        // Trigger DISTRIBUTE_MATERIAL TCP signal (opcode 0x05)
        await _tcpPairingService.SendDistributeMaterialAsync(
            request.DeviceId.ToString(), materialIds);

        return Ok(new { Message = "Material distribution triggered", MaterialCount = materialIds.Count });
    }

    /// <summary>
    /// Stages feedback for a device and triggers RETURN_FEEDBACK TCP signal.
    /// Per IntegrationTestSpecification §5(2)(b).
    /// </summary>
    [HttpPost("stage-feedback")]
    public async Task<IActionResult> StageFeedback([FromBody] StageFeedbackRequest request)
    {
        _logger.LogInformation("Staging feedback return for device {DeviceId}, response {ResponseId}",
            request.DeviceId, request.ResponseId);

        // Look up feedback entities linked to this response
        using var scope = _serviceProvider.CreateScope();
        var feedbackRepo = scope.ServiceProvider.GetRequiredService<IFeedbackRepository>();
        var feedback = await feedbackRepo.GetByResponseIdAsync(request.ResponseId);

        if (feedback == null)
        {
            return BadRequest(new { error = "No feedback found for this response" });
        }

        // Trigger RETURN_FEEDBACK TCP signal (opcode 0x07)
        await _tcpPairingService.SendReturnFeedbackAsync(
            request.DeviceId.ToString(), new[] { feedback.Id });

        return Ok(new { Message = "Feedback return triggered", FeedbackId = feedback.Id });
    }

    /// <summary>
    /// Sends a TCP command to a connected device.
    /// Per IntegrationTestSpecification §5(2)(c).
    /// </summary>
    [HttpPost("send-command")]
    public async Task<IActionResult> SendCommand([FromBody] SendCommandRequest request)
    {
        _logger.LogInformation("Sending command {Command} to device {DeviceId}",
            request.Command, request.DeviceId);

        var deviceIdStr = request.DeviceId.ToString();

        switch (request.Command)
        {
            case "LOCK_SCREEN":
                await _tcpPairingService.SendLockScreenAsync(deviceIdStr);
                break;
            case "UNLOCK_SCREEN":
                await _tcpPairingService.SendUnlockScreenAsync(deviceIdStr);
                break;
            case "REFRESH_CONFIG":
                await _tcpPairingService.SendRefreshConfigAsync(deviceIdStr);
                break;
            case "UNPAIR":
                await _tcpPairingService.SendUnpairAsync(deviceIdStr);
                break;
            default:
                return BadRequest(new { error = $"Unknown command: {request.Command}" });
        }

        return Ok(new { Message = $"Command {request.Command} sent" });
    }

    /// <summary>
    /// Stores an attachment file in the attachment directory.
    /// Per IntegrationTestSpecification §5(2)(d).
    /// </summary>
    [HttpPost("stage-attachment")]
    [Consumes("multipart/form-data")]
    public async Task<IActionResult> StageAttachment(
        [FromForm] IFormFile file,
        [FromForm] string attachmentId)
    {
        if (file == null || file.Length == 0)
        {
            return BadRequest(new { error = "File is required" });
        }

        if (string.IsNullOrWhiteSpace(attachmentId))
        {
            return BadRequest(new { error = "AttachmentId is required" });
        }

        _logger.LogInformation("Staging attachment {AttachmentId}", attachmentId);

        var attachmentsDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "ManuscriptaTeacherApp",
            "Attachments");
        Directory.CreateDirectory(attachmentsDir);

        var extension = Path.GetExtension(file.FileName);
        var filePath = Path.Combine(attachmentsDir, $"{attachmentId}{extension}");

        await using var stream = new FileStream(filePath, FileMode.Create);
        await file.CopyToAsync(stream);

        return Ok(new { Message = "Attachment staged", Path = filePath });
    }
}

public class UpdateStatusRequest
{
    public Guid DeviceId { get; set; }
    public DeviceStatus Status { get; set; }
}

public class DeviceIdRequest
{
    public Guid DeviceId { get; set; }
}

/// <summary>
/// Request DTO for stage-material endpoint.
/// Per IntegrationTestSpecification §5(2)(a).
/// </summary>
public class StageDeviceRequest
{
    public Guid DeviceId { get; set; }
}

/// <summary>
/// Request DTO for stage-feedback endpoint.
/// Per IntegrationTestSpecification §5(2)(b).
/// </summary>
public class StageFeedbackRequest
{
    public Guid DeviceId { get; set; }
    public Guid ResponseId { get; set; }
}

/// <summary>
/// Request DTO for send-command endpoint.
/// Per IntegrationTestSpecification §5(2)(c).
/// </summary>
public class SendCommandRequest
{
    public Guid DeviceId { get; set; }
    /// <summary>
    /// One of: LOCK_SCREEN, UNLOCK_SCREEN, REFRESH_CONFIG, UNPAIR.
    /// </summary>
    public string Command { get; set; } = string.Empty;
}
