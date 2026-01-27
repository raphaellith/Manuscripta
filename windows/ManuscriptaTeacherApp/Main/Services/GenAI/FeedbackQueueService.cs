using Main.Models.Entities;
using Main.Models.Enums;

namespace Main.Services.GenAI;

/// <summary>
/// Manages the feedback approval workflow and dispatch status.
/// See GenAISpec.md §3DA.
/// </summary>
public class FeedbackQueueService
{
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
