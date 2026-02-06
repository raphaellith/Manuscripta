using System;

namespace Main.Models.Dtos;

/// <summary>
/// DTO for sending response data to the frontend.
/// Flattens polymorphic ResponseEntity structure into a unified format.
/// </summary>
public class InternalResponseDto
{
    public Guid Id { get; set; }
    public Guid QuestionId { get; set; }
    public Guid DeviceId { get; set; }
    public DateTime Timestamp { get; set; }
    public bool? IsCorrect { get; set; }
    
    /// <summary>
    /// The string representation of the response.
    /// For MultipleChoice: The index as a string ("1").
    /// For WrittenAnswer: The text content.
    /// </summary>
    public string ResponseContent { get; set; } = string.Empty;
}
