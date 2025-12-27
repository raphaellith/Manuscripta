using System;

using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities;
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
    private readonly ILessonRepository _lessonRepository;

    public MaterialService(
        IMaterialRepository materialRepository, 
        IQuestionRepository questionRepository,
        ILessonRepository lessonRepository)
    {
        _materialRepository = materialRepository ?? throw new ArgumentNullException(nameof(materialRepository));
        _questionRepository = questionRepository ?? throw new ArgumentNullException(nameof(questionRepository));
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

    public async Task<MaterialEntity?> GetMaterialByIdAsync(Guid id)
    {
        return await _materialRepository.GetByIdAsync(id);
    }

    public async Task<IEnumerable<MaterialEntity>> GetAllMaterialsAsync()
    {
        return await _materialRepository.GetAllAsync();
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

    private async Task ValidateMaterialAsync(MaterialEntity material)
    {
        if (string.IsNullOrWhiteSpace(material.Title))
            throw new ArgumentException("Material title cannot be empty.", nameof(material));

        if (string.IsNullOrWhiteSpace(material.Content))
            throw new ArgumentException("Material content cannot be empty.", nameof(material));

        // Validate LessonId references a valid Lesson
        // Per AdditionalValidationRules.md §2D(1)(a)
        var lesson = await _lessonRepository.GetByIdAsync(material.LessonId);
        if (lesson == null)
            throw new InvalidOperationException($"Lesson with ID {material.LessonId} not found.");
    }
    #endregion
}