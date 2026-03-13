using System.ComponentModel.DataAnnotations;
using System.Text.Json.Nodes;
using Main.Models.Enums;

namespace Main.Models.Entities.Materials;

/// <summary>
/// Base class for polymorphic material entities.
/// Uses Table-Per-Hierarchy (TPH) inheritance strategy.
/// Per AdditionalValidationRules.md §2D.
/// </summary>
public abstract class MaterialEntity { 
	[Key]
  	[Required]
 	public Guid Id { get; set; }

    /// <summary>
    /// References the lesson to which this material belongs.
    /// Per AdditionalValidationRules.md §2D(1)(a).
    /// </summary>
    [Required]
    public Guid LessonId { get; set; }

 	[Required]
 	[MaxLength(500)]
 	public string Title { get; set; } = string.Empty;

 	[Required] 
	public string Content { get; set; } = string.Empty;

	[Required]
	public MaterialType MaterialType { get; set; }

  	public string? Metadata { get; set; }

  	public JsonArray? VocabularyTerms { get; set; }

  	[Required]
  	// Use DateTime for timestamps
  	public DateTime Timestamp { get; set; }

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
    /// Optional line pattern type for written-answer areas in the generated PDF.
    /// Per AdditionalValidationRules.md §2D(2)(c). Null = use global default.
    /// </summary>
    public LinePatternType? LinePatternType { get; set; }

    /// <summary>
    /// Optional line spacing preset for written-answer areas.
    /// Per AdditionalValidationRules.md §2D(2)(d). Null = use global default.
    /// </summary>
    public LineSpacingPreset? LineSpacingPreset { get; set; }

    /// <summary>
    /// Optional body text font size preset for the generated PDF.
    /// Per AdditionalValidationRules.md §2D(2)(e). Null = use global default.
    /// </summary>
    public FontSizePreset? FontSizePreset { get; set; }

  	protected MaterialEntity() { }

  	protected MaterialEntity(Guid id, Guid lessonId, string title, string content, MaterialType materialType, DateTime? timestamp = null, string? metadata = null, JsonArray? vocabularyTerms = null, int? readingAge = null, int? actualAge = null, LinePatternType? linePatternType = null, LineSpacingPreset? lineSpacingPreset = null, FontSizePreset? fontSizePreset = null) {
    	Id = id;
        LessonId = lessonId;
    	Title = title ?? throw new ArgumentNullException(nameof(title));
    	Content = content ?? throw new ArgumentNullException(nameof(content));
    	MaterialType = materialType;
    	Timestamp = timestamp ?? DateTime.UtcNow;
    	Metadata = metadata;
    	VocabularyTerms = vocabularyTerms;
        ReadingAge = readingAge;
        ActualAge = actualAge;
        LinePatternType = linePatternType;
        LineSpacingPreset = lineSpacingPreset;
        FontSizePreset = fontSizePreset;
 	}
}
