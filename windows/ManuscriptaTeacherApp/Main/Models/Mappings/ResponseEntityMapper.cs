using Main.Models.Entities;
using Main.Models.Entities.Responses;
using Main.Models.Entities.Questions;

namespace Main.Models.Mappings;

/// <summary>
/// Provides mapping methods between ResponseEntity (polymorphic) and ResponseDataEntity (persistence).
/// </summary>
public static class ResponseEntityMapper
{
    /// <summary>
    /// Maps a polymorphic ResponseEntity to a ResponseDataEntity for persistence.
    /// </summary>
    public static ResponseDataEntity ToDataEntity(ResponseEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        string answer = entity switch
        {
            MultipleChoiceResponseEntity mc => mc.AnswerIndex.ToString(),
            TrueFalseResponseEntity tf => tf.Answer.ToString(),
            WrittenAnswerResponseEntity wa => wa.Answer,
            _ => throw new InvalidOperationException($"Unknown response entity type: {entity.GetType().Name}")
        };

        return new ResponseDataEntity(
            id: entity.Id,
            questionId: entity.QuestionId,
            answer: answer,
            deviceId: entity.DeviceId,
            isCorrect: entity.IsCorrect ?? false,
            timestamp: entity.Timestamp
        );
    }

    /// <summary>
    /// Maps a ResponseDataEntity to the appropriate polymorphic ResponseEntity subclass.
    /// Requires the associated QuestionEntity to determine the correct response type.
    /// </summary>
    public static ResponseEntity ToEntity(ResponseDataEntity dataEntity, QuestionEntity questionEntity)
    {
        if (dataEntity == null)
            throw new ArgumentNullException(nameof(dataEntity));
        if (questionEntity == null)
            throw new ArgumentNullException(nameof(questionEntity));

        return questionEntity switch
        {
            MultipleChoiceQuestionEntity _ => new MultipleChoiceResponseEntity(
                id: dataEntity.Id,
                questionId: dataEntity.QuestionId,
                deviceId: dataEntity.DeviceId,
                answerIndex: ParseInt(dataEntity.Answer),
                timestamp: dataEntity.Timestamp,
                isCorrect: dataEntity.IsCorrect
            ),
            TrueFalseQuestionEntity _ => new TrueFalseResponseEntity(
                id: dataEntity.Id,
                questionId: dataEntity.QuestionId,
                deviceId: dataEntity.DeviceId,
                answer: ParseBool(dataEntity.Answer),
                timestamp: dataEntity.Timestamp,
                isCorrect: dataEntity.IsCorrect
            ),
            WrittenAnswerQuestionEntity _ => new WrittenAnswerResponseEntity(
                id: dataEntity.Id,
                questionId: dataEntity.QuestionId,
                deviceId: dataEntity.DeviceId,
                answer: dataEntity.Answer ?? string.Empty,
                timestamp: dataEntity.Timestamp,
                isCorrect: dataEntity.IsCorrect
            ),
            _ => throw new InvalidOperationException($"Unknown question entity type: {questionEntity.GetType().Name}")
        };
    }

    private static int ParseInt(string? value)
    {
        if (string.IsNullOrEmpty(value) || !int.TryParse(value, out int result))
            throw new InvalidOperationException($"Invalid integer value for answer: {value}");
        return result;
    }

    private static bool ParseBool(string? value)
    {
        if (string.IsNullOrEmpty(value) || !bool.TryParse(value, out bool result))
            throw new InvalidOperationException($"Invalid boolean value for answer: {value}");
        return result;
    }
}
