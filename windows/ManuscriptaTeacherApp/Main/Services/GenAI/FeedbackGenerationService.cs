using Main.Models.Entities;
using Main.Models.Entities.Questions;
using Main.Models.Entities.Responses;
using Main.Models.Enums;
using Main.Services.Hubs;
using Main.Services.Repositories;
using Microsoft.AspNetCore.SignalR;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;

namespace Main.Services.GenAI;

/// <summary>
/// Handles AI-powered feedback generation for student responses.
/// See GenAISpec.md §3D.
/// </summary>
public class FeedbackGenerationService : IHostedService, IFeedbackGenerationService
{
    private readonly OllamaClientService _ollamaClient;
    private readonly FeedbackQueueService _queueService;
    private readonly IHubContext<TeacherPortalHub> _hubContext;
    private readonly IServiceScopeFactory _scopeFactory;
    private readonly IResponseRepository _responseRepository;
    private readonly IFeedbackRepository _feedbackRepository;
    private readonly ILogger<FeedbackGenerationService> _logger;
    private const string PrimaryModel = "qwen3:8b";
    private const string FallbackModel = "granite4";
    
    private Task? _processingTask;
    private CancellationTokenSource? _cancellationTokenSource;
    private readonly SemaphoreSlim _processingSemaphore = new(1, 1);

    /// <summary>
    /// Tracks the response ID currently being processed for generation.
    /// See GenAISpec.md §3D(4).
    /// </summary>
    private string? _currentlyGeneratingResponseId;

    public FeedbackGenerationService(
        OllamaClientService ollamaClient,
        FeedbackQueueService queueService,
        IHubContext<TeacherPortalHub> hubContext,
        IServiceScopeFactory scopeFactory,
        IResponseRepository responseRepository,
        IFeedbackRepository feedbackRepository,
        ILogger<FeedbackGenerationService> logger)
    {
        _ollamaClient = ollamaClient;
        _queueService = queueService;
        _hubContext = hubContext;
        _scopeFactory = scopeFactory;
        _responseRepository = responseRepository;
        _feedbackRepository = feedbackRepository;
        _logger = logger;
    }

    /// <summary>
    /// Starts the background queue processing.
    /// </summary>
    public Task StartAsync(CancellationToken cancellationToken)
    {
        _cancellationTokenSource = new CancellationTokenSource();
        _processingTask = Task.Run(() => ProcessQueueAsync(_cancellationTokenSource.Token), cancellationToken);
        _logger.LogInformation("Feedback generation queue processor started");
        return Task.CompletedTask;
    }

    /// <summary>
    /// Stops the background queue processing.
    /// </summary>
    public async Task StopAsync(CancellationToken cancellationToken)
    {
        var cancellationTokenSource = _cancellationTokenSource;
        _cancellationTokenSource = null;

        var processingTask = _processingTask;
        _processingTask = null;

        if (cancellationTokenSource != null)
        {
            try
            {
                cancellationTokenSource.Cancel();
            }
            catch (ObjectDisposedException)
            {
                // Shutdown may call StopAsync multiple times; ignore if already disposed.
            }

            if (processingTask != null)
            {
                try
                {
                    await processingTask;
                }
                catch (OperationCanceledException)
                {
                    // Expected when shutting down.
                }
            }

            cancellationTokenSource.Dispose();
        }
    }

    /// <summary>
    /// Background task that continuously processes the feedback generation queue.
    /// See GenAISpec.md §3D(2) and §3D(4).
    /// </summary>
    private async Task ProcessQueueAsync(CancellationToken cancellationToken)
    {
        while (!cancellationToken.IsCancellationRequested)
        {
            try
            {
                // Wait before checking the queue again
                await Task.Delay(1000, cancellationToken);

                // Try to get the next item from the queue
                var responseId = _queueService.DequeueNext();
                if (!responseId.HasValue)
                {
                    continue;
                }

                _logger.LogInformation("Dequeued response {ResponseId} for feedback generation", responseId.Value);

                // Ensure only one generation happens at a time (§3D(4))
                await _processingSemaphore.WaitAsync(cancellationToken);
                try
                {
                    await ProcessResponseAsync(responseId.Value, cancellationToken);
                }
                finally
                {
                    _processingSemaphore.Release();
                }
            }
            catch (OperationCanceledException)
            {
                // Expected when shutting down
                break;
            }
            catch (Exception ex)
            {
                // Log error but continue processing
                _logger.LogError(ex, "Error in feedback generation queue processing");
            }
        }
    }

