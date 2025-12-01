using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Main.Models.Entities;

/// <summary>
/// Data entity for persisting responses to the database.
/// Uses compositional design with generic Answer field.
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

    // Foreign key navigation (internal: available to services/repositories within assembly)
    [ForeignKey("QuestionId")]
    internal QuestionDataEntity? Question { get; private set; }

    private ResponseDataEntity() { }

    public ResponseDataEntity(Guid id, Guid questionId, string? answer, bool isCorrect = false, DateTime? timestamp = null, bool synced = false)
    {
        Id = id;
        QuestionId = questionId;
        Answer = answer;
        IsCorrect = isCorrect;
        Timestamp = timestamp ?? DateTime.UtcNow;
    }
}