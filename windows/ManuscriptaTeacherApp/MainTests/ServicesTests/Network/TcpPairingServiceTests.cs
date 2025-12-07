using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using Moq;
using Main.Services;
using Main.Services.Network;
using Xunit;

namespace MainTests.ServicesTests.Network;

/// <summary>
/// Tests for TcpPairingService.
/// Verifies TCP pairing per Pairing Process.md §2(3)(a).
/// </summary>
public class TcpPairingServiceTests : IDisposable
{
    private readonly Mock<IDeviceRegistryService> _mockDeviceRegistry;
    private readonly Mock<ILogger<TcpPairingService>> _mockLogger;
    private readonly IOptions<NetworkSettings> _options;
    private readonly IServiceProvider _serviceProvider;
    private readonly TcpPairingService _service;

    public TcpPairingServiceTests()
    {
        _mockDeviceRegistry = new Mock<IDeviceRegistryService>();
        _mockLogger = new Mock<ILogger<TcpPairingService>>();
        
        // Use a random port to avoid conflicts in parallel tests
        var port = new Random().Next(10000, 60000);
        _options = Options.Create(new NetworkSettings
        {
            HttpPort = 5911,
            TcpPort = port,
            UdpBroadcastPort = 5913,
            BroadcastIntervalMs = 3000
        });
        
        // Set up a mock IServiceProvider that returns IDeviceRegistryService through a scope
        var mockScope = new Mock<IServiceScope>();
        mockScope.Setup(s => s.ServiceProvider.GetService(typeof(IDeviceRegistryService)))
            .Returns(_mockDeviceRegistry.Object);
        
        var mockScopeFactory = new Mock<IServiceScopeFactory>();
        mockScopeFactory.Setup(f => f.CreateScope()).Returns(mockScope.Object);
        
        var mockServiceProvider = new Mock<IServiceProvider>();
        mockServiceProvider.Setup(sp => sp.GetService(typeof(IServiceScopeFactory)))
            .Returns(mockScopeFactory.Object);
        
        _serviceProvider = mockServiceProvider.Object;
        
        _service = new TcpPairingService(_options, _serviceProvider, _mockLogger.Object);
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
            new TcpPairingService(null!, _serviceProvider, _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullServiceProvider_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() =>
            new TcpPairingService(_options, null!, _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullLogger_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() =>
            new TcpPairingService(_options, _serviceProvider, null!));
    }

    #endregion

    #region IsListening Tests

    [Fact]
    public void IsListening_Initially_ReturnsFalse()
    {
        // Assert
        Assert.False(_service.IsListening);
    }

    [Fact]
    public async Task IsListening_AfterStarting_ReturnsTrue()
    {
        // Arrange
        using var cts = new CancellationTokenSource();
        
        // Act
        var listenTask = _service.StartListeningAsync(cts.Token);
        await Task.Delay(50); // Give it time to start
        
        // Assert
        Assert.True(_service.IsListening);
        
        // Cleanup
        cts.Cancel();
        try { await listenTask; } catch (OperationCanceledException) { }
    }

    [Fact]
    public async Task IsListening_AfterStopping_ReturnsFalse()
    {
        // Arrange
        using var cts = new CancellationTokenSource();
        var listenTask = _service.StartListeningAsync(cts.Token);
        await Task.Delay(50);

        // Act
        _service.StopListening();
        
        // Assert
        Assert.False(_service.IsListening);
        
        // Cleanup
        cts.Cancel();
        try { await listenTask; } catch (OperationCanceledException) { }
    }

    #endregion

    #region StartListeningAsync Tests

    [Fact]
    public async Task StartListeningAsync_WhenCancelled_StopsListening()
    {
        // Arrange
        using var cts = new CancellationTokenSource();

        // Act
        var listenTask = _service.StartListeningAsync(cts.Token);
        await Task.Delay(50);
        cts.Cancel();

        // Assert - Service handles cancellation gracefully without throwing
        await listenTask; // Should complete without exception
        Assert.False(_service.IsListening);
    }

    [Fact]
    public async Task StartListeningAsync_CalledTwice_LogsWarning()
    {
        // Arrange
        using var cts = new CancellationTokenSource();
        var listenTask1 = _service.StartListeningAsync(cts.Token);
        await Task.Delay(50);

        // Act
        var listenTask2 = _service.StartListeningAsync(cts.Token);
        await listenTask2; // Should return immediately

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
        try { await listenTask1; } catch (OperationCanceledException) { }
    }

    #endregion

    #region StopListening Tests

    [Fact]
    public void StopListening_WhenNotListening_DoesNotThrow()
    {
        // Act & Assert - Should not throw
        _service.StopListening();
    }

    #endregion
}
