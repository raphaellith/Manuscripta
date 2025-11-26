using System.ComponentModel.DataAnnotations;

namespace Main.Models.Entities.Responses;

/// <summary>
/// Represents a response to a true/false question.
/// </summary>
public class TrueFalseResponseEntity : ResponseEntity
{
    [Required]
    public bool Answer { get; set; }

    private TrueFalseResponseEntity() : base() { }

    public TrueFalseResponseEntity(Guid id, Guid questionId, bool answer, DateTime? timestamp = null, bool? isCorrect = null)
        : base(id, questionId, timestamp, isCorrect)
    {
        Answer = answer;
    }
}
