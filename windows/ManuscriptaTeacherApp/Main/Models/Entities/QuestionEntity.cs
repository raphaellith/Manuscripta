using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Text.Json.Nodes;
using System.Collections.Generic;

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
    
    [Required]
    public bool Synced { get; set; }

    // Foreign key navigation (internal: available to services/repositories within assembly)
    [ForeignKey("MaterialId")]
    internal MaterialEntity? Material { get; set; }
}