using Microsoft.AspNetCore.SignalR;
using Main.Services.Network;
using Main.Models.Events;
using Main.Models.Entities;
using Main.Models.Enums;
using Main.Services.Hubs;
using Main.Services.Repositories;

namespace Main.Services;

/// <summary>
/// Hosted service that bridges backend events (TCP, Registry) to SignalR client handlers.
/// Implements NetworkingAPISpec §2(1).
/// </summary>
public class HubEventBridge : IHostedService, IDisposable
{
    private readonly ITcpPairingService _tcpPairingService;
    private readonly IDeviceRegistryService _deviceRegistryService;
    private readonly IHubContext<TeacherPortalHub> _hubContext;
    private readonly IServiceProvider _serviceProvider;
    private readonly ILogger<HubEventBridge> _logger;
    private bool _disposed;

    public HubEventBridge(
        ITcpPairingService tcpPairingService,
        IDeviceRegistryService deviceRegistryService,
        IHubContext<TeacherPortalHub> hubContext,
        IServiceProvider serviceProvider,
        ILogger<HubEventBridge> logger)
    {
        _tcpPairingService = tcpPairingService ?? throw new ArgumentNullException(nameof(tcpPairingService));
        _deviceRegistryService = deviceRegistryService ?? throw new ArgumentNullException(nameof(deviceRegistryService));
        _hubContext = hubContext ?? throw new ArgumentNullException(nameof(hubContext));
        _serviceProvider = serviceProvider ?? throw new ArgumentNullException(nameof(serviceProvider));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    public Task StartAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("Starting HubEventBridge");

        // Subscribe to TCP events
        _tcpPairingService.StatusUpdateReceived += OnStatusUpdateReceived;
        _tcpPairingService.DeviceDisconnected += OnDeviceDisconnected;
        _tcpPairingService.HandRaisedReceived += OnHandRaisedReceived;
        _tcpPairingService.ControlCommandTimedOut += OnControlCommandTimedOut;
        _tcpPairingService.DistributionTimedOut += OnDistributionTimedOut;
        _tcpPairingService.FeedbackDeliveryTimedOut += OnFeedbackDeliveryTimedOut;
        _tcpPairingService.FeedbackAckReceived += OnFeedbackAckReceived;
        _tcpPairingService.DistributionAckReceived += OnDistributionAckReceived;

        // Subscribe to Registry events
        _deviceRegistryService.DevicePaired += OnDevicePaired;

        return Task.CompletedTask;
    }

    public Task StopAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("Stopping HubEventBridge");

        // Unsubscribe
        _tcpPairingService.StatusUpdateReceived -= OnStatusUpdateReceived;
        _tcpPairingService.DeviceDisconnected -= OnDeviceDisconnected;
        _tcpPairingService.HandRaisedReceived -= OnHandRaisedReceived;
        _tcpPairingService.ControlCommandTimedOut -= OnControlCommandTimedOut;
        _tcpPairingService.DistributionTimedOut -= OnDistributionTimedOut;
        _tcpPairingService.FeedbackDeliveryTimedOut -= OnFeedbackDeliveryTimedOut;
        _tcpPairingService.FeedbackAckReceived -= OnFeedbackAckReceived;
        _tcpPairingService.DistributionAckReceived -= OnDistributionAckReceived;

        _deviceRegistryService.DevicePaired -= OnDevicePaired;

