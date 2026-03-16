using System.ComponentModel.DataAnnotations;
using Main.Models.Enums;

namespace Main.Models.Entities;

/// <summary>
/// Domain entity for feedback.
/// Validation rules defined in Validation Rules §2F.
/// </summary>
public class FeedbackEntity
{
    [Key]
    [Required]
    public Guid Id { get; set; }

    /// <summary>
    /// The response targeted by this feedback.
    /// Per Validation Rules §2F(1)(a).
    /// </summary>
    [Required]
    public Guid ResponseId { get; set; }

    /// <summary>
    /// Textual feedback.
    /// Per Validation Rules §2F(1)(b)(i).
    /// </summary>
    [MaxLength(5000)]
    public string? Text { get; set; }

    /// <summary>
    /// Number of marks awarded.
    /// Per Validation Rules §2F(1)(b)(ii).
    /// </summary>
    public int? Marks { get; set; }

    /// <summary>
    /// Status of the feedback (PROVISIONAL, READY, DELIVERED).
    /// Per AdditionalValidationRules §3AE(1)(a).
    /// </summary>
    [Required]
    public FeedbackStatus Status { get; set; } = FeedbackStatus.PROVISIONAL;

    /// <summary>
    /// Timestamp when feedback was created.
    /// </summary>
    public DateTime CreatedAt { get; set; }

    /// <summary>
    /// Convenience property for the feedback text.
    /// </summary>
    public string? FeedbackText
    {
        get => Text;
        set => Text = value;
    }

    public FeedbackEntity() { }

    public FeedbackEntity(Guid id, Guid responseId, string? text = null, int? marks = null)
    {
        // Per §2F(1)(b): At least one of Text or Marks must be present
        if (string.IsNullOrWhiteSpace(text) && marks == null)
            throw new ArgumentException("At least one of Text or Marks must be provided.");

        Id = id;
        ResponseId = responseId;
        Text = text;
        Marks = marks;
        Status = FeedbackStatus.PROVISIONAL;
        CreatedAt = DateTime.UtcNow;
    }
}
