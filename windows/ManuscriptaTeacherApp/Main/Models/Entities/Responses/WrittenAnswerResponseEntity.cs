using System.ComponentModel.DataAnnotations;

namespace Main.Models.Entities.Responses;

/// <summary>
/// Represents a response to a written answer question.
/// </summary>
public class WrittenAnswerResponseEntity : ResponseEntity
{
    [Required]
    [MaxLength(1000)]
    public string Answer { get; set; } = string.Empty;

    private WrittenAnswerResponseEntity() : base() { }

    public WrittenAnswerResponseEntity(Guid id, Guid questionId, string answer, DateTime? timestamp = null, bool? isCorrect = null)
        : base(id, questionId, timestamp, isCorrect)
    {
        Answer = answer ?? throw new ArgumentNullException(nameof(answer));
    }
}
