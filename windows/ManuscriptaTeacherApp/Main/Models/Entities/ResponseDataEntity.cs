using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Main.Models.Entities;

/// <summary>
/// Data entity for persisting responses to the database.
/// Uses compositional design with generic Answer field.
/// Validation rules defined in Validation Rules §2C.
/// </summary>
[Table("Responses")]
public class ResponseDataEntity
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.None)]
    public Guid Id { get; private set; }

    [Required]
    public Guid QuestionId { get; private set; }

    [Required]
    [MaxLength(1000)]
    public string? Answer { get; private set; }

    public bool IsCorrect { get; private set; }

    [Required]
    public DateTime Timestamp { get; private set; }

    /// <summary>
    /// The device ID the response is from.
    /// §2C(1)(d)
    /// </summary>
    [Required]
    public Guid DeviceId { get; private set; }

    // Foreign key navigation (internal: available to services/repositories within assembly)
    [ForeignKey("QuestionId")]
    internal QuestionDataEntity? Question { get; private set; }

    private ResponseDataEntity() { }

    public ResponseDataEntity(Guid id, Guid questionId, string? answer, Guid deviceId, bool isCorrect = false, DateTime? timestamp = null)
    {
        // TODO: Validate §2C(3)(e): DeviceId must correspond to a valid device
        // This requires DeviceIdValidator.IsValidDeviceId(deviceId) once device registration is implemented

        Id = id;
        QuestionId = questionId;
        Answer = answer;
        DeviceId = deviceId;
        IsCorrect = isCorrect;
        Timestamp = timestamp ?? DateTime.UtcNow;
    }
}