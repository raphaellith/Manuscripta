using System;
using System.Collections.Generic;
using Xunit;
using Main.Models.Entities;
using Main.Models.Enums;

namespace MainTests;

/// <summary>
/// Tests for QuestionDataEntity (persistence layer entity)
/// </summary>
public class QuestionDataEntityTests
{
    [Fact]
    public void DefaultConstructor_InitializesProperties()
    {
        // Act
        var q = new QuestionDataEntity();

        // Assert
        Assert.Equal(Guid.Empty, q.Id);
        Assert.Equal(Guid.Empty, q.MaterialId);
        Assert.Null(q.QuestionText);
    }

    [Fact]
    public void CanSetProperties()
    {
        // Arrange
        var id = Guid.NewGuid();
        var materialId = Guid.NewGuid();

        // Act
        var q = new QuestionDataEntity
        {
            Id = id,
            MaterialId = materialId,
            QuestionText = "What is 2 + 2?",
            QuestionType = QuestionType.MULTIPLE_CHOICE,
            Options = new List<string> { "3", "4", "5" },
            CorrectAnswer = "1"
        };

        // Assert
        Assert.Equal(id, q.Id);
        Assert.Equal(materialId, q.MaterialId);
        Assert.Equal("What is 2 + 2?", q.QuestionText);
        Assert.Equal(QuestionType.MULTIPLE_CHOICE, q.QuestionType);
        Assert.Equal(3, q.Options!.Count);
        Assert.Equal("1", q.CorrectAnswer);
    }

    [Fact]
    public void Options_CanBeNull()
    {
        // Act
        var q = new QuestionDataEntity
        {
            Id = Guid.NewGuid(),
            MaterialId = Guid.NewGuid(),
            QuestionText = "True or False?",
            QuestionType = QuestionType.TRUE_FALSE,
            Options = null,
            CorrectAnswer = "True"
        };

        // Assert
        Assert.Null(q.Options);
    }
}
