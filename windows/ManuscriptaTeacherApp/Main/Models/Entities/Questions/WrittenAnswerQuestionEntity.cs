using System.ComponentModel.DataAnnotations;
using Main.Models.Enums;

namespace Main.Models.Entities.Questions;

/// <summary>
/// Represents a question with a written/typed answer.
/// </summary>
public class WrittenAnswerQuestionEntity : QuestionEntity
{
    /// <summary>
    /// The expected correct answer for auto-marking.
    /// Null means no correct answer is set (auto-marking disabled).
    /// Per Session Interaction Spec §4(2).
    /// </summary>
    [MaxLength(500)]
    public string? CorrectAnswer { get; set; }

    private WrittenAnswerQuestionEntity() : base() { }

    public WrittenAnswerQuestionEntity(Guid id, Guid materialId, string questionText, string? correctAnswer, int? maxScore = null)
        : base(id, materialId, questionText, QuestionType.WRITTEN_ANSWER, maxScore)
    {
        CorrectAnswer = correctAnswer;
    }
}
