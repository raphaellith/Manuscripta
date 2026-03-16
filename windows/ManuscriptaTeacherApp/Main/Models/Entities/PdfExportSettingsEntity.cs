using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Main.Models.Enums;

namespace Main.Models.Entities;

/// <summary>
/// Represents global default PDF export settings.
/// Per AdditionalValidationRules.md §3F.
/// Long-term persisted per PersistenceAndCascadingRules §1(1)(k).
/// </summary>
[Table("PdfExportSettings")]
public class PdfExportSettingsEntity
{
    /// <summary>
    /// Well-known singleton ID for the global defaults row.
    /// Per §3F(2)(b): Only one PdfExportSettingsEntity may exist at any time.
    /// </summary>
    public static readonly Guid DefaultId = new("00000000-0000-0000-0000-000000000002");

    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.None)]
    public Guid Id { get; set; }

    /// <summary>
    /// The default line pattern for written-answer areas.
    /// Per §3F(1)(a): Default value RULED.
    /// </summary>
    [Required]
    public LinePatternType LinePatternType { get; set; }

    /// <summary>
    /// The default line spacing for written-answer areas.
    /// Per §3F(1)(b): Default value MEDIUM.
    /// </summary>
    [Required]
    public LineSpacingPreset LineSpacingPreset { get; set; }

    /// <summary>
    /// The default body text font size.
    /// Per §3F(1)(c): Default value MEDIUM.
    /// </summary>
    [Required]
    public FontSizePreset FontSizePreset { get; set; }

    /// <summary>
    /// Creates a new PdfExportSettingsEntity with spec-mandated defaults.
    /// Per AdditionalValidationRules §3F(1).
    /// </summary>
    public static PdfExportSettingsEntity CreateDefault() => new()
    {
        Id = DefaultId,
        LinePatternType = LinePatternType.RULED,
        LineSpacingPreset = LineSpacingPreset.MEDIUM,
        FontSizePreset = FontSizePreset.MEDIUM
    };
}
