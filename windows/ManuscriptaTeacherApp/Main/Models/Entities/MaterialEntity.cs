using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Text.Json.Nodes;
using System.Collections.Generic;

using Main.Models.Enums;

namespace Main.Models.Entities;

[Table("Materials")]
public class MaterialEntity
{
    [Key]  // Primary key
    [DatabaseGenerated(DatabaseGeneratedOption.None)]
    public Guid Id {get; set;}

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
    public long Timestamp {get; set;}
    
    // TODO: Change this to type HashSet<SessionEntity> 
    // The HashSet contains SessionEntity instances for which
    // the Android device's corresponding QuestionEntity data is synchronised with this one
    [Required]
    public bool Synced { get; set; }
}