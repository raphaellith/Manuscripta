using System.ComponentModel.DataAnnotations;

using Main.Models.Enums;

namespace Main.Models.Entities;

/// <summary>
/// Represents the status of a paired Android device at a point in time.
/// See Validation Rules.md §2E for field definitions and constraints.
/// </summary>
public class DeviceStatusEntity
{
    /// <summary>
    /// The unique identifier of the device this status belongs to.
    /// Per §2E(1)(a): References the device the status is linked to.
    /// </summary>
    [Required]
    public Guid DeviceId { get; set; }

    /// <summary>
    /// The current status of the device.
    /// Per §2E(1)(b): Possible values defined in DeviceStatus enum.
    /// </summary>
    [Required]
    public DeviceStatus Status { get; set; }

    /// <summary>
    /// The battery level of the device (0-100).
    /// Per §2E(1)(c) and §2E(2)(b): Must be between 0 and 100.
    /// </summary>
    [Required]
    [Range(0, 100)]
    public int BatteryLevel { get; set; }

    /// <summary>
    /// The material the device is currently viewing.
    /// Per §2E(1)(d): Must reference a valid material.
    /// </summary>
    [Required]
    public Guid CurrentMaterialId { get; set; }

    /// <summary>
    /// Describes the location which the student is viewing.
    /// Per §2E(1)(e): e.g., "page-5"
    /// </summary>
    [Required]
    public string StudentView { get; set; } = string.Empty;

    /// <summary>
    /// The time at which the device status is correct to.
    /// Per §2E(1)(f): Unix timestamp.
    /// </summary>
    [Required]
    public long Timestamp { get; set; }

    /// <summary>
    /// Creates a new DeviceStatusEntity with default values.
    /// </summary>
    public DeviceStatusEntity()
    {
    }

    /// <summary>
    /// Creates a new DeviceStatusEntity with all required fields.
    /// </summary>
    public DeviceStatusEntity(
        Guid deviceId,
        DeviceStatus status,
        int batteryLevel,
        Guid currentMaterialId,
        string studentView,
        long timestamp)
    {
        DeviceId = deviceId;
        Status = status;
        BatteryLevel = batteryLevel;
        CurrentMaterialId = currentMaterialId;
        StudentView = studentView ?? throw new ArgumentNullException(nameof(studentView));
        Timestamp = timestamp;
    }
}
