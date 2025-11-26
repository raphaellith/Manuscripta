using System;
using Xunit;
using Main.Models.Entities.Questions;
using Main.Models.Enums;

namespace MainTests.ModelsTests.EntitiesTests.Questions;

public class TrueFalseQuestionEntityTests
{
    [Fact]
    public void Constructor_WithValidData_CreatesEntity()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var questionText = "The sky is blue.";
        var correctAnswer = true;

        // Act
        var entity = new TrueFalseQuestionEntity(id, materialId, questionText, correctAnswer);

        // Assert
        Assert.Equal(id, entity.Id);
        Assert.Equal(materialId, entity.MaterialId);
        Assert.Equal(questionText, entity.QuestionText);
        Assert.Equal(QuestionType.TRUE_FALSE, entity.QuestionType);
        Assert.Equal(correctAnswer, entity.CorrectAnswer);
    }

    [Fact]
    public void Constructor_WithFalseAnswer_CreatesEntity()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var questionText = "Water is dry.";
        var correctAnswer = false;

        // Act
        var entity = new TrueFalseQuestionEntity(id, materialId, questionText, correctAnswer);

        // Assert
        Assert.Equal(correctAnswer, entity.CorrectAnswer);
        Assert.False(entity.CorrectAnswer);
    }
}
