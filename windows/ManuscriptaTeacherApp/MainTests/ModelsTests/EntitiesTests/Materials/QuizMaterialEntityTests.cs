using System;
using Xunit;
using Main.Models.Entities.Materials;
using Main.Models.Enums;

namespace MainTests.ModelsTests.EntitiesTests.Materials;

public class QuizMaterialEntityTests
{
    private readonly Guid _testLessonId = Guid.NewGuid();

    [Fact]
    public void Constructor_WithValidData_CreatesEntity()
    {
        // Arrange
        var id = Guid.NewGuid();
        var title = "Unit 1 Quiz";
        var content = "Answer all questions to the best of your ability.";

        // Act
        var entity = new QuizMaterialEntity(id, _testLessonId, title, content);

        // Assert
        Assert.Equal(id, entity.Id);
        Assert.Equal(_testLessonId, entity.LessonId);
        Assert.Equal(title, entity.Title);
        Assert.Equal(content, entity.Content);
        Assert.Equal(MaterialType.QUIZ, entity.MaterialType);
    }

    [Fact]
    public void Constructor_SetsCorrectMaterialType()
    {
        // Arrange & Act
        var entity = new QuizMaterialEntity(Guid.NewGuid(), _testLessonId, "Title", "Content");

        // Assert
        Assert.Equal(MaterialType.QUIZ, entity.MaterialType);
    }
}
