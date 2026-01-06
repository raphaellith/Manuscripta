using System;
using System.Threading.Tasks;
using Main.Models.Entities;
using Main.Models.Entities.Materials;
using Main.Services.Repositories;

namespace Main.Services;

/// <summary>
/// Service for managing materials.
/// Enforces business rules and data validation per AdditionalValidationRules.md §2D.
/// Cascade deletion of questions is handled by database FK constraints per PersistenceAndCascadingRules.md §2(1).
/// </summary>
public class MaterialService : IMaterialService
{
    private readonly IMaterialRepository _materialRepository;
    private readonly ILessonRepository _lessonRepository;

    public MaterialService(
        IMaterialRepository materialRepository, 
        ILessonRepository lessonRepository)
    {
        _materialRepository = materialRepository ?? throw new ArgumentNullException(nameof(materialRepository));
        _lessonRepository = lessonRepository ?? throw new ArgumentNullException(nameof(lessonRepository));
    }

    #region Material Operations

    public async Task<MaterialEntity> CreateMaterialAsync(MaterialEntity material)
    {
        if (material == null)
            throw new ArgumentNullException(nameof(material));

        // Validate material
        await ValidateMaterialAsync(material);

        await _materialRepository.AddAsync(material);
        return material;
    }

    public async Task<MaterialEntity> UpdateMaterialAsync(MaterialEntity material)
    {
        if (material == null)
            throw new ArgumentNullException(nameof(material));

        // Validate material
        await ValidateMaterialAsync(material);

        // Ensure material exists
        var existing = await _materialRepository.GetByIdAsync(material.Id);
        if (existing == null)
            throw new InvalidOperationException($"Material with ID {material.Id} not found.");

        await _materialRepository.UpdateAsync(material);
        return material;
    }

    public async Task DeleteMaterialAsync(Guid id)
    {
        // Cascade deletion of questions is handled by database FK constraints
        // per PersistenceAndCascadingRules.md §2(1)
        await _materialRepository.DeleteAsync(id);
    }

    #endregion

    #region Validation

    private async Task ValidateMaterialAsync(MaterialEntity material)
    {
        if (string.IsNullOrWhiteSpace(material.Title))
            throw new ArgumentException("Material title cannot be empty.", nameof(material));

        // Content is allowed to be empty at creation - will be filled in via editor

        // Validate LessonId references a valid Lesson
        // Per AdditionalValidationRules.md §2D(1)(a)
        var lesson = await _lessonRepository.GetByIdAsync(material.LessonId);
        if (lesson == null)
            throw new InvalidOperationException($"Lesson with ID {material.LessonId} not found.");
    }
    #endregion
}