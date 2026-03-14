namespace Main.Models.Events;

/// <summary>
/// Event arguments for control command timeout notifications.
/// Raised when a control signal expecting implicit acknowledgement times out.
/// Per Session Interaction.md §6(2) and §6(3).
/// </summary>
public class ControlTimeoutEventArgs : EventArgs
{
    /// <summary>
    /// Gets the device ID that the command was sent to.
    /// </summary>
    public string DeviceId { get; }

    /// <summary>
    /// Gets the name of the command that timed out.
    /// E.g., "LOCK_SCREEN", "UNLOCK_SCREEN", "REFRESH_CONFIG".
    /// </summary>
    public string CommandName { get; }

    public ControlTimeoutEventArgs(string deviceId, string commandName)
    {
        DeviceId = deviceId;
        CommandName = commandName;
    }
}
