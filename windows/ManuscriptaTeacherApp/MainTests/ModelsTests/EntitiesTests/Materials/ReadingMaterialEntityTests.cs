using System;
using Xunit;
using Main.Models.Entities.Materials;
using Main.Models.Enums;

namespace MainTests.ModelsTests.EntitiesTests.Materials;

public class ReadingMaterialEntityTests
{
    private readonly Guid _testLessonId = Guid.NewGuid();

    [Fact]
    public void Constructor_WithValidData_CreatesEntity()
    {
        // Arrange
        var id = Guid.NewGuid();
        var title = "Chapter 1: Introduction";
        var content = "This is the introduction content...";
        var timestamp = DateTime.UtcNow;

        // Act
        var entity = new ReadingMaterialEntity(id, _testLessonId, title, content, timestamp);

        // Assert
        Assert.Equal(id, entity.Id);
        Assert.Equal(_testLessonId, entity.LessonId);
        Assert.Equal(title, entity.Title);
        Assert.Equal(content, entity.Content);
        Assert.Equal(MaterialType.READING, entity.MaterialType);
        Assert.Equal(timestamp, entity.Timestamp);
        Assert.Null(entity.Metadata);
        Assert.Null(entity.VocabularyTerms);
    }

    [Fact]
    public void Constructor_WithAllParameters_CreatesEntity()
    {
        // Arrange
        var id = Guid.NewGuid();
        var title = "Chapter 1";
        var content = "Content";
        var timestamp = DateTime.UtcNow;
        var metadata = "{\"author\":\"John Doe\"}";
        var vocabularyTerms = new System.Text.Json.Nodes.JsonArray();

        // Act
        var entity = new ReadingMaterialEntity(id, _testLessonId, title, content, timestamp, metadata, vocabularyTerms);

        // Assert
        Assert.Equal(metadata, entity.Metadata);
        Assert.Equal(vocabularyTerms, entity.VocabularyTerms);
    }

    [Fact]
    public void Constructor_WithDefaultTimestamp_SetsCurrentTime()
    {
        // Arrange
        var id = Guid.NewGuid();
        var beforeCreation = DateTime.UtcNow;

        // Act
        var entity = new ReadingMaterialEntity(id, _testLessonId, "Title", "Content");
        var afterCreation = DateTime.UtcNow;

        // Assert
        Assert.True(entity.Timestamp >= beforeCreation && entity.Timestamp <= afterCreation);
    }

    [Fact]
    public void Constructor_WithNullTitle_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() =>
            new ReadingMaterialEntity(Guid.NewGuid(), _testLessonId, null!, "Content"));
    }

    [Fact]
    public void Constructor_WithNullContent_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() =>
            new ReadingMaterialEntity(Guid.NewGuid(), _testLessonId, "Title", null!));
    }
}
