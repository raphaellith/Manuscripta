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
                CorrectAnswer = mc.CorrectAnswerIndex.ToString()
            },
            TrueFalseQuestionEntity tf => new QuestionDataEntity
            {
                Id = tf.Id,
                MaterialId = tf.MaterialId,
                QuestionText = tf.QuestionText,
                QuestionType = QuestionType.TRUE_FALSE,
                Options = null,
                CorrectAnswer = tf.CorrectAnswer.ToString()
            },
            WrittenAnswerQuestionEntity wa => new QuestionDataEntity
            {
                Id = wa.Id,
                MaterialId = wa.MaterialId,
                QuestionText = wa.QuestionText,
                QuestionType = QuestionType.WRITTEN_ANSWER,
                Options = null,
                CorrectAnswer = wa.CorrectAnswer
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
                correctAnswerIndex: ParseInt(dataEntity.CorrectAnswer)
            ),
            QuestionType.TRUE_FALSE => new TrueFalseQuestionEntity(
                id: dataEntity.Id,
                materialId: dataEntity.MaterialId,
                questionText: dataEntity.QuestionText ?? string.Empty,
                correctAnswer: ParseBool(dataEntity.CorrectAnswer)
            ),
            QuestionType.WRITTEN_ANSWER => new WrittenAnswerQuestionEntity(
                id: dataEntity.Id,
                materialId: dataEntity.MaterialId,
                questionText: dataEntity.QuestionText ?? string.Empty,
                correctAnswer: dataEntity.CorrectAnswer ?? string.Empty
            ),
            _ => throw new InvalidOperationException($"Unknown question type: {dataEntity.QuestionType}")
        };
    }

    private static int ParseInt(string? value)
    {
        if (string.IsNullOrEmpty(value) || !int.TryParse(value, out int result))
            throw new InvalidOperationException($"Invalid integer value for correct answer: {value}");
        return result;
    }

    private static bool ParseBool(string? value)
    {
        if (string.IsNullOrEmpty(value) || !bool.TryParse(value, out bool result))
            throw new InvalidOperationException($"Invalid boolean value for correct answer: {value}");
        return result;
    }
}
