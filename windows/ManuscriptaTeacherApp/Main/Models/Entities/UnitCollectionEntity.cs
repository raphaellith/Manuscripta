using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Main.Models.Entities;

/// <summary>
/// Represents a unit collection in the material hierarchy.
/// Per AdditionalValidationRules.md §1(1)(a) - Top level of hierarchy.
/// Per AdditionalValidationRules.md §2A(1).
/// </summary>
[Table("UnitCollections")]
public class UnitCollectionEntity
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.None)]
    public Guid Id { get; set; }

    /// <summary>
    /// The title of the unit collection.
    /// Per §2A(1)(a): Maximum 500 characters.
    /// </summary>
    [Required]
    [MaxLength(500)]
    public string Title { get; set; } = string.Empty;

    public UnitCollectionEntity() { }

    public UnitCollectionEntity(Guid id, string title)
    {
        Id = id;
        Title = title ?? throw new ArgumentNullException(nameof(title));
    }
}
