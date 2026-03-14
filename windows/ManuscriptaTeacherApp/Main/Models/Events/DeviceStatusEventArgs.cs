namespace Main.Models.Events;

/// <summary>
/// Event arguments for device status update notifications.
/// Raised when a STATUS_UPDATE (0x10) message is received per Session Interaction.md §2.
/// </summary>
public class DeviceStatusEventArgs : EventArgs
{
    /// <summary>
    /// Gets the device ID that sent the status update.
    /// </summary>
    public string DeviceId { get; }

    /// <summary>
    /// Gets the status string reported by the device.
    /// </summary>
    public string Status { get; }

    /// <summary>
    /// Gets the battery level (0-100).
    /// </summary>
    public int BatteryLevel { get; }

    /// <summary>
    /// Gets the current material ID the device is viewing.
    /// </summary>
    public string? CurrentMaterialId { get; }

    /// <summary>
    /// Gets the student's current view position.
    /// </summary>
    public string? StudentView { get; }

    /// <summary>
    /// Gets the timestamp of the status update.
    /// </summary>
    public long Timestamp { get; }

    public DeviceStatusEventArgs(
        string deviceId,
        string status,
        int batteryLevel,
        string? currentMaterialId,
        string? studentView,
        long timestamp)
    {
        DeviceId = deviceId;
        Status = status;
        BatteryLevel = batteryLevel;
        CurrentMaterialId = currentMaterialId;
        StudentView = studentView;
        Timestamp = timestamp;
    }
}
