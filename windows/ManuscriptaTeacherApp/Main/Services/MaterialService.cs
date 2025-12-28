using System;

using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities.Materials;
using Main.Models.Entities.Questions;
using Main.Models.Enums;
using Main.Services.Repositories;

namespace Main.Services;

/// <summary>
/// Service for managing materials and their associated questions.
/// Enforces business rules and data validation.
/// </summary>
public class MaterialService : IMaterialService
{
    private readonly IMaterialRepository _materialRepository;
    private readonly IQuestionRepository _questionRepository;

    public MaterialService(IMaterialRepository materialRepository, IQuestionRepository questionRepository)
    {
        _materialRepository = materialRepository ?? throw new ArgumentNullException(nameof(materialRepository));
        _questionRepository = questionRepository ?? throw new ArgumentNullException(nameof(questionRepository));
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
        // Get all questions associated with this material
        var questions = await _questionRepository.GetByMaterialIdAsync(id);

        // Delete all associated questions first
        foreach (var question in questions)
        {
            await _questionRepository.DeleteAsync(question.Id);
        }

        // Delete the material
        await _materialRepository.DeleteAsync(id);
    }

    #endregion

    #region Validation

    private void ValidateMaterial(MaterialEntity material)
    {
        if (string.IsNullOrWhiteSpace(material.Title))
            throw new ArgumentException("Material title cannot be empty.", nameof(material));

        if (string.IsNullOrWhiteSpace(material.Content))
            throw new ArgumentException("Material content cannot be empty.", nameof(material));
    }
    #endregion
}