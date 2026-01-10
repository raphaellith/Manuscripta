using System;
using System.Threading.Tasks;
using Main.Models.Entities;
using Main.Services.Repositories;

namespace Main.Services;

/// <summary>
/// Service for managing source documents.
/// Enforces validation rules per AdditionalValidationRules.md §3A.
/// Cascade deletion is handled by database FK constraints per PersistenceAndCascadingRules.md §2(3A).
/// </summary>
public class SourceDocumentService : ISourceDocumentService
{
    private readonly ISourceDocumentRepository _repository;
    private readonly IUnitCollectionRepository _unitCollectionRepository;

    public SourceDocumentService(
        ISourceDocumentRepository repository,
        IUnitCollectionRepository unitCollectionRepository)
    {
        _repository = repository ?? throw new ArgumentNullException(nameof(repository));
        _unitCollectionRepository = unitCollectionRepository ?? throw new ArgumentNullException(nameof(unitCollectionRepository));
    }

    public async Task<SourceDocumentEntity> CreateAsync(SourceDocumentEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        await ValidateEntityAsync(entity);
        await _repository.AddAsync(entity);
        return entity;
    }

    public async Task DeleteAsync(Guid id)
    {
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
