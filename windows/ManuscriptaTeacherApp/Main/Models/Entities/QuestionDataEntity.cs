using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Main.Models.Enums;

namespace Main.Models.Entities;

/// <summary>
/// Data entity for persisting questions to the database.
/// Uses compositional design with optional fields for different question types.
/// </summary>
[Table("Questions")]
public class QuestionDataEntity
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.None)]
    public Guid Id { get; set; }

    [Required]
    public Guid MaterialId { get; set; }

    [Required]
    [MaxLength(1000)]
    public string? QuestionText { get; set; }

    [Required]
    [MaxLength(100)]
    public QuestionType QuestionType { get; set; }
    
    public List<string>? Options { get; set; }

    [MaxLength(500)]
    public string? CorrectAnswer { get; set; }
    
    // TODO: Change this to type HashSet<SessionEntity> 
    // The HashSet contains SessionEntity instances for which
    // the Android device's corresponding QuestionEntity data is synchronised with this one
    [Required]
    public bool Synced { get; set; }

    // Foreign key navigation (internal: available to services/repositories within assembly)
    [ForeignKey("MaterialId")]
    internal MaterialDataEntity? Material { get; set; }
}