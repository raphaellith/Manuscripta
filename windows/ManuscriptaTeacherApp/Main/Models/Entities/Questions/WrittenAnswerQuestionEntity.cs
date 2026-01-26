using System.ComponentModel.DataAnnotations;
using Main.Models.Enums;

namespace Main.Models.Entities.Questions;

/// <summary>
/// Represents a question with a written/typed answer.
/// </summary>
public class WrittenAnswerQuestionEntity : QuestionEntity
{
    /// <summary>
    /// The expected correct answer for auto-marking (exact match).
    /// Null means no correct answer is set (auto-marking disabled).
    /// Per Session Interaction Spec §4(2).
    /// Mutually exclusive with MarkScheme per AdditionalValidationRules §2E(2)(b).
    /// </summary>
    [MaxLength(500)]
    public string? CorrectAnswer { get; set; }

    /// <summary>
    /// Mark scheme for AI-marking. Per AdditionalValidationRules §2E(1)(a).
    /// Mutually exclusive with CorrectAnswer per §2E(2)(b).
    /// </summary>
    [MaxLength(2000)]
    public string? MarkScheme { get; set; }

    private WrittenAnswerQuestionEntity() : base() { }

    public WrittenAnswerQuestionEntity(Guid id, Guid materialId, string questionText, string? correctAnswer, string? markScheme = null, int? maxScore = null)
        : base(id, materialId, questionText, QuestionType.WRITTEN_ANSWER, maxScore)
    {
        CorrectAnswer = correctAnswer;
        MarkScheme = markScheme;
    }
}
