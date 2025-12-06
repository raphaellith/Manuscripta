using Moq;
using Main.Services;
using Xunit;

namespace MainTests.ServicesTests;

/// <summary>
/// Tests for DeviceIdValidator.
/// Verifies device validation per Pairing Process.md §1(3) and Validation Rules §2C(3)(e), §2D(3)(b).
/// </summary>
public class DeviceIdValidatorTests
{
    private readonly Mock<IDeviceRegistryService> _mockDeviceRegistry;
    private readonly DeviceIdValidator _validator;

    public DeviceIdValidatorTests()
    {
        _mockDeviceRegistry = new Mock<IDeviceRegistryService>();
        _validator = new DeviceIdValidator(_mockDeviceRegistry.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullDeviceRegistry_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => new DeviceIdValidator(null!));
    }

    #endregion

    #region IsValidDeviceIdAsync Tests

    [Fact]
    public async Task IsValidDeviceIdAsync_PairedDevice_ReturnsTrue()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        _mockDeviceRegistry.Setup(r => r.IsDevicePairedAsync(deviceId))
            .ReturnsAsync(true);

        // Act
        var result = await _validator.IsValidDeviceIdAsync(deviceId);

        // Assert
        Assert.True(result);
    }

    [Fact]
    public async Task IsValidDeviceIdAsync_UnpairedDevice_ReturnsFalse()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        _mockDeviceRegistry.Setup(r => r.IsDevicePairedAsync(deviceId))
            .ReturnsAsync(false);

        // Act
        var result = await _validator.IsValidDeviceIdAsync(deviceId);

        // Assert
        Assert.False(result);
    }

    [Fact]
    public async Task IsValidDeviceIdAsync_CallsDeviceRegistry()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        _mockDeviceRegistry.Setup(r => r.IsDevicePairedAsync(deviceId))
            .ReturnsAsync(true);

        // Act
        await _validator.IsValidDeviceIdAsync(deviceId);

        // Assert
        _mockDeviceRegistry.Verify(r => r.IsDevicePairedAsync(deviceId), Times.Once);
    }

    #endregion

    #region ValidateOrThrowAsync Tests

    [Fact]
    public async Task ValidateOrThrowAsync_PairedDevice_DoesNotThrow()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        _mockDeviceRegistry.Setup(r => r.IsDevicePairedAsync(deviceId))
            .ReturnsAsync(true);

        // Act & Assert - Should not throw
        await _validator.ValidateOrThrowAsync(deviceId);
    }

    [Fact]
    public async Task ValidateOrThrowAsync_UnpairedDevice_ThrowsArgumentException()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        _mockDeviceRegistry.Setup(r => r.IsDevicePairedAsync(deviceId))
            .ReturnsAsync(false);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<ArgumentException>(
            () => _validator.ValidateOrThrowAsync(deviceId));

        Assert.Contains(deviceId.ToString(), exception.Message);
        Assert.Contains("paired", exception.Message.ToLower());
    }

    #endregion

    #region IsValidDeviceId (Synchronous) Tests

    [Fact]
    public void IsValidDeviceId_PairedDevice_ReturnsTrue()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        _mockDeviceRegistry.Setup(r => r.IsDevicePairedAsync(deviceId))
            .ReturnsAsync(true);

        // Act
        var result = _validator.IsValidDeviceId(deviceId);

        // Assert
        Assert.True(result);
    }

    [Fact]
    public void IsValidDeviceId_UnpairedDevice_ReturnsFalse()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        _mockDeviceRegistry.Setup(r => r.IsDevicePairedAsync(deviceId))
            .ReturnsAsync(false);

        // Act
        var result = _validator.IsValidDeviceId(deviceId);

        // Assert
        Assert.False(result);
    }

    #endregion

    #region ValidateOrThrow (Synchronous) Tests

    [Fact]
    public void ValidateOrThrow_PairedDevice_DoesNotThrow()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        _mockDeviceRegistry.Setup(r => r.IsDevicePairedAsync(deviceId))
            .ReturnsAsync(true);

        // Act & Assert - Should not throw
        _validator.ValidateOrThrow(deviceId);
    }

    [Fact]
    public void ValidateOrThrow_UnpairedDevice_ThrowsArgumentException()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        _mockDeviceRegistry.Setup(r => r.IsDevicePairedAsync(deviceId))
            .ReturnsAsync(false);

        // Act & Assert
        var exception = Assert.Throws<ArgumentException>(
            () => _validator.ValidateOrThrow(deviceId));

        Assert.Contains(deviceId.ToString(), exception.Message);
        Assert.Contains("paired", exception.Message.ToLower());
    }

    #endregion

    #region Edge Case Tests

    [Fact]
    public async Task IsValidDeviceIdAsync_EmptyGuid_QueriesRegistry()
    {
        // Arrange
        var deviceId = Guid.Empty;
        _mockDeviceRegistry.Setup(r => r.IsDevicePairedAsync(deviceId))
            .ReturnsAsync(false);

        // Act
        var result = await _validator.IsValidDeviceIdAsync(deviceId);

        // Assert
        Assert.False(result);
        _mockDeviceRegistry.Verify(r => r.IsDevicePairedAsync(deviceId), Times.Once);
    }

    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public async Task IsValidDeviceIdAsync_ReturnsRegistryResult(bool isPaired)
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        _mockDeviceRegistry.Setup(r => r.IsDevicePairedAsync(deviceId))
            .ReturnsAsync(isPaired);

        // Act
        var result = await _validator.IsValidDeviceIdAsync(deviceId);

        // Assert
        Assert.Equal(isPaired, result);
    }

    #endregion
}
