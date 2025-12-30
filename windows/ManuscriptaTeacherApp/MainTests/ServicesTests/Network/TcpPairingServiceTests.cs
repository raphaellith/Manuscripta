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
    private readonly Mock<IRefreshConfigTracker> _mockRefreshConfigTracker;
    private readonly IOptions<NetworkSettings> _options;
    private readonly IServiceProvider _serviceProvider;
    private readonly TcpPairingService _service;

    public TcpPairingServiceTests()
    {
        _mockDeviceRegistry = new Mock<IDeviceRegistryService>();
        _mockLogger = new Mock<ILogger<TcpPairingService>>();
        _mockRefreshConfigTracker = new Mock<IRefreshConfigTracker>();
        
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
        
        _service = new TcpPairingService(_options, _serviceProvider, _mockLogger.Object, _mockRefreshConfigTracker.Object);
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
            new TcpPairingService(null!, _serviceProvider, _mockLogger.Object, _mockRefreshConfigTracker.Object));
    }

    [Fact]
    public void Constructor_NullServiceProvider_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() =>
            new TcpPairingService(_options, null!, _mockLogger.Object, _mockRefreshConfigTracker.Object));
    }

    [Fact]
    public void Constructor_NullLogger_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() =>
            new TcpPairingService(_options, _serviceProvider, null!, _mockRefreshConfigTracker.Object));
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
        // Arrange - Use a dedicated service with its own port to avoid conflicts
        var dedicatedOptions = Options.Create(new NetworkSettings
        {
            HttpPort = 5911,
            TcpPort = new Random().Next(10000, 60000),
            UdpBroadcastPort = 5913,
            BroadcastIntervalMs = 3000
        });
        using var dedicatedService = new TcpPairingService(dedicatedOptions, _serviceProvider, _mockLogger.Object, _mockRefreshConfigTracker.Object);
        
        using var cts = new CancellationTokenSource();
        var listenTask = dedicatedService.StartListeningAsync(cts.Token);
        await Task.Delay(100); // Give it more time to start

        // Act
        dedicatedService.StopListening();
        
        // Assert
        Assert.False(dedicatedService.IsListening);
        
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
        // Arrange - Use a dedicated service with its own port to avoid conflicts
        var dedicatedOptions = Options.Create(new NetworkSettings
        {
            HttpPort = 5911,
            TcpPort = new Random().Next(10000, 60000),
            UdpBroadcastPort = 5913,
            BroadcastIntervalMs = 3000
        });
        using var dedicatedService = new TcpPairingService(dedicatedOptions, _serviceProvider, _mockLogger.Object, _mockRefreshConfigTracker.Object);
        
        using var cts = new CancellationTokenSource();
        var listenTask1 = dedicatedService.StartListeningAsync(cts.Token);
        await Task.Delay(100);

        // Act
        var listenTask2 = dedicatedService.StartListeningAsync(cts.Token);
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

    #region SendLockScreenAsync Tests

    [Fact]
    public async Task SendLockScreenAsync_WhenDeviceNotConnected_LogsWarning()
    {
        // Arrange
        var deviceId = Guid.NewGuid().ToString();

        // Act
        await _service.SendLockScreenAsync(deviceId);

        // Assert - Verify warning was logged about client not connected
        _mockLogger.Verify(
            x => x.Log(
                LogLevel.Warning,
                It.IsAny<EventId>(),
                It.Is<It.IsAnyType>((v, t) => v.ToString()!.Contains("not connected")),
                It.IsAny<Exception>(),
                It.IsAny<Func<It.IsAnyType, Exception?, string>>()),
            Times.Once);
    }

    #endregion

    #region SendUnlockScreenAsync Tests

    [Fact]
    public async Task SendUnlockScreenAsync_WhenDeviceNotConnected_LogsWarning()
    {
        // Arrange
        var deviceId = Guid.NewGuid().ToString();

        // Act
        await _service.SendUnlockScreenAsync(deviceId);

        // Assert - Verify warning was logged about client not connected
        _mockLogger.Verify(
            x => x.Log(
                LogLevel.Warning,
                It.IsAny<EventId>(),
                It.Is<It.IsAnyType>((v, t) => v.ToString()!.Contains("not connected")),
                It.IsAny<Exception>(),
                It.IsAny<Func<It.IsAnyType, Exception?, string>>()),
            Times.Once);
    }

    #endregion

    #region SendDistributeMaterialAsync Tests

    [Fact]
    public async Task SendDistributeMaterialAsync_WhenDeviceNotConnected_LogsWarningAndTimesOut()
    {
        // Arrange
        var deviceId = Guid.NewGuid().ToString();

        // Act - Timeout is 30s but device is not connected so it will skip the wait
        await _service.SendDistributeMaterialAsync(deviceId);

        // Assert - Verify warning was logged about client not connected
        _mockLogger.Verify(
            x => x.Log(
                LogLevel.Warning,
                It.IsAny<EventId>(),
                It.Is<It.IsAnyType>((v, t) => v.ToString()!.Contains("not connected")),
                It.IsAny<Exception>(),
                It.IsAny<Func<It.IsAnyType, Exception?, string>>()),
            Times.Once);
    }

    #endregion

    #region SendUnpairAsync Tests

    [Fact]
    public async Task SendUnpairAsync_WhenDeviceNotConnected_LogsWarningAndUnregistersDevice()
    {
        // Arrange
        var deviceId = Guid.NewGuid().ToString();
        _mockDeviceRegistry.Setup(r => r.UnregisterDeviceAsync(It.IsAny<Guid>()))
            .ReturnsAsync(true);

        // Act
        await _service.SendUnpairAsync(deviceId);

        // Assert - Verify warning was logged about client not connected
        _mockLogger.Verify(
            x => x.Log(
                LogLevel.Warning,
                It.IsAny<EventId>(),
                It.Is<It.IsAnyType>((v, t) => v.ToString()!.Contains("not connected")),
                It.IsAny<Exception>(),
                It.IsAny<Func<It.IsAnyType, Exception?, string>>()),
            Times.Once);

        // Verify device was unregistered from registry
        _mockDeviceRegistry.Verify(
            r => r.UnregisterDeviceAsync(Guid.Parse(deviceId)),
            Times.Once);
    }

    [Fact]
    public async Task SendUnpairAsync_WithInvalidDeviceIdFormat_LogsWarning()
    {
        // Arrange - invalid GUID format
        var invalidDeviceId = "not-a-guid";

        // Act
        await _service.SendUnpairAsync(invalidDeviceId);

        // Assert - Verify warning was logged about invalid device ID format
        _mockLogger.Verify(
            x => x.Log(
                LogLevel.Warning,
                It.IsAny<EventId>(),
                It.Is<It.IsAnyType>((v, t) => v.ToString()!.Contains("Invalid device ID format")),
                It.IsAny<Exception>(),
                It.IsAny<Func<It.IsAnyType, Exception?, string>>()),
            Times.Once);

        // Verify UnregisterDeviceAsync was NOT called
        _mockDeviceRegistry.Verify(
            r => r.UnregisterDeviceAsync(It.IsAny<Guid>()),
            Times.Never);
    }

    #endregion
}
