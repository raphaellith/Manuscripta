using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities.Responses;
using Main.Models.Entities.Questions;
using Main.Models.Dtos;
using System.Globalization;
using Main.Services.Repositories;

namespace Main.Services;

/// <summary>
/// Service for managing responses.
/// Enforces business rules and data validation per Validation Rules §2C.
/// </summary>
public class ResponseService : IResponseService
{
    private readonly IResponseRepository _responseRepository;
    private readonly IQuestionRepository _questionRepository;
    private readonly IFeedbackRepository _feedbackRepository;
    private readonly DeviceIdValidator _deviceIdValidator;

    public ResponseService(
        IResponseRepository responseRepository, 
        IQuestionRepository questionRepository,
        IFeedbackRepository feedbackRepository,
        DeviceIdValidator deviceIdValidator)
    {
        _responseRepository = responseRepository ?? throw new ArgumentNullException(nameof(responseRepository));
        _questionRepository = questionRepository ?? throw new ArgumentNullException(nameof(questionRepository));
        _feedbackRepository = feedbackRepository ?? throw new ArgumentNullException(nameof(feedbackRepository));
        _deviceIdValidator = deviceIdValidator ?? throw new ArgumentNullException(nameof(deviceIdValidator));
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

    /// <summary>
    /// Creates a response from a DTO, handling type determination and validation internally.
    /// Optimizes DB access by fetching the question only once.
    /// </summary>
    public async Task<ResponseEntity> CreateResponseAsync(SubmitResponseDto dto)
    {
        // 1. Parse and Create Entity (includes fetching question and basic validation)
        // Pass 'validateBusinessRules: true' to perform full validation immediately
        var (response, _) = await ParseDtoToEntityAsync(dto, validateBusinessRules: true);

        // 2. Persist
        await _responseRepository.AddAsync(response);
        return response;
    }

    /// <summary>
    /// Creates a batch of responses from DTOs, strictly enforcing Validation Rules §1A(2) (All-or-Nothing).
    /// </summary>
    public async Task CreateBatchResponsesAsync(IEnumerable<SubmitResponseDto> dtos)
    {
        if (dtos == null) throw new ArgumentNullException(nameof(dtos));

        var validatedEntities = new List<ResponseEntity>();

        // Pass 1: Parse and Validate ALL responses
        foreach (var dto in dtos)
        {
            // Parse and perform full validation (fetching question once)
            var (response, _) = await ParseDtoToEntityAsync(dto, validateBusinessRules: true);
            validatedEntities.Add(response);
        }

        // Pass 2: Persist ALL responses
        foreach (var entity in validatedEntities)
        {
            await _responseRepository.AddAsync(entity);
        }
    }

    /// <summary>
    /// Helper to parse DTO to Entity.
    /// Fetches the question once.
    /// Optionally performs business rule validation (ValidateResponseInternal).
    /// </summary>
    private async Task<(ResponseEntity Entity, QuestionEntity Question)> ParseDtoToEntityAsync(SubmitResponseDto dto, bool validateBusinessRules)
    {
        // Validate GUID formats
        if (!Guid.TryParse(dto.Id, out var responseId))
            throw new FormatException("Id must be a valid GUID");
        
        if (!Guid.TryParse(dto.QuestionId, out var questionId))
            throw new FormatException("QuestionId must be a valid GUID");
        
        if (!Guid.TryParse(dto.DeviceId, out var deviceId))
            throw new FormatException("DeviceId must be a valid GUID");

        // Parse timestamp
        if (!DateTime.TryParse(dto.Timestamp, CultureInfo.InvariantCulture, DateTimeStyles.RoundtripKind, out var timestamp))
            throw new FormatException("Timestamp must be a valid ISO 8601 date-time string");

        // Fetch Question (ONCE)
        var question = await _questionRepository.GetByIdAsync(questionId);
        if (question == null)
            throw new InvalidOperationException($"Question with ID {questionId} not found");

        // Create Entity
        ResponseEntity response = question switch
        {
            MultipleChoiceQuestionEntity => CreateMultipleChoiceResponse(responseId, questionId, deviceId, dto.Answer, timestamp, dto.IsCorrect),
            WrittenAnswerQuestionEntity => new WrittenAnswerResponseEntity(responseId, questionId, deviceId, dto.Answer, timestamp, dto.IsCorrect),
            _ => throw new InvalidOperationException($"Unsupported question type: {question.GetType().Name}")
        };

        // Validate Business Rules if requested (using the already fetched question)
        if (validateBusinessRules)
        {
            await ValidateResponseInternalAsync(response, question);
        }

        return (response, question);
    }

    private static MultipleChoiceResponseEntity CreateMultipleChoiceResponse(
        Guid responseId, Guid questionId, Guid deviceId, string answer, DateTime timestamp, bool? isCorrect)
    {
        if (!int.TryParse(answer, out var answerIndex))
            throw new FormatException("Answer must be a valid integer index for multiple choice questions");

        return new MultipleChoiceResponseEntity(responseId, questionId, deviceId, answerIndex, timestamp, isCorrect);
    }



    /// <summary>
    /// Deletes a response and its associated feedback.
    /// Per PersistenceAndCascadingRules.md §2(2A): The deletion of a response R must delete any feedback associated with R.
    /// </summary>
    public async Task DeleteResponseAsync(Guid id)
    {
        await _feedbackRepository.DeleteByResponseIdAsync(id);
        await _responseRepository.DeleteAsync(id);
    }

    #region Validation

    /// <summary>
    /// Validates a response according to business rules:
    /// - §2C(3)(a): Responses must reference a Question
    /// - §2C(3)(b): Answer of a Multiple-Choice response must be a valid index
    /// - §2C(3)(e): DeviceId must correspond to a valid device
    /// </summary>
    public async Task ValidateResponseAsync(ResponseEntity response)
    {
        // If we only have the entity, we must fetch the question.
        // This method is kept for backward compatibility and internal calls where we don't have the question context.
        var question = await _questionRepository.GetByIdAsync(response.QuestionId);
        if (question == null)
            throw new InvalidOperationException($"Question with ID {response.QuestionId} not found.");

        await ValidateResponseInternalAsync(response, question);
    }

    /// <summary>
    /// Internal validation logic that accepts the pre-fetched question context.
    /// Avoids redundant DB lookups.
    /// </summary>
    private async Task ValidateResponseInternalAsync(ResponseEntity response, QuestionEntity question)
    {
        // Rule §2C(3)(e): DeviceId must correspond to a valid device
        await _deviceIdValidator.ValidateOrThrowAsync(response.DeviceId);

        // Rule §2C(3)(b): Answer of a Multiple-Choice response must be a valid index
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
