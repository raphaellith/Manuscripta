using System;
using Xunit;
using Main.Models.Entities.Materials;
using Main.Models.Enums;

namespace MainTests.ModelsTests.EntitiesTests.Materials;

public class PollMaterialEntityTests
{
    private readonly Guid _testLessonId = Guid.NewGuid();

    [Fact]
    public void Constructor_WithValidData_CreatesEntity()
    {
        // Arrange
        var id = Guid.NewGuid();
        var title = "Class Opinion Poll";
        var content = "What is your favorite topic?";

        // Act
        var entity = new PollMaterialEntity(id, _testLessonId, title, content);

        // Assert
        Assert.Equal(id, entity.Id);
        Assert.Equal(_testLessonId, entity.LessonId);
        Assert.Equal(title, entity.Title);
        Assert.Equal(content, entity.Content);
        Assert.Equal(MaterialType.POLL, entity.MaterialType);
    }

    [Fact]
    public void Constructor_SetsCorrectMaterialType()
    {
        // Arrange & Act
        var entity = new PollMaterialEntity(Guid.NewGuid(), _testLessonId, "Title", "Content");

        // Assert
        Assert.Equal(MaterialType.POLL, entity.MaterialType);
    }
}
