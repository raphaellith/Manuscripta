using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities.Responses;
using Main.Models.Entities.Questions;
using Main.Services.Repositories;

namespace Main.Services;

/// <summary>
/// Service for managing responses.
/// Enforces business rules and data validation.
/// </summary>
public class ResponseService : IResponseService
{
    private readonly IResponseRepository _responseRepository;
    private readonly IQuestionRepository _questionRepository;

    public ResponseService(IResponseRepository responseRepository, IQuestionRepository questionRepository)
    {
        _responseRepository = responseRepository ?? throw new ArgumentNullException(nameof(responseRepository));
        _questionRepository = questionRepository ?? throw new ArgumentNullException(nameof(questionRepository));
    }

    public async Task<ResponseEntity> CreateResponseAsync(ResponseEntity response)
    {
        if (response == null)
            throw new ArgumentNullException(nameof(response));

        // Validate response
        await ValidateResponseAsync(response);

        await _responseRepository.AddAsync(response);
        return response;
    }

    public async Task<ResponseEntity?> GetResponseByIdAsync(Guid id)
    {
        return await _responseRepository.GetByIdAsync(id);
    }

    public async Task<IEnumerable<ResponseEntity>> GetResponsesByQuestionIdAsync(Guid questionId)
    {
        return await _responseRepository.GetByQuestionIdAsync(questionId);
    }

    public async Task<ResponseEntity> UpdateResponseAsync(ResponseEntity response)
    {
        if (response == null)
            throw new ArgumentNullException(nameof(response));

        // Validate response
        await ValidateResponseAsync(response);

        // Ensure response exists
        var existing = await _responseRepository.GetByIdAsync(response.Id);
        if (existing == null)
            throw new InvalidOperationException($"Response with ID {response.Id} not found.");

        await _responseRepository.UpdateAsync(response);
        return response;
    }

    public async Task DeleteResponseAsync(Guid id)
    {
        await _responseRepository.DeleteAsync(id);
    }

    #region Validation

    /// <summary>
    /// Validates a response according to business rules:
    /// - 2C(3)(a): Responses must reference a Question
    /// - 2C(3)(b): Answer of a Multiple-Choice response must be a valid index
    /// </summary>
    private async Task ValidateResponseAsync(ResponseEntity response)
    {
        // Rule 2C(3)(a): Responses must reference a Question
        var question = await _questionRepository.GetByIdAsync(response.QuestionId);
        if (question == null)
            throw new InvalidOperationException($"Question with ID {response.QuestionId} not found.");

        // Rule 2C(3)(b): Answer of a Multiple-Choice response must be a valid index
        if (response is MultipleChoiceResponseEntity mcResponse && question is MultipleChoiceQuestionEntity mcQuestion)
        {
            if (mcResponse.AnswerIndex < 0 || mcResponse.AnswerIndex >= mcQuestion.Options.Count)
                throw new InvalidOperationException(
                    $"Answer index {mcResponse.AnswerIndex} is out of range. " +
                    $"Valid range is 0 to {mcQuestion.Options.Count - 1}.");
        }

        // Additional validation: ensure response type matches question type
        ValidateResponseTypeMatchesQuestion(response, question);
    }

    /// <summary>
    /// Validates that the response type matches the question type.
    /// </summary>
    private void ValidateResponseTypeMatchesQuestion(ResponseEntity response, QuestionEntity question)
    {
        var isValid = (response, question) switch
        {
            (MultipleChoiceResponseEntity, MultipleChoiceQuestionEntity) => true,
            (TrueFalseResponseEntity, TrueFalseQuestionEntity) => true,
            (WrittenAnswerResponseEntity, WrittenAnswerQuestionEntity) => true,
            _ => false
        };

        if (!isValid)
        {
            throw new InvalidOperationException(
                $"Response type {response.GetType().Name} does not match question type {question.GetType().Name}.");
        }
    }

    #endregion
}
