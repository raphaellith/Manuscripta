using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.SignalR;
using Main.Models.Enums;
using Main.Models.Entities;
using Main.Services;
using Main.Services.Hubs;
using System.Text.Json;

namespace Main.Testing.Simulation;

[ApiController]
[Route("api/simulation")]
public class SimulationController : ControllerBase
{
    private readonly IHubContext<TeacherPortalHub> _hubContext;
    private readonly IDeviceRegistryService _registryService;
    private readonly IDeviceStatusCacheService _statusCache;
    private readonly ILogger<SimulationController> _logger;

    public SimulationController(
        IHubContext<TeacherPortalHub> hubContext,
        IDeviceRegistryService registryService,
        IDeviceStatusCacheService statusCache,
        ILogger<SimulationController> logger)
    {
        _hubContext = hubContext;
        _registryService = registryService;
        _statusCache = statusCache;
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
