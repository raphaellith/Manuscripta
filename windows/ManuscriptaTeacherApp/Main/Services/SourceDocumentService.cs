using System;
using System.Threading.Tasks;
using Main.Models.Entities;
using Main.Services.Repositories;
using Main.Services.GenAI;

namespace Main.Services;

/// <summary>
/// Service for managing source documents.
/// Enforces validation rules per AdditionalValidationRules.md §3A.
/// Cascade deletion is handled by database FK constraints per PersistenceAndCascadingRules.md §2(3A).
/// Triggers AI indexing per GenAISpec.md §3A(1).
/// </summary>
public class SourceDocumentService : ISourceDocumentService
{
    private readonly ISourceDocumentRepository _repository;
    private readonly IUnitCollectionRepository _unitCollectionRepository;
    private readonly DocumentEmbeddingService _embeddingService;

    public SourceDocumentService(
        ISourceDocumentRepository repository,
        IUnitCollectionRepository unitCollectionRepository,
        DocumentEmbeddingService embeddingService)
    {
        _repository = repository ?? throw new ArgumentNullException(nameof(repository));
        _unitCollectionRepository = unitCollectionRepository ?? throw new ArgumentNullException(nameof(unitCollectionRepository));
        _embeddingService = embeddingService ?? throw new ArgumentNullException(nameof(embeddingService));
    }

    public async Task<SourceDocumentEntity> CreateAsync(SourceDocumentEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        await ValidateEntityAsync(entity);
        await _repository.AddAsync(entity);
        
        // §3A(1): When a SourceDocumentEntity is created, index it for semantic retrieval
        // This runs asynchronously in the background
        _ = _embeddingService.IndexSourceDocumentAsync(entity);
        
        return entity;
    }

    public async Task UpdateAsync(SourceDocumentEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        await ValidateEntityAsync(entity);
        
        // §3A(3): When a SourceDocumentEntity is updated, re-index it
        // This removes old chunks and creates new ones
        await _embeddingService.ReIndexSourceDocumentAsync(entity);
        
        await _repository.UpdateAsync(entity);
    }

    public async Task DeleteAsync(Guid id)
    {
        // §3A(4): When a SourceDocumentEntity is deleted, remove its chunks from ChromaDB
        await _embeddingService.RemoveSourceDocumentAsync(id);
        await _repository.DeleteAsync(id);
    }

    private async Task ValidateEntityAsync(SourceDocumentEntity entity)
    {
        // §3A(2)(a): UnitCollectionId must reference a valid UnitCollectionEntity
        var unitCollection = await _unitCollectionRepository.GetByIdAsync(entity.UnitCollectionId);
        if (unitCollection == null)
            throw new InvalidOperationException($"UnitCollection with ID {entity.UnitCollectionId} not found.");

        // §3A(1)(b): Transcript is required
        if (entity.Transcript == null)
            throw new ArgumentException("Transcript cannot be null.", nameof(entity));
    }
}
