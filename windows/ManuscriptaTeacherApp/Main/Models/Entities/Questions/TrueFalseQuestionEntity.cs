using System.ComponentModel.DataAnnotations;
using Main.Models.Enums;

namespace Main.Models.Entities.Questions;

/// <summary>
/// Represents a true/false question.
/// </summary>
public class TrueFalseQuestionEntity : QuestionEntity
{
    [Required]
    public bool CorrectAnswer { get; set; }

    private TrueFalseQuestionEntity() : base() { }

    public TrueFalseQuestionEntity(Guid id, Guid materialId, string questionText, bool correctAnswer)
        : base(id, materialId, questionText, QuestionType.TRUE_FALSE)
    {
        CorrectAnswer = correctAnswer;
    }
}
