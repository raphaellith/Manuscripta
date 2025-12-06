using System.Collections.Concurrent;
using System.Net;
using System.Net.Sockets;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

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

            default:
                _logger.LogWarning("Unknown opcode {Opcode:X2} from {ClientId}", opcode, clientId);
                break;
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
}
