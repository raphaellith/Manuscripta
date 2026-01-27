using Microsoft.AspNetCore.SignalR;
using Main.Services.Network;
using Main.Models.Events;
using Main.Models.Entities;
using Main.Services.Hubs;

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
    private readonly ILogger<HubEventBridge> _logger;
    private bool _disposed;

    public HubEventBridge(
        ITcpPairingService tcpPairingService,
        IDeviceRegistryService deviceRegistryService,
        IHubContext<TeacherPortalHub> hubContext,
        ILogger<HubEventBridge> logger)
    {
        _tcpPairingService = tcpPairingService ?? throw new ArgumentNullException(nameof(tcpPairingService));
        _deviceRegistryService = deviceRegistryService ?? throw new ArgumentNullException(nameof(deviceRegistryService));
        _hubContext = hubContext ?? throw new ArgumentNullException(nameof(hubContext));
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

        _deviceRegistryService.DevicePaired -= OnDevicePaired;

        return Task.CompletedTask;
    }

    private async void OnStatusUpdateReceived(object? sender, DeviceStatusEventArgs e)
    {
        try
        {
            // Per NetworkingAPISpec §2(1)(a)
            // Backend maps StatusUpdateReceived -> UpdateDeviceStatus client handler
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
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error bridging StatusUpdateReceived to SignalR");
        }
    }

    private async void OnDeviceDisconnected(object? sender, DeviceStatusEventArgs e)
    {
        try
        {
            // Per NetworkingAPISpec §2(1)(a)(i)
            // Maps to UpdateDeviceStatus with DISCONNECTED
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
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error bridging DeviceDisconnected to SignalR");
        }
    }

    private async void OnHandRaisedReceived(object? sender, Guid deviceId)
    {
        try
        {
            // Per NetworkingAPISpec §2(1)(d)(i)
            await _hubContext.Clients.All.SendAsync("HandRaised", deviceId.ToString());
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error bridging HandRaisedReceived to SignalR");
        }
    }

    private async void OnDistributionTimedOut(object? sender, Guid deviceId)
    {
        try
        {
            // Per NetworkingAPISpec §2(1)(d)(ii)
            await _hubContext.Clients.All.SendAsync("DistributionFailed", deviceId.ToString());
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error bridging DistributionTimedOut to SignalR");
        }
    }

    private async void OnFeedbackDeliveryTimedOut(object? sender, Guid deviceId)
    {
        try
        {
            // Per NetworkingAPISpec §2(1)(d)(v)
            await _hubContext.Clients.All.SendAsync("FeedbackDeliveryFailed", deviceId.ToString());
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error bridging FeedbackDeliveryTimedOut to SignalR");
        }
    }

    private async void OnControlCommandTimedOut(object? sender, ControlTimeoutEventArgs e)
    {
        try
        {
            // Per NetworkingAPISpec §2(1)(d)(iii) and (iv)
            // Differentiate based on command type if needed, but spec maps generic failures
            // Currently spec says "RemoteControlFailed" for lock/unlock
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
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error bridging ControlCommandTimedOut to SignalR");
        }
    }

    private async void OnDevicePaired(object? sender, PairedDeviceEntity e)
    {
        try
        {
            // Per FrontendWorkflowSpecifications §5A(3)(a)
            await _hubContext.Clients.All.SendAsync("DevicePaired", e);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error bridging DevicePaired to SignalR");
        }
    }

    public void Dispose()
    {
        if (_disposed) return;
        _disposed = true;
        // StopAsync handles unsubscription
    }
}
