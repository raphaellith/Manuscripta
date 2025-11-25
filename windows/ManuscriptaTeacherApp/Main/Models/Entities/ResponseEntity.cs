using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Text.Json.Nodes;

namespace Main.Models.Entities;

[Table("Responses")]
public class ResponseEntity
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.None)]
    public int Id { get; private set; }

    [Required]
    public int QuestionId { get; private set; }

    [Required]
    [MaxLength(1000)]
    public string? Answer { get; private set; }

    public bool IsCorrect { get; private set; }

    [Required]
    public DateTime Timestamp { get; private set; }

    // Foreign key navigation (internal: available to services/repositories within assembly)
    [ForeignKey("QuestionId")]
    internal QuestionEntity? Question { get; private set; }

    private ResponseEntity() { }

    public ResponseEntity(int id, int questionId, string? answer, bool isCorrect = false, DateTime? timestamp = null, bool synced = false)
    {
        Id = id;
        QuestionId = questionId;
        Answer = answer;
        IsCorrect = isCorrect;
        Timestamp = timestamp ?? DateTime.UtcNow;
    }
}