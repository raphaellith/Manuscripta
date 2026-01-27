using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Main.Models.Entities;

/// <summary>
/// Represents a unit within a unit collection.
/// Per AdditionalValidationRules.md §1(1)(b) - Second level of hierarchy.
/// Per AdditionalValidationRules.md §2B(1).
/// </summary>
[Table("Units")]
public class UnitEntity
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.None)]
    public Guid Id { get; set; }

    /// <summary>
    /// References the unit collection to which this unit belongs.
    /// Per §2B(1)(a).
    /// </summary>
    [Required]
    public Guid UnitCollectionId { get; set; }

    /// <summary>
    /// The title of the unit.
    /// Per §2B(1)(b): Maximum 500 characters.
    /// </summary>
    [Required]
    [MaxLength(500)]
    public string Title { get; set; } = string.Empty;

    // Foreign key navigation
    [ForeignKey("UnitCollectionId")]
    internal UnitCollectionEntity? UnitCollection { get; set; }

    public UnitEntity() { }

    public UnitEntity(Guid id, Guid unitCollectionId, string title)
    {
        Id = id;
        UnitCollectionId = unitCollectionId;
        Title = title ?? throw new ArgumentNullException(nameof(title));
    }
}
