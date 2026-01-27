using System.Collections.Concurrent;
using System.Net;
using System.Net.Sockets;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using System.Text;

using Main.Models.Network;
using Main.Models.Events;
using Main.Models.Entities;
using Main.Models.Enums;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace Main.Services.Network;

/// <summary>
/// Implementation of TCP pairing service.
/// Handles PAIRING_REQUEST messages and responds with PAIRING_ACK per Pairing Process.md §2(3)(a).
/// </summary>
/// <remarks>
/// This service is registered as a singleton but needs to access scoped services (IDeviceRegistryService).
/// It uses IServiceProvider to create scopes when resolving scoped dependencies to avoid captive dependency issues.
/// </remarks>
public class TcpPairingService : ITcpPairingService, IDisposable
{
    private readonly NetworkSettings _settings;
    private readonly IServiceProvider _serviceProvider;
    private readonly ILogger<TcpPairingService> _logger;
    private readonly IRefreshConfigTracker _refreshConfigTracker;
    
    private TcpListener? _listener;
    private CancellationTokenSource? _cts;
    private bool _isListening;
    private bool _disposed;
    
    private readonly ConcurrentDictionary<string, TcpClient> _connectedClients = new();
    
    // Heartbeat tracking per Session Interaction.md §2(3)
    private readonly ConcurrentDictionary<string, DateTime> _lastHeartbeat = new();
    private Timer? _heartbeatMonitorTimer;
    private const int HeartbeatTimeoutSeconds = 10;

    // Pending LOCK/UNLOCK commands per Session Interaction.md §6(2)(c)
    // Key: deviceId, Value: expected DeviceStatus (LOCKED for lock, null means ON_TASK or IDLE for unlock)
    private readonly ConcurrentDictionary<string, (DeviceStatus ExpectedStatus, TaskCompletionSource<bool> Tcs, DateTime SentAt)> _pendingLockUnlock = new();
    private const int LockUnlockTimeoutSeconds = 6;

    // Events
    public event EventHandler<DeviceStatusEventArgs>? StatusUpdateReceived;
    public event EventHandler<DeviceStatusEventArgs>? DeviceDisconnected;
    public event EventHandler<ControlTimeoutEventArgs>? ControlCommandTimedOut;
    // New events per NetworkingAPISpec §2(1)
    public event EventHandler<Guid>? HandRaisedReceived;
    public event EventHandler<Guid>? DistributionTimedOut;
    public event EventHandler<Guid>? FeedbackDeliveryTimedOut;

    public TcpPairingService(
        IOptions<NetworkSettings> settings,
        IServiceProvider serviceProvider,
        ILogger<TcpPairingService> logger,
        IRefreshConfigTracker refreshConfigTracker)
    {
        _settings = settings?.Value ?? throw new ArgumentNullException(nameof(settings));
        _serviceProvider = serviceProvider ?? throw new ArgumentNullException(nameof(serviceProvider));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
        _refreshConfigTracker = refreshConfigTracker ?? throw new ArgumentNullException(nameof(refreshConfigTracker));
    }

    /// <inheritdoc />
    public bool IsListening => _isListening;

    /// <inheritdoc />
    public async Task StartListeningAsync(CancellationToken cancellationToken)
    {
        if (_isListening)
        {
            _logger.LogWarning("TCP listener is already active");
            return;
        }

        _cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        _listener = new TcpListener(IPAddress.Any, _settings.TcpPort);
        
        try
        {
            _listener.Start();
            _isListening = true;
            _logger.LogInformation("TCP pairing service listening on port {Port}", _settings.TcpPort);

            // Start heartbeat monitoring per Session Interaction.md §2(3)
            _heartbeatMonitorTimer = new Timer(
                CheckHeartbeats!,
                null,
                TimeSpan.FromSeconds(1),
                TimeSpan.FromSeconds(1));

            // Run client acceptance loop in background so we don't block the caller (Hub)
            _ = Task.Run(() => AcceptClientsAsync(_cts.Token), _cts.Token);

            // Return immediately once listener is active
        }
        catch (OperationCanceledException)
        {
            _logger.LogInformation("TCP listener stopped");
            _isListening = false;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in TCP listener");
            _isListening = false;
            throw;
        }
    }

