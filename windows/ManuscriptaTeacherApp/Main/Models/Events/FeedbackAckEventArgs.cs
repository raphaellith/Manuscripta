namespace Main.Models.Events;

/// <summary>
/// Event arguments for feedback acknowledgement per entity.
/// Per API Contract.md §3.6.2: FEEDBACK_ACK uses per-entity acknowledgement.
/// </summary>
public class FeedbackAckEventArgs : EventArgs
{
    /// <summary>
    /// Gets the device ID that acknowledged receipt.
    /// </summary>
    public Guid DeviceId { get; }

    /// <summary>
    /// Gets the feedback ID that was acknowledged.
    /// </summary>
    public Guid FeedbackId { get; }

    public FeedbackAckEventArgs(Guid deviceId, Guid feedbackId)
    {
        DeviceId = deviceId;
        FeedbackId = feedbackId;
    }
}
