using System;
using Xunit;
using Main.Models.Entities.Sessions;
using Main.Models.Enums;

namespace MainTests.ModelsTests.EntitiesTests;

/// <summary>
/// Tests for SessionEntity (polymorphic business entity).
/// </summary>
public class SessionEntityTests
{
    [Fact]
    public void Constructor_WithAllParameters_SetsPropertiesCorrectly()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var startTime = DateTime.UtcNow;
        var endTime = DateTime.UtcNow.AddHours(1);

        // Act
        var session = new SessionEntity(id, materialId, startTime, SessionStatus.COMPLETED, deviceId, endTime);

        // Assert
        Assert.Equal(id, session.Id);
        Assert.Equal(materialId, session.MaterialId);
        Assert.Equal(startTime, session.StartTime);
        Assert.Equal(SessionStatus.COMPLETED, session.SessionStatus);
        Assert.Equal(deviceId, session.DeviceId);
        Assert.Equal(endTime, session.EndTime);
    }

    [Fact]
    public void Constructor_ActiveSessionWithoutEndTime_SetsNullEndTime()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var startTime = DateTime.UtcNow;

        // Act
        var session = new SessionEntity(id, materialId, startTime, SessionStatus.ACTIVE, deviceId);

        // Assert
        Assert.Equal(SessionStatus.ACTIVE, session.SessionStatus);
        Assert.Null(session.EndTime);
    }

    [Fact]
    public void Constructor_ActiveSessionWithEndTime_SetsEndTime()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var startTime = DateTime.UtcNow;
        var endTime = DateTime.UtcNow.AddHours(1);

        // Act
        var session = new SessionEntity(id, materialId, startTime, SessionStatus.ACTIVE, deviceId, endTime);

        // Assert
        Assert.Equal(endTime, session.EndTime);
    }

    [Theory]
    [InlineData(SessionStatus.ACTIVE)]
    [InlineData(SessionStatus.PAUSED)]
    [InlineData(SessionStatus.COMPLETED)]
    [InlineData(SessionStatus.CANCELLED)]
    public void Constructor_AllStatusValues_AcceptedWithEndTime(SessionStatus status)
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var startTime = DateTime.UtcNow;
        var endTime = DateTime.UtcNow.AddHours(1);

        // Act
        var session = new SessionEntity(id, materialId, startTime, status, deviceId, endTime);

        // Assert
        Assert.Equal(status, session.SessionStatus);
        Assert.Equal(endTime, session.EndTime);
    }

    [Fact]
    public void DefaultConstructor_CreatesInstance()
    {
        // Act
        var session = new SessionEntity();

        // Assert
        Assert.NotNull(session);
        Assert.Equal(Guid.Empty, session.Id);
        Assert.Equal(Guid.Empty, session.MaterialId);
        Assert.Equal(Guid.Empty, session.DeviceId);
        Assert.Equal(default(DateTime), session.StartTime);
        Assert.Equal(default(SessionStatus), session.SessionStatus);
        Assert.Null(session.EndTime);
    }

    [Fact]
    public void Properties_CanBeModified()
    {
        // Arrange
        var session = new SessionEntity();
        var newId = Guid.NewGuid();
        var newMaterialId = Guid.NewGuid();
        var newDeviceId = Guid.NewGuid();
        var newStartTime = DateTime.UtcNow;
        var newEndTime = DateTime.UtcNow.AddHours(1);

        // Act
        session.Id = newId;
        session.MaterialId = newMaterialId;
        session.DeviceId = newDeviceId;
        session.StartTime = newStartTime;
        session.SessionStatus = SessionStatus.PAUSED;
        session.EndTime = newEndTime;

        // Assert
        Assert.Equal(newId, session.Id);
        Assert.Equal(newMaterialId, session.MaterialId);
        Assert.Equal(newDeviceId, session.DeviceId);
        Assert.Equal(newStartTime, session.StartTime);
        Assert.Equal(SessionStatus.PAUSED, session.SessionStatus);
        Assert.Equal(newEndTime, session.EndTime);
    }

    [Fact]
    public void EndTime_CanBeSetToNull()
    {
        // Arrange
        var session = new SessionEntity(
            Guid.NewGuid(),
            Guid.NewGuid(),
            DateTime.UtcNow,
            SessionStatus.ACTIVE,
            Guid.NewGuid(),
            DateTime.UtcNow.AddHours(1)
        );

        // Act
        session.EndTime = null;

        // Assert
        Assert.Null(session.EndTime);
    }

    [Fact]
    public void SessionStatus_CanTransitionFromActiveToCompleted()
    {
        // Arrange
        var session = new SessionEntity(
            Guid.NewGuid(),
            Guid.NewGuid(),
            DateTime.UtcNow,
            SessionStatus.ACTIVE,
            Guid.NewGuid()
        );

        // Act
        session.SessionStatus = SessionStatus.COMPLETED;
        session.EndTime = DateTime.UtcNow;

        // Assert
        Assert.Equal(SessionStatus.COMPLETED, session.SessionStatus);
        Assert.NotNull(session.EndTime);
    }

    [Fact]
    public void SessionStatus_CanTransitionFromActiveToPaused()
    {
        // Arrange
        var session = new SessionEntity(
            Guid.NewGuid(),
            Guid.NewGuid(),
            DateTime.UtcNow,
            SessionStatus.ACTIVE,
            Guid.NewGuid()
        );

        // Act
        session.SessionStatus = SessionStatus.PAUSED;
        session.EndTime = DateTime.UtcNow;

        // Assert
        Assert.Equal(SessionStatus.PAUSED, session.SessionStatus);
        Assert.NotNull(session.EndTime);
    }

    [Fact]
    public void SessionStatus_CanTransitionFromActiveToCancelled()
    {
        // Arrange
        var session = new SessionEntity(
            Guid.NewGuid(),
            Guid.NewGuid(),
            DateTime.UtcNow,
            SessionStatus.ACTIVE,
            Guid.NewGuid()
        );

        // Act
        session.SessionStatus = SessionStatus.CANCELLED;
        session.EndTime = DateTime.UtcNow;

        // Assert
        Assert.Equal(SessionStatus.CANCELLED, session.SessionStatus);
        Assert.NotNull(session.EndTime);
    }

    [Fact]
    public void SessionStatus_CanTransitionFromPausedToActive()
    {
        // Arrange
        var session = new SessionEntity(
            Guid.NewGuid(),
            Guid.NewGuid(),
            DateTime.UtcNow.AddHours(-1),
            SessionStatus.PAUSED,
            Guid.NewGuid(),
            DateTime.UtcNow
        );

        // Act
        session.SessionStatus = SessionStatus.ACTIVE;
        session.EndTime = null;

        // Assert
        Assert.Equal(SessionStatus.ACTIVE, session.SessionStatus);
        Assert.Null(session.EndTime);
    }
}