    /// <inheritdoc />
    public void StopListening()
    {
        if (!_isListening)
        {
            return;
        }

        _logger.LogInformation("Stopping TCP listener");
        _cts?.Cancel();
        _listener?.Stop();
        _heartbeatMonitorTimer?.Dispose();
        _heartbeatMonitorTimer = null;
        
        // Close all connected clients
        foreach (var client in _connectedClients.Values)
        {
            try
            {
                client.Close();
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Error closing client connection");
            }
        }
        _connectedClients.Clear();
        
        _isListening = false;
    }

    private async Task AcceptClientsAsync(CancellationToken cancellationToken)
    {
        while (!cancellationToken.IsCancellationRequested)
        {
            try
            {
                var client = await _listener!.AcceptTcpClientAsync(cancellationToken);
                var clientId = client.Client.RemoteEndPoint?.ToString() ?? Guid.NewGuid().ToString();
                
                _logger.LogInformation("TCP client connected from {Endpoint}", clientId);
                _connectedClients.TryAdd(clientId, client);

                // Handle client in background
                _ = HandleClientAsync(client, clientId, cancellationToken);
            }
            catch (OperationCanceledException)
            {
                break;
            }
            catch (SocketException ex) when (ex.SocketErrorCode == SocketError.Interrupted)
            {
                // Listener was stopped
                break;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error accepting TCP client");
            }
        }
    }

    private async Task HandleClientAsync(TcpClient client, string clientId, CancellationToken cancellationToken)
    {
        try
        {
            using var stream = client.GetStream();
            var buffer = new byte[1024];

            while (!cancellationToken.IsCancellationRequested && client.Connected)
            {
                int bytesRead;
                try
                {
                    bytesRead = await stream.ReadAsync(buffer, 0, buffer.Length, cancellationToken);
                }
                catch (IOException)
                {
                    // Client disconnected
                    break;
                }

                if (bytesRead == 0)
                {
                    // Client disconnected
                    break;
                }

                var data = new byte[bytesRead];
                Array.Copy(buffer, data, bytesRead);

                await ProcessMessageAsync(stream, data, clientId);
            }
        }
        catch (OperationCanceledException)
        {
            // Expected
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error handling TCP client {ClientId}", clientId);
        }
        finally
        {
            _connectedClients.TryRemove(clientId, out _);
            client.Close();
            _logger.LogInformation("TCP client {ClientId} disconnected", clientId);
        }
    }

    /// <summary>
    /// Sends LOCK_SCREEN (0x01) to device and waits for LOCKED status confirmation.
    /// Per Session Interaction.md §6(2)(a) and §6(2)(c): 6-second timeout.
    /// </summary>
    public async Task SendLockScreenAsync(string deviceId)
    {
        var tcs = new TaskCompletionSource<bool>();
        _pendingLockUnlock[deviceId] = (DeviceStatus.LOCKED, tcs, DateTime.UtcNow);

        try
        {
            await SendCommandAsync(deviceId, BinaryOpcodes.LockScreen, null);
            _logger.LogInformation("Sent LOCK_SCREEN to {DeviceId}", deviceId);

            // Wait up to 6 seconds for STATUS_UPDATE with LOCKED status
            var completedTask = await Task.WhenAny(tcs.Task, Task.Delay(TimeSpan.FromSeconds(LockUnlockTimeoutSeconds)));

            if (completedTask == tcs.Task && tcs.Task.Result)
            {
                _logger.LogInformation("LOCK_SCREEN confirmed for {DeviceId}", deviceId);
            }
            else
            {
                _logger.LogWarning("Timeout waiting for LOCK_SCREEN confirmation from {DeviceId}", deviceId);
                Console.WriteLine($"[WARNING] Lock screen failed for device {deviceId} (Timeout)");
                ControlCommandTimedOut?.Invoke(this, new ControlTimeoutEventArgs(deviceId, "LOCK_SCREEN"));
            }
        }
        finally
        {
            _pendingLockUnlock.TryRemove(deviceId, out _);
        }
    }

