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

    protected QuestionEntity() { }

    protected QuestionEntity(Guid id, Guid materialId, string questionText, QuestionType questionType)
    {
        Id = id;
        MaterialId = materialId;
        QuestionText = questionText;
        QuestionType = questionType;
    }
}
