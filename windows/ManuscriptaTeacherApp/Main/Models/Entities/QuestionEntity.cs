using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Main.Models.Enums;

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
    public QuestionType QuestionType { get; set; }
    
    public List<string>? Options { get; set; }

    [MaxLength(500)]
    public string? CorrectAnswer { get; set; }
    
    [Required]
    public bool Synced { get; set; }

    // Foreign key navigation (internal: available to services/repositories within assembly)
    [ForeignKey("MaterialId")]
    internal MaterialEntity? Material { get; set; }
}