    /// <summary>
    /// Sends UNLOCK_SCREEN (0x02) to device and waits for ON_TASK/IDLE status confirmation.
    /// Per Session Interaction.md §6(2)(b) and §6(2)(c): 6-second timeout.
    /// </summary>
    public async Task SendUnlockScreenAsync(string deviceId)
    {
        var tcs = new TaskCompletionSource<bool>();
        // Use ON_TASK as placeholder; we accept either ON_TASK or IDLE
        _pendingLockUnlock[deviceId] = (DeviceStatus.ON_TASK, tcs, DateTime.UtcNow);

        try
        {
            await SendCommandAsync(deviceId, BinaryOpcodes.UnlockScreen, null);
            _logger.LogInformation("Sent UNLOCK_SCREEN to {DeviceId}", deviceId);

            // Wait up to 6 seconds for STATUS_UPDATE with ON_TASK or IDLE status
            var completedTask = await Task.WhenAny(tcs.Task, Task.Delay(TimeSpan.FromSeconds(LockUnlockTimeoutSeconds)));

            if (completedTask == tcs.Task && tcs.Task.Result)
            {
                _logger.LogInformation("UNLOCK_SCREEN confirmed for {DeviceId}", deviceId);
            }
            else
            {
                _logger.LogWarning("Timeout waiting for UNLOCK_SCREEN confirmation from {DeviceId}", deviceId);
                Console.WriteLine($"[WARNING] Unlock screen failed for device {deviceId} (Timeout)");
                ControlCommandTimedOut?.Invoke(this, new ControlTimeoutEventArgs(deviceId, "UNLOCK_SCREEN"));
            }
        }
        finally
        {
            _pendingLockUnlock.TryRemove(deviceId, out _);
        }
    }

    private readonly ConcurrentDictionary<string, TaskCompletionSource<bool>> _distributionAcks = new();
    private readonly ConcurrentDictionary<string, TaskCompletionSource<bool>> _feedbackAcks = new();

    public async Task SendDistributeMaterialAsync(string deviceId)
    {
        var tcs = new TaskCompletionSource<bool>();
        _distributionAcks[deviceId] = tcs;

        try
        {
            await SendCommandAsync(deviceId, BinaryOpcodes.DistributeMaterial, null);

            // Wait for 30 seconds for ACK
            var completedTask = await Task.WhenAny(tcs.Task, Task.Delay(TimeSpan.FromSeconds(30)));

            if (completedTask == tcs.Task && tcs.Task.Result)
            {
                // ACK received
                _logger.LogInformation("Received DISTRIBUTE_ACK for device {DeviceId}", deviceId);
            }
            else
            {
                // Timeout or explicit failure
                _logger.LogWarning("Timeout waiting for DISTRIBUTE_ACK from device {DeviceId}", deviceId);
                // Report to console as per requirement
                Console.WriteLine($"[ERROR] Distribution to device {deviceId} failed (Timeout)");
                
                if (Guid.TryParse(deviceId, out var guid))
                {
                    DistributionTimedOut?.Invoke(this, guid);
                }
            }
        }
        finally
        {
            _distributionAcks.TryRemove(deviceId, out _);
        }
    }

    /// <summary>
    /// Sends RETURN_FEEDBACK (0x07) to device and waits for FEEDBACK_ACK.
    /// Per Session Interaction.md §7 and API Contract.md §3.4.
    /// </summary>
    public async Task SendReturnFeedbackAsync(string deviceId)
    {
        var tcs = new TaskCompletionSource<bool>();
        _feedbackAcks[deviceId] = tcs;

        try
        {
            await SendCommandAsync(deviceId, BinaryOpcodes.ReturnFeedback, null);
            _logger.LogInformation("Sent RETURN_FEEDBACK to {DeviceId}", deviceId);

            // Wait for 30 seconds for ACK (similar to distribution)
            var completedTask = await Task.WhenAny(tcs.Task, Task.Delay(TimeSpan.FromSeconds(30)));

            if (completedTask == tcs.Task && tcs.Task.Result)
            {
                _logger.LogInformation("Received FEEDBACK_ACK for device {DeviceId}", deviceId);
            }
            else
            {
                _logger.LogWarning("Timeout waiting for FEEDBACK_ACK from device {DeviceId}", deviceId);
                Console.WriteLine($"[ERROR] Feedback delivery to device {deviceId} failed (Timeout)");
                
                if (Guid.TryParse(deviceId, out var guid))
                {
                    FeedbackDeliveryTimedOut?.Invoke(this, guid);
                }
            }
        }
        finally
        {
            _feedbackAcks.TryRemove(deviceId, out _);
        }
    }

