using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Text.Json.Nodes;

namespace Main.Models.Entities;

[Table("Responses")]
public class ResponseEntity
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.None)]
    public int Id { get; set; }

    [Required]
    public int QuestionId { get; }

    [Required]
    [MaxLength(1000)]
    public string? Answer { get; }

    public bool IsCorrect { get; }

    [Required]
    public DateTime Timestamp { get; } = DateTime.UtcNow;

    public bool Synced { get; } = false;

    // Foreign key navigation
    [ForeignKey("QuestionId")]
    public QuestionEntity? Question { get; }
}