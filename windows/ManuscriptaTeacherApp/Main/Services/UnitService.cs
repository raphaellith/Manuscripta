using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities;
using Main.Services.Repositories;

namespace Main.Services;

/// <summary>
/// Service for managing units.
/// Enforces business rules and handles cascade deletion per PersistenceAndCascadingRules.md §2(4).
/// </summary>
public class UnitService : IUnitService
{
    private readonly IUnitRepository _repository;
    private readonly IUnitCollectionRepository _unitCollectionRepository;
    private readonly ILessonRepository _lessonRepository;

    public UnitService(
        IUnitRepository repository,
        IUnitCollectionRepository unitCollectionRepository,
        ILessonRepository lessonRepository)
    {
        _repository = repository ?? throw new ArgumentNullException(nameof(repository));
        _unitCollectionRepository = unitCollectionRepository ?? throw new ArgumentNullException(nameof(unitCollectionRepository));
        _lessonRepository = lessonRepository ?? throw new ArgumentNullException(nameof(lessonRepository));
    }

    public async Task<UnitEntity> CreateAsync(UnitEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        await ValidateEntityAsync(entity);
        await _repository.AddAsync(entity);
        return entity;
    }

    public async Task<UnitEntity> UpdateAsync(UnitEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        await ValidateEntityAsync(entity);

        var existing = await _repository.GetByIdAsync(entity.Id);
        if (existing == null)
            throw new InvalidOperationException($"Unit with ID {entity.Id} not found.");

        await _repository.UpdateAsync(entity);
        return entity;
    }

    public async Task DeleteAsync(Guid id)
    {
        // Per PersistenceAndCascadingRules.md §2(4): Delete associated lessons first
        var lessons = await _lessonRepository.GetByUnitIdAsync(id);
        foreach (var lesson in lessons)
        {
            await _lessonRepository.DeleteAsync(lesson.Id);
        }

        await _repository.DeleteAsync(id);
    }

    private async Task ValidateEntityAsync(UnitEntity entity)
    {
        if (string.IsNullOrWhiteSpace(entity.Title))
            throw new ArgumentException("Title cannot be empty.", nameof(entity));

        if (entity.Title.Length > 500)
            throw new ArgumentException("Title cannot exceed 500 characters.", nameof(entity));

        // Validate UnitCollectionId references a valid UnitCollection
        var unitCollection = await _unitCollectionRepository.GetByIdAsync(entity.UnitCollectionId);
        if (unitCollection == null)
            throw new InvalidOperationException($"UnitCollection with ID {entity.UnitCollectionId} not found.");
    }
}
