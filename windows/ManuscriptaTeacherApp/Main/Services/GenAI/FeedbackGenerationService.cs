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

namespace Main.Services.GenAI;

/// <summary>
/// Handles AI-powered feedback generation for student responses.
/// See GenAISpec.md §3D.
/// </summary>
public class FeedbackGenerationService : IHostedService
{
    private readonly OllamaClientService _ollamaClient;
    private readonly FeedbackQueueService _queueService;
    private readonly IHubContext<TeacherPortalHub> _hubContext;
    private readonly IServiceScopeFactory _scopeFactory;
    private readonly IResponseRepository _responseRepository;
    private readonly IFeedbackRepository _feedbackRepository;
    private const string FeedbackModel = "granite4";
    
    private Task? _processingTask;
    private CancellationTokenSource? _cancellationTokenSource;
    private readonly SemaphoreSlim _processingSemaphore = new(1, 1);

    public FeedbackGenerationService(
        OllamaClientService ollamaClient,
        FeedbackQueueService queueService,
        IHubContext<TeacherPortalHub> hubContext,
        IServiceScopeFactory scopeFactory,
        IResponseRepository responseRepository,
        IFeedbackRepository feedbackRepository)
    {
        _ollamaClient = ollamaClient;
        _queueService = queueService;
        _hubContext = hubContext;
        _scopeFactory = scopeFactory;
        _responseRepository = responseRepository;
        _feedbackRepository = feedbackRepository;
    }

    /// <summary>
    /// Starts the background queue processing.
    /// </summary>
    public Task StartAsync(CancellationToken cancellationToken)
    {
        _cancellationTokenSource = new CancellationTokenSource();
        _processingTask = Task.Run(() => ProcessQueueAsync(_cancellationTokenSource.Token), cancellationToken);
        return Task.CompletedTask;
    }

    /// <summary>
    /// Stops the background queue processing.
    /// </summary>
    public async Task StopAsync(CancellationToken cancellationToken)
    {
        if (_cancellationTokenSource != null)
        {
            _cancellationTokenSource.Cancel();
            if (_processingTask != null)
            {
                await _processingTask;
            }
            _cancellationTokenSource.Dispose();
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
                Console.Error.WriteLine($"Error in feedback generation queue processing: {ex.Message}");
            }
        }
    }

    /// <summary>
    /// Processes a single response from the queue.
    /// </summary>
    private async Task ProcessResponseAsync(Guid responseId, CancellationToken cancellationToken)
    {
        using var scope = _scopeFactory.CreateScope();
        var questionRepository = scope.ServiceProvider.GetRequiredService<IQuestionRepository>();

        try
        {
            // Retrieve the response
            var response = await _responseRepository.GetByIdAsync(responseId);
            if (response == null)
            {
                return;
            }

            // Retrieve the question
            var question = await questionRepository.GetByIdAsync(response.QuestionId);
            if (question == null)
            {
                return;
            }

            // Check if feedback already exists
            var existingFeedback = await _feedbackRepository.GetByResponseIdAsync(responseId);
            if (existingFeedback != null)
            {
                return;
            }

            // Generate feedback
            var feedback = await GenerateFeedbackAsync(question, response);

            // Save feedback
            await _feedbackRepository.AddAsync(feedback);

            // Notify frontend of successful generation
            await _hubContext.Clients.All.SendAsync("OnFeedbackGenerated", feedback.Id, responseId, cancellationToken);
        }
        catch (Exception ex)
        {
            // Error handling is done in GenerateFeedbackAsync, but catch any unexpected errors
            Console.Error.WriteLine($"Unexpected error processing response {responseId}: {ex.Message}");
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
            await _ollamaClient.EnsureModelReadyAsync(FeedbackModel);

            // §3D(9)(b): Construct prompt
            var prompt = ConstructFeedbackPrompt(question, response);

            // §3D(9)(c): Invoke model
            var feedbackText = await _ollamaClient.GenerateChatCompletionAsync(FeedbackModel, prompt);

            // §3D(9)(d): Create feedback entity with PROVISIONAL status
            var feedback = new FeedbackEntity
            {
                Id = Guid.NewGuid(),
                ResponseId = response.Id,
                FeedbackText = feedbackText,
                Status = FeedbackStatus.PROVISIONAL,
                CreatedAt = DateTime.UtcNow
            };

            return feedback;
        }
        catch (Exception ex)
        {
            // §3D(7): Remove from queue on failure
            _queueService.RemoveFromQueue(response.Id);
            
            // §3D(7)(b): Notify frontend via SignalR OnFeedbackGenerationFailed
            _ = _hubContext.Clients.All.SendAsync("OnFeedbackGenerationFailed", response.Id, ex.Message);
            throw new InvalidOperationException($"Failed to generate feedback for response {response.Id}", ex);
        }
    }

    /// <summary>
    /// Constructs the feedback generation prompt.
    /// See GenAISpec.md §3D(9)(b).
    /// </summary>
    private string ConstructFeedbackPrompt(QuestionEntity question, ResponseEntity response)
    {
        // §3D(9)(b)(iii): Include maximum score if present
        var maxScoreSection = question.MaxScore.HasValue
            ? $"Maximum Score:\n{question.MaxScore.Value}"
            : "";

        return $@"
TASK:
Generate constructive feedback for a student's response to the question given below.
Include a score justification explaining how well the response meets the mark scheme.
Include specific strengths in the response.
Include improvement suggestions for areas that could be enhanced.
Format the feedback in a clear, constructive manner suitable for the student to understand and learn from.

QUESTION:
{question.QuestionText}

STUDENT'S RESPONSE:
{response.ResponseText}

MARK SCHEME:
{question.MarkScheme}

{maxScoreSection}
";
    }
}
