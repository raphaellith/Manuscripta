using System;
using System.Text.Json.Nodes;
using Xunit;
using Main.Models.Entities;
using Main.Models.Enums;

namespace MainTests;

public class MaterialEntityTests
{
    [Fact]
    public void DefaultConstructor_InitializesCollectionsAndDefaults()
    {
        var m = new MaterialEntity();

        Assert.NotNull(m.VocabularyTerms);
        Assert.Empty(m.VocabularyTerms);

        // Defaults
        Assert.Equal(Guid.Empty, m.Id);
        Assert.Equal(0, m.Timestamp);
    }

    [Fact]
    public void CanSetProperties()
    {
        var id = Guid.NewGuid();
        var m = new MaterialEntity
        {
            Id = id,
            MaterialType = MaterialType.QUIZ,
            Title = "Sample",
            Content = "Body",
            Metadata = "{\"a\":1}",
            Timestamp = 12345,
            Synced = true
        };

        Assert.Equal(id, m.Id);
        Assert.Equal(MaterialType.QUIZ, m.MaterialType);
        Assert.Equal("Sample", m.Title);
        Assert.Equal("Body", m.Content);
        Assert.Equal("{\"a\":1}", m.Metadata);
        Assert.Equal(12345, m.Timestamp);
        Assert.True(m.Synced);
    }
}
