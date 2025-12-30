using System;
using System.Threading.Tasks;
using Moq;
using Xunit;
using Main.Models.Entities;
using Main.Services;
using Main.Services.Repositories;

namespace MainTests.ServicesTests;

/// <summary>
/// Tests for UnitCollectionService.
/// Verifies service behavior per AdditionalValidationRules.md §2A.
/// Cascade deletion is handled by database FK constraints per PersistenceAndCascadingRules.md §2(3).
/// </summary>
public class UnitCollectionServiceTests
{
    private readonly Mock<IUnitCollectionRepository> _mockUnitCollectionRepo;
    private readonly UnitCollectionService _service;

    public UnitCollectionServiceTests()
    {
        _mockUnitCollectionRepo = new Mock<IUnitCollectionRepository>();
        _service = new UnitCollectionService(_mockUnitCollectionRepo.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullUnitCollectionRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new UnitCollectionService(null!));
    }

    #endregion

    #region CreateAsync Tests

    [Fact]
    public async Task CreateAsync_ValidUnitCollection_Success()
    {
        // Arrange
        var unitCollection = new UnitCollectionEntity(Guid.NewGuid(), "Test Collection");
        _mockUnitCollectionRepo.Setup(r => r.AddAsync(It.IsAny<UnitCollectionEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateAsync(unitCollection);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(unitCollection.Id, result.Id);
        _mockUnitCollectionRepo.Verify(r => r.AddAsync(unitCollection), Times.Once);
    }

    [Fact]
    public async Task CreateAsync_NullUnitCollection_ThrowsArgumentNullException()
    {
        // Act & Assert
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => _service.CreateAsync(null!));
    }

    [Fact]
    public async Task CreateAsync_EmptyTitle_ThrowsArgumentException()
    {
        // Arrange - §2A(1)(a): Title is mandatory
        var unitCollection = new UnitCollectionEntity(Guid.NewGuid(), "");

        // Act & Assert
        await Assert.ThrowsAsync<ArgumentException>(
            () => _service.CreateAsync(unitCollection));
    }

    [Fact]
    public async Task CreateAsync_WhitespaceTitle_ThrowsArgumentException()
    {
        // Arrange
        var unitCollection = new UnitCollectionEntity(Guid.NewGuid(), "   ");

        // Act & Assert
        await Assert.ThrowsAsync<ArgumentException>(
            () => _service.CreateAsync(unitCollection));
    }

    [Fact]
    public async Task CreateAsync_TitleExceeds500Chars_ThrowsArgumentException()
    {
        // Arrange - §2A(1)(a): Title max 500 chars
        var longTitle = new string('a', 501);
        var unitCollection = new UnitCollectionEntity(Guid.NewGuid(), longTitle);

        // Act & Assert
        await Assert.ThrowsAsync<ArgumentException>(
            () => _service.CreateAsync(unitCollection));
    }

    [Fact]
    public async Task CreateAsync_TitleExactly500Chars_Success()
    {
        // Arrange - boundary case
        var exactTitle = new string('a', 500);
        var unitCollection = new UnitCollectionEntity(Guid.NewGuid(), exactTitle);
        _mockUnitCollectionRepo.Setup(r => r.AddAsync(It.IsAny<UnitCollectionEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateAsync(unitCollection);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(500, result.Title.Length);
    }

    #endregion

    #region UpdateAsync Tests

    [Fact]
    public async Task UpdateAsync_ValidUpdate_Success()
    {
        // Arrange
        var unitCollection = new UnitCollectionEntity(Guid.NewGuid(), "Updated Title");
        _mockUnitCollectionRepo.Setup(r => r.GetByIdAsync(unitCollection.Id))
            .ReturnsAsync(unitCollection);
        _mockUnitCollectionRepo.Setup(r => r.UpdateAsync(It.IsAny<UnitCollectionEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.UpdateAsync(unitCollection);

        // Assert
        Assert.NotNull(result);
        _mockUnitCollectionRepo.Verify(r => r.UpdateAsync(unitCollection), Times.Once);
    }

    [Fact]
    public async Task UpdateAsync_NullUnitCollection_ThrowsArgumentNullException()
    {
        // Act & Assert
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => _service.UpdateAsync(null!));
    }

    [Fact]
    public async Task UpdateAsync_NonExisting_ThrowsInvalidOperationException()
    {
        // Arrange
        var unitCollection = new UnitCollectionEntity(Guid.NewGuid(), "Title");
        _mockUnitCollectionRepo.Setup(r => r.GetByIdAsync(unitCollection.Id))
            .ReturnsAsync((UnitCollectionEntity?)null);

        // Act & Assert
        await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.UpdateAsync(unitCollection));
    }

    #endregion

    #region DeleteAsync Tests

    [Fact]
    public async Task DeleteAsync_CallsRepository()
    {
        // Arrange
        // Cascade deletion is handled by database FK constraints
        // per PersistenceAndCascadingRules.md §2(3)
        var unitCollectionId = Guid.NewGuid();
        _mockUnitCollectionRepo.Setup(r => r.DeleteAsync(unitCollectionId))
            .Returns(Task.CompletedTask);

        // Act
        await _service.DeleteAsync(unitCollectionId);

        // Assert
        _mockUnitCollectionRepo.Verify(r => r.DeleteAsync(unitCollectionId), Times.Once);
    }

    #endregion
}

