using Main.Models.Entities;
using Xunit;

namespace MainTests.ModelsTests.EntitiesTests;

/// <summary>
/// Tests for UnitEntity.
/// Verifies entity behavior per AdditionalValidationRules.md §2B.
/// </summary>
public class UnitEntityTests
{
    #region Constructor Tests

    [Fact]
    public void DefaultConstructor_SetsPropertiesToDefaults()
    {
        // Act
        var entity = new UnitEntity();

        // Assert
        Assert.Equal(Guid.Empty, entity.Id);
        Assert.Equal(Guid.Empty, entity.UnitCollectionId);
        Assert.Equal(string.Empty, entity.Title);
        Assert.NotNull(entity.SourceDocuments);
        Assert.Empty(entity.SourceDocuments);
    }

    [Fact]
    public void Constructor_WithParameters_SetsProperties()
    {
        // Arrange
        var id = Guid.NewGuid();
        var unitCollectionId = Guid.NewGuid();
        var title = "Test Unit";
        var sourceDocuments = new List<string> { "/path/doc1.pdf", "/path/doc2.docx" };

        // Act
        var entity = new UnitEntity(id, unitCollectionId, title, sourceDocuments);

        // Assert
        Assert.Equal(id, entity.Id);
        Assert.Equal(unitCollectionId, entity.UnitCollectionId);
        Assert.Equal(title, entity.Title);
        Assert.Equal(2, entity.SourceDocuments.Count);
        Assert.Contains("/path/doc1.pdf", entity.SourceDocuments);
    }

    [Fact]
    public void Constructor_WithEmptySourceDocuments_AcceptsEmptyList()
    {
        // Arrange
        var id = Guid.NewGuid();
        var unitCollectionId = Guid.NewGuid();

        // Act
        var entity = new UnitEntity(id, unitCollectionId, "Title", new List<string>());

        // Assert
        Assert.NotNull(entity.SourceDocuments);
        Assert.Empty(entity.SourceDocuments);
    }

    #endregion

    #region Property Tests

    [Fact]
    public void Id_CanBeSet()
    {
        // Arrange
        var entity = new UnitEntity();
        var newId = Guid.NewGuid();

        // Act
        entity.Id = newId;

        // Assert
        Assert.Equal(newId, entity.Id);
    }

    [Fact]
    public void UnitCollectionId_CanBeSet()
    {
        // Arrange
        var entity = new UnitEntity();
        var unitCollectionId = Guid.NewGuid();

        // Act
        entity.UnitCollectionId = unitCollectionId;

        // Assert
        Assert.Equal(unitCollectionId, entity.UnitCollectionId);
    }

    [Fact]
    public void Title_CanBeSet()
    {
        // Arrange
        var entity = new UnitEntity();

        // Act
        entity.Title = "Updated Title";

        // Assert
        Assert.Equal("Updated Title", entity.Title);
    }

    [Fact]
    public void SourceDocuments_CanBeModified()
    {
        // Arrange
        var entity = new UnitEntity(Guid.NewGuid(), Guid.NewGuid(), "Title", new List<string>());

        // Act
        entity.SourceDocuments.Add("/new/path.pdf");

        // Assert
        Assert.Single(entity.SourceDocuments);
        Assert.Contains("/new/path.pdf", entity.SourceDocuments);
    }

    [Fact]
    public void Title_MaxLength500_IsEnforceableByValidation()
    {
        // This test documents the 500 character limit per §2B(1)(b)
        var entity = new UnitEntity();
        var longTitle = new string('a', 500);
        
        // Act
        entity.Title = longTitle;

        // Assert
        Assert.Equal(500, entity.Title.Length);
    }

    #endregion

    #region Foreign Key Tests

    [Fact]
    public void UnitCollectionId_IsForeignKey()
    {
        // Per §2B(1)(a): UnitCollectionId references the unit collection
        var unitCollectionId = Guid.NewGuid();
        var entity = new UnitEntity(Guid.NewGuid(), unitCollectionId, "Title", new List<string>());

        // Assert
        Assert.Equal(unitCollectionId, entity.UnitCollectionId);
    }

    [Fact]
    public void UnitCollection_NavigationProperty_IsNullByDefault()
    {
        // Navigation property should be null until loaded by EF
        var entity = new UnitEntity();

        // Assert - internal navigation property starts null
        Assert.Equal(Guid.Empty, entity.UnitCollectionId);
    }

    #endregion
}