    /// <summary>
    /// Sends an UNPAIR (0x04) command to the specified device and removes it from registry.
    /// Per Pairing Process.md §3(2): "send a TCP UNPAIR message (opcode 0x04)...and remove the Android client from its registry."
    /// </summary>
    public async Task SendUnpairAsync(string deviceId)
    {
        _logger.LogInformation("Initiating unpair for device {DeviceId}", deviceId);

        // 1. Send UNPAIR command to the device
        await SendCommandAsync(deviceId, BinaryOpcodes.Unpair, null);

        // 2. Remove device from registry
        if (Guid.TryParse(deviceId, out var deviceGuid))
        {
            using var scope = _serviceProvider.CreateScope();
            var deviceRegistry = scope.ServiceProvider.GetRequiredService<IDeviceRegistryService>();
            await deviceRegistry.UnregisterDeviceAsync(deviceGuid);
        }
        else
        {
            _logger.LogWarning("Invalid device ID format for unpair: {DeviceId}", deviceId);
        }

        // 3. Close the TCP connection
        if (_deviceConnections.TryRemove(deviceId, out var client))
        {
            try
            {
                client.Close();
                _logger.LogInformation("Closed TCP connection for unpaired device {DeviceId}", deviceId);
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Error closing connection for device {DeviceId}", deviceId);
            }
        }

        Console.WriteLine($"[INFO] Device {deviceId} has been unpaired");
    }

    private async Task SendCommandAsync(string deviceId, byte opcode, byte[]? operand)
    {
        // Per Pairing Process.md §2(4), we must have stored the deviceId during the pairing phase.
        // We look up the active TCP connection associated with this deviceId from our registry.
        TcpClient? client = null;
        if (_deviceConnections.TryGetValue(deviceId, out client) && client.Connected) 
        {
            // Found it
        }
        else 
        {
            _logger.LogWarning("Client {DeviceId} not connected", deviceId);
            return;
        }

        try {
            var stream = client.GetStream();
            var message = new byte[1 + (operand?.Length ?? 0)];
            message[0] = opcode;
            if (operand != null)
            {
                Array.Copy(operand, 0, message, 1, operand.Length);
            }
            await stream.WriteAsync(message, 0, message.Length);
            _logger.LogInformation("Sent opcode {Opcode:X2} to {DeviceId}", opcode, deviceId);
        } catch (Exception ex) {
            _logger.LogError(ex, "Failed to send command to {DeviceId}", deviceId);
        }
    }

    // Secondary index for device ID to client
    private readonly ConcurrentDictionary<string, TcpClient> _deviceConnections = new();

    private async Task ProcessMessageAsync(NetworkStream stream, byte[] data, string clientId)
    {
        var opcode = PairingMessage.GetOpcode(data);
        
        if (opcode == null)
        {
            _logger.LogWarning("Received empty message from {ClientId}", clientId);
            return;
        }

        _logger.LogDebug("Received opcode {Opcode:X2} from {ClientId}", opcode, clientId);

        switch (opcode)
        {
            case BinaryOpcodes.PairingRequest:
                await HandlePairingRequestAsync(stream, data, clientId);
                break;
                
            case BinaryOpcodes.HandRaised:
                await HandleHandRaisedAsync(stream, data, clientId);
                break;

            case BinaryOpcodes.DistributeAck: // 0x12
                HandleDistributeAck(data);
                break;

            case BinaryOpcodes.FeedbackAck: // 0x13
                HandleFeedbackAck(data);
                break;

            case BinaryOpcodes.StatusUpdate: // 0x10
                HandleStatusUpdate(data, clientId);
                break;

            default:
                _logger.LogWarning("Unknown opcode {Opcode:X2} from {ClientId}", opcode, clientId);
                break;
        }
    }

    private async Task HandleHandRaisedAsync(NetworkStream stream, byte[] data, string clientId)
    {
        try 
        {
            // Message format: Opcode (1) + DeviceId (UTF-8)
            // Same format as PairingRequest essentially for decoding the ID
            var deviceIdString = ParsingHelper.ExtractString(data);
            
            _logger.LogInformation("HAND_RAISED received from {DeviceId}", deviceIdString);
            
            // 1. Signal teacher (Console for now)
            Console.WriteLine($"[ALERT] Student {deviceIdString} raised hand!");
            
            if (Guid.TryParse(deviceIdString, out var guid))
            {
                HandRaisedReceived?.Invoke(this, guid);
            }

            // 2. Send HAND_ACK (0x06) + DeviceID
            var ackPayload = Encoding.UTF8.GetBytes(deviceIdString);
            var ackMessage = new byte[1 + ackPayload.Length];
            ackMessage[0] = BinaryOpcodes.HandAck;
            Array.Copy(ackPayload, 0, ackMessage, 1, ackPayload.Length);
            
            await stream.WriteAsync(ackMessage, 0, ackMessage.Length);
            _logger.LogInformation("Sent HAND_ACK to {DeviceId}", deviceIdString);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error handling HAND_RAISED from {ClientId}", clientId);
        }
    }

