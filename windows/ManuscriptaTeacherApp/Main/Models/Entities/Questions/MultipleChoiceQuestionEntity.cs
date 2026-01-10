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

    /// <summary>
    /// Index of the correct answer in the Options list.
    /// Null means no correct answer is set (auto-marking disabled).
    /// Per Session Interaction Spec §4(2).
    /// </summary>
    public int? CorrectAnswerIndex { get; set; }

    private MultipleChoiceQuestionEntity() : base() { }

    public MultipleChoiceQuestionEntity(Guid id, Guid materialId, string questionText, List<string> options, int? correctAnswerIndex, int? maxScore = null)
        : base(id, materialId, questionText, QuestionType.MULTIPLE_CHOICE, maxScore)
    {
        Options = options ?? throw new ArgumentNullException(nameof(options));
        
        if (options.Count == 0)
            throw new ArgumentException("Options list cannot be empty for multiple choice questions.", nameof(options));
        
        // Only validate if a correct answer is specified
        if (correctAnswerIndex.HasValue && (correctAnswerIndex.Value < 0 || correctAnswerIndex.Value >= options.Count))
            throw new ArgumentOutOfRangeException(nameof(correctAnswerIndex), "Correct answer index must be a valid index in the options list.");

        CorrectAnswerIndex = correctAnswerIndex;
    }
}
