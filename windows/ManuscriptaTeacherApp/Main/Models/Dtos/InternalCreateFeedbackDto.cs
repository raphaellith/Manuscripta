using System.ComponentModel.DataAnnotations;

namespace Main.Models.Dtos;

/// <summary>
/// DTO for creating feedback.
/// Per NetworkingAPISpec §1(1)(h)(i).
/// </summary>
public class InternalCreateFeedbackDto
{
    [Required]
    public Guid ResponseId { get; set; }

    [MaxLength(5000)]
    public string? Text { get; set; }

    public int? Marks { get; set; }
}
