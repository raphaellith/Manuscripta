using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Main.Models.Entities.Responses;
using Main.Models.Entities.Questions;
using Main.Models.Enums;
using Main.Services.Repositories;
using Main.Services.GenAI;

namespace Main.Services;

/// <summary>
/// Service for managing responses.
/// Enforces business rules and data validation.
/// Integrates with AI feedback generation per GenAISpec.md §3D.
/// </summary>
public class ResponseService : IResponseService
{
    private readonly IResponseRepository _responseRepository;
    private readonly IQuestionRepository _questionRepository;
    private readonly IFeedbackRepository _feedbackRepository;
    private readonly FeedbackQueueService _feedbackQueueService;
    private readonly FeedbackGenerationService _feedbackGenerationService;
    private readonly DeviceIdValidator _deviceIdValidator;

    public ResponseService(
        IResponseRepository responseRepository,
        IQuestionRepository questionRepository,
        IFeedbackRepository feedbackRepository,
        DeviceIdValidator deviceIdValidator,
        FeedbackQueueService feedbackQueueService,
        FeedbackGenerationService feedbackGenerationService)
    {
        _responseRepository = responseRepository ?? throw new ArgumentNullException(nameof(responseRepository));
        _questionRepository = questionRepository ?? throw new ArgumentNullException(nameof(questionRepository));
        _feedbackRepository = feedbackRepository ?? throw new ArgumentNullException(nameof(feedbackRepository));
        _feedbackQueueService = feedbackQueueService ?? throw new ArgumentNullException(nameof(feedbackQueueService));
        _feedbackGenerationService = feedbackGenerationService ?? throw new ArgumentNullException(nameof(feedbackGenerationService));
        _deviceIdValidator = deviceIdValidator ?? throw new ArgumentNullException(nameof(deviceIdValidator));
    }

    public async Task<ResponseEntity> CreateResponseAsync(ResponseEntity response)
    {
        if (response == null)
            throw new ArgumentNullException(nameof(response));

        // Validate response
        await ValidateResponseAsync(response);

        await _responseRepository.AddAsync(response);
        
        // §3D(1): Automatically queue for AI feedback if applicable
        var question = await _questionRepository.GetByIdAsync(response.QuestionId);
        if (question != null && _feedbackGenerationService.ShouldGenerateFeedback(question))
        {
            _feedbackQueueService.QueueForAiGeneration(response.Id);
        }
        
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
    /// Creates multiple responses atomically with optimized validation.
    /// Validates unique device IDs once before processing.
    /// All-or-nothing semantics: if any response fails, none are stored.
    /// </summary>
    public async Task CreateResponseBatchAsync(IEnumerable<ResponseEntity> responses)
    {
        var responseList = responses?.ToList() ?? throw new ArgumentNullException(nameof(responses));
        
        if (responseList.Count == 0)
            throw new ArgumentException("Response batch cannot be empty.", nameof(responses));

        // Optimization: Validate unique device IDs once instead of per-response
        var uniqueDeviceIds = responseList.Select(r => r.DeviceId).Distinct().ToList();
        foreach (var deviceId in uniqueDeviceIds)
        {
            if (!await _deviceIdValidator.IsValidDeviceIdAsync(deviceId))
                throw new InvalidOperationException($"Device ID {deviceId} does not correspond to a valid paired device.");
        }

        // Validate all responses (question validation and answer validation)
        foreach (var response in responseList)
        {
            await ValidateResponseWithoutDeviceCheckAsync(response);
        }

        // All validations passed - add all responses atomically
        var addedIds = new List<Guid>();
        try
        {
            foreach (var response in responseList)
            {
                await _responseRepository.AddAsync(response);
                addedIds.Add(response.Id);
            }
        }
        catch
        {
            // Rollback: remove any responses that were added
            foreach (var id in addedIds)
            {
                try
                {
                    await _responseRepository.DeleteAsync(id);
                }
                catch
                {
                    // Best effort rollback - log in production
                }
            }
            throw;
        }
    }

    /// <summary>
    /// Deletes a response and its associated feedback.
    /// Per PersistenceAndCascadingRules.md §2(2A): The deletion of a response R must delete any feedback associated with R.
    /// </summary>
    public async Task DeleteResponseAsync(Guid id)
    {
        // §3D(6): Remove from generation queue if present
        _feedbackQueueService.RemoveFromQueue(id);
        
        await _feedbackRepository.DeleteByResponseIdAsync(id);
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

        // Rule 2C(3)(e): DeviceId must correspond to a valid device
        if (!await _deviceIdValidator.IsValidDeviceIdAsync(response.DeviceId))
            throw new InvalidOperationException($"Device ID {response.DeviceId} does not correspond to a valid paired device.");

        // Rule 2C(3)(b): Answer of a Multiple-Choice response must be a valid index
        if (response is MultipleChoiceResponseEntity mcResponse && question is MultipleChoiceQuestionEntity mcQuestion)
        {
            if (mcResponse.AnswerIndex < 0 || mcResponse.AnswerIndex >= mcQuestion.Options.Count)
                throw new InvalidOperationException(
                    $"Answer index {mcResponse.AnswerIndex} is out of range. " +
                    $"Valid range is 0 to {mcQuestion.Options.Count - 1}.");
        }

        // Rule 2C(3)(f): One response per question per device
        if (await _responseRepository.ExistsForQuestionAndDeviceAsync(response.QuestionId, response.DeviceId))
            throw new InvalidOperationException(
                $"A response for question {response.QuestionId} from device {response.DeviceId} already exists.");

        // Additional validation: ensure response type matches question type
        ValidateResponseTypeMatchesQuestion(response, question);

        // Auto-marking logic per FrontendWorkflowSpecifications
        PerformAutoMarking(response, question);
    }

    /// <summary>
    /// Validates a response without device check (for batch optimization).
    /// Device validation is performed once per unique device ID in batch mode.
    /// </summary>
    private async Task ValidateResponseWithoutDeviceCheckAsync(ResponseEntity response)
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

        // Rule 2C(3)(f): One response per question per device
        if (await _responseRepository.ExistsForQuestionAndDeviceAsync(response.QuestionId, response.DeviceId))
            throw new InvalidOperationException(
                $"A response for question {response.QuestionId} from device {response.DeviceId} already exists.");

        // Additional validation: ensure response type matches question type
        ValidateResponseTypeMatchesQuestion(response, question);

        // Auto-marking logic per FrontendWorkflowSpecifications
        PerformAutoMarking(response, question);
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

    /// <summary>
    /// Performs auto-marking by comparing response against correct answer.
    /// Per Session Interaction §4(2): preserves the Android client's IsCorrect value
    /// when available for student-teacher consistency; falls back to server-side
    /// calculation when the client did not provide one.
    /// </summary>
    private void PerformAutoMarking(ResponseEntity response, QuestionEntity question)
    {
        // If Android client already evaluated (per Session Interaction §4(2)), preserve it
        if (response.IsCorrect.HasValue)
            return;

        // Fall back to server-side evaluation
        if (question is MultipleChoiceQuestionEntity mcq
            && response is MultipleChoiceResponseEntity mcr
            && mcq.CorrectAnswerIndex.HasValue)
        {
            mcr.IsCorrect = mcr.AnswerIndex == mcq.CorrectAnswerIndex.Value;
        }
        else if (question is WrittenAnswerQuestionEntity waq
                 && response is WrittenAnswerResponseEntity war
                 && !string.IsNullOrEmpty(waq.CorrectAnswer))
        {
            // Exact match marking for written answers
            war.IsCorrect = war.Answer == waq.CorrectAnswer;
        }
    }

    #endregion
}
