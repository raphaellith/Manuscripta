using System;
using System.Text.Json.Nodes;
using Xunit;
using Main.Models.Entities;
using Main.Models.Enums;

namespace MainTests;

/// <summary>
/// Tests for MaterialDataEntity (persistence layer entity)
/// </summary>
public class MaterialDataEntityTests
{
    [Fact]
    public void DefaultConstructor_InitializesCollectionsAndDefaults()
    {
        var m = new MaterialDataEntity();

        Assert.NotNull(m.VocabularyTerms);
        Assert.Empty(m.VocabularyTerms);

        // Defaults
        Assert.Equal(Guid.Empty, m.Id);
        Assert.Equal(DateTime.MinValue, m.Timestamp);
    }

    [Fact]
    public void CanSetProperties()
    {
        var id = Guid.NewGuid();
        var m = new MaterialDataEntity
        {
            Id = id,
            MaterialType = MaterialType.QUIZ,
            Title = "Sample",
            Content = "Body",
            Metadata = "{\"a\":1}",
            Timestamp = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc).AddSeconds(12345)
        };

        Assert.Equal(id, m.Id);
        Assert.Equal(MaterialType.QUIZ, m.MaterialType);
        Assert.Equal("Sample", m.Title);
        Assert.Equal("Body", m.Content);
        Assert.Equal("{\"a\":1}", m.Metadata);
        Assert.Equal(new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc).AddSeconds(12345), m.Timestamp);
    }

    [Fact]
    public void CanSetAllMaterialTypes()
    {
        // Test each material type
        var epoch = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);
        var reading = new MaterialDataEntity { Id = Guid.NewGuid(), MaterialType = MaterialType.READING, Title = "R", Content = "C", Timestamp = epoch.AddSeconds(1) };
        var worksheet = new MaterialDataEntity { Id = Guid.NewGuid(), MaterialType = MaterialType.WORKSHEET, Title = "W", Content = "C", Timestamp = epoch.AddSeconds(1) };
        var poll = new MaterialDataEntity { Id = Guid.NewGuid(), MaterialType = MaterialType.POLL, Title = "P", Content = "C", Timestamp = epoch.AddSeconds(1) };
        var quiz = new MaterialDataEntity { Id = Guid.NewGuid(), MaterialType = MaterialType.QUIZ, Title = "Q", Content = "C", Timestamp = epoch.AddSeconds(1) };

        Assert.Equal(MaterialType.READING, reading.MaterialType);
        Assert.Equal(MaterialType.WORKSHEET, worksheet.MaterialType);
        Assert.Equal(MaterialType.POLL, poll.MaterialType);
        Assert.Equal(MaterialType.QUIZ, quiz.MaterialType);
    }
}
