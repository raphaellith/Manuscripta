using System.ComponentModel.DataAnnotations;
using Main.Models.Enums;

namespace Main.Models.Entities.Questions;

/// <summary>
/// Represents a question with a written/typed answer.
/// </summary>
public class WrittenAnswerQuestionEntity : QuestionEntity
{
    [Required]
    [MaxLength(500)]
    public string CorrectAnswer { get; set; } = string.Empty;

    private WrittenAnswerQuestionEntity() : base() { }

    public WrittenAnswerQuestionEntity(Guid id, Guid materialId, string questionText, string correctAnswer, int? maxScore = null)
        : base(id, materialId, questionText, QuestionType.WRITTEN_ANSWER, maxScore)
    {
        CorrectAnswer = correctAnswer ?? throw new ArgumentNullException(nameof(correctAnswer));
    }
}
