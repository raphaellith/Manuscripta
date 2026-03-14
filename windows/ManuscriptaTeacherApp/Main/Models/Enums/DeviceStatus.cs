namespace Main.Models.Enums;

/// <summary>
/// Represents the status of a paired Android device.
/// See Validation Rules.md §2E(1)(b) for possible values.
/// </summary>
public enum DeviceStatus
{
    /// <summary>Student is active in the app.</summary>
    ON_TASK,

    /// <summary>No activity for a threshold period.</summary>
    IDLE,

    /// <summary>Device is remotely locked.</summary>
    LOCKED,

    /// <summary>Server-side inferred status when device is unreachable.</summary>
    DISCONNECTED
}
