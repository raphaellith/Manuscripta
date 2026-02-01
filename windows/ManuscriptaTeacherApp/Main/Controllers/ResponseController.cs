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
            // Delegate logic to Service (optimized 1-query flow)
            var response = await _responseService.CreateResponseAsync(request);
            
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
    /// Implements Validation Rules §1A(2): "All-or-Nothing" atomic validation via Service.
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
            // Delegate ALL orchestration to service (Validate All -> Save All)
            await _responseService.CreateBatchResponsesAsync(request.Responses);

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
}
