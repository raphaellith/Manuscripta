using System;
using Xunit;
using Main.Models.Entities;

namespace MainTests;

/// <summary>
/// Tests for FeedbackDataEntity (persistence layer entity)
/// Per Validation Rules §2F
/// </summary>
public class FeedbackDataEntityTests
{
    [Fact]
    public void Constructor_WithTextOnly_CreatesValidEntity()
    {
        // Arrange
        var id = Guid.NewGuid();
        var responseId = Guid.NewGuid();
        var text = "Good work!";

        // Act
        var feedback = new FeedbackDataEntity(id, responseId, text: text);

        // Assert
        Assert.Equal(id, feedback.Id);
        Assert.Equal(responseId, feedback.ResponseId);
        Assert.Equal(text, feedback.Text);
        Assert.Null(feedback.Marks);
    }

    [Fact]
    public void Constructor_WithMarksOnly_CreatesValidEntity()
    {
        // Arrange
        var id = Guid.NewGuid();
        var responseId = Guid.NewGuid();
        var marks = 8;

        // Act
        var feedback = new FeedbackDataEntity(id, responseId, marks: marks);

        // Assert
        Assert.Equal(id, feedback.Id);
        Assert.Equal(responseId, feedback.ResponseId);
        Assert.Null(feedback.Text);
        Assert.Equal(marks, feedback.Marks);
    }

    [Fact]
    public void Constructor_WithBothTextAndMarks_CreatesValidEntity()
    {
        // Arrange
        var id = Guid.NewGuid();
        var responseId = Guid.NewGuid();
        var text = "Good attempt!";
        var marks = 7;

        // Act
        var feedback = new FeedbackDataEntity(id, responseId, text, marks);

        // Assert
        Assert.Equal(id, feedback.Id);
        Assert.Equal(responseId, feedback.ResponseId);
        Assert.Equal(text, feedback.Text);
        Assert.Equal(marks, feedback.Marks);
    }

    [Fact]
    public void Constructor_WithNeitherTextNorMarks_ThrowsArgumentException()
    {
        // Arrange
        var id = Guid.NewGuid();
        var responseId = Guid.NewGuid();

        // Act & Assert - Per §2F(1)(b): At least one of Text or Marks must be present
        var exception = Assert.Throws<ArgumentException>(() =>
            new FeedbackDataEntity(id, responseId));

        Assert.Contains("At least one of Text or Marks must be provided", exception.Message);
    }

    [Fact]
    public void Constructor_WithEmptyText_ThrowsArgumentException()
    {
        // Arrange
        var id = Guid.NewGuid();
        var responseId = Guid.NewGuid();

        // Act & Assert - Empty string is treated same as null
        var exception = Assert.Throws<ArgumentException>(() =>
            new FeedbackDataEntity(id, responseId, text: ""));

        Assert.Contains("At least one of Text or Marks must be provided", exception.Message);
    }

    [Fact]
    public void Constructor_WithWhitespaceOnlyText_ThrowsArgumentException()
    {
        // Arrange
        var id = Guid.NewGuid();
        var responseId = Guid.NewGuid();

        // Act & Assert - Whitespace-only is treated same as null
        var exception = Assert.Throws<ArgumentException>(() =>
            new FeedbackDataEntity(id, responseId, text: "   "));

        Assert.Contains("At least one of Text or Marks must be provided", exception.Message);
    }
}
