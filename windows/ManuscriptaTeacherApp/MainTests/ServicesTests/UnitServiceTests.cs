using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Moq;
using Xunit;
using Main.Models.Entities;
using Main.Services;
using Main.Services.Repositories;

namespace MainTests.ServicesTests;

/// <summary>
/// Tests for UnitService.
/// Verifies service behavior per AdditionalValidationRules.md §2B
/// and PersistenceAndCascadingRules.md §2(4).
/// </summary>
public class UnitServiceTests
{
    private readonly Mock<IUnitRepository> _mockUnitRepo;
    private readonly Mock<IUnitCollectionRepository> _mockUnitCollectionRepo;
    private readonly Mock<ILessonRepository> _mockLessonRepo;
    private readonly UnitService _service;

    public UnitServiceTests()
    {
        _mockUnitRepo = new Mock<IUnitRepository>();
        _mockUnitCollectionRepo = new Mock<IUnitCollectionRepository>();
        _mockLessonRepo = new Mock<ILessonRepository>();
        _service = new UnitService(
            _mockUnitRepo.Object, 
            _mockUnitCollectionRepo.Object, 
            _mockLessonRepo.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullUnitRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new UnitService(null!, _mockUnitCollectionRepo.Object, _mockLessonRepo.Object));
    }

