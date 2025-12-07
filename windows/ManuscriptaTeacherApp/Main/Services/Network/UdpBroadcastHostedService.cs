using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;

namespace Main.Services.Network;

/// <summary>
/// Background service that runs UDP broadcasting.
/// Implements IHostedService to integrate with ASP.NET Core hosting.
/// </summary>
public class UdpBroadcastHostedService : BackgroundService
{
    private readonly IUdpBroadcastService _udpBroadcastService;
    private readonly ILogger<UdpBroadcastHostedService> _logger;

    public UdpBroadcastHostedService(
        IUdpBroadcastService udpBroadcastService,
        ILogger<UdpBroadcastHostedService> logger)
    {
        _udpBroadcastService = udpBroadcastService ?? throw new ArgumentNullException(nameof(udpBroadcastService));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _logger.LogInformation("UDP Broadcast Hosted Service starting");

        try
        {
            await _udpBroadcastService.StartBroadcastingAsync(stoppingToken);
        }
        catch (OperationCanceledException)
        {
            // Expected when the service is stopped
            _logger.LogInformation("UDP Broadcast Hosted Service stopping");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "UDP Broadcast Hosted Service encountered an error");
            throw;
        }
    }

    public override Task StopAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("UDP Broadcast Hosted Service is stopping");
        _udpBroadcastService.StopBroadcasting();
        return base.StopAsync(cancellationToken);
    }
}
