using Main.Models.Enums;
using Main.Models.Entities.Questions;

namespace Main.Models.Dtos;

/// <summary>
/// DTO for QuestionEntity when communicating with Android clients.
/// Per AdditionalValidationRules s1A(2): Excludes Windows-only fields (MarkScheme).
/// Per AdditionalValidationRules s1A(3): Flat composition-like structure matching Validation Rules §2B.
/// </summary>
public record AndroidQuestionDto(
    Guid Id,
    Guid MaterialId,
    QuestionType QuestionType,
    string QuestionText,
    List<string>? Options,
    object? CorrectAnswer,
    int? MaxScore)
{
    /// <summary>
    /// Maps a polymorphic QuestionEntity to an AndroidQuestionDto.
    /// Excludes Windows-only fields per s1A(2).
    /// </summary>
    public static AndroidQuestionDto FromEntity(QuestionEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        return entity switch
        {
            MultipleChoiceQuestionEntity mc => new AndroidQuestionDto(
                Id: mc.Id,
                MaterialId: mc.MaterialId,
                QuestionType: mc.QuestionType,
                QuestionText: mc.QuestionText,
                Options: mc.Options?.ToList(),
                CorrectAnswer: mc.CorrectAnswerIndex,  // int? for MC
                MaxScore: mc.MaxScore
            ),
            WrittenAnswerQuestionEntity wa => new AndroidQuestionDto(
                Id: wa.Id,
                MaterialId: wa.MaterialId,
                QuestionType: wa.QuestionType,
                QuestionText: wa.QuestionText,
                Options: null,
                CorrectAnswer: wa.CorrectAnswer,  // string? for written answer
                MaxScore: wa.MaxScore
                // Note: MarkScheme excluded per s1A(2)
            ),
            _ => throw new InvalidOperationException($"Unknown question entity type: {entity.GetType().Name}")
        };
    }
}
