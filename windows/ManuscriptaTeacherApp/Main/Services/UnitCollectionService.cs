using System;
using System.Threading.Tasks;
using Main.Models.Entities;
using Main.Services.Repositories;

namespace Main.Services;

/// <summary>
/// Service for managing unit collections.
/// Enforces business rules per AdditionalValidationRules.md §2A.
/// Cascade deletion of units is handled by database FK constraints per PersistenceAndCascadingRules.md §2(3).
/// </summary>
public class UnitCollectionService : IUnitCollectionService
{
    private readonly IUnitCollectionRepository _repository;

    public UnitCollectionService(IUnitCollectionRepository repository)
    {
        _repository = repository ?? throw new ArgumentNullException(nameof(repository));
    }

    public async Task<UnitCollectionEntity> CreateAsync(UnitCollectionEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        ValidateEntity(entity);
        await _repository.AddAsync(entity);
        return entity;
    }

    public async Task<UnitCollectionEntity> UpdateAsync(UnitCollectionEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        ValidateEntity(entity);

        var existing = await _repository.GetByIdAsync(entity.Id);
        if (existing == null)
            throw new InvalidOperationException($"UnitCollection with ID {entity.Id} not found.");

        await _repository.UpdateAsync(entity);
        return entity;
    }

    public async Task DeleteAsync(Guid id)
    {
        // Cascade deletion of units is handled by database FK constraints
        // per PersistenceAndCascadingRules.md §2(3)
        await _repository.DeleteAsync(id);
    }

    private void ValidateEntity(UnitCollectionEntity entity)
    {
        if (string.IsNullOrWhiteSpace(entity.Title))
            throw new ArgumentException("Title cannot be empty.", nameof(entity));

        if (entity.Title.Length > 500)
            throw new ArgumentException("Title cannot exceed 500 characters.", nameof(entity));
    }
}
