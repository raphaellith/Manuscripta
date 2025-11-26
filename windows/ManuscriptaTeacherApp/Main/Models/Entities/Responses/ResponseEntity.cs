using System.ComponentModel.DataAnnotations;

namespace Main.Models.Entities.Responses;

/// <summary>
/// Base class for polymorphic response entities.
/// Uses Table-Per-Hierarchy (TPH) inheritance strategy.
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

    public bool? IsCorrect { get; set; }

    protected ResponseEntity() { }

    protected ResponseEntity(Guid id, Guid questionId, DateTime? timestamp = null, bool? isCorrect = null)
    {
        Id = id;
        QuestionId = questionId;
        Timestamp = timestamp ?? DateTime.UtcNow;
        IsCorrect = isCorrect;
    }
}
