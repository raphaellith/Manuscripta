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

    public MaterialService(IMaterialRepository materialRepository)
    {
        _materialRepository = materialRepository ?? throw new ArgumentNullException(nameof(materialRepository));
    }

    #region Material Operations

    public async Task<MaterialEntity> CreateMaterialAsync(MaterialEntity material)
    {
        if (material == null)
            throw new ArgumentNullException(nameof(material));

        // Validate material
        ValidateMaterial(material);

        await _materialRepository.AddAsync(material);
        return material;
    }

    public async Task<MaterialEntity> UpdateMaterialAsync(MaterialEntity material)
    {
        if (material == null)
            throw new ArgumentNullException(nameof(material));

        // Validate material
        ValidateMaterial(material);

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

    private void ValidateMaterial(MaterialEntity material)
    {
        if (string.IsNullOrWhiteSpace(material.Title))
            throw new ArgumentException("Material title cannot be empty.", nameof(material));

        // Ensure the material is associated with a lesson.
        // Existence of the referenced lesson is enforced by database foreign key constraints;
        // this check prevents obviously invalid IDs and yields clearer validation errors.
        if (material.LessonId == Guid.Empty)
            throw new ArgumentException("Material must reference a valid lesson.", nameof(material));
    }
    #endregion
}