using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities;
using Main.Services.Repositories;

namespace Main.Services;

/// <summary>
/// Service for managing unit collections.
/// Enforces business rules and handles cascade deletion per PersistenceAndCascadingRules.md §2(3).
/// </summary>
public class UnitCollectionService : IUnitCollectionService
{
    private readonly IUnitCollectionRepository _repository;
    private readonly IUnitRepository _unitRepository;

    public UnitCollectionService(
        IUnitCollectionRepository repository,
        IUnitRepository unitRepository)
    {
        _repository = repository ?? throw new ArgumentNullException(nameof(repository));
        _unitRepository = unitRepository ?? throw new ArgumentNullException(nameof(unitRepository));
    }

    public async Task<UnitCollectionEntity> CreateAsync(UnitCollectionEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        ValidateEntity(entity);
        await _repository.AddAsync(entity);
        return entity;
    }

    public async Task<UnitCollectionEntity?> GetByIdAsync(Guid id)
    {
        return await _repository.GetByIdAsync(id);
    }

    public async Task<IEnumerable<UnitCollectionEntity>> GetAllAsync()
    {
        return await _repository.GetAllAsync();
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
        // Per PersistenceAndCascadingRules.md §2(3): Delete associated units first
        // Note: Database cascade delete will handle this automatically,
        // but we explicitly delete here for consistency with service-layer cascade pattern
        var units = await _unitRepository.GetByUnitCollectionIdAsync(id);
        foreach (var unit in units)
        {
            await _unitRepository.DeleteAsync(unit.Id);
        }

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