    /// <summary>
    /// Processes a single response from the queue.
    /// </summary>
    private async Task ProcessResponseAsync(Guid responseId, CancellationToken cancellationToken)
    {
        // Set generating ID immediately so the frontend sees this response as
        // "generating" for the entire duration, not in a limbo state between
        // dequeue and the actual Ollama call.
        _currentlyGeneratingResponseId = responseId.ToString();

        // Track result for post-finally notification.
        // Notifications are sent AFTER clearing _currentlyGeneratingResponseId
        // to prevent a race condition: the frontend's loadData() calls
        // GetCurrentlyGeneratingResponseId() and can read the stale value
        // if the finally block hasn't executed yet, causing the UI to
        // permanently show "generating" even though processing completed.
        Guid? savedFeedbackId = null;
        string? failureMessage = null;

        try
        {
            using var scope = _scopeFactory.CreateScope();
            var questionRepository = scope.ServiceProvider.GetRequiredService<IQuestionRepository>();

            // Retrieve the response
            var response = await _responseRepository.GetByIdAsync(responseId);
            if (response == null)
            {
                _logger.LogWarning("Feedback generation: response {ResponseId} not found", responseId);
                failureMessage = $"Response {responseId} not found";
                return;
            }

            // Retrieve the question
            var question = await questionRepository.GetByIdAsync(response.QuestionId);
            if (question == null)
            {
                _logger.LogWarning("Feedback generation: question {QuestionId} not found for response {ResponseId}",
                    response.QuestionId, responseId);
                failureMessage = $"Question {response.QuestionId} not found";
                return;
            }

            // Check if feedback already exists — per §6A(1A), overwrite PROVISIONAL feedback
            var existingFeedback = await _feedbackRepository.GetByResponseIdAsync(responseId);
            if (existingFeedback != null)
            {
                if (existingFeedback.Status == FeedbackStatus.PROVISIONAL)
                {
                    // Delete PROVISIONAL feedback to allow re-generation
                    await _feedbackRepository.DeleteAsync(existingFeedback.Id);
                }
                else
                {
                    // Preserve READY/DELIVERED feedback — do not overwrite finalized assessments
                    _logger.LogInformation(
                        "Skipping generation for response {ResponseId}: feedback already in {Status} state",
                        responseId, existingFeedback.Status);
                    return;
                }
            }

            // Generate feedback
            _logger.LogInformation("Starting Ollama feedback generation for response {ResponseId}", responseId);
            var feedback = await GenerateFeedbackAsync(question, response);
            _logger.LogInformation("Ollama feedback generation completed for response {ResponseId}", responseId);

            // Save feedback
            await _feedbackRepository.AddAsync(feedback);
            savedFeedbackId = feedback.Id;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to generate feedback for response {ResponseId}", responseId);
            failureMessage = ex.InnerException?.Message ?? ex.Message;
        }
        finally
        {
            _currentlyGeneratingResponseId = null;
        }

        // Notify AFTER clearing _currentlyGeneratingResponseId so the
        // frontend's loadData() → GetCurrentlyGeneratingResponseId() reads null.
        if (savedFeedbackId.HasValue)
        {
            // Per NetworkingAPISpec §2(1)(c)(iii).
            await _hubContext.Clients.All.SendAsync("OnFeedbackGenerated", savedFeedbackId.Value, responseId, cancellationToken);
        }
        else if (failureMessage != null)
        {
            // §3D(7)(b): Notify frontend so the UI can show the error and offer retry.
            await _hubContext.Clients.All.SendAsync("OnFeedbackGenerationFailed", responseId,
                failureMessage, cancellationToken);
        }
    }

    /// <summary>
    /// Checks if a response should be queued for AI feedback generation.
    /// See GenAISpec.md §3D(1).
    /// </summary>
    public bool ShouldGenerateFeedback(QuestionEntity question)
    {
        return question.QuestionType == QuestionType.WRITTEN_ANSWER &&
               !string.IsNullOrEmpty(question.MarkScheme);
    }

    /// <summary>
    /// Returns the response ID currently being processed, or null if idle.
    /// See GenAISpec.md §3D(4).
    /// </summary>
    public string? GetCurrentlyGeneratingResponseId() => _currentlyGeneratingResponseId;

