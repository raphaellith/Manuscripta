using System;
using Xunit;
using Main.Models.Entities;
using Main.Models.Enums;

namespace MainTests;

/// <summary>
/// Tests for SessionDataEntity (persistence layer entity)
/// Validates rules from Validation Rules §2D.
/// </summary>
public class SessionDataEntityTests
{
    [Fact]
    public void Constructor_ActiveSession_SetsPropertiesWithoutEndTime()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var startTime = DateTime.UtcNow;

        // Act
        var session = new SessionDataEntity(id, materialId, startTime, SessionStatus.ACTIVE, deviceId);

        // Assert
        Assert.Equal(id, session.Id);
        Assert.Equal(materialId, session.MaterialId);
        Assert.Equal(startTime, session.StartTime);
        Assert.Equal(SessionStatus.ACTIVE, session.SessionStatus);
        Assert.Equal(deviceId, session.DeviceId);
        Assert.Null(session.EndTime);
    }

    [Fact]
    public void Constructor_ActiveSession_WithOptionalEndTime_SetsEndTime()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var startTime = DateTime.UtcNow;
        var endTime = startTime.AddHours(1);

        // Act
        var session = new SessionDataEntity(id, materialId, startTime, SessionStatus.ACTIVE, deviceId, endTime);

        // Assert
        Assert.Equal(endTime, session.EndTime);
    }

    [Theory]
    [InlineData(SessionStatus.PAUSED)]
    [InlineData(SessionStatus.COMPLETED)]
    [InlineData(SessionStatus.CANCELLED)]
    public void Constructor_NonActiveSession_WithEndTime_Succeeds(SessionStatus status)
    {
        // Arrange - §2D(3)(a): Non-active sessions must have EndTime
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var startTime = DateTime.UtcNow;
        var endTime = startTime.AddHours(1);

        // Act
        var session = new SessionDataEntity(id, materialId, startTime, status, deviceId, endTime);

        // Assert
        Assert.Equal(status, session.SessionStatus);
        Assert.Equal(endTime, session.EndTime);
    }

    [Theory]
    [InlineData(SessionStatus.PAUSED)]
    [InlineData(SessionStatus.COMPLETED)]
    [InlineData(SessionStatus.CANCELLED)]
    public void Constructor_NonActiveSession_WithoutEndTime_ThrowsArgumentException(SessionStatus status)
    {
        // Arrange - §2D(3)(a): Non-active sessions must have EndTime
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var startTime = DateTime.UtcNow;

        // Act & Assert
        var ex = Assert.Throws<ArgumentException>(() =>
            new SessionDataEntity(id, materialId, startTime, status, deviceId));

        Assert.Contains("EndTime", ex.Message);
        Assert.Contains("§2D(3)(a)", ex.Message);
    }

    [Fact]
    public void Constructor_SetsAllMandatoryFields()
    {
        // Arrange - Testing §2D(1)(a-d)
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var startTime = new DateTime(2025, 11, 26, 12, 0, 0, DateTimeKind.Utc);

        // Act
        var session = new SessionDataEntity(id, materialId, startTime, SessionStatus.ACTIVE, deviceId);

        // Assert - All mandatory fields from §2D(1)
        Assert.NotEqual(Guid.Empty, session.Id);           // ID per §1(2)
        Assert.NotEqual(Guid.Empty, session.MaterialId);   // §2D(1)(a)
        Assert.Equal(startTime, session.StartTime);        // §2D(1)(b)
        Assert.Equal(SessionStatus.ACTIVE, session.SessionStatus); // §2D(1)(c)
        Assert.NotEqual(Guid.Empty, session.DeviceId);     // §2D(1)(d)
    }
}
