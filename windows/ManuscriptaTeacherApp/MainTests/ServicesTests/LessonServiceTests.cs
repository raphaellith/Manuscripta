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
/// Tests for LessonService.
/// Verifies service behavior per AdditionalValidationRules.md §2C
/// and PersistenceAndCascadingRules.md §2(5).
/// </summary>
public class LessonServiceTests
{
    private readonly Mock<ILessonRepository> _mockLessonRepo;
    private readonly Mock<IUnitRepository> _mockUnitRepo;
    private readonly Mock<IMaterialRepository> _mockMaterialRepo;
    private readonly LessonService _service;

    public LessonServiceTests()
    {
        _mockLessonRepo = new Mock<ILessonRepository>();
        _mockUnitRepo = new Mock<IUnitRepository>();
        _mockMaterialRepo = new Mock<IMaterialRepository>();
        _service = new LessonService(_mockLessonRepo.Object, _mockUnitRepo.Object, _mockMaterialRepo.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullLessonRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new LessonService(null!, _mockUnitRepo.Object, _mockMaterialRepo.Object));
    }

    [Fact]
    public void Constructor_NullUnitRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new LessonService(_mockLessonRepo.Object, null!, _mockMaterialRepo.Object));
    }

    [Fact]
    public void Constructor_NullMaterialRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new LessonService(_mockLessonRepo.Object, _mockUnitRepo.Object, null!));
    }

    #endregion

    #region CreateAsync Tests

    [Fact]
    public async Task CreateAsync_ValidLesson_Success()
    {
        // Arrange
        var unitId = Guid.NewGuid();
        var lesson = new LessonEntity(Guid.NewGuid(), unitId, "Test Lesson", "Description");
        
        _mockUnitRepo.Setup(r => r.GetByIdAsync(unitId))
            .ReturnsAsync(new UnitEntity(unitId, Guid.NewGuid(), "Unit", new List<string>()));
        _mockLessonRepo.Setup(r => r.AddAsync(It.IsAny<LessonEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateAsync(lesson);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(lesson.Id, result.Id);
        _mockLessonRepo.Verify(r => r.AddAsync(lesson), Times.Once);
    }

    [Fact]
    public async Task CreateAsync_NullLesson_ThrowsArgumentNullException()
    {
        // Act & Assert
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => _service.CreateAsync(null!));
    }

    [Fact]
    public async Task CreateAsync_EmptyTitle_ThrowsArgumentException()
    {
        // Arrange - §2C(1)(b): Title is mandatory
        var unitId = Guid.NewGuid();
        var lesson = new LessonEntity(Guid.NewGuid(), unitId, "", "Description");
        
        _mockUnitRepo.Setup(r => r.GetByIdAsync(unitId))
            .ReturnsAsync(new UnitEntity(unitId, Guid.NewGuid(), "Unit", new List<string>()));

        // Act & Assert
        await Assert.ThrowsAsync<ArgumentException>(
            () => _service.CreateAsync(lesson));
    }

    [Fact]
    public async Task CreateAsync_WhitespaceTitle_ThrowsArgumentException()
    {
        // Arrange
        var unitId = Guid.NewGuid();
        var lesson = new LessonEntity(Guid.NewGuid(), unitId, "   ", "Description");
        
        _mockUnitRepo.Setup(r => r.GetByIdAsync(unitId))
            .ReturnsAsync(new UnitEntity(unitId, Guid.NewGuid(), "Unit", new List<string>()));

        // Act & Assert
        await Assert.ThrowsAsync<ArgumentException>(
            () => _service.CreateAsync(lesson));
    }

    [Fact]
    public async Task CreateAsync_TitleExceeds500Chars_ThrowsArgumentException()
    {
        // Arrange - §2C(1)(b): Title max 500 chars
        var unitId = Guid.NewGuid();
        var longTitle = new string('a', 501);
        var lesson = new LessonEntity(Guid.NewGuid(), unitId, longTitle, "Description");
        
        _mockUnitRepo.Setup(r => r.GetByIdAsync(unitId))
            .ReturnsAsync(new UnitEntity(unitId, Guid.NewGuid(), "Unit", new List<string>()));

        // Act & Assert
        await Assert.ThrowsAsync<ArgumentException>(
            () => _service.CreateAsync(lesson));
    }

    [Fact]
    public async Task CreateAsync_EmptyDescription_ThrowsArgumentException()
    {
        // Arrange - §2C(1)(c): Description is mandatory
        var unitId = Guid.NewGuid();
        var lesson = new LessonEntity(Guid.NewGuid(), unitId, "Title", "");
        
        _mockUnitRepo.Setup(r => r.GetByIdAsync(unitId))
            .ReturnsAsync(new UnitEntity(unitId, Guid.NewGuid(), "Unit", new List<string>()));

        // Act & Assert
        await Assert.ThrowsAsync<ArgumentException>(
            () => _service.CreateAsync(lesson));
    }

    [Fact]
    public async Task CreateAsync_InvalidUnitId_ThrowsInvalidOperationException()
    {
        // Arrange - §2C(1)(a): UnitId must reference existing unit
        var invalidUnitId = Guid.NewGuid();
        var lesson = new LessonEntity(Guid.NewGuid(), invalidUnitId, "Title", "Description");
        
        _mockUnitRepo.Setup(r => r.GetByIdAsync(invalidUnitId))
            .ReturnsAsync((UnitEntity?)null);

        // Act & Assert
        await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CreateAsync(lesson));
    }

    [Fact]
    public async Task CreateAsync_TitleExactly500Chars_Success()
    {
        // Arrange - boundary case
        var unitId = Guid.NewGuid();
        var exactTitle = new string('a', 500);
        var lesson = new LessonEntity(Guid.NewGuid(), unitId, exactTitle, "Description");
        
        _mockUnitRepo.Setup(r => r.GetByIdAsync(unitId))
            .ReturnsAsync(new UnitEntity(unitId, Guid.NewGuid(), "Unit", new List<string>()));
        _mockLessonRepo.Setup(r => r.AddAsync(It.IsAny<LessonEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateAsync(lesson);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(500, result.Title.Length);
    }

    #endregion

    #region GetByIdAsync Tests

    [Fact]
    public async Task GetByIdAsync_ExistingId_ReturnsLesson()
    {
        // Arrange
        var id = Guid.NewGuid();
        var lesson = new LessonEntity(id, Guid.NewGuid(), "Test", "Description");
        _mockLessonRepo.Setup(r => r.GetByIdAsync(id))
            .ReturnsAsync(lesson);

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
        _mockLessonRepo.Setup(r => r.GetByIdAsync(id))
            .ReturnsAsync((LessonEntity?)null);

        // Act
        var result = await _service.GetByIdAsync(id);

        // Assert
        Assert.Null(result);
    }

    #endregion

    #region GetByUnitIdAsync Tests

    [Fact]
    public async Task GetByUnitIdAsync_ReturnsAllLessonsForUnit()
    {
        // Arrange
        var unitId = Guid.NewGuid();
        var lessons = new List<LessonEntity>
        {
            new LessonEntity(Guid.NewGuid(), unitId, "Lesson 1", "Description 1"),
            new LessonEntity(Guid.NewGuid(), unitId, "Lesson 2", "Description 2")
        };
        _mockLessonRepo.Setup(r => r.GetByUnitIdAsync(unitId))
            .ReturnsAsync(lessons);

        // Act
        var result = await _service.GetByUnitIdAsync(unitId);

        // Assert
        Assert.Equal(2, result.Count());
    }

    [Fact]
    public async Task GetByUnitIdAsync_NoLessons_ReturnsEmptyCollection()
    {
        // Arrange
        var unitId = Guid.NewGuid();
        _mockLessonRepo.Setup(r => r.GetByUnitIdAsync(unitId))
            .ReturnsAsync(new List<LessonEntity>());

        // Act
        var result = await _service.GetByUnitIdAsync(unitId);

        // Assert
        Assert.Empty(result);
    }

    #endregion

    #region UpdateAsync Tests

    [Fact]
    public async Task UpdateAsync_ValidUpdate_Success()
    {
        // Arrange
        var unitId = Guid.NewGuid();
        var lesson = new LessonEntity(Guid.NewGuid(), unitId, "Updated", "Updated Description");
        
        _mockUnitRepo.Setup(r => r.GetByIdAsync(unitId))
            .ReturnsAsync(new UnitEntity(unitId, Guid.NewGuid(), "Unit", new List<string>()));
        _mockLessonRepo.Setup(r => r.GetByIdAsync(lesson.Id))
            .ReturnsAsync(lesson);
        _mockLessonRepo.Setup(r => r.UpdateAsync(It.IsAny<LessonEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.UpdateAsync(lesson);

        // Assert
        Assert.NotNull(result);
        _mockLessonRepo.Verify(r => r.UpdateAsync(lesson), Times.Once);
    }

    [Fact]
    public async Task UpdateAsync_NullLesson_ThrowsArgumentNullException()
    {
        // Act & Assert
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => _service.UpdateAsync(null!));
    }

    [Fact]
    public async Task UpdateAsync_NonExisting_ThrowsInvalidOperationException()
    {
        // Arrange
        var unitId = Guid.NewGuid();
        var lesson = new LessonEntity(Guid.NewGuid(), unitId, "Title", "Description");
        
        _mockUnitRepo.Setup(r => r.GetByIdAsync(unitId))
            .ReturnsAsync(new UnitEntity(unitId, Guid.NewGuid(), "Unit", new List<string>()));
        _mockLessonRepo.Setup(r => r.GetByIdAsync(lesson.Id))
            .ReturnsAsync((LessonEntity?)null);

        // Act & Assert
        await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.UpdateAsync(lesson));
    }

    #endregion

    #region DeleteAsync Tests - Cascade Delete per §2(5)

    [Fact]
    public async Task DeleteAsync_DeletesLesson()
    {
        // Per PersistenceAndCascadingRules.md §2(5):
        // Deletion of a lesson must delete all materials within it
        // Note: Material cascade deletion is handled by database FK constraint
        var lessonId = Guid.NewGuid();
        _mockLessonRepo.Setup(r => r.DeleteAsync(lessonId))
            .Returns(Task.CompletedTask);

        // Act
        await _service.DeleteAsync(lessonId);

        // Assert
        _mockLessonRepo.Verify(r => r.DeleteAsync(lessonId), Times.Once);
    }

    [Fact]
    public async Task DeleteAsync_CallsRepositoryWithCorrectId()
    {
        // Arrange
        var lessonId = Guid.NewGuid();
        Guid? capturedId = null;
        
        _mockLessonRepo.Setup(r => r.DeleteAsync(It.IsAny<Guid>()))
            .Callback<Guid>(id => capturedId = id)
            .Returns(Task.CompletedTask);

        // Act
        await _service.DeleteAsync(lessonId);

        // Assert
        Assert.Equal(lessonId, capturedId);
    }

    #endregion

    #region GetAllAsync Tests

    [Fact]
    public async Task GetAllAsync_ReturnsAllLessons()
    {
        // Arrange
        var lessons = new List<LessonEntity>
        {
            new LessonEntity(Guid.NewGuid(), Guid.NewGuid(), "Lesson 1", "Desc 1"),
            new LessonEntity(Guid.NewGuid(), Guid.NewGuid(), "Lesson 2", "Desc 2"),
            new LessonEntity(Guid.NewGuid(), Guid.NewGuid(), "Lesson 3", "Desc 3")
        };
        _mockLessonRepo.Setup(r => r.GetAllAsync())
            .ReturnsAsync(lessons);

        // Act
        var result = await _service.GetAllAsync();

        // Assert
        Assert.Equal(3, result.Count());
    }

    [Fact]
    public async Task GetAllAsync_EmptyRepository_ReturnsEmptyCollection()
    {
        // Arrange
        _mockLessonRepo.Setup(r => r.GetAllAsync())
            .ReturnsAsync(new List<LessonEntity>());

        // Act
        var result = await _service.GetAllAsync();

        // Assert
        Assert.Empty(result);
    }

    #endregion
}
