using System;
using Xunit;
using Main.Models.Entities;
using Main.Models.Entities.Questions;
using Main.Models.Enums;
using Main.Models.Mappings;

namespace MainTests.ModelsTests.Mappings;

public class QuestionEntityMapperTests
{
    [Fact]
    public void ToDataEntity_WithMultipleChoiceQuestion_MapsCorrectly()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var options = new List<string> { "A", "B", "C" };
        var question = new MultipleChoiceQuestionEntity(id, materialId, "Question?", options, 1);

        // Act
        var dataEntity = QuestionEntityMapper.ToDataEntity(question);

        // Assert
        Assert.Equal(id, dataEntity.Id);
        Assert.Equal(materialId, dataEntity.MaterialId);
        Assert.Equal("Question?", dataEntity.QuestionText);
        Assert.Equal(QuestionType.MULTIPLE_CHOICE, dataEntity.QuestionType);
        Assert.Equal(options, dataEntity.Options);
        Assert.Equal("1", dataEntity.CorrectAnswer);
    }

    [Fact]
    public void ToDataEntity_WithWrittenAnswerQuestion_MapsCorrectly()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var question = new WrittenAnswerQuestionEntity(id, materialId, "What is the capital?", "Paris");

        // Act
        var dataEntity = QuestionEntityMapper.ToDataEntity(question);

        // Assert
        Assert.Equal(id, dataEntity.Id);
        Assert.Equal(materialId, dataEntity.MaterialId);
        Assert.Equal("What is the capital?", dataEntity.QuestionText);
        Assert.Equal(QuestionType.WRITTEN_ANSWER, dataEntity.QuestionType);
        Assert.Null(dataEntity.Options);
        Assert.Equal("Paris", dataEntity.CorrectAnswer);
    }

    [Fact]
    public void ToDataEntity_WithNull_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => QuestionEntityMapper.ToDataEntity(null!));
    }

    [Fact]
    public void ToEntity_WithMultipleChoiceDataEntity_MapsCorrectly()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var dataEntity = new QuestionDataEntity
        {
            Id = id,
            MaterialId = materialId,
            QuestionText = "Question?",
            QuestionType = QuestionType.MULTIPLE_CHOICE,
            Options = new List<string> { "A", "B", "C" },
            CorrectAnswer = "2"
        };

        // Act
        var entity = QuestionEntityMapper.ToEntity(dataEntity);

        // Assert
        Assert.IsType<MultipleChoiceQuestionEntity>(entity);
        var mcEntity = (MultipleChoiceQuestionEntity)entity;
        Assert.Equal(id, mcEntity.Id);
        Assert.Equal(materialId, mcEntity.MaterialId);
        Assert.Equal("Question?", mcEntity.QuestionText);
        Assert.Equal(QuestionType.MULTIPLE_CHOICE, mcEntity.QuestionType);
        Assert.Equal(3, mcEntity.Options.Count);
        Assert.Equal(2, mcEntity.CorrectAnswerIndex);
    }

    [Fact]
    public void ToEntity_WithWrittenAnswerDataEntity_MapsCorrectly()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var dataEntity = new QuestionDataEntity
        {
            Id = id,
            MaterialId = materialId,
            QuestionText = "What is the answer?",
            QuestionType = QuestionType.WRITTEN_ANSWER,
            CorrectAnswer = "42"
        };

        // Act
        var entity = QuestionEntityMapper.ToEntity(dataEntity);

        // Assert
        Assert.IsType<WrittenAnswerQuestionEntity>(entity);
        var waEntity = (WrittenAnswerQuestionEntity)entity;
        Assert.Equal(id, waEntity.Id);
        Assert.Equal(materialId, waEntity.MaterialId);
        Assert.Equal("What is the answer?", waEntity.QuestionText);
        Assert.Equal(QuestionType.WRITTEN_ANSWER, waEntity.QuestionType);
        Assert.Equal("42", waEntity.CorrectAnswer);
    }

    [Fact]
    public void ToEntity_WithNull_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => QuestionEntityMapper.ToEntity(null!));
    }

    [Fact]
    public void ToEntity_WithInvalidCorrectAnswerForMultipleChoice_ReturnsNullCorrectAnswerIndex()
    {
        // Arrange - an invalid string for CorrectAnswer should be treated as null (no correct answer)
        var dataEntity = new QuestionDataEntity
        {
            Id = Guid.NewGuid(),
            MaterialId = Guid.NewGuid(),
            QuestionText = "Question?",
            QuestionType = QuestionType.MULTIPLE_CHOICE,
            Options = new List<string> { "A", "B" },
            CorrectAnswer = "not a number"
        };

        // Act
        var entity = QuestionEntityMapper.ToEntity(dataEntity);

        // Assert - invalid values become null (auto-marking disabled)
        var mcEntity = Assert.IsType<MultipleChoiceQuestionEntity>(entity);
        Assert.Null(mcEntity.CorrectAnswerIndex);
    }

    [Fact]
    public void RoundTrip_PreservesData()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var original = new MultipleChoiceQuestionEntity(
            id, materialId, "Original Question?", 
            new List<string> { "Option1", "Option2", "Option3" }, 
            1
        );

        // Act
        var dataEntity = QuestionEntityMapper.ToDataEntity(original);
        var roundTripped = QuestionEntityMapper.ToEntity(dataEntity) as MultipleChoiceQuestionEntity;

        // Assert
        Assert.NotNull(roundTripped);
        Assert.Equal(original.Id, roundTripped!.Id);
        Assert.Equal(original.MaterialId, roundTripped.MaterialId);
        Assert.Equal(original.QuestionText, roundTripped.QuestionText);
        Assert.Equal(original.QuestionType, roundTripped.QuestionType);
        Assert.Equal(original.Options, roundTripped.Options);
        Assert.Equal(original.CorrectAnswerIndex, roundTripped.CorrectAnswerIndex);
    }
}
