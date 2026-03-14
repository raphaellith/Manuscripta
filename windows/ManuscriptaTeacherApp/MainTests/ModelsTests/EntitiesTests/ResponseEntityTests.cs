using System;
using Xunit;
using Main.Models.Entities;

namespace MainTests;

/// <summary>
/// Tests for ResponseDataEntity (persistence layer entity)
/// </summary>
public class ResponseDataEntityTests
{
    [Fact]
    public void Constructor_SetsProperties()
    {
        // Arrange
        var id = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var answer = "Yes";
        var timestamp = DateTime.UtcNow;

        // Act
        var r = new ResponseDataEntity(id, questionId, deviceId, answer, true, timestamp);

        // Assert
        Assert.Equal(id, r.Id);
        Assert.Equal(questionId, r.QuestionId);
        Assert.Equal(answer, r.Answer);
        Assert.Equal(deviceId, r.DeviceId);
        Assert.True(r.IsCorrect);
        Assert.Equal(timestamp, r.Timestamp);
    }

    [Fact]
    public void Constructor_WithDefaultTimestamp_SetsCurrentTime()
    {
        // Arrange
        var id = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var beforeCreation = DateTime.UtcNow;

        // Act
        var r = new ResponseDataEntity(id, questionId, deviceId, "answer");
        var afterCreation = DateTime.UtcNow;

        // Assert
        Assert.True(r.Timestamp >= beforeCreation && r.Timestamp <= afterCreation);
    }

    [Fact]
    public void Constructor_WithDefaultValues_SetsDefaults()
    {
        // Arrange
        var id = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();

        // Act
        var r = new ResponseDataEntity(id, questionId, deviceId, "answer");

        // Assert
        Assert.Equal(id, r.Id);
        Assert.Equal(questionId, r.QuestionId);
        Assert.Equal("answer", r.Answer);
        Assert.Equal(deviceId, r.DeviceId);
        Assert.False(r.IsCorrect); // Default is false
    }
}
