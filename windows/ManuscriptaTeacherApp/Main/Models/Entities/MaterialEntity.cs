using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Text.Json.Nodes;

using Main.Models.Enums;

namespace Main.Models.Entities;

[Table("Materials")]
public class MaterialEntity
{
    [Key]  // Primary key
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    public int Id {get; set;}

    [Required]
    public MaterialType Type {get; set;}

    [Required]
    [MaxLength(500)]
    public string? Title {get; set;}

    [Required]
    public string? Content {get; set;}

    public string? Metadata {get; set;}

    public JsonArray VocabularyTerms { get; set; } = new();

    [Required]
    public long Timestamp {get; set;}

    public ICollection<QuestionEntity> Questions { get; set; } = new List<QuestionEntity>();
}