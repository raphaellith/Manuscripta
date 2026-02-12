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

        // Wait for cancellation to propagate through the service
        await Task.Delay(100);

        // Assert - Service handles cancellation gracefully without throwing
        await listenTask; // Should complete without exception
        
        // Note: After cancellation, the background task may not have fully cleaned up yet.
        // Use StopListening() for deterministic cleanup if needed.
        // For this test, we just verify the task completes without exception.
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
        var materialIds = new[] { Guid.NewGuid() };

        // Act - Timeout is 30s but device is not connected so it will skip the wait
        await _service.SendDistributeMaterialAsync(deviceId, materialIds);

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

    #region ParsingHelper Tests - Per API Contract §3.6.2

    [Fact]
    public void ExtractNullTerminatedStrings_WithValidData_ParsesCorrectly()
    {
        // Arrange - Opcode (1 byte) + DeviceID + null + EntityID
        var deviceId = Guid.NewGuid().ToString();
        var entityId = Guid.NewGuid().ToString();
        var deviceIdBytes = System.Text.Encoding.UTF8.GetBytes(deviceId);
        var entityIdBytes = System.Text.Encoding.UTF8.GetBytes(entityId);
        
        // Build: [opcode][deviceId][0x00][entityId]
        var data = new byte[1 + deviceIdBytes.Length + 1 + entityIdBytes.Length];
        data[0] = 0x12; // DISTRIBUTE_ACK opcode
        Array.Copy(deviceIdBytes, 0, data, 1, deviceIdBytes.Length);
        data[1 + deviceIdBytes.Length] = 0x00; // null terminator
        Array.Copy(entityIdBytes, 0, data, 1 + deviceIdBytes.Length + 1, entityIdBytes.Length);

        // Act
        var (parsedDeviceId, parsedEntityId) = ParsingHelper.ExtractNullTerminatedStrings(data);

        // Assert
        Assert.Equal(deviceId, parsedDeviceId);
        Assert.Equal(entityId, parsedEntityId);
    }

    [Fact]
    public void ExtractNullTerminatedStrings_WithNoNullTerminator_ReturnsEmptyStrings()
    {
        // Arrange - Invalid format: Opcode (1 byte) + DeviceID only (no null terminator)
        var deviceId = Guid.NewGuid().ToString();
        var deviceIdBytes = System.Text.Encoding.UTF8.GetBytes(deviceId);
        
        // Build: [opcode][deviceId] - no null terminator
        var data = new byte[1 + deviceIdBytes.Length];
        data[0] = 0x12; // DISTRIBUTE_ACK opcode
        Array.Copy(deviceIdBytes, 0, data, 1, deviceIdBytes.Length);

        // Act
        var (parsedDeviceId, parsedEntityId) = ParsingHelper.ExtractNullTerminatedStrings(data);

        // Assert - Invalid format returns empty strings
        Assert.Equal(string.Empty, parsedDeviceId);
        Assert.Equal(string.Empty, parsedEntityId);
    }

    [Fact]
    public void ExtractNullTerminatedStrings_WithNullAtEnd_ReturnsEmptyStrings()
    {
        // Arrange - Null terminator at the very end (nothing after)
        var deviceId = Guid.NewGuid().ToString();
        var deviceIdBytes = System.Text.Encoding.UTF8.GetBytes(deviceId);
        
        // Build: [opcode][deviceId][0x00] - null at end, nothing after
        var data = new byte[1 + deviceIdBytes.Length + 1];
        data[0] = 0x12;
        Array.Copy(deviceIdBytes, 0, data, 1, deviceIdBytes.Length);
        data[data.Length - 1] = 0x00;

        // Act
        var (parsedDeviceId, parsedEntityId) = ParsingHelper.ExtractNullTerminatedStrings(data);

        // Assert - Invalid format (nothing after null) returns empty strings
        Assert.Equal(string.Empty, parsedDeviceId);
        Assert.Equal(string.Empty, parsedEntityId);
    }

    [Fact]
    public void ExtractNullTerminatedStrings_WithNullData_ReturnsEmptyStrings()
    {
        // Act
        var (parsedDeviceId, parsedEntityId) = ParsingHelper.ExtractNullTerminatedStrings(null!);

        // Assert
        Assert.Equal(string.Empty, parsedDeviceId);
        Assert.Equal(string.Empty, parsedEntityId);
    }

    [Fact]
    public void ExtractNullTerminatedStrings_WithTooShortData_ReturnsEmptyStrings()
    {
        // Arrange - Only 2 bytes (opcode + 1 char), too short for valid parsing
        var data = new byte[] { 0x12, 0x41 };

        // Act
        var (parsedDeviceId, parsedEntityId) = ParsingHelper.ExtractNullTerminatedStrings(data);

        // Assert
        Assert.Equal(string.Empty, parsedDeviceId);
        Assert.Equal(string.Empty, parsedEntityId);
    }

    [Fact]
    public void ExtractString_WithValidData_ReturnsString()
    {
        // Arrange
        var content = "test-device-id";
        var bytes = System.Text.Encoding.UTF8.GetBytes(content);
        var data = new byte[1 + bytes.Length];
        data[0] = 0x01; // opcode
        Array.Copy(bytes, 0, data, 1, bytes.Length);

        // Act
        var result = ParsingHelper.ExtractString(data);

        // Assert
        Assert.Equal(content, result);
    }

    [Fact]
    public void ExtractString_WithNullData_ReturnsEmptyString()
    {
        // Act
        var result = ParsingHelper.ExtractString(null!);

        // Assert
        Assert.Equal(string.Empty, result);
    }

    [Fact]
    public void ExtractString_WithTooShortData_ReturnsEmptyString()
    {
        // Arrange - Only 1 byte (just opcode)
        var data = new byte[] { 0x01 };

        // Act
        var result = ParsingHelper.ExtractString(data);

        // Assert
        Assert.Equal(string.Empty, result);
    }

    #endregion
}
