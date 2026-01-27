using Main.Models.Entities.Questions;
using Main.Models.Enums;

namespace Main.Models.Dtos;

/// <summary>
/// Flattened DTO for returning question data via SignalR.
/// Required because polymorphic serialization doesn't include derived class properties.
/// </summary>
public record InternalQuestionResponseDto(
    Guid Id,
    Guid MaterialId,
    QuestionType QuestionType,
    string QuestionText,
    int? MaxScore,
    List<string>? Options = null,           // For MULTIPLE_CHOICE
    int? CorrectAnswerIndex = null,         // For MULTIPLE_CHOICE (null = auto-marking disabled)
    string? CorrectAnswer = null,           // For WRITTEN_ANSWER (null = auto-marking disabled)
    string? MarkScheme = null)              // Per §2E(1)(a) - for AI-marking
{
    /// <summary>
    /// Creates a DTO from a QuestionEntity, extracting type-specific properties.
    /// </summary>
    public static InternalQuestionResponseDto FromEntity(QuestionEntity entity)
    {
        return entity switch
        {
            MultipleChoiceQuestionEntity mcq => new InternalQuestionResponseDto(
                mcq.Id,
                mcq.MaterialId,
                mcq.QuestionType,
                mcq.QuestionText,
                mcq.MaxScore,
                mcq.Options,
                mcq.CorrectAnswerIndex),
            WrittenAnswerQuestionEntity waq => new InternalQuestionResponseDto(
                waq.Id,
                waq.MaterialId,
                waq.QuestionType,
                waq.QuestionText,
                waq.MaxScore,
                null,
                null,
                waq.CorrectAnswer,
                waq.MarkScheme),
            _ => new InternalQuestionResponseDto(
                entity.Id,
                entity.MaterialId,
                entity.QuestionType,
                entity.QuestionText,
                entity.MaxScore)
        };
    }
}