        return Task.CompletedTask;
    }

    private void OnStatusUpdateReceived(object? sender, DeviceStatusEventArgs e) 
        => FireAndForget(() => HandleStatusUpdateReceivedAsync(e), "StatusUpdateReceived");

    internal async Task HandleStatusUpdateReceivedAsync(DeviceStatusEventArgs e)
    {
        // Per NetworkingAPISpec §2(1)(a)
        await _hubContext.Clients.All.SendAsync("UpdateDeviceStatus", new
        {
            deviceId = e.DeviceId,
            status = e.Status,
            batteryLevel = e.BatteryLevel,
            currentMaterialId = e.CurrentMaterialId,
            studentView = e.StudentView,
            timestamp = e.Timestamp
        });
    }

    private void OnDeviceDisconnected(object? sender, DeviceStatusEventArgs e) 
        => FireAndForget(() => HandleDeviceDisconnectedAsync(e), "DeviceDisconnected");

    internal async Task HandleDeviceDisconnectedAsync(DeviceStatusEventArgs e)
    {
        // Per NetworkingAPISpec §2(1)(a)(i)
        await _hubContext.Clients.All.SendAsync("UpdateDeviceStatus", new
        {
            deviceId = e.DeviceId,
            status = "DISCONNECTED",
            batteryLevel = e.BatteryLevel,
            currentMaterialId = e.CurrentMaterialId,
            studentView = e.StudentView,
            timestamp = e.Timestamp
        });
    }

    private void OnHandRaisedReceived(object? sender, Guid deviceId) 
        => FireAndForget(() => HandleHandRaisedReceivedAsync(deviceId), "HandRaisedReceived");

    internal async Task HandleHandRaisedReceivedAsync(Guid deviceId)
    {
        // Per NetworkingAPISpec §2(1)(d)(i)
        await _hubContext.Clients.All.SendAsync("HandRaised", deviceId.ToString());
    }

    private void OnDistributionTimedOut(object? sender, EntityDeliveryFailedEventArgs e) 
        => FireAndForget(() => HandleDistributionTimedOutAsync(e), "DistributionTimedOut");

    /// <summary>
    /// Handles distribution timeout for a specific material.
    /// Per NetworkingAPISpec §2(1)(d)(ii): includes both deviceId and materialId.
    /// </summary>
    internal async Task HandleDistributionTimedOutAsync(EntityDeliveryFailedEventArgs e)
    {
        // Per NetworkingAPISpec §2(1)(d)(ii)
        await _hubContext.Clients.All.SendAsync("DistributionFailed", new
        {
            deviceId = e.DeviceId.ToString(),
            materialId = e.EntityId.ToString()
        });
    }

    private void OnFeedbackDeliveryTimedOut(object? sender, EntityDeliveryFailedEventArgs e) 
        => FireAndForget(() => HandleFeedbackDeliveryTimedOutAsync(e), "FeedbackDeliveryTimedOut");

    /// <summary>
    /// Handles feedback delivery timeout for a specific feedback entity.
    /// Per NetworkingAPISpec §2(1)(d)(v): includes both deviceId and feedbackId.
    /// </summary>
    internal async Task HandleFeedbackDeliveryTimedOutAsync(EntityDeliveryFailedEventArgs e)
    {
        // Per NetworkingAPISpec §2(1)(d)(v)
        await _hubContext.Clients.All.SendAsync("FeedbackDeliveryFailed", new
        {
            deviceId = e.DeviceId.ToString(),
            feedbackId = e.EntityId.ToString()
        });
    }

    private void OnControlCommandTimedOut(object? sender, ControlTimeoutEventArgs e) 
        => FireAndForget(() => HandleControlCommandTimedOutAsync(e), "ControlCommandTimedOut");

    internal async Task HandleControlCommandTimedOutAsync(ControlTimeoutEventArgs e)
    {
        // Per NetworkingAPISpec §2(1)(d)(iii) and (iv)
        if (e.CommandName == "REFRESH_CONFIG")
        {
            await _hubContext.Clients.All.SendAsync("ConfigRefreshFailed", e.DeviceId);
        }
        else
        {
            await _hubContext.Clients.All.SendAsync("RemoteControlFailed", new 
            { 
                deviceId = e.DeviceId, 
                command = e.CommandName 
            });
        }
    }

    private void OnDevicePaired(object? sender, PairedDeviceEntity e) 
        => FireAndForget(() => HandleDevicePairedAsync(e), "DevicePaired");

    internal async Task HandleDevicePairedAsync(PairedDeviceEntity e)
    {
        // Per FrontendWorkflowSpecifications §5A(3)(a)
        await _hubContext.Clients.All.SendAsync("DevicePaired", e);
    }

    private void OnFeedbackAckReceived(object? sender, FeedbackAckEventArgs e)
        => FireAndForget(() => HandleFeedbackAckReceivedAsync(e), "FeedbackAckReceived");

    /// <summary>
    /// Handles FEEDBACK_ACK reception by transitioning a single feedback to DELIVERED.
    /// Per API Contract.md §3.6.2: one ACK per feedback entity.
    /// Per GenAISpec §3DA(3): triggers status transition to DELIVERED and removes from batch.
    /// </summary>
    internal async Task HandleFeedbackAckReceivedAsync(FeedbackAckEventArgs e)
    {
        using var scope = _serviceProvider.CreateScope();
        var feedbackRepository = scope.ServiceProvider.GetRequiredService<IFeedbackRepository>();

        var feedback = await feedbackRepository.GetByIdAsync(e.FeedbackId);
        if (feedback != null && feedback.Status == FeedbackStatus.READY)
        {
            feedback.Status = FeedbackStatus.DELIVERED;
            await feedbackRepository.UpdateAsync(feedback);
            _logger.LogInformation("Feedback {FeedbackId} transitioned to DELIVERED for device {DeviceId}", 
                e.FeedbackId, e.DeviceId);
        }
    }

    private void OnDistributionAckReceived(object? sender, DistributionAckEventArgs e)
        => FireAndForget(() => HandleDistributionAckReceivedAsync(e), "DistributionAckReceived");

    /// <summary>
    /// Handles DISTRIBUTE_ACK reception for a single material.
    /// Per API Contract.md §3.6.2: one ACK per material entity.
    /// </summary>
    internal async Task HandleDistributionAckReceivedAsync(DistributionAckEventArgs e)
    {
        _logger.LogInformation("Distribution acknowledged for material {MaterialId} on device {DeviceId}", 
            e.MaterialId, e.DeviceId);
        // Per Session Interaction §3(7): duplicate prevention is handled in TcpPairingService
        // Could notify frontend of successful delivery if needed
        await Task.CompletedTask;
    }

    private async void FireAndForget(Func<Task> action, string context)
    {
        try
        {
            await action();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error bridging {Context} to SignalR", context);
        }
    }

    public void Dispose()
    {
        if (_disposed) return;
        _disposed = true;
        // StopAsync handles unsubscription
    }
}
