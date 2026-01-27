using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

using Main.Models.Network;

namespace Main.Services.Network;

/// <summary>
/// Configuration settings for network services.
/// </summary>
public class NetworkSettings
{
    public int HttpPort { get; set; } = 5911;
    public int TcpPort { get; set; } = 5912;
    public int UdpBroadcastPort { get; set; } = 5913;
    public int BroadcastIntervalMs { get; set; } = 3000;
}

/// <summary>
/// Implementation of UDP broadcasting service.
/// Broadcasts server presence per Pairing Process.md §2(1) and API Contract.md §1.1.
/// </summary>
public class UdpBroadcastService : IUdpBroadcastService, IDisposable
{
    private readonly NetworkSettings _settings;
    private readonly ILogger<UdpBroadcastService> _logger;
    private UdpClient? _udpClient;
    private CancellationTokenSource? _cts;
    private bool _isBroadcasting;
    private bool _disposed;

    public UdpBroadcastService(
        IOptions<NetworkSettings> settings,
        ILogger<UdpBroadcastService> logger)
    {
        _settings = settings?.Value ?? throw new ArgumentNullException(nameof(settings));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <inheritdoc />
    public bool IsBroadcasting => _isBroadcasting;

    /// <inheritdoc />
    public async Task StartBroadcastingAsync(CancellationToken cancellationToken)
    {
        if (_isBroadcasting)
        {
            _logger.LogWarning("UDP broadcasting is already active");
            return;
        }

        _cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        _udpClient = new UdpClient();
        _udpClient.EnableBroadcast = true;

        _isBroadcasting = true;
        _logger.LogInformation(
            "Starting UDP broadcast on port {Port} every {Interval}ms",
            _settings.UdpBroadcastPort,
            _settings.BroadcastIntervalMs);

        try
        {
            // Run broadcast loop in background so we don't block the caller (Hub)
            _ = Task.Run(() => BroadcastLoopAsync(_cts.Token), _cts.Token);
        }
        catch (OperationCanceledException)
        {
            _logger.LogInformation("UDP broadcasting stopped");
            _isBroadcasting = false;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error during UDP broadcasting");
            _isBroadcasting = false;
            throw;
        }
    }

    /// <inheritdoc />
    public void StopBroadcasting()
    {
        if (!_isBroadcasting)
        {
            return;
        }

        _logger.LogInformation("Stopping UDP broadcasting");
        _cts?.Cancel();
        _udpClient?.Close();
        _isBroadcasting = false;
    }

    private async Task BroadcastLoopAsync(CancellationToken cancellationToken)
    {
        var broadcastEndpoint = new IPEndPoint(IPAddress.Broadcast, _settings.UdpBroadcastPort);

        while (!cancellationToken.IsCancellationRequested)
        {
            try
            {
                var localIp = GetLocalIPAddress();
                if (localIp == null)
                {
                    _logger.LogWarning("Could not determine local IP address, skipping broadcast");
                    await Task.Delay(_settings.BroadcastIntervalMs, cancellationToken);
                    continue;
                }

                var message = new DiscoveryMessage(
                    localIp,
                    (ushort)_settings.HttpPort,
                    (ushort)_settings.TcpPort);

                var data = message.Encode();

                await _udpClient!.SendAsync(data, data.Length, broadcastEndpoint);

                _logger.LogDebug(
                    "Broadcast discovery message: IP={IP}, HTTP={HttpPort}, TCP={TcpPort}",
                    localIp,
                    _settings.HttpPort,
                    _settings.TcpPort);
            }
            catch (SocketException ex)
            {
                _logger.LogWarning(ex, "Socket error during broadcast, will retry");
            }

            await Task.Delay(_settings.BroadcastIntervalMs, cancellationToken);
        }
    }

    /// <summary>
    /// Gets the local IPv4 address of the machine.
    /// Prefers addresses from network interfaces that are up and operational.
    /// </summary>
    private IPAddress? GetLocalIPAddress()
    {
        try
        {
            // Get all network interfaces
            var interfaces = NetworkInterface.GetAllNetworkInterfaces()
                .Where(ni => ni.OperationalStatus == OperationalStatus.Up
                          && ni.NetworkInterfaceType != NetworkInterfaceType.Loopback
                          && ni.NetworkInterfaceType != NetworkInterfaceType.Tunnel);

            foreach (var networkInterface in interfaces)
            {
                var properties = networkInterface.GetIPProperties();
                var addresses = properties.UnicastAddresses
                    .Where(ua => ua.Address.AddressFamily == AddressFamily.InterNetwork
                              && !IPAddress.IsLoopback(ua.Address));

                var address = addresses.FirstOrDefault()?.Address;
                if (address != null)
                {
                    return address;
                }
            }

            // Fallback: use DNS resolution
            var host = Dns.GetHostEntry(Dns.GetHostName());
            return host.AddressList
                .FirstOrDefault(ip => ip.AddressFamily == AddressFamily.InterNetwork);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting local IP address");
            return null;
        }
    }

    public void Dispose()
    {
        if (_disposed) return;

        StopBroadcasting();
        _udpClient?.Dispose();
        _cts?.Dispose();
        _disposed = true;
    }
}
