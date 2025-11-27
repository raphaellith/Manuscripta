using System;
using Xunit;
using Main.Models.Entities;
using Main.Models.Entities.Materials;
using Main.Models.Enums;
using Main.Models.Mappings;

namespace MainTests.ModelsTests.Mappings;

public class MaterialEntityMapperTests
{
    [Fact]
    public void ToDataEntity_WithReadingMaterial_MapsCorrectly()
    {
        // Arrange
        var id = Guid.NewGuid();
        var title = "Reading Material";
        var content = "Content here";
        var timestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
        var entity = new ReadingMaterialEntity(id, title, content, timestamp);

        // Act
        var dataEntity = MaterialEntityMapper.ToDataEntity(entity);

        // Assert
        Assert.Equal(id, dataEntity.Id);
        Assert.Equal(title, dataEntity.Title);
        Assert.Equal(content, dataEntity.Content);
        Assert.Equal(MaterialType.READING, dataEntity.MaterialType);
        Assert.Equal(timestamp, dataEntity.Timestamp);
        Assert.False(dataEntity.Synced);
    }

    [Fact]
    public void ToDataEntity_WithWorksheet_MapsCorrectly()
    {
        // Arrange
        var entity = new WorksheetMaterialEntity(Guid.NewGuid(), "Worksheet", "Content");

        // Act
        var dataEntity = MaterialEntityMapper.ToDataEntity(entity);

        // Assert
        Assert.Equal(MaterialType.WORKSHEET, dataEntity.MaterialType);
    }

    [Fact]
    public void ToDataEntity_WithPoll_MapsCorrectly()
    {
        // Arrange
        var entity = new PollMaterialEntity(Guid.NewGuid(), "Poll", "Content");

        // Act
        var dataEntity = MaterialEntityMapper.ToDataEntity(entity);

        // Assert
        Assert.Equal(MaterialType.POLL, dataEntity.MaterialType);
    }

    [Fact]
    public void ToDataEntity_WithQuiz_MapsCorrectly()
    {
        // Arrange
        var entity = new QuizMaterialEntity(Guid.NewGuid(), "Quiz", "Content");

        // Act
        var dataEntity = MaterialEntityMapper.ToDataEntity(entity);

        // Assert
        Assert.Equal(MaterialType.QUIZ, dataEntity.MaterialType);
    }

    [Fact]
    public void ToDataEntity_WithMetadataAndVocabulary_MapsCorrectly()
    {
        // Arrange
        var metadata = "{\"author\":\"Test\"}";
        var vocabularyTerms = new System.Text.Json.Nodes.JsonArray();
        var entity = new ReadingMaterialEntity(
            Guid.NewGuid(), "Title", "Content", null, metadata, vocabularyTerms);

        // Act
        var dataEntity = MaterialEntityMapper.ToDataEntity(entity);

        // Assert
        Assert.Equal(metadata, dataEntity.Metadata);
        Assert.Equal(vocabularyTerms, dataEntity.VocabularyTerms);
    }

    [Fact]
    public void ToDataEntity_WithNull_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => MaterialEntityMapper.ToDataEntity(null!));
    }

    [Fact]
    public void ToEntity_WithReadingDataEntity_MapsCorrectly()
    {
        // Arrange
        var id = Guid.NewGuid();
        var title = "Reading";
        var content = "Content";
        var timestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
        var dataEntity = new MaterialDataEntity
        {
            Id = id,
            MaterialType = MaterialType.READING,
            Title = title,
            Content = content,
            Timestamp = timestamp,
            Synced = false
        };

        // Act
        var entity = MaterialEntityMapper.ToEntity(dataEntity);

        // Assert
        Assert.IsType<ReadingMaterialEntity>(entity);
        Assert.Equal(id, entity.Id);
        Assert.Equal(title, entity.Title);
        Assert.Equal(content, entity.Content);
        Assert.Equal(MaterialType.READING, entity.MaterialType);
        Assert.Equal(timestamp, entity.Timestamp);
    }

    [Fact]
    public void ToEntity_WithWorksheetDataEntity_MapsCorrectly()
    {
        // Arrange
        var dataEntity = new MaterialDataEntity
        {
            Id = Guid.NewGuid(),
            MaterialType = MaterialType.WORKSHEET,
            Title = "Worksheet",
            Content = "Content",
            Timestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
            Synced = false
        };

        // Act
        var entity = MaterialEntityMapper.ToEntity(dataEntity);

        // Assert
        Assert.IsType<WorksheetMaterialEntity>(entity);
        Assert.Equal(MaterialType.WORKSHEET, entity.MaterialType);
    }

    [Fact]
    public void ToEntity_WithPollDataEntity_MapsCorrectly()
    {
        // Arrange
        var dataEntity = new MaterialDataEntity
        {
            Id = Guid.NewGuid(),
            MaterialType = MaterialType.POLL,
            Title = "Poll",
            Content = "Content",
            Timestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
            Synced = false
        };

        // Act
        var entity = MaterialEntityMapper.ToEntity(dataEntity);

        // Assert
        Assert.IsType<PollMaterialEntity>(entity);
        Assert.Equal(MaterialType.POLL, entity.MaterialType);
    }

    [Fact]
    public void ToEntity_WithQuizDataEntity_MapsCorrectly()
    {
        // Arrange
        var dataEntity = new MaterialDataEntity
        {
            Id = Guid.NewGuid(),
            MaterialType = MaterialType.QUIZ,
            Title = "Quiz",
            Content = "Content",
            Timestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
            Synced = false
        };

        // Act
        var entity = MaterialEntityMapper.ToEntity(dataEntity);

        // Assert
        Assert.IsType<QuizMaterialEntity>(entity);
        Assert.Equal(MaterialType.QUIZ, entity.MaterialType);
    }

    [Fact]
    public void ToEntity_WithNull_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => MaterialEntityMapper.ToEntity(null!));
    }

    [Fact]
    public void RoundTrip_PreservesData()
    {
        // Arrange
        var id = Guid.NewGuid();
        var title = "Original Title";
        var content = "Original Content";
        var timestamp = 1234567890L;
        var metadata = "{\"test\":true}";
        var vocabularyTerms = new System.Text.Json.Nodes.JsonArray();

        var original = new QuizMaterialEntity(id, title, content, timestamp, metadata, vocabularyTerms);

        // Act
        var dataEntity = MaterialEntityMapper.ToDataEntity(original);
        var roundTripped = MaterialEntityMapper.ToEntity(dataEntity) as QuizMaterialEntity;

        // Assert
        Assert.NotNull(roundTripped);
        Assert.Equal(original.Id, roundTripped!.Id);
        Assert.Equal(original.Title, roundTripped.Title);
        Assert.Equal(original.Content, roundTripped.Content);
        Assert.Equal(original.MaterialType, roundTripped.MaterialType);
        Assert.Equal(original.Timestamp, roundTripped.Timestamp);
        Assert.Equal(original.Metadata, roundTripped.Metadata);
    }
}
