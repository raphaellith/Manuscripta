using System.ComponentModel.DataAnnotations;
using System.Text.Json.Nodes;
using Main.Models.Enums;

namespace Main.Models.Entities.Materials;

/// <summary>
/// Base class for polymorphic material entities.
/// Uses Table-Per-Hierarchy (TPH) inheritance strategy.
/// </summary>
public abstract class MaterialEntity { 
	[Key]
  	[Required]
 	public Guid Id { get; set; }

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

  	protected MaterialEntity() { }

  	protected MaterialEntity(Guid id, string title, string content, MaterialType materialType, DateTime? timestamp = null, string? metadata = null, JsonArray? vocabularyTerms = null) {
    	Id = id;
    	Title = title ?? throw new ArgumentNullException(nameof(title));
    	Content = content ?? throw new ArgumentNullException(nameof(content));
    	MaterialType = materialType;
    	Timestamp = timestamp ?? DateTime.UtcNow;
    	Metadata = metadata;
    	VocabularyTerms = vocabularyTerms;
 	}
}
