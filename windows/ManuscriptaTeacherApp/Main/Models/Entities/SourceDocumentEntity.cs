using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Main.Models.Entities;

/// <summary>
/// Represents a source document imported into a unit collection.
/// Per AdditionalValidationRules.md §3A(1).
/// </summary>
[Table("SourceDocuments")]
public class SourceDocumentEntity
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.None)]
    public Guid Id { get; set; }

    /// <summary>
    /// References the unit collection to which this source document is imported.
    /// Per §3A(1)(a). Must reference a valid UnitCollectionEntity per §3A(2)(a).
    /// </summary>
    [Required]
    public Guid UnitCollectionId { get; set; }

    /// <summary>
    /// Navigation property for cascade delete.
    /// Per PersistenceAndCascadingRules.md §2(3A).
    /// </summary>
    [ForeignKey(nameof(UnitCollectionId))]
    public UnitCollectionEntity? UnitCollection { get; set; }

    /// <summary>
    /// A textual transcript of the source document contents.
    /// Per §3A(1)(b).
    /// </summary>
    [Required]
    public string Transcript { get; set; } = string.Empty;

    public SourceDocumentEntity() { }

    public SourceDocumentEntity(Guid id, Guid unitCollectionId, string transcript)
    {
        Id = id;
        UnitCollectionId = unitCollectionId;
        Transcript = transcript ?? throw new ArgumentNullException(nameof(transcript));
    }
}
