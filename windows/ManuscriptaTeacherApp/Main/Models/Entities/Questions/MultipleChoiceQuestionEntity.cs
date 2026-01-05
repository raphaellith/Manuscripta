using System.ComponentModel.DataAnnotations;
using Main.Models.Enums;

namespace Main.Models.Entities.Questions;

/// <summary>
/// Represents a multiple choice question with a list of options.
/// The correct answer is stored as the index of the correct option.
/// </summary>
public class MultipleChoiceQuestionEntity : QuestionEntity
{
    [Required]
    public List<string> Options { get; set; } = new();

    [Required]
    public int CorrectAnswerIndex { get; set; }

    private MultipleChoiceQuestionEntity() : base() { }

    public MultipleChoiceQuestionEntity(Guid id, Guid materialId, string questionText, List<string> options, int correctAnswerIndex, int? maxScore = null)
        : base(id, materialId, questionText, QuestionType.MULTIPLE_CHOICE, maxScore)
    {
        Options = options ?? throw new ArgumentNullException(nameof(options));
        
        if (options.Count == 0)
            throw new ArgumentException("Options list cannot be empty for multiple choice questions.", nameof(options));
        
        if (correctAnswerIndex < 0 || correctAnswerIndex >= options.Count)
            throw new ArgumentOutOfRangeException(nameof(correctAnswerIndex), "Correct answer index must be a valid index in the options list.");

        CorrectAnswerIndex = correctAnswerIndex;
    }
}
