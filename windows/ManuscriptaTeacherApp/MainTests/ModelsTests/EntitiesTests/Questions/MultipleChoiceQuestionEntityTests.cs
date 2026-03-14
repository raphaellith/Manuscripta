using System;
using Xunit;
using Main.Models.Entities.Questions;
using Main.Models.Enums;

namespace MainTests.ModelsTests.EntitiesTests.Questions;

public class MultipleChoiceQuestionEntityTests
{
    [Fact]
    public void Constructor_WithValidData_CreatesEntity()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var questionText = "What is 2 + 2?";
        var options = new List<string> { "3", "4", "5", "6" };
        var correctAnswerIndex = 1;

        // Act
        var entity = new MultipleChoiceQuestionEntity(id, materialId, questionText, options, correctAnswerIndex);

        // Assert
        Assert.Equal(id, entity.Id);
        Assert.Equal(materialId, entity.MaterialId);
        Assert.Equal(questionText, entity.QuestionText);
        Assert.Equal(QuestionType.MULTIPLE_CHOICE, entity.QuestionType);
        Assert.Equal(options, entity.Options);
        Assert.Equal(correctAnswerIndex, entity.CorrectAnswerIndex);
    }

    [Fact]
    public void Constructor_WithEmptyOptions_ThrowsArgumentException()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var emptyOptions = new List<string>();

        // Act & Assert
        var exception = Assert.Throws<ArgumentException>(() =>
            new MultipleChoiceQuestionEntity(id, materialId, "Question?", emptyOptions, 0));
        
        Assert.Contains("cannot be empty", exception.Message);
    }

    [Fact]
    public void Constructor_WithNullOptions_ThrowsArgumentNullException()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();

        // Act & Assert
        Assert.Throws<ArgumentNullException>(() =>
            new MultipleChoiceQuestionEntity(id, materialId, "Question?", null!, 0));
    }

    [Fact]
    public void Constructor_WithInvalidCorrectAnswerIndex_ThrowsArgumentOutOfRangeException()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var options = new List<string> { "A", "B", "C" };

        // Act & Assert - Index too high
        Assert.Throws<ArgumentOutOfRangeException>(() =>
            new MultipleChoiceQuestionEntity(id, materialId, "Question?", options, 3));

        // Act & Assert - Negative index
        Assert.Throws<ArgumentOutOfRangeException>(() =>
            new MultipleChoiceQuestionEntity(id, materialId, "Question?", options, -1));
    }
}
