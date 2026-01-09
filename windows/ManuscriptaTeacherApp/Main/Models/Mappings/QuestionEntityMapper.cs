using Main.Models.Entities;
using Main.Models.Entities.Questions;
using Main.Models.Enums;

namespace Main.Models.Mappings;

/// <summary>
/// Provides mapping methods between QuestionEntity (polymorphic) and QuestionDataEntity (persistence).
/// </summary>
public static class QuestionEntityMapper
{
    /// <summary>
    /// Maps a polymorphic QuestionEntity to a QuestionDataEntity for persistence.
    /// </summary>
    public static QuestionDataEntity ToDataEntity(QuestionEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        return entity switch
        {
            MultipleChoiceQuestionEntity mc => new QuestionDataEntity
            {
                Id = mc.Id,
                MaterialId = mc.MaterialId,
                QuestionText = mc.QuestionText,
                QuestionType = QuestionType.MULTIPLE_CHOICE,
                Options = mc.Options,
                CorrectAnswer = mc.CorrectAnswerIndex?.ToString(),  // null if no correct answer
                MaxScore = mc.MaxScore
            },
            WrittenAnswerQuestionEntity wa => new QuestionDataEntity
            {
                Id = wa.Id,
                MaterialId = wa.MaterialId,
                QuestionText = wa.QuestionText,
                QuestionType = QuestionType.WRITTEN_ANSWER,
                Options = null,
                CorrectAnswer = wa.CorrectAnswer,
                MaxScore = wa.MaxScore
            },
            _ => throw new InvalidOperationException($"Unknown question entity type: {entity.GetType().Name}")
        };
    }

    /// <summary>
    /// Maps a QuestionDataEntity to the appropriate polymorphic QuestionEntity subclass.
    /// </summary>
    public static QuestionEntity ToEntity(QuestionDataEntity dataEntity)
    {
        if (dataEntity == null)
            throw new ArgumentNullException(nameof(dataEntity));

        return dataEntity.QuestionType switch
        {
            QuestionType.MULTIPLE_CHOICE => new MultipleChoiceQuestionEntity(
                id: dataEntity.Id,
                materialId: dataEntity.MaterialId,
                questionText: dataEntity.QuestionText ?? string.Empty,
                options: dataEntity.Options ?? new List<string>(),
                correctAnswerIndex: ParseIntOrNull(dataEntity.CorrectAnswer),  // null if no correct answer
                maxScore: dataEntity.MaxScore
            ),
            QuestionType.WRITTEN_ANSWER => new WrittenAnswerQuestionEntity(
                id: dataEntity.Id,
                materialId: dataEntity.MaterialId,
                questionText: dataEntity.QuestionText ?? string.Empty,
                correctAnswer: dataEntity.CorrectAnswer ?? string.Empty,
                maxScore: dataEntity.MaxScore
            ),
            _ => throw new InvalidOperationException($"Unknown question type: {dataEntity.QuestionType}")
        };
    }

    /// <summary>
    /// Parses a string to int, returning null if the string is null/empty or not a valid integer.
    /// </summary>
    private static int? ParseIntOrNull(string? value)
    {
        if (string.IsNullOrEmpty(value))
            return null;
        
        if (int.TryParse(value, out int result))
            return result;
        
        return null;
    }
}