    private void HandleDistributeAck(byte[] data)
    {
        try
        {
             var deviceIdString = ParsingHelper.ExtractString(data);
             if (_distributionAcks.TryGetValue(deviceIdString, out var tcs))
             {
                 tcs.TrySetResult(true);
             }
        }
        catch(Exception ex)
        {
            _logger.LogError(ex, "Error handling DISTRIBUTE_ACK");
        }
    }

    /// <summary>
    /// Handles FEEDBACK_ACK (0x13) messages from Android devices.
    /// Per API Contract.md §3.6: acknowledges successful receipt of feedback via HTTP.
    /// </summary>
    private void HandleFeedbackAck(byte[] data)
    {
        try
        {
            var deviceIdString = ParsingHelper.ExtractString(data);
            _logger.LogInformation("Received FEEDBACK_ACK from {DeviceId}", deviceIdString);
            
            if (_feedbackAcks.TryGetValue(deviceIdString, out var tcs))
            {
                tcs.TrySetResult(true);
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error handling FEEDBACK_ACK");
        }
    }

    private async Task HandlePairingRequestAsync(NetworkStream stream, byte[] data, string clientId)
    {
        try
        {
            var deviceId = PairingMessage.DecodePairingRequest(data);
            _logger.LogInformation("Received PAIRING_REQUEST from {ClientId}, DeviceId: {DeviceId}", clientId, deviceId);

            // Note: Device registration (with name) happens via the HTTP endpoint per Pairing Process §2(2)(c).
            // The TCP PAIRING_REQUEST just establishes the connection - we store it for future communication.
            
            // Store connection mapping for this device
            if (_connectedClients.TryGetValue(clientId, out var client)) 
            {
                _deviceConnections.AddOrUpdate(deviceId.ToString(), client, (k, v) => client);
                _logger.LogInformation("Stored TCP connection for device {DeviceId}", deviceId);
            }

            // Send PAIRING_ACK per Pairing Process §2(3)(a)
            var ackData = PairingMessage.EncodePairingAck();
            await stream.WriteAsync(ackData, 0, ackData.Length);
            
            _logger.LogInformation("Sent PAIRING_ACK to {ClientId}", clientId);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning(ex, "Invalid PAIRING_REQUEST from {ClientId}", clientId);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing PAIRING_REQUEST from {ClientId}", clientId);
        }
    }

    public void Dispose()
    {
        if (_disposed) return;

        StopListening();
        _listener?.Dispose();
        _cts?.Dispose();
        _disposed = true;
    }

    /// <summary>
    /// Handles STATUS_UPDATE (0x10) messages from Android devices.
    /// Per Session Interaction.md §2(1): devices send status updates at heart rate.
    /// </summary>
    private void HandleStatusUpdate(byte[] data, string clientId)
    {
        try
        {
            // Parse JSON payload (skip opcode byte)
            var jsonPayload = Encoding.UTF8.GetString(data, 1, data.Length - 1);
            
            // Configure JSON options to handle string-to-enum conversion
            var jsonOptions = new JsonSerializerOptions
            {
                PropertyNameCaseInsensitive = true,
                Converters = { new JsonStringEnumConverter() }
            };
            
            var statusEntity = JsonSerializer.Deserialize<DeviceStatusEntity>(jsonPayload, jsonOptions);

            if (statusEntity == null || statusEntity.DeviceId == Guid.Empty)
            {
                _logger.LogWarning("Invalid STATUS_UPDATE payload from {ClientId}", clientId);
                return;
            }

            var deviceIdString = statusEntity.DeviceId.ToString();

            // Update heartbeat tracking
            _lastHeartbeat[deviceIdString] = DateTime.UtcNow;

            _logger.LogDebug(
                "STATUS_UPDATE from {DeviceId}: Status={Status}, Battery={Battery}",
                deviceIdString, statusEntity.Status, statusEntity.BatteryLevel);

            // Raise event with DeviceStatusEntity data
            var eventArgs = new DeviceStatusEventArgs(
                deviceIdString,
                statusEntity.Status.ToString(),
                statusEntity.BatteryLevel,
                statusEntity.CurrentMaterialId.ToString(),
                statusEntity.StudentView,
                statusEntity.Timestamp);

            StatusUpdateReceived?.Invoke(this, eventArgs);

            // Check for pending LOCK/UNLOCK confirmations per §6(2)(c)
            if (_pendingLockUnlock.TryGetValue(deviceIdString, out var pending))
            {
                bool confirmed = false;
                
                if (pending.ExpectedStatus == DeviceStatus.LOCKED)
                {
                    // For LOCK_SCREEN, expect LOCKED
                    confirmed = statusEntity.Status == DeviceStatus.LOCKED;
                }
                else
                {
                    // For UNLOCK_SCREEN, accept ON_TASK or IDLE
                    confirmed = statusEntity.Status == DeviceStatus.ON_TASK || 
                                statusEntity.Status == DeviceStatus.IDLE;
                }

                if (confirmed)
                {
                    pending.Tcs.TrySetResult(true);
                }
            }
        }
        catch (JsonException ex)
        {
            _logger.LogWarning(ex, "Failed to parse STATUS_UPDATE JSON from {ClientId}", clientId);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error handling STATUS_UPDATE from {ClientId}", clientId);
        }
    }

    /// <summary>
    /// Checks for heartbeat silence and marks devices as DISCONNECTED.
    /// Per Session Interaction.md §2(3): after 10 seconds of no heartbeat.
    /// </summary>
    private void CheckHeartbeats(object? state)
    {
        var now = DateTime.UtcNow;
        var timeout = TimeSpan.FromSeconds(HeartbeatTimeoutSeconds);

        foreach (var kvp in _lastHeartbeat)
        {
            if (now - kvp.Value > timeout)
            {
                var deviceId = kvp.Key;
                _logger.LogWarning(
                    "Device {DeviceId} deemed DISCONNECTED (no heartbeat for {Seconds}s)",
                    deviceId, HeartbeatTimeoutSeconds);

                Console.WriteLine($"[WARNING] Device {deviceId} has been DISCONNECTED (no heartbeat)");

                // Raise disconnection event
                var eventArgs = new DeviceStatusEventArgs(
                    deviceId,
                    "DISCONNECTED",
                    0, null, null,
                    DateTimeOffset.UtcNow.ToUnixTimeMilliseconds());

                DeviceDisconnected?.Invoke(this, eventArgs);

                // Remove from tracking to avoid repeated notifications
                _lastHeartbeat.TryRemove(deviceId, out _);
            }
        }
    }

    /// <summary>
    /// Sends REFRESH_CONFIG (0x03) command to trigger device to re-fetch configuration.
    /// Per Session Interaction.md §6(3): 5-second timeout for GET /config request.
    /// </summary>
    public async Task SendRefreshConfigAsync(string deviceId)
    {
        var tcs = new TaskCompletionSource<bool>();
        _refreshConfigTracker.ExpectConfigRequest(deviceId, tcs);

        try
        {
            await SendCommandAsync(deviceId, BinaryOpcodes.RefreshConfig, null);
            _logger.LogInformation("Sent REFRESH_CONFIG to {DeviceId}", deviceId);

            // Wait up to 5 seconds for GET /config request (implicit ACK)
            var completedTask = await Task.WhenAny(tcs.Task, Task.Delay(TimeSpan.FromSeconds(5)));

            if (completedTask == tcs.Task && tcs.Task.Result)
            {
                _logger.LogInformation("REFRESH_CONFIG acknowledged by {DeviceId}", deviceId);
            }
            else
            {
                _logger.LogWarning("Timeout waiting for REFRESH_CONFIG acknowledgement from {DeviceId}", deviceId);
                Console.WriteLine($"[WARNING] Configuration refresh failed for device {deviceId} (Timeout)");
                ControlCommandTimedOut?.Invoke(this, new ControlTimeoutEventArgs(deviceId, "REFRESH_CONFIG"));
            }
        }
        finally
        {
            // Ensure we clean up if timeout occurred
            tcs.TrySetCanceled();
        }
    }

    
    
    // Helper class for parsing (inlined for simplicity in this artifact)
    private static class ParsingHelper {
        public static string ExtractString(byte[] data) {
            if (data == null || data.Length < 2) return string.Empty;
            return Encoding.UTF8.GetString(data, 1, data.Length - 1);
        }
    }
}
