namespace Main.Models.Enums;

/// <summary>
/// Session status enum as defined in Validation Rules §2D(1)(c).
/// </summary>
public enum SessionStatus
{
    /// <summary>
    /// Session is currently active/ongoing.
    /// </summary>
    ACTIVE,

    /// <summary>
    /// Session has been paused.
    /// </summary>
    PAUSED,

    /// <summary>
    /// Session completed normally.
    /// </summary>
    COMPLETED,

    /// <summary>
    /// Session was cancelled before completion.
    /// </summary>
    CANCELLED
}
