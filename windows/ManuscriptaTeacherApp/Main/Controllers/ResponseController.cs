using System.Globalization;
using Microsoft.AspNetCore.Mvc;

using Main.Models.Dtos;
using Main.Models.Entities.Responses;
using Main.Models.Entities.Questions;
using Main.Services;
using Main.Services.Repositories;

namespace Main.Controllers;

/// <summary>
/// Controller for student response submissions.
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
    /// Submits a single response to a question.
    /// Per API Contract.md §2.3 and Validation Rules §2C.
    /// </summary>
    [HttpPost("responses")]
    [ProducesResponseType(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> SubmitResponse([FromBody] SubmitResponseDto request)
    {
        if (request == null)
        {
            _logger.LogWarning("Response submission with null request body");
            return BadRequest(new { error = "Request body is required" });
        }

        try
        {
            var response = await ParseAndCreateResponseAsync(request);
            _logger.LogInformation("Response {ResponseId} submitted for question {QuestionId}", response.Id, response.QuestionId);
            
            // Per API Contract §2.3: Return 201 Created with empty body
            return StatusCode(StatusCodes.Status201Created, new { });
        }
        catch (FormatException ex)
        {
            _logger.LogWarning(ex, "Invalid format in response submission");
            return BadRequest(new { error = ex.Message });
        }
        catch (ArgumentException ex)
        {
            // Per Validation Rules §1A(3): Return 400 for validation failures
            _logger.LogWarning(ex, "Validation failed for response submission");
            return BadRequest(new { error = ex.Message });
        }
        catch (InvalidOperationException ex)
        {
            // Per Validation Rules §1A(3): Return 400 for validation failures
            _logger.LogWarning(ex, "Validation failed for response submission");
            return BadRequest(new { error = ex.Message });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing response submission");
            return StatusCode(StatusCodes.Status500InternalServerError, new { error = "Internal server error" });
        }
    }

    /// <summary>
    /// Submits multiple responses at once.
    /// Per API Contract.md §2.3 - for offline reconnection scenarios.
    /// </summary>
    [HttpPost("responses/batch")]
    [ProducesResponseType(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> SubmitBatchResponses([FromBody] BatchSubmitResponsesDto request)
    {
        if (request == null || request.Responses == null || !request.Responses.Any())
        {
            _logger.LogWarning("Batch response submission with empty request");
            return BadRequest(new { error = "Responses array is required and must not be empty" });
        }

        try
        {
            foreach (var responseDto in request.Responses)
            {
                await ParseAndCreateResponseAsync(responseDto);
            }

            _logger.LogInformation("Batch submitted {Count} responses", request.Responses.Count);
            
            // Per API Contract §2.3: Return 201 Created
            return StatusCode(StatusCodes.Status201Created, new { });
        }
        catch (FormatException ex)
        {
            _logger.LogWarning(ex, "Invalid format in batch response submission");
            return BadRequest(new { error = ex.Message });
        }
        catch (ArgumentException ex)
        {
            // Per Validation Rules §1A(3): Return 400 for validation failures
            _logger.LogWarning(ex, "Validation failed for batch response submission");
            return BadRequest(new { error = ex.Message });
        }
        catch (InvalidOperationException ex)
        {
            // Per Validation Rules §1A(3): Return 400 for validation failures
            _logger.LogWarning(ex, "Validation failed for batch response submission");
            return BadRequest(new { error = ex.Message });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing batch response submission");
            return StatusCode(StatusCodes.Status500InternalServerError, new { error = "Internal server error" });
        }
    }

    /// <summary>
    /// Parses the DTO and creates the appropriate ResponseEntity.
    /// </summary>
    private async Task<ResponseEntity> ParseAndCreateResponseAsync(SubmitResponseDto dto)
    {
        // Validate GUID formats
        if (!Guid.TryParse(dto.Id, out var responseId))
            throw new FormatException("Id must be a valid GUID");
        
        if (!Guid.TryParse(dto.QuestionId, out var questionId))
            throw new FormatException("QuestionId must be a valid GUID");
        
        if (!Guid.TryParse(dto.DeviceId, out var deviceId))
            throw new FormatException("DeviceId must be a valid GUID");

        // Parse timestamp per API Contract §4.4 - ISO 8601
        if (!DateTime.TryParse(dto.Timestamp, CultureInfo.InvariantCulture, DateTimeStyles.RoundtripKind, out var timestamp))
            throw new FormatException("Timestamp must be a valid ISO 8601 date-time string");

        // Determine response type based on question type
        var question = await _questionRepository.GetByIdAsync(questionId);
        if (question == null)
            throw new InvalidOperationException($"Question with ID {questionId} not found");

        ResponseEntity response = question switch
        {
            MultipleChoiceQuestionEntity => CreateMultipleChoiceResponse(responseId, questionId, deviceId, dto.Answer, timestamp, dto.IsCorrect),
            WrittenAnswerQuestionEntity => new WrittenAnswerResponseEntity(responseId, questionId, deviceId, dto.Answer, timestamp, dto.IsCorrect),
            _ => throw new InvalidOperationException($"Unsupported question type: {question.GetType().Name}")
        };

        return await _responseService.CreateResponseAsync(response);
    }

    private static MultipleChoiceResponseEntity CreateMultipleChoiceResponse(
        Guid responseId, Guid questionId, Guid deviceId, string answer, DateTime timestamp, bool? isCorrect)
    {
        if (!int.TryParse(answer, out var answerIndex))
            throw new FormatException("Answer must be a valid integer index for multiple choice questions");

        return new MultipleChoiceResponseEntity(responseId, questionId, deviceId, answerIndex, timestamp, isCorrect);
    }
}
