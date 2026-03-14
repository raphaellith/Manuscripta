using System;
using Xunit;
using Main.Models.Entities.Materials;
using Main.Models.Enums;

namespace MainTests.ModelsTests.EntitiesTests.Materials;

public class WorksheetMaterialEntityTests
{
    private readonly Guid _testLessonId = Guid.NewGuid();

    [Fact]
    public void Constructor_WithValidData_CreatesEntity()
    {
        // Arrange
        var id = Guid.NewGuid();
        var title = "Practice Worksheet";
        var content = "Complete the following exercises...";

        // Act
        var entity = new WorksheetMaterialEntity(id, _testLessonId, title, content);

        // Assert
        Assert.Equal(id, entity.Id);
        Assert.Equal(_testLessonId, entity.LessonId);
        Assert.Equal(title, entity.Title);
        Assert.Equal(content, entity.Content);
        Assert.Equal(MaterialType.WORKSHEET, entity.MaterialType);
    }

    [Fact]
    public void Constructor_SetsCorrectMaterialType()
    {
        // Arrange & Act
        var entity = new WorksheetMaterialEntity(Guid.NewGuid(), _testLessonId, "Title", "Content");

        // Assert
        Assert.Equal(MaterialType.WORKSHEET, entity.MaterialType);
    }
}
