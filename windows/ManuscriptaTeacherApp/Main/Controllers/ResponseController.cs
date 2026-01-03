using Microsoft.AspNetCore.Mvc;

using Main.Models.Dtos;
using Main.Models.Entities.Responses;
using Main.Models.Entities.Questions;
using Main.Services;
using Main.Services.Repositories;

namespace Main.Controllers;

/// <summary>
/// Controller for student response submission operations.
/// Implements API Contract.md §2.3 and Session Interaction.md §4.
/// </summary>
[ApiController]
[Route("api/v1")]
public class ResponseController : ControllerBase
{
    private readonly IResponseService _responseService;
    private readonly IQuestionRepository _questionRepository;
    private readonly ILogger<ResponseController> _logger;

    public ResponseController(
        IResponseService responseService,
        IQuestionRepository questionRepository,
        ILogger<ResponseController> logger)
    {
        _responseService = responseService ?? throw new ArgumentNullException(nameof(responseService));
        _questionRepository = questionRepository ?? throw new ArgumentNullException(nameof(questionRepository));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <summary>
    /// Submits a single answer to a question.
    /// Per API Contract.md §2.3: POST /responses
    /// </summary>
    /// <param name="request">The response request DTO.</param>
    /// <returns>201 Created on success, 400 Bad Request if invalid.</returns>
    [HttpPost("responses")]
    [ProducesResponseType(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> SubmitResponse([FromBody] ResponseRequest request)
    {
        // Validate DTO structure per Validation Rules §1A(3)
        var validationResult = ValidateResponseRequest(request);
        if (validationResult != null)
        {
            return validationResult;
        }

        try
        {
            var entity = await MapToEntityAsync(request);
            if (entity == null)
            {
                return BadRequest(new { error = "Unable to map response to entity - question type mismatch or invalid answer format" });
            }

            await _responseService.CreateResponseAsync(entity);
            
            _logger.LogInformation("Response {ResponseId} submitted for question {QuestionId}", 
                request.Id, request.QuestionId);

            // Per API Contract §2.3: Return 201 Created with empty body
            return StatusCode(StatusCodes.Status201Created, new { });
        }
        catch (InvalidOperationException ex)
        {
            _logger.LogWarning(ex, "Validation failed for response {ResponseId}", request.Id);
            return BadRequest(new { error = ex.Message });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing response {ResponseId}", request.Id);
            return StatusCode(StatusCodes.Status500InternalServerError, new { error = "Internal server error" });
        }
    }

    /// <summary>
    /// Submits multiple responses at once.
    /// Per API Contract.md §2.3: POST /responses/batch
    /// Any invalid response invalidates the entire batch.
    /// </summary>
    /// <param name="request">The batch response request DTO.</param>
    /// <returns>201 Created on success, 400 Bad Request if any response is invalid.</returns>
    [HttpPost("responses/batch")]
    [ProducesResponseType(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> SubmitBatchResponses([FromBody] BatchResponseRequest request)
    {
        if (request == null || request.Responses == null || request.Responses.Count == 0)
        {
            _logger.LogWarning("Received empty batch response request");
            return BadRequest(new { error = "Responses array is required and must not be empty" });
        }

        // Validate all DTOs up-front before any processing
        for (int i = 0; i < request.Responses.Count; i++)
        {
            var validationResult = ValidateResponseRequest(request.Responses[i], $"Responses[{i}]");
            if (validationResult != null)
            {
                return validationResult;
            }
        }

        try
        {
            // Map all responses first - fail fast if any mapping fails
            var entities = new List<ResponseEntity>();
            for (int i = 0; i < request.Responses.Count; i++)
            {
                var entity = await MapToEntityAsync(request.Responses[i]);
                if (entity == null)
                {
                    return BadRequest(new { error = $"Responses[{i}]: Unable to map response - question type mismatch or invalid answer format" });
                }
                entities.Add(entity);
            }

            // All validations passed - now create all responses
            foreach (var entity in entities)
            {
                await _responseService.CreateResponseAsync(entity);
            }

            _logger.LogInformation("Batch of {Count} responses submitted successfully", request.Responses.Count);

            // Per API Contract §2.3: Return 201 Created
            return StatusCode(StatusCodes.Status201Created, new { });
        }
        catch (InvalidOperationException ex)
        {
            _logger.LogWarning(ex, "Validation failed during batch response submission");
            return BadRequest(new { error = ex.Message });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing batch response submission");
            return StatusCode(StatusCodes.Status500InternalServerError, new { error = "Internal server error" });
        }
    }

    #region Validation and Mapping

    /// <summary>
    /// Validates the structure of a ResponseRequest DTO.
    /// Per Validation Rules §1A(3): Return 400 if invalid.
    /// </summary>
    private IActionResult? ValidateResponseRequest(ResponseRequest? request, string prefix = "")
    {
        string fieldPrefix = string.IsNullOrEmpty(prefix) ? "" : $"{prefix}.";

        if (request == null)
        {
            return BadRequest(new { error = "Request body is required" });
        }

        if (string.IsNullOrWhiteSpace(request.Id))
        {
            return BadRequest(new { error = $"{fieldPrefix}Id is required" });
        }

        if (!Guid.TryParse(request.Id, out _))
        {
            return BadRequest(new { error = $"{fieldPrefix}Id must be a valid GUID" });
        }

        if (string.IsNullOrWhiteSpace(request.QuestionId))
        {
            return BadRequest(new { error = $"{fieldPrefix}QuestionId is required" });
        }

        if (!Guid.TryParse(request.QuestionId, out _))
        {
            return BadRequest(new { error = $"{fieldPrefix}QuestionId must be a valid GUID" });
        }

        if (string.IsNullOrWhiteSpace(request.DeviceId))
        {
            return BadRequest(new { error = $"{fieldPrefix}DeviceId is required" });
        }

        if (!Guid.TryParse(request.DeviceId, out _))
        {
            return BadRequest(new { error = $"{fieldPrefix}DeviceId must be a valid GUID" });
        }

        if (string.IsNullOrWhiteSpace(request.Answer))
        {
            return BadRequest(new { error = $"{fieldPrefix}Answer is required" });
        }

        if (string.IsNullOrWhiteSpace(request.Timestamp))
        {
            return BadRequest(new { error = $"{fieldPrefix}Timestamp is required" });
        }

        if (!DateTime.TryParse(request.Timestamp, out _))
        {
            return BadRequest(new { error = $"{fieldPrefix}Timestamp must be a valid ISO 8601 date" });
        }

        return null; // Valid
    }

    /// <summary>
    /// Maps a ResponseRequest DTO to the appropriate ResponseEntity subtype
    /// based on the question type.
    /// </summary>
    private async Task<ResponseEntity?> MapToEntityAsync(ResponseRequest request)
    {
        var id = Guid.Parse(request.Id);
        var questionId = Guid.Parse(request.QuestionId);
        var deviceId = Guid.Parse(request.DeviceId);
        var timestamp = DateTime.Parse(request.Timestamp);

        // Get the question to determine response type
        var question = await _questionRepository.GetByIdAsync(questionId);
        if (question == null)
        {
            _logger.LogWarning("Question {QuestionId} not found for response {ResponseId}", questionId, id);
            throw new InvalidOperationException($"Question with ID {questionId} not found.");
        }

        return question switch
        {
            MultipleChoiceQuestionEntity => CreateMultipleChoiceResponse(
                id, questionId, deviceId, timestamp, request.Answer, request.IsCorrect),
            WrittenAnswerQuestionEntity => new WrittenAnswerResponseEntity(
                id, questionId, deviceId, request.Answer, timestamp, request.IsCorrect),
            _ => null
        };
    }

    /// <summary>
    /// Creates a MultipleChoiceResponseEntity, parsing the answer as an integer index.
    /// </summary>
    private ResponseEntity? CreateMultipleChoiceResponse(
        Guid id, Guid questionId, Guid deviceId, DateTime timestamp, string answer, bool? isCorrect)
    {
        if (!int.TryParse(answer, out var answerIndex))
        {
            _logger.LogWarning("Invalid answer format for multiple choice response: {Answer}", answer);
            return null;
        }

        return new MultipleChoiceResponseEntity(id, questionId, deviceId, answerIndex, timestamp, isCorrect);
    }

    #endregion
}
