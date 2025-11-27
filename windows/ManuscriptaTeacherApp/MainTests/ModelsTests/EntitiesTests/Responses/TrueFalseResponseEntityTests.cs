using System;
using Xunit;
using Main.Models.Entities.Responses;

namespace MainTests.ModelsTests.EntitiesTests.Responses;

public class TrueFalseResponseEntityTests
{
    [Fact]
    public void Constructor_WithValidData_CreatesEntity()
    {
        // Arrange
        var id = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var answer = true;
        var timestamp = DateTime.UtcNow;
        var isCorrect = true;

        // Act
        var entity = new TrueFalseResponseEntity(id, questionId, answer, timestamp, isCorrect);

        // Assert
        Assert.Equal(id, entity.Id);
        Assert.Equal(questionId, entity.QuestionId);
        Assert.Equal(answer, entity.Answer);
        Assert.Equal(timestamp, entity.Timestamp);
        Assert.Equal(isCorrect, entity.IsCorrect);
    }

    [Fact]
    public void Constructor_WithFalseAnswer_CreatesEntity()
    {
        // Arrange
        var id = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var answer = false;

        // Act
        var entity = new TrueFalseResponseEntity(id, questionId, answer);

        // Assert
        Assert.False(entity.Answer);
    }

    [Fact]
    public void Constructor_WithDefaultTimestamp_SetsCurrentTime()
    {
        // Arrange
        var id = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var beforeCreation = DateTime.UtcNow;

        // Act
        var entity = new TrueFalseResponseEntity(id, questionId, true);
        var afterCreation = DateTime.UtcNow;

        // Assert
        Assert.True(entity.Timestamp >= beforeCreation && entity.Timestamp <= afterCreation);
    }
}
