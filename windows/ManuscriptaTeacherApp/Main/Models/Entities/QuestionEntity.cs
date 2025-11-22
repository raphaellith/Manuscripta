using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Text.Json.Nodes;

namespace Main.Models.Entities;

[Table("Questions")]
public class QuestionEntity
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    public int Id { get; set; }

    [Required]
    public int MaterialId { get; set; }

    [Required]
    [MaxLength(1000)]
    public string? QuestionText { get; set; }

    [Required]
    [MaxLength(100)]
    public string? QuestionType { get; set; }

    public string? Options { get; set; } // JSON

    [MaxLength(500)]
    public string? CorrectAnswer { get; set; }

    // Foreign key navigation
    [ForeignKey("MaterialId")]
    public MaterialEntity? Material { get; set; }

    // Navigation property
    public ICollection<ResponseEntity> Responses { get; set; } = new List<ResponseEntity>();
}