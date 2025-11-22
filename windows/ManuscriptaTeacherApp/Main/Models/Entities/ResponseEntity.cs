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
    public int QuestionId { get; set; }

    [Required]
    [MaxLength(1000)]
    public string? Answer { get; set; }

    public bool IsCorrect { get; set; }

    [Required]
    public DateTime Timestamp { get; set; } = DateTime.UtcNow;

    public bool Synced { get; set; } = false;

    // Foreign key navigation
    [ForeignKey("QuestionId")]
    public QuestionEntity? Question { get; set; }
}