    [Fact]
    public void Constructor_NullUnitCollectionRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new UnitService(_mockUnitRepo.Object, null!, _mockLessonRepo.Object));
    }

    [Fact]
    public void Constructor_NullLessonRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new UnitService(_mockUnitRepo.Object, _mockUnitCollectionRepo.Object, null!));
    }

    #endregion

    #region CreateAsync Tests

    [Fact]
    public async Task CreateAsync_ValidUnit_Success()
    {
        // Arrange
        var unitCollectionId = Guid.NewGuid();
        var unit = new UnitEntity(Guid.NewGuid(), unitCollectionId, "Test Unit", new List<string>());
        
        _mockUnitCollectionRepo.Setup(r => r.GetByIdAsync(unitCollectionId))
            .ReturnsAsync(new UnitCollectionEntity(unitCollectionId, "Collection"));
        _mockUnitRepo.Setup(r => r.AddAsync(It.IsAny<UnitEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateAsync(unit);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(unit.Id, result.Id);
        _mockUnitRepo.Verify(r => r.AddAsync(unit), Times.Once);
    }

    [Fact]
    public async Task CreateAsync_NullUnit_ThrowsArgumentNullException()
    {
        // Act & Assert
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => _service.CreateAsync(null!));
    }

    [Fact]
    public async Task CreateAsync_EmptyTitle_ThrowsArgumentException()
    {
        // Arrange - §2B(1)(b): Title is mandatory
        var unitCollectionId = Guid.NewGuid();
        var unit = new UnitEntity(Guid.NewGuid(), unitCollectionId, "", new List<string>());
        
        _mockUnitCollectionRepo.Setup(r => r.GetByIdAsync(unitCollectionId))
            .ReturnsAsync(new UnitCollectionEntity(unitCollectionId, "Collection"));

        // Act & Assert
        await Assert.ThrowsAsync<ArgumentException>(
            () => _service.CreateAsync(unit));
    }

    [Fact]
    public async Task CreateAsync_TitleExceeds500Chars_ThrowsArgumentException()
    {
        // Arrange - §2B(1)(b): Title max 500 chars
        var unitCollectionId = Guid.NewGuid();
        var longTitle = new string('a', 501);
        var unit = new UnitEntity(Guid.NewGuid(), unitCollectionId, longTitle, new List<string>());
        
        _mockUnitCollectionRepo.Setup(r => r.GetByIdAsync(unitCollectionId))
            .ReturnsAsync(new UnitCollectionEntity(unitCollectionId, "Collection"));

        // Act & Assert
        await Assert.ThrowsAsync<ArgumentException>(
            () => _service.CreateAsync(unit));
    }

    [Fact]
    public async Task CreateAsync_InvalidUnitCollectionId_ThrowsInvalidOperationException()
    {
        // Arrange - §2B(1)(a): UnitCollectionId must reference existing collection
        var invalidUnitCollectionId = Guid.NewGuid();
        var unit = new UnitEntity(Guid.NewGuid(), invalidUnitCollectionId, "Title", new List<string>());
        
        _mockUnitCollectionRepo.Setup(r => r.GetByIdAsync(invalidUnitCollectionId))
            .ReturnsAsync((UnitCollectionEntity?)null);

        // Act & Assert
        await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CreateAsync(unit));
    }

    [Fact]
    public async Task CreateAsync_WithSourceDocuments_Success()
    {
        // Arrange - §2B(1)(c): SourceDocuments is optional
        var unitCollectionId = Guid.NewGuid();
        var sourceDocuments = new List<string> { "/path/doc1.pdf", "/path/doc2.docx" };
        var unit = new UnitEntity(Guid.NewGuid(), unitCollectionId, "Title", sourceDocuments);
        
        _mockUnitCollectionRepo.Setup(r => r.GetByIdAsync(unitCollectionId))
            .ReturnsAsync(new UnitCollectionEntity(unitCollectionId, "Collection"));
        _mockUnitRepo.Setup(r => r.AddAsync(It.IsAny<UnitEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateAsync(unit);

        // Assert
        Assert.Equal(2, result.SourceDocuments.Count);
    }

    #endregion

    #region GetByIdAsync Tests

    [Fact]
    public async Task GetByIdAsync_ExistingId_ReturnsUnit()
    {
        // Arrange
        var id = Guid.NewGuid();
        var unit = new UnitEntity(id, Guid.NewGuid(), "Test", new List<string>());
        _mockUnitRepo.Setup(r => r.GetByIdAsync(id))
            .ReturnsAsync(unit);

        // Act
        var result = await _service.GetByIdAsync(id);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(id, result!.Id);
    }

    [Fact]
    public async Task GetByIdAsync_NonExistingId_ReturnsNull()
    {
        // Arrange
        var id = Guid.NewGuid();
        _mockUnitRepo.Setup(r => r.GetByIdAsync(id))
            .ReturnsAsync((UnitEntity?)null);

        // Act
        var result = await _service.GetByIdAsync(id);

        // Assert
        Assert.Null(result);
    }

    #endregion

    #region GetByUnitCollectionIdAsync Tests

    [Fact]
    public async Task GetByUnitCollectionIdAsync_ReturnsAllUnitsForCollection()
    {
        // Arrange
        var unitCollectionId = Guid.NewGuid();
        var units = new List<UnitEntity>
        {
            new UnitEntity(Guid.NewGuid(), unitCollectionId, "Unit 1", new List<string>()),
            new UnitEntity(Guid.NewGuid(), unitCollectionId, "Unit 2", new List<string>())
        };
        _mockUnitRepo.Setup(r => r.GetByUnitCollectionIdAsync(unitCollectionId))
            .ReturnsAsync(units);

        // Act
        var result = await _service.GetByUnitCollectionIdAsync(unitCollectionId);

        // Assert
        Assert.Equal(2, result.Count());
    }

    #endregion

    #region UpdateAsync Tests

    [Fact]
    public async Task UpdateAsync_ValidUpdate_Success()
    {
        // Arrange
        var unitCollectionId = Guid.NewGuid();
        var unit = new UnitEntity(Guid.NewGuid(), unitCollectionId, "Updated", new List<string>());
        
        _mockUnitCollectionRepo.Setup(r => r.GetByIdAsync(unitCollectionId))
            .ReturnsAsync(new UnitCollectionEntity(unitCollectionId, "Collection"));
        _mockUnitRepo.Setup(r => r.GetByIdAsync(unit.Id))
            .ReturnsAsync(unit);
        _mockUnitRepo.Setup(r => r.UpdateAsync(It.IsAny<UnitEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.UpdateAsync(unit);

        // Assert
        Assert.NotNull(result);
        _mockUnitRepo.Verify(r => r.UpdateAsync(unit), Times.Once);
    }

    [Fact]
    public async Task UpdateAsync_NullUnit_ThrowsArgumentNullException()
    {
        // Act & Assert
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => _service.UpdateAsync(null!));
    }

    [Fact]
    public async Task UpdateAsync_NonExisting_ThrowsInvalidOperationException()
    {
        // Arrange
        var unitCollectionId = Guid.NewGuid();
        var unit = new UnitEntity(Guid.NewGuid(), unitCollectionId, "Title", new List<string>());
        
        _mockUnitCollectionRepo.Setup(r => r.GetByIdAsync(unitCollectionId))
            .ReturnsAsync(new UnitCollectionEntity(unitCollectionId, "Collection"));
        _mockUnitRepo.Setup(r => r.GetByIdAsync(unit.Id))
            .ReturnsAsync((UnitEntity?)null);

        // Act & Assert
        await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.UpdateAsync(unit));
    }

    #endregion

    #region DeleteAsync Tests - Cascade Delete per §2(4)

    [Fact]
    public async Task DeleteAsync_WithLessons_CascadeDeletesLessons()
    {
        // Arrange - Per PersistenceAndCascadingRules.md §2(4):
        // Deletion of a unit must delete all lessons within it
        var unitId = Guid.NewGuid();
        var lessons = new List<LessonEntity>
        {
            new LessonEntity(Guid.NewGuid(), unitId, "Lesson 1", "Description 1"),
            new LessonEntity(Guid.NewGuid(), unitId, "Lesson 2", "Description 2")
        };

        _mockLessonRepo.Setup(r => r.GetByUnitIdAsync(unitId))
            .ReturnsAsync(lessons);
        _mockLessonRepo.Setup(r => r.DeleteAsync(It.IsAny<Guid>()))
            .Returns(Task.CompletedTask);
        _mockUnitRepo.Setup(r => r.DeleteAsync(unitId))
            .Returns(Task.CompletedTask);

        // Act
        await _service.DeleteAsync(unitId);

        // Assert - verify cascade deletion
        _mockLessonRepo.Verify(r => r.DeleteAsync(It.IsAny<Guid>()), Times.Exactly(2));
        _mockUnitRepo.Verify(r => r.DeleteAsync(unitId), Times.Once);
    }

    [Fact]
    public async Task DeleteAsync_NoLessons_DeletesOnlyUnit()
    {
        // Arrange
        var unitId = Guid.NewGuid();
        _mockLessonRepo.Setup(r => r.GetByUnitIdAsync(unitId))
            .ReturnsAsync(new List<LessonEntity>());
        _mockUnitRepo.Setup(r => r.DeleteAsync(unitId))
            .Returns(Task.CompletedTask);

        // Act
        await _service.DeleteAsync(unitId);

        // Assert
        _mockLessonRepo.Verify(r => r.DeleteAsync(It.IsAny<Guid>()), Times.Never);
        _mockUnitRepo.Verify(r => r.DeleteAsync(unitId), Times.Once);
    }

    [Fact]
    public async Task DeleteAsync_DeletesLessonsBeforeUnit()
    {
        // Arrange - verify order
        var unitId = Guid.NewGuid();
        var callOrder = new List<string>();

        var lessons = new List<LessonEntity>
        {
            new LessonEntity(Guid.NewGuid(), unitId, "Lesson 1", "Description")
        };

        _mockLessonRepo.Setup(r => r.GetByUnitIdAsync(unitId))
            .ReturnsAsync(lessons);
        _mockLessonRepo.Setup(r => r.DeleteAsync(It.IsAny<Guid>()))
            .Callback(() => callOrder.Add("DeleteLesson"))
            .Returns(Task.CompletedTask);
        _mockUnitRepo.Setup(r => r.DeleteAsync(unitId))
            .Callback(() => callOrder.Add("DeleteUnit"))
            .Returns(Task.CompletedTask);

        // Act
        await _service.DeleteAsync(unitId);

        // Assert
        Assert.Equal(2, callOrder.Count);
        Assert.Equal("DeleteLesson", callOrder[0]);
        Assert.Equal("DeleteUnit", callOrder[1]);
    }

    #endregion
}
