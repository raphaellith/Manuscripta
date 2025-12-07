using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using Moq;
using Main.Services.Network;
using Xunit;

namespace MainTests.ServicesTests.Network;

/// <summary>
/// Tests for UdpBroadcastService.
/// Verifies UDP broadcasting per Pairing Process.md §2(1).
/// </summary>
public class UdpBroadcastServiceTests : IDisposable
{
    private readonly Mock<ILogger<UdpBroadcastService>> _mockLogger;
    private readonly IOptions<NetworkSettings> _options;
    private readonly UdpBroadcastService _service;

    public UdpBroadcastServiceTests()
    {
        _mockLogger = new Mock<ILogger<UdpBroadcastService>>();
        _options = Options.Create(new NetworkSettings
        {
            HttpPort = 5911,
            TcpPort = 5912,
            UdpBroadcastPort = 5913,
            BroadcastIntervalMs = 100 // Short interval for testing
        });
        _service = new UdpBroadcastService(_options, _mockLogger.Object);
    }

    public void Dispose()
    {
        _service.Dispose();
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullSettings_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() =>
            new UdpBroadcastService(null!, _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullLogger_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() =>
            new UdpBroadcastService(_options, null!));
    }

    #endregion

    #region IsBroadcasting Tests

    [Fact]
    public void IsBroadcasting_Initially_ReturnsFalse()
    {
        // Assert
        Assert.False(_service.IsBroadcasting);
    }

    [Fact]
    public async Task IsBroadcasting_AfterStarting_ReturnsTrue()
    {
        // Arrange
        using var cts = new CancellationTokenSource();
        
        // Act
        var broadcastTask = _service.StartBroadcastingAsync(cts.Token);
        await Task.Delay(50); // Give it time to start
        
        // Assert
        Assert.True(_service.IsBroadcasting);
        
        // Cleanup
        cts.Cancel();
        try { await broadcastTask; } catch (OperationCanceledException) { }
    }

    [Fact]
    public async Task IsBroadcasting_AfterStopping_ReturnsFalse()
    {
        // Arrange
        using var cts = new CancellationTokenSource();
        var broadcastTask = _service.StartBroadcastingAsync(cts.Token);
        await Task.Delay(50);

        // Act
        _service.StopBroadcasting();
        
        // Assert
        Assert.False(_service.IsBroadcasting);
        
        // Cleanup
        cts.Cancel();
        try { await broadcastTask; } catch (OperationCanceledException) { }
    }

    #endregion

    #region StartBroadcastingAsync Tests

    [Fact]
    public async Task StartBroadcastingAsync_WhenCancelled_StopsBroadcasting()
    {
        // Arrange
        using var cts = new CancellationTokenSource();

        // Act
        var broadcastTask = _service.StartBroadcastingAsync(cts.Token);
        await Task.Delay(50);
        cts.Cancel();

        // Assert - Service handles cancellation gracefully without throwing
        await broadcastTask; // Should complete without exception
        Assert.False(_service.IsBroadcasting);
    }

    [Fact]
    public async Task StartBroadcastingAsync_CalledTwice_LogsWarning()
    {
        // Arrange
        using var cts = new CancellationTokenSource();
        var broadcastTask1 = _service.StartBroadcastingAsync(cts.Token);
        await Task.Delay(50);

        // Act
        var broadcastTask2 = _service.StartBroadcastingAsync(cts.Token);
        await broadcastTask2; // Should return immediately

        // Assert - Verify warning was logged
        _mockLogger.Verify(
            x => x.Log(
                LogLevel.Warning,
                It.IsAny<EventId>(),
                It.Is<It.IsAnyType>((v, t) => v.ToString()!.Contains("already active")),
                It.IsAny<Exception>(),
                It.IsAny<Func<It.IsAnyType, Exception?, string>>()),
            Times.Once);

        // Cleanup
        cts.Cancel();
        try { await broadcastTask1; } catch (OperationCanceledException) { }
    }

    #endregion

    #region StopBroadcasting Tests

    [Fact]
    public void StopBroadcasting_WhenNotBroadcasting_DoesNotThrow()
    {
        // Act & Assert - Should not throw
        _service.StopBroadcasting();
    }

    #endregion
}