    /// <summary>
    /// Generates AI feedback for a student response.
    /// See GenAISpec.md §3D(9).
    /// </summary>
    public async Task<FeedbackEntity> GenerateFeedbackAsync(
        QuestionEntity question,
        ResponseEntity response)
    {
        try
        {
            // Ensure model is ready
            string modelToUse = PrimaryModel;
            await _ollamaClient.EnsureModelReadyAsync(PrimaryModel);

            // §3D(9)(b): Construct prompt
            var prompt = ConstructFeedbackPrompt(question, response);

            // §3D(9)(d): Invoke model (or granite4 if fallback per §1(6))
            string rawResponse;
            try
            {
                rawResponse = await _ollamaClient.GenerateChatCompletionAsync(modelToUse, prompt);
            }
            catch (HttpRequestException ex) when (
                ex.StatusCode == System.Net.HttpStatusCode.InternalServerError ||
                ex.Message.Contains("system memory", StringComparison.OrdinalIgnoreCase) ||
                ex.Message.Contains("500", StringComparison.OrdinalIgnoreCase) ||
                ex.Message.Contains("InternalServerError", StringComparison.OrdinalIgnoreCase))
            {
                // §1(6)(a): Fall back when primary model fails due to resource constraints
                _logger.LogWarning(ex, "Primary model failed for feedback generation on response {ResponseId}, falling back to {FallbackModel}",
                    response.Id, FallbackModel);
                await _ollamaClient.UnloadModelAsync(PrimaryModel);
                await Task.Delay(500);
                await _ollamaClient.EnsureModelReadyAsync(FallbackModel);
                modelToUse = FallbackModel;
                rawResponse = await _ollamaClient.GenerateChatCompletionAsync(modelToUse, prompt);
            }

            // §3D(9)(e): Parse mark and feedback text from model response
            var (marks, feedbackText) = ParseFeedbackResponse(rawResponse, question.MaxScore);

            // §3D(9)(e): Create feedback entity with PROVISIONAL status
            var feedback = new FeedbackEntity
            {
                Id = Guid.NewGuid(),
                ResponseId = response.Id,
                FeedbackText = feedbackText,
                Marks = marks,
                Status = FeedbackStatus.PROVISIONAL,
                CreatedAt = DateTime.UtcNow
            };

            return feedback;
        }
        catch (Exception ex)
        {
            throw new InvalidOperationException($"Failed to generate feedback for response {response.Id}", ex);
        }
    }

    /// <summary>
    /// Constructs the feedback generation prompt.
    /// See GenAISpec.md §3D(9)(b).
    /// </summary>
    public static string ConstructFeedbackPrompt(QuestionEntity question, ResponseEntity response)
    {
        // §3D(9)(b)(iii): Include maximum score if present
        // §3D(9)(c): Instruct the model to output a MARK line if MaxScore is present
        var maxScoreSection = question.MaxScore.HasValue
            ? $"Maximum Score:\n{question.MaxScore.Value}"
            : "";

        var markInstruction = question.MaxScore.HasValue
            ? $"Begin your response with a line in the exact format: MARK: X\nwhere X is an integer between 0 and {question.MaxScore.Value} (inclusive) representing the mark you award.\nFollow the MARK line with a blank line, then provide the feedback text."
            : "";

        return $@"
TASK:
Generate constructive feedback for a student's response to the question given below.
{markInstruction}
Include a score justification explaining how well the response meets the mark scheme.
Your mark must strictly adhere to the mark scheme provided and be supported by specific evidence from the student's response.
If a mark scheme provides a specific mark breakdown, you must show evidence of how each mark is awarded based on the breakdown, and provide the breakdown in your feedback.
You must be ready to award the full range of marks, including zero and the maximum score, if justified by the response.
Include specific strengths in the response.
Where the response could be improved, include suggestions for enhancement.
Format the feedback in a clear, constructive manner suitable for the student to understand and learn from.
Use British English throughout your response.
Your feedback should speak directly to the student, using you statements to make it more engaging and personalised.
You may not use markdown or HTML formatting in your feedback.

QUESTION:
{question.QuestionText}

STUDENT'S RESPONSE:
{GetResponseText(response)}

MARK SCHEME:
{question.MarkScheme}

{maxScoreSection}
";
    }

    /// <summary>
    /// Extracts the student's answer text from a polymorphic response entity.
    /// </summary>
    private static string GetResponseText(ResponseEntity response)
    {
        return response switch
        {
            WrittenAnswerResponseEntity wa => wa.Answer,
            _ => response.ResponseText ?? string.Empty
        };
    }

    /// <summary>
    /// Parses the AI model response to extract a numeric mark and feedback text.
    /// See GenAISpec.md §3D(9)(e).
    /// Expected format: first line "MARK: X" followed by feedback text.
    /// </summary>
    public static (int? marks, string feedbackText) ParseFeedbackResponse(string rawResponse, int? maxScore)
    {
        if (string.IsNullOrWhiteSpace(rawResponse))
            return (null, rawResponse ?? "");

        var lines = rawResponse.Split('\n');
        var firstLine = lines[0].Trim();

        // §3D(9)(e)(i): Extract MARK: X from the first line
        var match = System.Text.RegularExpressions.Regex.Match(firstLine, @"^MARK:\s*(\d+)$");
        if (maxScore.HasValue && match.Success && int.TryParse(match.Groups[1].Value, out var mark))
        {
            // Clamp to valid range [0, MaxScore] per §2F(2)(c)
            mark = Math.Clamp(mark, 0, maxScore.Value);

            // §3D(9)(e)(ii): Remaining text after the MARK line
            var feedbackText = string.Join('\n', lines.Skip(1)).TrimStart('\n', '\r');
            return (mark, feedbackText);
        }

        // No valid MARK line found or no MaxScore — return entire response as text, no marks
        return (null, rawResponse);
    }
}
