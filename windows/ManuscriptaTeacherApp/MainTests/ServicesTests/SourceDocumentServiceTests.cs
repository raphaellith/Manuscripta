using System;
using System.Threading.Tasks;
using Moq;
using Xunit;
using Main.Models.Entities;
using Main.Services;
using Main.Services.Repositories;
using Main.Services.GenAI;
using Microsoft.Extensions.DependencyInjection;
using Main.Models.Enums;
using Microsoft.Extensions.Logging;

namespace MainTests.ServicesTests;

/// <summary>
/// Tests for SourceDocumentService.
/// Verifies service behavior per AdditionalValidationRules.md §3A.
/// Cascade deletion is handled by database FK constraints per PersistenceAndCascadingRules.md §2(3A).
/// </summary>
public class SourceDocumentServiceTests
{
    private readonly Mock<ISourceDocumentRepository> _mockSourceDocumentRepo;
    private readonly Mock<ILogger<SourceDocumentService>> _mockLogger;
    private readonly Mock<IUnitCollectionRepository> _mockUnitCollectionRepo;
    private readonly Mock<IEmbeddingService> _mockEmbeddingService;
    private readonly Mock<IServiceScopeFactory> _mockScopeFactory;
    private readonly Mock<IServiceScope> _mockScope;
    private readonly Mock<IServiceProvider> _mockScopeServiceProvider;
    private readonly SourceDocumentService _service;

