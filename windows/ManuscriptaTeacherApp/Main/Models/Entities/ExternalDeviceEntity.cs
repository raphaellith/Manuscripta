using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;
using Main.Models.Enums;

namespace Main.Models.Entities;

/// <summary>
/// Defines the types of external devices capable of receiving materials.
/// Per ExternalDeviceIntegrationSpecification §1(2).
/// </summary>
public enum ExternalDeviceType
{
    REMARKABLE,
    KINDLE
}

/// <summary>
/// Represents an external device that has been paired with the application.
/// Per AdditionalValidationRules §3D.
/// </summary>
public class ExternalDeviceEntity
{
    [Key]
    public Guid DeviceId { get; set; }

    /// <summary>
    /// The user-friendly name of the device.
    /// </summary>
    public string Name { get; set; }

    /// <summary>
    /// The type of external device.
    /// </summary>
    public ExternalDeviceType Type { get; set; }

    /// <summary>
    /// Type-specific configuration data (e.g. Kindle email address).
    /// </summary>
    public string? ConfigurationData { get; set; }

    /// <summary>
    /// Per-device line pattern override for written-answer areas.
    /// Per AdditionalValidationRules §3D(1)(e).
    /// </summary>
    public LinePatternType? LinePatternType { get; set; }

    /// <summary>
    /// Per-device line spacing override for written-answer areas.
    /// Per AdditionalValidationRules §3D(1)(f).
    /// </summary>
    public LineSpacingPreset? LineSpacingPreset { get; set; }

    /// <summary>
    /// Per-device body text font size override.
    /// Per AdditionalValidationRules §3D(1)(g).
    /// </summary>
    public FontSizePreset? FontSizePreset { get; set; }

    public ExternalDeviceEntity() { }

    public ExternalDeviceEntity(Guid id, string name, ExternalDeviceType type)
    {
        DeviceId = id;
        Name = name;
        Type = type;
    }
}
