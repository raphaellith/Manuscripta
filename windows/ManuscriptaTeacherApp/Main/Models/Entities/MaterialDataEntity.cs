using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Text.Json.Nodes;
using System.Collections.Generic;

using Main.Models.Enums;

namespace Main.Models.Entities;

/// <summary>
/// Data entity for persisting materials to the database.
/// Uses compositional design for different material types.
/// Per AdditionalValidationRules.md §2D.
/// </summary>
[Table("Materials")]
public class MaterialDataEntity
{
    [Key]  // Primary key
    [DatabaseGenerated(DatabaseGeneratedOption.None)]
    public Guid Id {get; set;}

    /// <summary>
    /// References the lesson to which this material belongs.
    /// Per AdditionalValidationRules.md §2D(1)(a).
    /// </summary>
    [Required]
    public Guid LessonId { get; set; }

    [Required]
    public MaterialType MaterialType {get; set;}

    [Required]
    [MaxLength(500)]
    public string? Title {get; set;}

    [Required]
    public string? Content {get; set;}

    public string? Metadata {get; set;}

    public JsonArray VocabularyTerms { get; set; } = new();

    [Required]
    // Use DateTime for timestamps
    public DateTime Timestamp {get; set;}
    
    /// <summary>
    /// Optional reading age field.
    /// Per AdditionalValidationRules.md §2D(2)(a).
    /// </summary>
    public int? ReadingAge { get; set; }

    /// <summary>
    /// Optional actual age field.
    /// Per AdditionalValidationRules.md §2D(2)(b).
    /// </summary>
    public int? ActualAge { get; set; }

    /// <summary>
    /// Optional line pattern type for written-answer areas.
    /// Per AdditionalValidationRules.md §2D(2)(c). Null = use global default.
    /// </summary>
    public LinePatternType? LinePatternType { get; set; }

    /// <summary>
    /// Optional line spacing preset for written-answer areas.
    /// Per AdditionalValidationRules.md §2D(2)(d). Null = use global default.
    /// </summary>
    public LineSpacingPreset? LineSpacingPreset { get; set; }

    /// <summary>
    /// Optional body text font size preset.
    /// Per AdditionalValidationRules.md §2D(2)(e). Null = use global default.
    /// </summary>
    public FontSizePreset? FontSizePreset { get; set; }

    // Foreign key navigation
    [ForeignKey("LessonId")]
    internal LessonEntity? Lesson { get; set; }
}