    public SourceDocumentServiceTests()
    {
        _mockSourceDocumentRepo = new Mock<ISourceDocumentRepository>();
        _mockLogger = new Mock<ILogger<SourceDocumentService>>();
        _mockUnitCollectionRepo = new Mock<IUnitCollectionRepository>();
        _mockEmbeddingService = new Mock<IEmbeddingService>();
        _mockEmbeddingService
            .Setup(s => s.IndexSourceDocumentAsync(It.IsAny<SourceDocumentEntity>()))
            .Returns(Task.CompletedTask);
        _mockEmbeddingService
            .Setup(s => s.IndexSourceDocumentByIdAsync(It.IsAny<Guid>()))
            .Returns(Task.CompletedTask);

        // Set up IServiceScopeFactory to return a scope with a fresh IEmbeddingService
        _mockScopeServiceProvider = new Mock<IServiceProvider>();
        _mockScopeServiceProvider
            .Setup(sp => sp.GetService(typeof(IEmbeddingService)))
            .Returns(_mockEmbeddingService.Object);

        _mockScope = new Mock<IServiceScope>();
        _mockScope.Setup(s => s.ServiceProvider).Returns(_mockScopeServiceProvider.Object);

        _mockScopeFactory = new Mock<IServiceScopeFactory>();
        _mockScopeFactory.Setup(f => f.CreateScope()).Returns(_mockScope.Object);

        _service = new SourceDocumentService(
            _mockSourceDocumentRepo.Object,
            _mockUnitCollectionRepo.Object,
            _mockEmbeddingService.Object,
            _mockScopeFactory.Object,
            _mockLogger.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullSourceDocumentRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new SourceDocumentService(null!, _mockUnitCollectionRepo.Object, _mockEmbeddingService.Object, _mockScopeFactory.Object, _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullUnitCollectionRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new SourceDocumentService(_mockSourceDocumentRepo.Object, null!, _mockEmbeddingService.Object, _mockScopeFactory.Object, _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullServiceScopeFactory_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new SourceDocumentService(_mockSourceDocumentRepo.Object, _mockUnitCollectionRepo.Object, _mockEmbeddingService.Object, null!, _mockLogger.Object));
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

    [Fact]
    public async Task CreateAsync_ValidSourceDocument_TriggersBackgroundIndexing()
    {
        // Arrange - §3A(1): When a SourceDocumentEntity is created, index it for semantic retrieval
        var unitCollectionId = Guid.NewGuid();
        var sourceDocument = new SourceDocumentEntity(Guid.NewGuid(), unitCollectionId, "Sample transcript");
        
        _mockUnitCollectionRepo.Setup(r => r.GetByIdAsync(unitCollectionId))
            .ReturnsAsync(new UnitCollectionEntity(unitCollectionId, "Test Collection"));
        _mockSourceDocumentRepo.Setup(r => r.AddAsync(It.IsAny<SourceDocumentEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        await _service.CreateAsync(sourceDocument);

        // Allow background task to execute
        await Task.Delay(200);

        // Assert - background task should create a new scope and call IndexSourceDocumentByIdAsync
        _mockScopeFactory.Verify(f => f.CreateScope(), Times.Once);
        _mockEmbeddingService.Verify(
            s => s.IndexSourceDocumentByIdAsync(sourceDocument.Id), Times.Once);
    }

    [Fact]
    public async Task CreateAsync_SetsEmbeddingStatusToPendingBeforeSaving()
    {
        // Arrange - FrontendWorkflowSpec §4AA(2)(e): The entity returned to the
        // frontend must have PENDING status so that the polling mechanism activates.
        var unitCollectionId = Guid.NewGuid();
        var sourceDocument = new SourceDocumentEntity(Guid.NewGuid(), unitCollectionId, "Sample transcript");

        _mockUnitCollectionRepo.Setup(r => r.GetByIdAsync(unitCollectionId))
            .ReturnsAsync(new UnitCollectionEntity(unitCollectionId, "Test Collection"));

        EmbeddingStatus? statusAtSaveTime = null;
        _mockSourceDocumentRepo.Setup(r => r.AddAsync(It.IsAny<SourceDocumentEntity>()))
            .Callback<SourceDocumentEntity>(e => statusAtSaveTime = e.EmbeddingStatus)
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateAsync(sourceDocument);

        // Assert - status must be PENDING both when saved and on the returned entity
        Assert.Equal(EmbeddingStatus.PENDING, statusAtSaveTime);
        Assert.Equal(EmbeddingStatus.PENDING, result.EmbeddingStatus);
    }

    #endregion

    #region UpdateAsync Tests

    [Fact]
    public async Task UpdateAsync_NullEntity_ThrowsArgumentNullException()
    {
        // Act & Assert
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => _service.UpdateAsync(null!));
    }

    [Fact]
    public async Task UpdateAsync_NonExistentUnitCollectionId_ThrowsInvalidOperationException()
    {
        // Arrange - §3A(2)(a): UnitCollectionId must reference a valid UnitCollectionEntity
        var entity = new SourceDocumentEntity(Guid.NewGuid(), Guid.NewGuid(), "Transcript");

        _mockUnitCollectionRepo.Setup(r => r.GetByIdAsync(entity.UnitCollectionId))
            .ReturnsAsync((UnitCollectionEntity?)null);

        // Act & Assert
        await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.UpdateAsync(entity));
    }

    [Fact]
    public async Task UpdateAsync_ValidEntity_CallsRepositoryAndReIndex()
    {
        // Arrange - §3A(3): After update, re-index the document
        var unitCollectionId = Guid.NewGuid();
        var entity = new SourceDocumentEntity(Guid.NewGuid(), unitCollectionId, "Updated transcript");

        _mockUnitCollectionRepo.Setup(r => r.GetByIdAsync(unitCollectionId))
            .ReturnsAsync(new UnitCollectionEntity(unitCollectionId, "Test Collection"));
        _mockSourceDocumentRepo.Setup(r => r.UpdateAsync(entity))
            .Returns(Task.CompletedTask);
        _mockEmbeddingService.Setup(s => s.ReIndexSourceDocumentAsync(entity))
            .Returns(Task.CompletedTask);

        // Act
        await _service.UpdateAsync(entity);

        // Assert
        _mockSourceDocumentRepo.Verify(r => r.UpdateAsync(entity), Times.Once);
        _mockEmbeddingService.Verify(s => s.ReIndexSourceDocumentAsync(entity), Times.Once);
    }

    #endregion

    #region DeleteAsync Tests
    [Fact]
    public async Task DeleteAsync_CallsRepositoryAndEmbeddingRemoval()
    {
        // Arrange
        // Cascade deletion is handled by database FK constraints
        // per PersistenceAndCascadingRules.md §2(3A)
        var sourceDocumentId = Guid.NewGuid();
        _mockSourceDocumentRepo.Setup(r => r.DeleteAsync(sourceDocumentId))
            .Returns(Task.CompletedTask);
        _mockEmbeddingService.Setup(s => s.RemoveSourceDocumentAsync(sourceDocumentId))
            .Returns(Task.CompletedTask);

        // Act
        await _service.DeleteAsync(sourceDocumentId);

        // Assert
        _mockSourceDocumentRepo.Verify(r => r.DeleteAsync(sourceDocumentId), Times.Once);
        _mockEmbeddingService.Verify(s => s.RemoveSourceDocumentAsync(sourceDocumentId), Times.Once);
    }

    #endregion
}
