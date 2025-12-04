using System;
using Xunit;
using Main.Models.Entities;
using Main.Models.Entities.Sessions;
using Main.Models.Enums;
using Main.Models.Mappings;

namespace MainTests.ModelsTests.Mappings;

public class SessionEntityMapperTests
{
    [Fact]
    public void ToDataEntity_WithActiveSession_MapsCorrectly()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var startTime = DateTime.UtcNow;
        var session = new SessionEntity(id, materialId, startTime, SessionStatus.ACTIVE, deviceId);

        // Act
        var dataEntity = SessionEntityMapper.ToDataEntity(session);

        // Assert
        Assert.Equal(id, dataEntity.Id);
        Assert.Equal(materialId, dataEntity.MaterialId);
        Assert.Equal(startTime, dataEntity.StartTime);
        Assert.Equal(SessionStatus.ACTIVE, dataEntity.SessionStatus);
        Assert.Equal(deviceId, dataEntity.DeviceId);
        Assert.Null(dataEntity.EndTime);
    }

    [Fact]
    public void ToDataEntity_WithCompletedSession_MapsCorrectly()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var startTime = DateTime.UtcNow.AddHours(-1);
        var endTime = DateTime.UtcNow;
        var session = new SessionEntity(id, materialId, startTime, SessionStatus.COMPLETED, deviceId, endTime);

        // Act
        var dataEntity = SessionEntityMapper.ToDataEntity(session);

        // Assert
        Assert.Equal(id, dataEntity.Id);
        Assert.Equal(materialId, dataEntity.MaterialId);
        Assert.Equal(startTime, dataEntity.StartTime);
        Assert.Equal(SessionStatus.COMPLETED, dataEntity.SessionStatus);
        Assert.Equal(deviceId, dataEntity.DeviceId);
        Assert.Equal(endTime, dataEntity.EndTime);
    }

    [Fact]
    public void ToDataEntity_WithPausedSession_MapsCorrectly()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var startTime = DateTime.UtcNow.AddHours(-1);
        var endTime = DateTime.UtcNow;
        var session = new SessionEntity(id, materialId, startTime, SessionStatus.PAUSED, deviceId, endTime);

        // Act
        var dataEntity = SessionEntityMapper.ToDataEntity(session);

        // Assert
        Assert.Equal(SessionStatus.PAUSED, dataEntity.SessionStatus);
        Assert.Equal(endTime, dataEntity.EndTime);
    }

    [Fact]
    public void ToDataEntity_WithCancelledSession_MapsCorrectly()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var startTime = DateTime.UtcNow.AddHours(-1);
        var endTime = DateTime.UtcNow;
        var session = new SessionEntity(id, materialId, startTime, SessionStatus.CANCELLED, deviceId, endTime);

        // Act
        var dataEntity = SessionEntityMapper.ToDataEntity(session);

        // Assert
        Assert.Equal(SessionStatus.CANCELLED, dataEntity.SessionStatus);
        Assert.Equal(endTime, dataEntity.EndTime);
    }

    [Fact]
    public void ToDataEntity_WithNull_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => SessionEntityMapper.ToDataEntity(null!));
    }

    [Fact]
    public void ToEntity_WithActiveSession_MapsCorrectly()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var startTime = DateTime.UtcNow;

        var dataEntity = new SessionDataEntity(id, materialId, startTime, SessionStatus.ACTIVE, deviceId);

        // Act
        var entity = SessionEntityMapper.ToEntity(dataEntity);

        // Assert
        Assert.Equal(id, entity.Id);
        Assert.Equal(materialId, entity.MaterialId);
        Assert.Equal(startTime, entity.StartTime);
        Assert.Equal(SessionStatus.ACTIVE, entity.SessionStatus);
        Assert.Equal(deviceId, entity.DeviceId);
        Assert.Null(entity.EndTime);
    }

    [Fact]
    public void ToEntity_WithCompletedSession_MapsCorrectly()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var startTime = DateTime.UtcNow.AddHours(-1);
        var endTime = DateTime.UtcNow;

        var dataEntity = new SessionDataEntity(id, materialId, startTime, SessionStatus.COMPLETED, deviceId, endTime);

        // Act
        var entity = SessionEntityMapper.ToEntity(dataEntity);

        // Assert
        Assert.Equal(id, entity.Id);
        Assert.Equal(materialId, entity.MaterialId);
        Assert.Equal(startTime, entity.StartTime);
        Assert.Equal(SessionStatus.COMPLETED, entity.SessionStatus);
        Assert.Equal(deviceId, entity.DeviceId);
        Assert.Equal(endTime, entity.EndTime);
    }

    [Fact]
    public void ToEntity_WithPausedSession_MapsCorrectly()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var startTime = DateTime.UtcNow.AddHours(-1);
        var endTime = DateTime.UtcNow;

        var dataEntity = new SessionDataEntity(id, materialId, startTime, SessionStatus.PAUSED, deviceId, endTime);

        // Act
        var entity = SessionEntityMapper.ToEntity(dataEntity);

        // Assert
        Assert.Equal(SessionStatus.PAUSED, entity.SessionStatus);
        Assert.Equal(endTime, entity.EndTime);
    }

    [Fact]
    public void ToEntity_WithCancelledSession_MapsCorrectly()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var startTime = DateTime.UtcNow.AddHours(-1);
        var endTime = DateTime.UtcNow;

        var dataEntity = new SessionDataEntity(id, materialId, startTime, SessionStatus.CANCELLED, deviceId, endTime);

        // Act
        var entity = SessionEntityMapper.ToEntity(dataEntity);

        // Assert
        Assert.Equal(SessionStatus.CANCELLED, entity.SessionStatus);
        Assert.Equal(endTime, entity.EndTime);
    }

    [Fact]
    public void ToEntity_WithNull_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => SessionEntityMapper.ToEntity(null!));
    }

    [Fact]
    public void RoundTrip_ActiveSession_PreservesAllProperties()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var startTime = DateTime.UtcNow;
        var original = new SessionEntity(id, materialId, startTime, SessionStatus.ACTIVE, deviceId);

        // Act
        var dataEntity = SessionEntityMapper.ToDataEntity(original);
        var restored = SessionEntityMapper.ToEntity(dataEntity);

        // Assert
        Assert.Equal(original.Id, restored.Id);
        Assert.Equal(original.MaterialId, restored.MaterialId);
        Assert.Equal(original.StartTime, restored.StartTime);
        Assert.Equal(original.SessionStatus, restored.SessionStatus);
        Assert.Equal(original.DeviceId, restored.DeviceId);
        Assert.Equal(original.EndTime, restored.EndTime);
    }

    [Fact]
    public void RoundTrip_CompletedSession_PreservesAllProperties()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var startTime = DateTime.UtcNow.AddHours(-1);
        var endTime = DateTime.UtcNow;
        var original = new SessionEntity(id, materialId, startTime, SessionStatus.COMPLETED, deviceId, endTime);

        // Act
        var dataEntity = SessionEntityMapper.ToDataEntity(original);
        var restored = SessionEntityMapper.ToEntity(dataEntity);

        // Assert
        Assert.Equal(original.Id, restored.Id);
        Assert.Equal(original.MaterialId, restored.MaterialId);
        Assert.Equal(original.StartTime, restored.StartTime);
        Assert.Equal(original.SessionStatus, restored.SessionStatus);
        Assert.Equal(original.DeviceId, restored.DeviceId);
        Assert.Equal(original.EndTime, restored.EndTime);
    }
}
