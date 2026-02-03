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
public class DeviceRegistryServiceTests
{
    private readonly Mock<ILogger<DeviceRegistryService>> _mockLogger;
    private readonly DeviceRegistryService _service;

    public DeviceRegistryServiceTests()
    {
        _mockLogger = new Mock<ILogger<DeviceRegistryService>>();
        _service = new DeviceRegistryService(_mockLogger.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullLogger_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() =>
            new DeviceRegistryService(null!));
    }

    #endregion

    #region RegisterDeviceAsync Tests

    [Fact]
    public async Task RegisterDeviceAsync_NewDevice_ReturnsEntity()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        var deviceName = "Test Device";

        // Act
        var result = await _service.RegisterDeviceAsync(deviceId, deviceName);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(deviceId, result.DeviceId);
        Assert.Equal(deviceName, result.Name);
    }

    [Fact]
    public async Task RegisterDeviceAsync_NewDevice_AddsToRegistry()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        var deviceName = "Test Device";

        // Act
        await _service.RegisterDeviceAsync(deviceId, deviceName);

        // Assert
        Assert.True(await _service.IsDevicePairedAsync(deviceId));
        var devices = await _service.GetAllAsync();
        var device = devices.FirstOrDefault(d => d.DeviceId == deviceId);
        Assert.NotNull(device);
        Assert.Equal(deviceName, device.Name);
    }

    [Fact]
    public async Task RegisterDeviceAsync_ExistingDevice_ReturnsNull()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        await _service.RegisterDeviceAsync(deviceId, "Device 1");

        // Act
        var result = await _service.RegisterDeviceAsync(deviceId, "Device 1 Again");

        // Assert
        Assert.Null(result);
    }

    [Fact]
    public async Task RegisterDeviceAsync_ExistingDevice_DoesNotDuplicate()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        await _service.RegisterDeviceAsync(deviceId, "Device");

        // Act
        await _service.RegisterDeviceAsync(deviceId, "Device Again");

        // Assert
        var devices = await _service.GetAllAsync();
        var count = devices.Count(d => d.DeviceId == deviceId);
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
        await _service.RegisterDeviceAsync(deviceId1, "Device 1");
        await _service.RegisterDeviceAsync(deviceId2, "Device 2");
        await _service.RegisterDeviceAsync(deviceId3, "Device 3");

        // Assert
        var devices = await _service.GetAllAsync();
        Assert.Equal(3, devices.Count());
    }

    #endregion

    #region IsDevicePairedAsync Tests

    [Fact]
    public async Task IsDevicePairedAsync_PairedDevice_ReturnsTrue()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        await _service.RegisterDeviceAsync(deviceId, "Test Device");

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
        await _service.RegisterDeviceAsync(deviceId, "Test Device");

        // Assert
        Assert.True(await _service.IsDevicePairedAsync(deviceId));
    }

    #endregion

    #region UnregisterDeviceAsync Tests

    [Fact]
    public async Task UnregisterDeviceAsync_ExistingDevice_ReturnsTrue()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        await _service.RegisterDeviceAsync(deviceId, "Test Device");

        // Act
        var result = await _service.UnregisterDeviceAsync(deviceId);

        // Assert
        Assert.True(result);
    }

    [Fact]
    public async Task UnregisterDeviceAsync_ExistingDevice_RemovesFromRegistry()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        await _service.RegisterDeviceAsync(deviceId, "Test Device");
        Assert.True(await _service.IsDevicePairedAsync(deviceId));

        // Act
        await _service.UnregisterDeviceAsync(deviceId);

        // Assert
        Assert.False(await _service.IsDevicePairedAsync(deviceId));
        var devices = await _service.GetAllAsync();
        Assert.DoesNotContain(devices, d => d.DeviceId == deviceId);
    }

    [Fact]
    public async Task UnregisterDeviceAsync_NonExistentDevice_ReturnsFalse()
    {
        // Arrange
        var deviceId = Guid.NewGuid();

        // Act
        var result = await _service.UnregisterDeviceAsync(deviceId);

        // Assert
        Assert.False(result);
    }

    [Fact]
    public async Task UnregisterDeviceAsync_AfterUnregister_CanRegisterAgain()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        await _service.RegisterDeviceAsync(deviceId, "Test Device");
        await _service.UnregisterDeviceAsync(deviceId);

        // Act
        var result = await _service.RegisterDeviceAsync(deviceId, "Test Device Re-paired");

        // Assert
        Assert.NotNull(result);
        Assert.True(await _service.IsDevicePairedAsync(deviceId));
    }

    [Fact]
    public async Task UnregisterDeviceAsync_DoesNotAffectOtherDevices()
    {
        // Arrange
        var deviceId1 = Guid.NewGuid();
        var deviceId2 = Guid.NewGuid();
        await _service.RegisterDeviceAsync(deviceId1, "Device 1");
        await _service.RegisterDeviceAsync(deviceId2, "Device 2");

        // Act
        await _service.UnregisterDeviceAsync(deviceId1);

        // Assert
        Assert.False(await _service.IsDevicePairedAsync(deviceId1));
        Assert.True(await _service.IsDevicePairedAsync(deviceId2));
    }

    #endregion
}
