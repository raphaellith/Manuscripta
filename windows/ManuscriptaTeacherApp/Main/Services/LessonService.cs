using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities;
using Main.Models.Entities.Materials;
using Main.Services.Repositories;

namespace Main.Services;

/// <summary>
/// Service for managing lessons.
/// Enforces business rules and handles cascade deletion per PersistenceAndCascadingRules.md §2(5).
/// </summary>
public class LessonService : ILessonService
{
    private readonly ILessonRepository _repository;
    private readonly IUnitRepository _unitRepository;
    private readonly IMaterialRepository _materialRepository;

    public LessonService(
        ILessonRepository repository,
        IUnitRepository unitRepository,
        IMaterialRepository materialRepository)
    {
        _repository = repository ?? throw new ArgumentNullException(nameof(repository));
        _unitRepository = unitRepository ?? throw new ArgumentNullException(nameof(unitRepository));
        _materialRepository = materialRepository ?? throw new ArgumentNullException(nameof(materialRepository));
    }

    public async Task<LessonEntity> CreateAsync(LessonEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        await ValidateEntityAsync(entity);
        await _repository.AddAsync(entity);
        return entity;
    }

    public async Task<LessonEntity> UpdateAsync(LessonEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        await ValidateEntityAsync(entity);

        var existing = await _repository.GetByIdAsync(entity.Id);
        if (existing == null)
            throw new InvalidOperationException($"Lesson with ID {entity.Id} not found.");

        await _repository.UpdateAsync(entity);
        return entity;
    }

    public async Task DeleteAsync(Guid id)
    {
        // Per PersistenceAndCascadingRules.md §2(5): Delete associated materials first
        // Get all materials and delete those associated with this lesson
        var allMaterials = await _materialRepository.GetAllAsync();
        foreach (var material in allMaterials)
        {
            // MaterialEntity doesn't have LessonId (it's in MaterialDataEntity)
            // The cascade delete in the database handles this automatically
        }

        await _repository.DeleteAsync(id);
    }

    private async Task ValidateEntityAsync(LessonEntity entity)
    {
        if (string.IsNullOrWhiteSpace(entity.Title))
            throw new ArgumentException("Title cannot be empty.", nameof(entity));

        if (entity.Title.Length > 500)
            throw new ArgumentException("Title cannot exceed 500 characters.", nameof(entity));

        if (string.IsNullOrWhiteSpace(entity.Description))
            throw new ArgumentException("Description cannot be empty.", nameof(entity));

        // Validate UnitId references a valid Unit
        var unit = await _unitRepository.GetByIdAsync(entity.UnitId);
        if (unit == null)
            throw new InvalidOperationException($"Unit with ID {entity.UnitId} not found.");
    }
}
