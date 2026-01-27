using Main.Models.Entities;
using Main.Models.Enums;

namespace Main.Services.GenAI;

/// <summary>
/// Manages the feedback approval workflow and dispatch status, as well as the generation queue.
/// See GenAISpec.md §3DA and §3D.
/// </summary>
public class FeedbackQueueService
{
    private readonly Queue<Guid> _generationQueue = new();

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
    /// See GenAISpec.md §3D(6) and §3D(7).
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
    /// Checks if feedback should be dispatched to the student device.
    /// See GenAISpec.md §3DA(1).
    /// </summary>
    public bool ShouldDispatchFeedback(FeedbackEntity feedback)
    {
        // PROVISIONAL feedback shall not be dispatched
        return feedback.Status != FeedbackStatus.PROVISIONAL;
    }

    /// <summary>
    /// Approves feedback and updates status to READY.
    /// See GenAISpec.md §3DA(2).
    /// </summary>
    public void ApproveFeedback(FeedbackEntity feedback)
    {
        if (feedback.Status == FeedbackStatus.PROVISIONAL)
        {
            feedback.Status = FeedbackStatus.READY;
            // TODO: Trigger dispatch immediately via Session Interaction Specification §7
        }
    }

    /// <summary>
    /// Marks feedback as delivered after receiving acknowledgement.
    /// See GenAISpec.md §3DA(3).
    /// </summary>
    public void MarkFeedbackDelivered(FeedbackEntity feedback)
    {
        if (feedback.Status == FeedbackStatus.READY)
        {
            feedback.Status = FeedbackStatus.DELIVERED;
        }
    }

    /// <summary>
    /// Handles feedback dispatch failure.
    /// See GenAISpec.md §3DA(4).
    /// </summary>
    public void HandleDispatchFailure(FeedbackEntity feedback)
    {
        // Feedback remains in READY status
        // TODO: Notify frontend via SignalR OnFeedbackDispatchFailed
    }
}
