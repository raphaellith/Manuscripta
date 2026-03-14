using System;
using Xunit;
using Main.Models.Entities.Responses;

namespace MainTests.ModelsTests.EntitiesTests.Responses;

public class MultipleChoiceResponseEntityTests
{
    [Fact]
    public void Constructor_WithValidData_CreatesEntity()
    {
        // Arrange
        var id = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var answerIndex = 2;
        var timestamp = DateTime.UtcNow;
        var isCorrect = true;

        // Act
        var entity = new MultipleChoiceResponseEntity(id, questionId, deviceId, answerIndex, timestamp, isCorrect);

        // Assert
        Assert.Equal(id, entity.Id);
        Assert.Equal(questionId, entity.QuestionId);
        Assert.Equal(deviceId, entity.DeviceId);
        Assert.Equal(answerIndex, entity.AnswerIndex);
        Assert.Equal(timestamp, entity.Timestamp);
        Assert.Equal(isCorrect, entity.IsCorrect);
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
        var entity = new MultipleChoiceResponseEntity(id, questionId, deviceId, 1);
        var afterCreation = DateTime.UtcNow;

        // Assert
        Assert.True(entity.Timestamp >= beforeCreation && entity.Timestamp <= afterCreation);
    }

    [Fact]
    public void Constructor_WithNullIsCorrect_SetsNull()
    {
        // Arrange
        var id = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();

        // Act
        var entity = new MultipleChoiceResponseEntity(id, questionId, deviceId, 0, null, null);

        // Assert
        Assert.Null(entity.IsCorrect);
    }
}
