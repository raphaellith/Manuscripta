using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Main.Models.Entities;

/// <summary>
/// Data entity for persisting feedback to in-memory storage.
/// Validation rules defined in Validation Rules §2F.
/// Per PersistenceAndCascadingRules.md §1(2)(c), uses short-term persistence.
/// </summary>
[Table("Feedback")]
public class FeedbackDataEntity
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.None)]
    public Guid Id { get; private set; }

    /// <summary>
    /// The response targeted by this feedback.
    /// Per Validation Rules §2F(1)(a).
    /// </summary>
    [Required]
    public Guid ResponseId { get; private set; }

    /// <summary>
    /// Textual feedback.
    /// Per Validation Rules §2F(1)(b)(i).
    /// </summary>
    [MaxLength(5000)]
    public string? Text { get; private set; }

    /// <summary>
    /// Number of marks awarded.
    /// Per Validation Rules §2F(1)(b)(ii).
    /// </summary>
    public int? Marks { get; private set; }

    // Foreign key navigation (internal: available to services/repositories within assembly)
    [ForeignKey("ResponseId")]
    internal ResponseDataEntity? Response { get; private set; }

    private FeedbackDataEntity() { }

    public FeedbackDataEntity(Guid id, Guid responseId, string? text = null, int? marks = null)
    {
        // Per §2F(1)(b): At least one of Text or Marks must be present
        if (string.IsNullOrWhiteSpace(text) && marks == null)
            throw new ArgumentException("At least one of Text or Marks must be provided.");

        Id = id;
        ResponseId = responseId;
        Text = text;
        Marks = marks;
    }
}
