using System.ComponentModel.DataAnnotations;
using Main.Models.Enums;

namespace Main.Models.Entities.Questions;

/// <summary>
/// Base class for polymorphic question entities.
/// Uses Table-Per-Hierarchy (TPH) inheritance strategy.
/// </summary>
public abstract class QuestionEntity
{
    [Key]
    [Required]
    public Guid Id { get; set; }

    [Required]
    public Guid MaterialId { get; set; }

    [Required]
    [MaxLength(1000)]
    public string QuestionText { get; set; } = string.Empty;

    [Required]
    public QuestionType QuestionType { get; set; }

    /// <summary>
    /// The maximum number of marks available for this question.
    /// Per Validation Rules §2B(2)(c).
    /// </summary>
    public int? MaxScore { get; set; }

    protected QuestionEntity() { }

    protected QuestionEntity(Guid id, Guid materialId, string questionText, QuestionType questionType, int? maxScore = null)
    {
        Id = id;
        MaterialId = materialId;
        QuestionText = questionText;
        QuestionType = questionType;
        MaxScore = maxScore;
    }
}
