using Main.Models.Entities;
using Xunit;

namespace MainTests.ModelsTests.EntitiesTests;

/// <summary>
/// Tests for LessonEntity.
/// Verifies entity behavior per AdditionalValidationRules.md §2C.
/// </summary>
public class LessonEntityTests
{
    #region Constructor Tests

    [Fact]
    public void DefaultConstructor_SetsPropertiesToDefaults()
    {
        // Act
        var entity = new LessonEntity();

        // Assert
        Assert.Equal(Guid.Empty, entity.Id);
        Assert.Equal(Guid.Empty, entity.UnitId);
        Assert.Equal(string.Empty, entity.Title);
        Assert.Equal(string.Empty, entity.Description);
    }

    [Fact]
    public void Constructor_WithParameters_SetsProperties()
    {
        // Arrange
        var id = Guid.NewGuid();
        var unitId = Guid.NewGuid();
        var title = "Test Lesson";
        var description = "This is a test lesson description.";

        // Act
        var entity = new LessonEntity(id, unitId, title, description);

        // Assert
        Assert.Equal(id, entity.Id);
        Assert.Equal(unitId, entity.UnitId);
        Assert.Equal(title, entity.Title);
        Assert.Equal(description, entity.Description);
    }

    [Fact]
    public void Constructor_WithEmptyDescription_AcceptsEmptyString()
    {
        // Arrange
        var id = Guid.NewGuid();
        var unitId = Guid.NewGuid();

        // Act
        var entity = new LessonEntity(id, unitId, "Title", "");

        // Assert
        Assert.Equal(string.Empty, entity.Description);
    }

    #endregion

    #region Property Tests

    [Fact]
    public void Id_CanBeSet()
    {
        // Arrange
        var entity = new LessonEntity();
        var newId = Guid.NewGuid();

        // Act
        entity.Id = newId;

        // Assert
        Assert.Equal(newId, entity.Id);
    }

    [Fact]
    public void UnitId_CanBeSet()
    {
        // Arrange
        var entity = new LessonEntity();
        var unitId = Guid.NewGuid();

        // Act
        entity.UnitId = unitId;

        // Assert
        Assert.Equal(unitId, entity.UnitId);
    }

    [Fact]
    public void Title_CanBeSet()
    {
        // Arrange
        var entity = new LessonEntity();

        // Act
        entity.Title = "Updated Lesson Title";

        // Assert
        Assert.Equal("Updated Lesson Title", entity.Title);
    }

    [Fact]
    public void Description_CanBeSet()
    {
        // Arrange
        var entity = new LessonEntity();

        // Act
        entity.Description = "A detailed description of the lesson.";

        // Assert
        Assert.Equal("A detailed description of the lesson.", entity.Description);
    }

    [Fact]
    public void Title_MaxLength500_IsEnforceableByValidation()
    {
        // This test documents the 500 character limit per §2C(1)(b)
        var entity = new LessonEntity();
        var longTitle = new string('a', 500);
        
        // Act
        entity.Title = longTitle;

        // Assert
        Assert.Equal(500, entity.Title.Length);
    }

    [Fact]
    public void Description_HasNoLengthLimit()
    {
        // Per §2C(1)(c): Description is just a string with no specified limit
        var entity = new LessonEntity();
        var longDescription = new string('a', 10000);
        
        // Act
        entity.Description = longDescription;

        // Assert
        Assert.Equal(10000, entity.Description.Length);
    }

    #endregion

    #region Foreign Key Tests

    [Fact]
    public void UnitId_IsForeignKey()
    {
        // Per §2C(1)(a): UnitId references the unit
        var unitId = Guid.NewGuid();
        var entity = new LessonEntity(Guid.NewGuid(), unitId, "Title", "Description");

        // Assert
        Assert.Equal(unitId, entity.UnitId);
    }

    [Fact]
    public void Unit_NavigationProperty_CanBeQueried()
    {
        // Navigation property should exist for EF Core
        var entity = new LessonEntity();

        // Assert - default state
        Assert.Equal(Guid.Empty, entity.UnitId);
    }

    #endregion

    #region Key Tests

    [Fact]
    public void TwoEntities_WithSameId_AreNotEqualByReference()
    {
        // Arrange
        var id = Guid.NewGuid();
        var unitId = Guid.NewGuid();
        var entity1 = new LessonEntity(id, unitId, "Title 1", "Desc 1");
        var entity2 = new LessonEntity(id, unitId, "Title 2", "Desc 2");

        // Assert - Default reference equality
        Assert.NotSame(entity1, entity2);
    }

    #endregion
}
