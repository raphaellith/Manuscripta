using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Main.Models.Enums;

namespace Main.Models.Entities;

/// <summary>
/// Data entity for persisting questions to the database.
/// Uses compositional design with optional fields for different question types.
/// </summary>
[Table("Questions")]
public class QuestionDataEntity
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.None)]
    public Guid Id { get; set; }

    [Required]
    public Guid MaterialId { get; set; }

    [Required]
    [MaxLength(1000)]
    public string? QuestionText { get; set; }

    [Required]
    [MaxLength(100)]
    public QuestionType QuestionType { get; set; }
    
    public List<string>? Options { get; set; }

    [MaxLength(500)]
    public string? CorrectAnswer { get; set; }

    /// <summary>
    /// Mark scheme for AI-marking. Per AdditionalValidationRules §2E(1)(a).
    /// </summary>
    [MaxLength(2000)]
    public string? MarkScheme { get; set; }

    /// <summary>
    /// The maximum number of marks available for this question.
    /// Per Validation Rules §2B(2)(c).
    /// </summary>
    public int? MaxScore { get; set; }

    // Foreign key navigation (internal: available to services/repositories within assembly)
    [ForeignKey("MaterialId")]
    internal MaterialDataEntity? Material { get; set; }
}