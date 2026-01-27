using Main.Models.Entities;
using Main.Models.Entities.Questions;
using Main.Models.Entities.Responses;
using Main.Models.Enums;

namespace Main.Services.GenAI;

/// <summary>
/// Handles AI-powered feedback generation for student responses.
/// See GenAISpec.md §3D.
/// </summary>
public class FeedbackGenerationService
{
    private readonly OllamaClientService _ollamaClient;
    private readonly Queue<Guid> _generationQueue;
    private const string FeedbackModel = "granite4";

    public FeedbackGenerationService(OllamaClientService ollamaClient)
    {
        _ollamaClient = ollamaClient;
        _generationQueue = new Queue<Guid>();
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
    /// Checks if a response is currently queued for generation.
    /// See GenAISpec.md §3D(3).
    /// </summary>
    public bool IsQueued(Guid responseId)
    {
        return _generationQueue.Contains(responseId);
    }

    /// <summary>
    /// Adds a response to the generation queue.
    /// See GenAISpec.md §3D(5).
    /// </summary>
    public void QueueForAiGeneration(Guid responseId)
    {
        if (!_generationQueue.Contains(responseId))
        {
            _generationQueue.Enqueue(responseId);
        }
    }

    /// <summary>
    /// Removes a response from the generation queue.
    /// See GenAISpec.md §3D(6).
    /// </summary>
    public void RemoveFromQueue(Guid responseId)
    {
        var tempQueue = new Queue<Guid>();
        while (_generationQueue.Count > 0)
        {
            var id = _generationQueue.Dequeue();
            if (id != responseId)
            {
                tempQueue.Enqueue(id);
            }
        }
        
        while (tempQueue.Count > 0)
        {
            _generationQueue.Enqueue(tempQueue.Dequeue());
        }
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

            // Remove from queue on success
            RemoveFromQueue(response.Id);

            return feedback;
        }
        catch (Exception ex)
        {
            // §3D(7): Remove from queue on failure
            RemoveFromQueue(response.Id);
            
            // TODO: Notify frontend via SignalR OnFeedbackGenerationFailed
            throw new InvalidOperationException($"Failed to generate feedback for response {response.Id}", ex);
        }
    }

    /// <summary>
    /// Constructs the feedback generation prompt.
    /// See GenAISpec.md §3D(9)(b).
    /// </summary>
    private string ConstructFeedbackPrompt(QuestionEntity question, ResponseEntity response)
    {
        return $@"Generate constructive feedback for a student's response to the following question.

Question:
{question.QuestionText}

Mark Scheme:
{question.MarkScheme}

Student's Response:
{response.ResponseText}

Provide feedback that includes:
1. A score justification explaining how well the response meets the mark scheme
2. Specific strengths in the response
3. Improvement suggestions for areas that could be enhanced

Format the feedback in a clear, constructive manner suitable for the student to understand and learn from.

Feedback:";
    }
}
