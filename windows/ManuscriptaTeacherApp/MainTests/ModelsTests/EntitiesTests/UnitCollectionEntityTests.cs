using Main.Models.Entities;
using Xunit;

namespace MainTests.ModelsTests.EntitiesTests;

/// <summary>
/// Tests for UnitCollectionEntity.
/// Verifies entity behavior per AdditionalValidationRules.md §2A.
/// </summary>
public class UnitCollectionEntityTests
{
    #region Constructor Tests

    [Fact]
    public void DefaultConstructor_SetsIdToEmpty()
    {
        // Act
        var entity = new UnitCollectionEntity();

        // Assert
        Assert.Equal(Guid.Empty, entity.Id);
        Assert.Equal(string.Empty, entity.Title);
    }

    [Fact]
    public void Constructor_WithParameters_SetsProperties()
    {
        // Arrange
        var id = Guid.NewGuid();
        var title = "Test Unit Collection";

        // Act
        var entity = new UnitCollectionEntity(id, title);

        // Assert
        Assert.Equal(id, entity.Id);
        Assert.Equal(title, entity.Title);
    }

    [Fact]
    public void Constructor_WithEmptyGuid_AcceptsEmptyGuid()
    {
        // Act
        var entity = new UnitCollectionEntity(Guid.Empty, "Title");

        // Assert
        Assert.Equal(Guid.Empty, entity.Id);
    }

    #endregion

    #region Property Tests

    [Fact]
    public void Id_CanBeSet()
    {
        // Arrange
        var entity = new UnitCollectionEntity();
        var newId = Guid.NewGuid();

        // Act
        entity.Id = newId;

        // Assert
        Assert.Equal(newId, entity.Id);
    }

    [Fact]
    public void Title_CanBeSet()
    {
        // Arrange
        var entity = new UnitCollectionEntity();
        var newTitle = "Updated Title";

        // Act
        entity.Title = newTitle;

        // Assert
        Assert.Equal(newTitle, entity.Title);
    }

    [Fact]
    public void Title_MaxLength500_IsEnforceableByValidation()
    {
        // This test documents the 500 character limit per §2A(1)(a)
        // Actual validation is performed at the service layer
        var entity = new UnitCollectionEntity();
        var longTitle = new string('a', 500);
        
        // Act
        entity.Title = longTitle;

        // Assert
        Assert.Equal(500, entity.Title.Length);
    }

    #endregion

    #region Key Tests

    [Fact]
    public void TwoEntities_WithSameId_AreNotEqualByReference()
    {
        // Arrange
        var id = Guid.NewGuid();
        var entity1 = new UnitCollectionEntity(id, "Title 1");
        var entity2 = new UnitCollectionEntity(id, "Title 2");

        // Assert - Default reference equality
        Assert.NotSame(entity1, entity2);
    }

    [Fact]
    public void Entity_Id_IsUsedAsKey()
    {
        // Arrange
        var id = Guid.NewGuid();
        var entity = new UnitCollectionEntity(id, "Title");

        // Assert
        Assert.Equal(id, entity.Id);
    }

    #endregion
}
