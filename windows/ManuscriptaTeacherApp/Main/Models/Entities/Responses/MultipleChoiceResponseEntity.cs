using System.ComponentModel.DataAnnotations;

namespace Main.Models.Entities.Responses;

/// <summary>
/// Represents a response to a multiple choice question.
/// The answer is stored as the index of the selected option.
/// </summary>
public class MultipleChoiceResponseEntity : ResponseEntity
{
    [Required]
    public int AnswerIndex { get; set; }

    private MultipleChoiceResponseEntity() : base() { }

    public MultipleChoiceResponseEntity(Guid id, Guid questionId, int answerIndex, DateTime? timestamp = null, bool? isCorrect = null)
        : base(id, questionId, timestamp, isCorrect)
    {
        // Allow any integer here (including negative). Business-rule validation
        // (e.g., index range checks and type matching) is performed in the
        // ResponseService so that errors are surfaced as InvalidOperationException
        // with consistent messages during service operations.

        AnswerIndex = answerIndex;
    }
}
