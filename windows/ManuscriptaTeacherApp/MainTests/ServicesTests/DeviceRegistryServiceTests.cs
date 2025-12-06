using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using Moq;
using Main.Data;
using Main.Models.Entities;
using Main.Services;
using Xunit;

namespace MainTests.ServicesTests;

/// <summary>
/// Tests for DeviceRegistryService.
/// Verifies device registration per Pairing Process.md §2(4).
/// </summary>
public class DeviceRegistryServiceTests : IDisposable
{
    private readonly MainDbContext _context;
    private readonly Mock<ILogger<DeviceRegistryService>> _mockLogger;
    private readonly DeviceRegistryService _service;

    public DeviceRegistryServiceTests()
    {
        var options = new DbContextOptionsBuilder<MainDbContext>()
            .UseInMemoryDatabase(databaseName: Guid.NewGuid().ToString())
            .Options;

        _context = new MainDbContext(options);
        _mockLogger = new Mock<ILogger<DeviceRegistryService>>();
        _service = new DeviceRegistryService(_context, _mockLogger.Object);
    }

    public void Dispose()
    {
        _context.Database.EnsureDeleted();
        _context.Dispose();
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullContext_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() =>
            new DeviceRegistryService(null!, _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullLogger_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() =>
            new DeviceRegistryService(_context, null!));
    }

    #endregion

    #region RegisterDeviceAsync Tests

    [Fact]
    public async Task RegisterDeviceAsync_NewDevice_ReturnsTrue()
    {
        // Arrange
        var deviceId = Guid.NewGuid();

        // Act
        var result = await _service.RegisterDeviceAsync(deviceId);

        // Assert
        Assert.True(result);
    }

    [Fact]
    public async Task RegisterDeviceAsync_NewDevice_AddsToDatabase()
    {
        // Arrange
        var deviceId = Guid.NewGuid();

        // Act
        await _service.RegisterDeviceAsync(deviceId);

        // Assert
        var device = await _context.PairedDevices.FindAsync(deviceId);
        Assert.NotNull(device);
        Assert.Equal(deviceId, device.DeviceId);
    }

    [Fact]
    public async Task RegisterDeviceAsync_ExistingDevice_ReturnsFalse()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        await _service.RegisterDeviceAsync(deviceId);

        // Act
        var result = await _service.RegisterDeviceAsync(deviceId);

        // Assert
        Assert.False(result);
    }

    [Fact]
    public async Task RegisterDeviceAsync_ExistingDevice_DoesNotDuplicate()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        await _service.RegisterDeviceAsync(deviceId);

        // Act
        await _service.RegisterDeviceAsync(deviceId);

        // Assert
        var count = await _context.PairedDevices.CountAsync(d => d.DeviceId == deviceId);
        Assert.Equal(1, count);
    }

    [Fact]
    public async Task RegisterDeviceAsync_MultipleDevices_AllPersisted()
    {
        // Arrange
        var deviceId1 = Guid.NewGuid();
        var deviceId2 = Guid.NewGuid();
        var deviceId3 = Guid.NewGuid();

        // Act
        await _service.RegisterDeviceAsync(deviceId1);
        await _service.RegisterDeviceAsync(deviceId2);
        await _service.RegisterDeviceAsync(deviceId3);

        // Assert
        var count = await _context.PairedDevices.CountAsync();
        Assert.Equal(3, count);
    }

    #endregion

    #region IsDevicePairedAsync Tests

    [Fact]
    public async Task IsDevicePairedAsync_PairedDevice_ReturnsTrue()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        await _service.RegisterDeviceAsync(deviceId);

        // Act
        var result = await _service.IsDevicePairedAsync(deviceId);

        // Assert
        Assert.True(result);
    }

    [Fact]
    public async Task IsDevicePairedAsync_UnpairedDevice_ReturnsFalse()
    {
        // Arrange
        var deviceId = Guid.NewGuid();

        // Act
        var result = await _service.IsDevicePairedAsync(deviceId);

        // Assert
        Assert.False(result);
    }

    [Fact]
    public async Task IsDevicePairedAsync_EmptyGuid_ReturnsFalse()
    {
        // Act
        var result = await _service.IsDevicePairedAsync(Guid.Empty);

        // Assert
        Assert.False(result);
    }

    [Fact]
    public async Task IsDevicePairedAsync_AfterRegister_ReturnsTrue()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        Assert.False(await _service.IsDevicePairedAsync(deviceId));

        // Act
        await _service.RegisterDeviceAsync(deviceId);

        // Assert
        Assert.True(await _service.IsDevicePairedAsync(deviceId));
    }

    #endregion
}
