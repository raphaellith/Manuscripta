using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Main.Models.Entities;

/// <summary>
/// Represents a lesson within a unit.
/// Per AdditionalValidationRules.md §1(1)(c) - Third level of hierarchy.
/// Per AdditionalValidationRules.md §2C(1).
/// </summary>
[Table("Lessons")]
public class LessonEntity
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.None)]
    public Guid Id { get; set; }

    /// <summary>
    /// References the unit to which this lesson belongs.
    /// Per §2C(1)(a).
    /// </summary>
    [Required]
    public Guid UnitId { get; set; }

    /// <summary>
    /// The title of the lesson.
    /// Per §2C(1)(b): Maximum 500 characters.
    /// </summary>
    [Required]
    [MaxLength(500)]
    public string Title { get; set; } = string.Empty;

    /// <summary>
    /// The description of the lesson.
    /// Per §2C(1)(c).
    /// </summary>
    [Required]
    public string Description { get; set; } = string.Empty;

    // Foreign key navigation
    [ForeignKey("UnitId")]
    internal UnitEntity? Unit { get; set; }

    public LessonEntity() { }

    public LessonEntity(Guid id, Guid unitId, string title, string description)
    {
        Id = id;
        UnitId = unitId;
        Title = title ?? throw new ArgumentNullException(nameof(title));
        Description = description ?? throw new ArgumentNullException(nameof(description));
    }
}
