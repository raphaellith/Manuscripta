using System;
using Xunit;
using Main.Models.Entities.Materials;
using Main.Models.Enums;

namespace MainTests.ModelsTests.EntitiesTests.Materials;

public class ReadingMaterialEntityTests
{
    [Fact]
    public void Constructor_WithValidData_CreatesEntity()
    {
        // Arrange
        var id = Guid.NewGuid();
        var title = "Chapter 1: Introduction";
        var content = "This is the introduction content...";
        var timestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds();

        // Act
        var entity = new ReadingMaterialEntity(id, title, content, timestamp);

        // Assert
        Assert.Equal(id, entity.Id);
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
        var timestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
        var metadata = "{\"author\":\"John Doe\"}";
        var vocabularyTerms = new System.Text.Json.Nodes.JsonArray();

        // Act
        var entity = new ReadingMaterialEntity(id, title, content, timestamp, metadata, vocabularyTerms);

        // Assert
        Assert.Equal(metadata, entity.Metadata);
        Assert.Equal(vocabularyTerms, entity.VocabularyTerms);
    }

    [Fact]
    public void Constructor_WithDefaultTimestamp_SetsCurrentTime()
    {
        // Arrange
        var id = Guid.NewGuid();
        var beforeCreation = DateTimeOffset.UtcNow.ToUnixTimeSeconds();

        // Act
        var entity = new ReadingMaterialEntity(id, "Title", "Content");
        var afterCreation = DateTimeOffset.UtcNow.ToUnixTimeSeconds();

        // Assert
        Assert.True(entity.Timestamp >= beforeCreation && entity.Timestamp <= afterCreation);
    }

    [Fact]
    public void Constructor_WithNullTitle_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() =>
            new ReadingMaterialEntity(Guid.NewGuid(), null!, "Content"));
    }

    [Fact]
    public void Constructor_WithNullContent_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() =>
            new ReadingMaterialEntity(Guid.NewGuid(), "Title", null!));
    }
}
