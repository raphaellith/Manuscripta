using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

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
    public Guid Id { get; set; }

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

    public ExternalDeviceEntity() { }

    public ExternalDeviceEntity(Guid id, string name, ExternalDeviceType type)
    {
        Id = id;
        Name = name;
        Type = type;
    }
}
