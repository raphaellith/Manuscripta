using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;

namespace Main.Services.Network;

/// <summary>
/// Background service that runs the TCP pairing listener.
/// Implements IHostedService to integrate with ASP.NET Core hosting.
/// </summary>
public class TcpPairingHostedService : BackgroundService
{
    private readonly ITcpPairingService _tcpPairingService;
    private readonly ILogger<TcpPairingHostedService> _logger;

    public TcpPairingHostedService(
        ITcpPairingService tcpPairingService,
        ILogger<TcpPairingHostedService> logger)
    {
        _tcpPairingService = tcpPairingService ?? throw new ArgumentNullException(nameof(tcpPairingService));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _logger.LogInformation("TCP Pairing Hosted Service starting");

        try
        {
            await _tcpPairingService.StartListeningAsync(stoppingToken);
        }
        catch (OperationCanceledException)
        {
            // Expected when the service is stopped
            _logger.LogInformation("TCP Pairing Hosted Service stopping");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "TCP Pairing Hosted Service encountered an error");
            throw;
        }
    }

    public override Task StopAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("TCP Pairing Hosted Service is stopping");
        _tcpPairingService.StopListening();
        return base.StopAsync(cancellationToken);
    }
}
