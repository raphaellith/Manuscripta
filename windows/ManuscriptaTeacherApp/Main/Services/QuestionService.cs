using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities.Questions;
using Main.Models.Entities.Materials;
using Main.Models.Enums;
using Main.Services.Repositories;

namespace Main.Services;

/// <summary>
/// Service for managing questions.
/// Enforces business rules and data validation.
/// </summary>
public class QuestionService : IQuestionService
{
    private readonly IQuestionRepository _questionRepository;
    private readonly IMaterialRepository _materialRepository;
    private readonly IResponseRepository _responseRepository;

    public QuestionService(
        IQuestionRepository questionRepository, 
        IMaterialRepository materialRepository,
        IResponseRepository responseRepository)
    {
        _questionRepository = questionRepository ?? throw new ArgumentNullException(nameof(questionRepository));
        _materialRepository = materialRepository ?? throw new ArgumentNullException(nameof(materialRepository));
        _responseRepository = responseRepository ?? throw new ArgumentNullException(nameof(responseRepository));
    }

    public async Task<QuestionEntity> CreateQuestionAsync(QuestionEntity question)
    {
        if (question == null)
            throw new ArgumentNullException(nameof(question));

        // Validate question
        await ValidateQuestionAsync(question);

        await _questionRepository.AddAsync(question);
        return question;
    }

    public async Task<QuestionEntity?> GetQuestionByIdAsync(Guid id)
    {
        return await _questionRepository.GetByIdAsync(id);
    }

    public async Task<IEnumerable<QuestionEntity>> GetQuestionsByMaterialIdAsync(Guid materialId)
    {
        return await _questionRepository.GetByMaterialIdAsync(materialId);
    }

    public async Task<QuestionEntity> UpdateQuestionAsync(QuestionEntity question)
    {
        if (question == null)
            throw new ArgumentNullException(nameof(question));

        // Validate question
        await ValidateQuestionAsync(question);

        // Ensure question exists
        var existing = await _questionRepository.GetByIdAsync(question.Id);
        if (existing == null)
            throw new InvalidOperationException($"Question with ID {question.Id} not found.");

        await _questionRepository.UpdateAsync(question);
        return question;
    }

    public async Task DeleteQuestionAsync(Guid id)
    {
        // Per PersistenceAndCascadingRules.md §2(2): A deletion of a question Q must delete any responses associated with Q.
        await _responseRepository.DeleteByQuestionIdAsync(id);
        await _questionRepository.DeleteAsync(id);
    }

    #region Validation

    /// <summary>
    /// Validates a question according to business rules:
    /// - 2B(3)(a): Questions must reference a Material which is not a reading material
    /// - 2B(3)(b): Written Questions must not be associated with Polls and Quizzes
    /// </summary>
    private async Task ValidateQuestionAsync(QuestionEntity question)
    {
        if (string.IsNullOrWhiteSpace(question.QuestionText))
            throw new ArgumentException("Question text cannot be empty.", nameof(question));

        // Rule 2B(3)(a): Questions must reference a Material which is not a reading material
        var material = await _materialRepository.GetByIdAsync(question.MaterialId);
        if (material == null)
            throw new InvalidOperationException($"Material with ID {question.MaterialId} not found.");

        if (material.MaterialType == MaterialType.READING)
            throw new InvalidOperationException("Questions cannot be associated with reading materials.");

        // Rule 2B(3)(b): Written Questions must not be associated with Polls and Quizzes
        if (question is WrittenAnswerQuestionEntity)
        {
            if (material.MaterialType == MaterialType.POLL)
                throw new InvalidOperationException("Written answer questions cannot be associated with polls.");

            if (material.MaterialType == MaterialType.QUIZ)
                throw new InvalidOperationException("Written answer questions cannot be associated with quizzes.");
        }
    }

    #endregion
}

