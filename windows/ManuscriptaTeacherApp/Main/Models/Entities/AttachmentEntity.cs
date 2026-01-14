using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Main.Models.Entities;

/// <summary>
/// Represents an attachment file associated with a material.
/// Per AdditionalValidationRules.md §3B(1).
/// </summary>
[Table("Attachments")]
public class AttachmentEntity
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.None)]
    public Guid Id { get; set; }

    /// <summary>
    /// References the material to which this attachment belongs.
    /// Per §3B(1)(a). Must reference a valid MaterialDataEntity per §3B(2)(a).
    /// </summary>
    [Required]
    public Guid MaterialId { get; set; }

    /// <summary>
    /// Navigation property for cascade delete.
    /// Per PersistenceAndCascadingRules.md §2(6).
    /// </summary>
    [ForeignKey(nameof(MaterialId))]
    public MaterialDataEntity? Material { get; set; }

    /// <summary>
    /// The base name of the attachment file, as inputted by the user.
    /// Per §3B(1)(b).
    /// </summary>
    [Required]
    public string FileBaseName { get; set; } = string.Empty;

    /// <summary>
    /// The extension of the attachment file (png, jpeg, or pdf).
    /// Per §3B(1)(c) and §3B(2)(b).
    /// </summary>
    [Required]
    public string FileExtension { get; set; } = string.Empty;

    public AttachmentEntity() { }

    public AttachmentEntity(Guid id, Guid materialId, string fileBaseName, string fileExtension)
    {
        Id = id;
        MaterialId = materialId;
        FileBaseName = fileBaseName ?? throw new ArgumentNullException(nameof(fileBaseName));
        FileExtension = fileExtension ?? throw new ArgumentNullException(nameof(fileExtension));
    }
}
