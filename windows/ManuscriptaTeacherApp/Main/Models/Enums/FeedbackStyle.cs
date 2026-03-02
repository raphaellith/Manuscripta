namespace Main.Models.Enums;

/// <summary>
/// Represents the feedback style for student device configuration.
/// See Validation Rules.md §2G(1)(b).
/// </summary>
public enum FeedbackStyle
{
    /// <summary>Correct/Incorrect feedback is shown immediately.</summary>
    IMMEDIATE,

    /// <summary>Only "Response Submitted" confirmation is shown.</summary>
    NEUTRAL
}
