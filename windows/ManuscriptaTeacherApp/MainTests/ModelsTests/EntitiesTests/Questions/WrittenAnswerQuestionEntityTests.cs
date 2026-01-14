using System;
using Xunit;
using Main.Models.Entities.Questions;
using Main.Models.Enums;

namespace MainTests.ModelsTests.EntitiesTests.Questions;

public class WrittenAnswerQuestionEntityTests
{
    [Fact]
    public void Constructor_WithValidData_CreatesEntity()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var questionText = "What is the capital of France?";
        var correctAnswer = "Paris";

        // Act
        var entity = new WrittenAnswerQuestionEntity(id, materialId, questionText, correctAnswer);

        // Assert
        Assert.Equal(id, entity.Id);
        Assert.Equal(materialId, entity.MaterialId);
        Assert.Equal(questionText, entity.QuestionText);
        Assert.Equal(QuestionType.WRITTEN_ANSWER, entity.QuestionType);
        Assert.Equal(correctAnswer, entity.CorrectAnswer);
    }

    [Fact]
    public void Constructor_WithNullCorrectAnswer_CreatesEntity()
    {
        // Per Validation Rules refactor: null CorrectAnswer = auto-marking disabled
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();

        // Act
        var entity = new WrittenAnswerQuestionEntity(id, materialId, "Question?", null);

        // Assert
        Assert.Null(entity.CorrectAnswer);
    }

    [Fact]
    public void Constructor_WithEmptyCorrectAnswer_CreatesEntity()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var correctAnswer = "";

        // Act
        var entity = new WrittenAnswerQuestionEntity(id, materialId, "Question?", correctAnswer);

        // Assert
        Assert.Equal(string.Empty, entity.CorrectAnswer);
    }
}
