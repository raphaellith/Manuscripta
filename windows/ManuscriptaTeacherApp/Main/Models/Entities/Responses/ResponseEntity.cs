using System.ComponentModel.DataAnnotations;

namespace Main.Models.Entities.Responses;

/// <summary>
/// Base class for polymorphic response entities.
/// Uses Table-Per-Hierarchy (TPH) inheritance strategy.
/// Validation rules defined in Validation Rules §2C.
/// </summary>
public abstract class ResponseEntity
{
    [Key]
    [Required]
    public Guid Id { get; set; }

    [Required]
    public Guid QuestionId { get; set; }

    [Required]
    public DateTime Timestamp { get; set; }

    /// <summary>
    /// The device ID the response is from.
    /// §2C(1)(d)
    /// </summary>
    [Required]
    public Guid DeviceId { get; set; }

    public bool? IsCorrect { get; set; }

    protected ResponseEntity() { }

    protected ResponseEntity(Guid id, Guid questionId, Guid deviceId, DateTime? timestamp = null, bool? isCorrect = null)
    {
        Id = id;
        QuestionId = questionId;
        DeviceId = deviceId;
        Timestamp = timestamp ?? DateTime.UtcNow;
        IsCorrect = isCorrect;
    }
}
