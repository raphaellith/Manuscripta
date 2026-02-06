namespace Main.Models.Enums;

/// <summary>
/// Represents the status of a feedback entity.
/// See AdditionalValidationRules.md §3AE(1)(a).
/// </summary>
public enum FeedbackStatus
{
    /// <summary>Feedback exists but has not been approved by the teacher. Feedback in this status shall not be dispatched.</summary>
    PROVISIONAL,

    /// <summary>Feedback has been approved and is awaiting dispatch or acknowledgement.</summary>
    READY,

    /// <summary>Feedback has been dispatched and acknowledged by the student device.</summary>
    DELIVERED
}
