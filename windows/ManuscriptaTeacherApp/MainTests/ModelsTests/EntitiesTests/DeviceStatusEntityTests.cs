using Main.Models.Entities;
using Main.Models.Enums;
using Xunit;

namespace MainTests.ModelsTests.EntitiesTests;

/// <summary>
/// Tests for DeviceStatusEntity.
/// Verifies fields and constraints per Validation Rules.md §2E.
/// </summary>
public class DeviceStatusEntityTests
{
    private readonly Guid _testDeviceId = Guid.NewGuid();
    private readonly Guid _testMaterialId = Guid.NewGuid();

    #region Constructor Tests

    [Fact]
    public void DefaultConstructor_CreatesInstance()
    {
        var entity = new DeviceStatusEntity();

        Assert.NotNull(entity);
        Assert.Equal(Guid.Empty, entity.DeviceId);
        Assert.Equal(DeviceStatus.ON_TASK, entity.Status);
        Assert.Equal(0, entity.BatteryLevel);
        Assert.Equal(Guid.Empty, entity.CurrentMaterialId);
        Assert.Equal(string.Empty, entity.StudentView);
        Assert.Equal(0, entity.Timestamp);
    }

    [Fact]
    public void ParameterizedConstructor_SetsAllProperties()
    {
        // Arrange
        var status = DeviceStatus.ON_TASK;
        var batteryLevel = 85;
        var studentView = "page-5";
        var timestamp = 1702147200L;

        // Act
        var entity = new DeviceStatusEntity(
            _testDeviceId,
            status,
            batteryLevel,
            _testMaterialId,
            studentView,
            timestamp);

        // Assert
        Assert.Equal(_testDeviceId, entity.DeviceId);
        Assert.Equal(status, entity.Status);
        Assert.Equal(batteryLevel, entity.BatteryLevel);
        Assert.Equal(_testMaterialId, entity.CurrentMaterialId);
        Assert.Equal(studentView, entity.StudentView);
        Assert.Equal(timestamp, entity.Timestamp);
    }

    [Fact]
    public void ParameterizedConstructor_NullStudentView_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new DeviceStatusEntity(
                _testDeviceId,
                DeviceStatus.ON_TASK,
                50,
                _testMaterialId,
                null!,
                1702147200L));
    }

    #endregion

    #region Property Tests

    [Fact]
    public void DeviceId_CanBeSetAndRetrieved()
    {
        // Per §2E(1)(a): DeviceId (uuid)
        var entity = new DeviceStatusEntity { DeviceId = _testDeviceId };
        Assert.Equal(_testDeviceId, entity.DeviceId);
    }

    [Theory]
    [InlineData(DeviceStatus.ON_TASK)]
    [InlineData(DeviceStatus.IDLE)]
    [InlineData(DeviceStatus.HAND_RAISED)]
    [InlineData(DeviceStatus.LOCKED)]
    [InlineData(DeviceStatus.DISCONNECTED)]
    public void Status_CanBeSetToAllValidValues(DeviceStatus status)
    {
        // Per §2E(1)(b): Status enum with 5 possible values
        var entity = new DeviceStatusEntity { Status = status };
        Assert.Equal(status, entity.Status);
    }

    [Theory]
    [InlineData(0)]
    [InlineData(50)]
    [InlineData(100)]
    public void BatteryLevel_ValidValues_CanBeSet(int level)
    {
        // Per §2E(1)(c) and §2E(2)(b): BatteryLevel must be between 0 and 100
        var entity = new DeviceStatusEntity { BatteryLevel = level };
        Assert.Equal(level, entity.BatteryLevel);
    }

    [Fact]
    public void CurrentMaterialId_CanBeSetAndRetrieved()
    {
        // Per §2E(1)(d): CurrentMaterialId (uuid)
        var entity = new DeviceStatusEntity { CurrentMaterialId = _testMaterialId };
        Assert.Equal(_testMaterialId, entity.CurrentMaterialId);
    }

    [Theory]
    [InlineData("page-5")]
    [InlineData("question-3")]
    [InlineData("summary")]
    public void StudentView_CanBeSetToValidStrings(string view)
    {
        // Per §2E(1)(e): StudentView describes the location
        var entity = new DeviceStatusEntity { StudentView = view };
        Assert.Equal(view, entity.StudentView);
    }

    [Fact]
    public void Timestamp_CanBeSetAndRetrieved()
    {
        // Per §2E(1)(f): Timestamp (long) - Unix timestamp
        var timestamp = 1702147200L;
        var entity = new DeviceStatusEntity { Timestamp = timestamp };
        Assert.Equal(timestamp, entity.Timestamp);
    }

    #endregion

    #region Validation Constraint Documentation Tests

    [Fact]
    public void BatteryLevel_HasRangeAttribute()
    {
        // Per §2E(2)(b): BatteryLevel must be between 0 and 100
        var property = typeof(DeviceStatusEntity).GetProperty(nameof(DeviceStatusEntity.BatteryLevel));
        var rangeAttribute = property?.GetCustomAttributes(typeof(System.ComponentModel.DataAnnotations.RangeAttribute), false)
            .FirstOrDefault() as System.ComponentModel.DataAnnotations.RangeAttribute;

        Assert.NotNull(rangeAttribute);
        Assert.Equal(0, rangeAttribute.Minimum);
        Assert.Equal(100, rangeAttribute.Maximum);
    }

    [Fact]
    public void DeviceId_HasRequiredAttribute()
    {
        // Per §2E(1)(a): DeviceId is a required field
        var property = typeof(DeviceStatusEntity).GetProperty(nameof(DeviceStatusEntity.DeviceId));
        var hasRequired = property?.GetCustomAttributes(typeof(System.ComponentModel.DataAnnotations.RequiredAttribute), false).Any();

        Assert.True(hasRequired);
    }

    [Fact]
    public void AllMandatoryFields_HaveRequiredAttribute()
    {
        // Per §2E(1): All fields (a)-(f) are mandatory
        var mandatoryFields = new[]
        {
            nameof(DeviceStatusEntity.DeviceId),
            nameof(DeviceStatusEntity.Status),
            nameof(DeviceStatusEntity.BatteryLevel),
            nameof(DeviceStatusEntity.CurrentMaterialId),
            nameof(DeviceStatusEntity.StudentView),
            nameof(DeviceStatusEntity.Timestamp)
        };

        foreach (var fieldName in mandatoryFields)
        {
            var property = typeof(DeviceStatusEntity).GetProperty(fieldName);
            var hasRequired = property?.GetCustomAttributes(typeof(System.ComponentModel.DataAnnotations.RequiredAttribute), false).Any();
            Assert.True(hasRequired, $"Field {fieldName} should have [Required] attribute per §2E(1)");
        }
    }

    #endregion
}
