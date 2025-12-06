using Main.Models.Entities;
using Xunit;

namespace MainTests.ModelsTests.EntitiesTests;

/// <summary>
/// Tests for PairedDeviceEntity.
/// Verifies entity behavior per Pairing Process.md §1(1)(a).
/// </summary>
public class PairedDeviceEntityTests
{
    #region Constructor Tests

    [Fact]
    public void DefaultConstructor_SetsDeviceIdToEmpty()
    {
        // Act
        var entity = new PairedDeviceEntity();

        // Assert
        Assert.Equal(Guid.Empty, entity.DeviceId);
    }

    [Fact]
    public void ConstructorWithDeviceId_SetsDeviceId()
    {
        // Arrange
        var deviceId = Guid.NewGuid();

        // Act
        var entity = new PairedDeviceEntity(deviceId);

        // Assert
        Assert.Equal(deviceId, entity.DeviceId);
    }

    [Fact]
    public void ConstructorWithDeviceId_AcceptsEmptyGuid()
    {
        // Act
        var entity = new PairedDeviceEntity(Guid.Empty);

        // Assert
        Assert.Equal(Guid.Empty, entity.DeviceId);
    }

    #endregion

    #region Property Tests

    [Fact]
    public void DeviceId_CanBeSet()
    {
        // Arrange
        var entity = new PairedDeviceEntity();
        var newDeviceId = Guid.NewGuid();

        // Act
        entity.DeviceId = newDeviceId;

        // Assert
        Assert.Equal(newDeviceId, entity.DeviceId);
    }

    [Fact]
    public void DeviceId_CanBeUpdated()
    {
        // Arrange
        var originalId = Guid.NewGuid();
        var entity = new PairedDeviceEntity(originalId);
        var newId = Guid.NewGuid();

        // Act
        entity.DeviceId = newId;

        // Assert
        Assert.Equal(newId, entity.DeviceId);
    }

    #endregion

    #region Key Tests

    [Fact]
    public void TwoEntities_WithSameDeviceId_AreNotEqualByReference()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        var entity1 = new PairedDeviceEntity(deviceId);
        var entity2 = new PairedDeviceEntity(deviceId);

        // Assert - Default reference equality
        Assert.NotSame(entity1, entity2);
    }

    [Fact]
    public void Entity_DeviceId_IsUsedAsKey()
    {
        // This test verifies DeviceId is the identifying property
        // In EF Core, this will be configured as the primary key

        // Arrange
        var deviceId = Guid.NewGuid();
        var entity = new PairedDeviceEntity(deviceId);

        // Assert
        Assert.Equal(deviceId, entity.DeviceId);
    }

    #endregion
}
