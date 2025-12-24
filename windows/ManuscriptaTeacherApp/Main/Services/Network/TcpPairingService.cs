using System.Collections.Concurrent;
using System.Net;
using System.Net.Sockets;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using System.Text;

using Main.Models.Network;

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
    
    private TcpListener? _listener;
    private CancellationTokenSource? _cts;
    private bool _isListening;
    private bool _disposed;
    
    private readonly ConcurrentDictionary<string, TcpClient> _connectedClients = new();

    public TcpPairingService(
        IOptions<NetworkSettings> settings,
        IServiceProvider serviceProvider,
        ILogger<TcpPairingService> logger)
    {
        _settings = settings?.Value ?? throw new ArgumentNullException(nameof(settings));
        _serviceProvider = serviceProvider ?? throw new ArgumentNullException(nameof(serviceProvider));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
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

            await AcceptClientsAsync(_cts.Token);
        }
        catch (OperationCanceledException)
        {
            _logger.LogInformation("TCP listener stopped");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in TCP listener");
            throw;
        }
        finally
        {
            _isListening = false;
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

    public async Task SendLockScreenAsync(string deviceId)
    {
        await SendCommandAsync(deviceId, BinaryOpcodes.LockScreen, null);
    }

    public async Task SendUnlockScreenAsync(string deviceId)
    {
        await SendCommandAsync(deviceId, BinaryOpcodes.UnlockScreen, null);
    }

    private readonly ConcurrentDictionary<string, TaskCompletionSource<bool>> _distributionAcks = new();

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
            }
        }
        finally
        {
            _distributionAcks.TryRemove(deviceId, out _);
        }
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

    private async Task HandlePairingRequestAsync(NetworkStream stream, byte[] data, string clientId)
    {
        try
        {
            var deviceId = PairingMessage.DecodePairingRequest(data);
            _logger.LogInformation("Received PAIRING_REQUEST from {ClientId}, DeviceId: {DeviceId}", clientId, deviceId);

            // Create a scope to resolve the scoped IDeviceRegistryService
            // This avoids the captive dependency issue (singleton depending on scoped service)
            using var scope = _serviceProvider.CreateScope();
            var deviceRegistry = scope.ServiceProvider.GetRequiredService<IDeviceRegistryService>();
            
            // Register the device
            var isNewDevice = await deviceRegistry.RegisterDeviceAsync(deviceId);
            
            if (isNewDevice)
            {
                _logger.LogInformation("New device {DeviceId} registered via TCP", deviceId);
            }
            else
            {
                _logger.LogInformation("Device {DeviceId} re-paired via TCP", deviceId);
            }

            // Store connection mapping
            if (_connectedClients.TryGetValue(clientId, out var client)) 
            {
                _deviceConnections.AddOrUpdate(deviceId.ToString(), client, (k, v) => client);
            }

            // Send PAIRING_ACK
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
    
    // Helper class for parsing (inlined for simplicity in this artifact)
    private static class ParsingHelper {
        public static string ExtractString(byte[] data) {
            if (data == null || data.Length < 2) return string.Empty;
            return Encoding.UTF8.GetString(data, 1, data.Length - 1);
        }
    }
}
