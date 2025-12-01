using System;
using Xunit;
using Main.Models.Entities.Materials;
using Main.Models.Enums;

namespace MainTests.ModelsTests.EntitiesTests.Materials;

public class WorksheetMaterialEntityTests
{
    [Fact]
    public void Constructor_WithValidData_CreatesEntity()
    {
        // Arrange
        var id = Guid.NewGuid();
        var title = "Practice Worksheet";
        var content = "Complete the following exercises...";

        // Act
        var entity = new WorksheetMaterialEntity(id, title, content);

        // Assert
        Assert.Equal(id, entity.Id);
        Assert.Equal(title, entity.Title);
        Assert.Equal(content, entity.Content);
        Assert.Equal(MaterialType.WORKSHEET, entity.MaterialType);
    }

    [Fact]
    public void Constructor_SetsCorrectMaterialType()
    {
        // Arrange & Act
        var entity = new WorksheetMaterialEntity(Guid.NewGuid(), "Title", "Content");

        // Assert
        Assert.Equal(MaterialType.WORKSHEET, entity.MaterialType);
    }
}
