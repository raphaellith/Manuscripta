using System;
using System.Threading.Tasks;
using Moq;
using Xunit;
using Main.Models.Entities;
using Main.Services;
using Main.Services.Repositories;

namespace MainTests.ServicesTests;

/// <summary>
/// Tests for SourceDocumentService.
/// Verifies service behavior per AdditionalValidationRules.md §3A.
/// Cascade deletion is handled by database FK constraints per PersistenceAndCascadingRules.md §2(3A).
/// </summary>
public class SourceDocumentServiceTests
{
    private readonly Mock<ISourceDocumentRepository> _mockSourceDocumentRepo;
    private readonly Mock<IUnitCollectionRepository> _mockUnitCollectionRepo;
    private readonly SourceDocumentService _service;

    public SourceDocumentServiceTests()
    {
        _mockSourceDocumentRepo = new Mock<ISourceDocumentRepository>();
        _mockUnitCollectionRepo = new Mock<IUnitCollectionRepository>();
        _service = new SourceDocumentService(
            _mockSourceDocumentRepo.Object, 
            _mockUnitCollectionRepo.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullSourceDocumentRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new SourceDocumentService(null!, _mockUnitCollectionRepo.Object));
    }

    [Fact]
    public void Constructor_NullUnitCollectionRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new SourceDocumentService(_mockSourceDocumentRepo.Object, null!));
    }

    #endregion

    #region CreateAsync Tests

    [Fact]
    public async Task CreateAsync_ValidSourceDocument_Success()
    {
        // Arrange
        var unitCollectionId = Guid.NewGuid();
        var sourceDocument = new SourceDocumentEntity(Guid.NewGuid(), unitCollectionId, "Sample transcript");
        
        _mockUnitCollectionRepo.Setup(r => r.GetByIdAsync(unitCollectionId))
            .ReturnsAsync(new UnitCollectionEntity(unitCollectionId, "Test Collection"));
        _mockSourceDocumentRepo.Setup(r => r.AddAsync(It.IsAny<SourceDocumentEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateAsync(sourceDocument);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(sourceDocument.Id, result.Id);
        _mockSourceDocumentRepo.Verify(r => r.AddAsync(sourceDocument), Times.Once);
    }

    [Fact]
    public async Task CreateAsync_NullSourceDocument_ThrowsArgumentNullException()
    {
        // Act & Assert
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => _service.CreateAsync(null!));
    }

    [Fact]
    public async Task CreateAsync_NonExistentUnitCollectionId_ThrowsInvalidOperationException()
    {
        // Arrange - §3A(2)(a): UnitCollectionId must reference a valid UnitCollectionEntity
        var sourceDocument = new SourceDocumentEntity(Guid.NewGuid(), Guid.NewGuid(), "Sample transcript");
        
        _mockUnitCollectionRepo.Setup(r => r.GetByIdAsync(sourceDocument.UnitCollectionId))
            .ReturnsAsync((UnitCollectionEntity?)null);

        // Act & Assert
        await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CreateAsync(sourceDocument));
    }

    [Fact]
    public async Task CreateAsync_NullTranscript_ThrowsArgumentException()
    {
        // Arrange - §3A(1)(b): Transcript is required
        var unitCollectionId = Guid.NewGuid();
        var sourceDocument = new SourceDocumentEntity
        {
            Id = Guid.NewGuid(),
            UnitCollectionId = unitCollectionId,
            Transcript = null!
        };
        
        _mockUnitCollectionRepo.Setup(r => r.GetByIdAsync(unitCollectionId))
            .ReturnsAsync(new UnitCollectionEntity(unitCollectionId, "Test Collection"));

        // Act & Assert
        await Assert.ThrowsAsync<ArgumentException>(
            () => _service.CreateAsync(sourceDocument));
    }

    [Fact]
    public async Task CreateAsync_EmptyTranscript_Success()
    {
        // Arrange - Empty string is valid (only null is invalid)
        var unitCollectionId = Guid.NewGuid();
        var sourceDocument = new SourceDocumentEntity(Guid.NewGuid(), unitCollectionId, "");
        
        _mockUnitCollectionRepo.Setup(r => r.GetByIdAsync(unitCollectionId))
            .ReturnsAsync(new UnitCollectionEntity(unitCollectionId, "Test Collection"));
        _mockSourceDocumentRepo.Setup(r => r.AddAsync(It.IsAny<SourceDocumentEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateAsync(sourceDocument);

        // Assert
        Assert.NotNull(result);
        Assert.Equal("", result.Transcript);
    }

    #endregion

    #region DeleteAsync Tests

    [Fact]
    public async Task DeleteAsync_CallsRepository()
    {
        // Arrange
        // Cascade deletion is handled by database FK constraints
        // per PersistenceAndCascadingRules.md §2(3A)
        var sourceDocumentId = Guid.NewGuid();
        _mockSourceDocumentRepo.Setup(r => r.DeleteAsync(sourceDocumentId))
            .Returns(Task.CompletedTask);

        // Act
        await _service.DeleteAsync(sourceDocumentId);

        // Assert
        _mockSourceDocumentRepo.Verify(r => r.DeleteAsync(sourceDocumentId), Times.Once);
    }

    #endregion
}
