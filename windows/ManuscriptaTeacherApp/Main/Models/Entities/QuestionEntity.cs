using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Text.Json.Nodes;

namespace Main.Models.Entities;

[Table("Questions")]
public class QuestionEntity
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    public int Id { get; set; }

    [Required]
    public int MaterialId { get; set; }

    [Required]
    [MaxLength(1000)]
    public string QuestionText { get; set; } = null!;

    [Required]
    [MaxLength(100)]
    public string QuestionType { get; set; } = null!;

    [Column(TypeName = "nvarchar(max)")]
    public string Options { get; set; } = null!; // JSON

    [MaxLength(500)]
    public string CorrectAnswer { get; set; } = null!;

    // Foreign key navigation
    [ForeignKey("MaterialId")]
    public virtual MaterialEntity Material { get; set; } = null!;

    // Navigation property
    public virtual ICollection<ResponseEntity> Responses { get; set; } = new List<ResponseEntity>();
}