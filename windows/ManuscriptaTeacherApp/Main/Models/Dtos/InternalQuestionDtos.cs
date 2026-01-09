using Main.Models.Enums;

namespace Main.Models.Dtos;

/// <summary>
/// DTO for creating a new question.
/// Per NetworkingAPISpec §1(1)(d1)(i).
/// </summary>
public record InternalCreateQuestionDto(
    Guid MaterialId,
    QuestionType QuestionType,
    string QuestionText,
    List<string>? Options = null,           // Required for MULTIPLE_CHOICE
    int? CorrectAnswerIndex = null,         // For MULTIPLE_CHOICE (null = auto-marking disabled)
    string? CorrectAnswer = null,           // For WRITTEN_ANSWER (null = auto-marking disabled)
    int? MaxScore = null);

/// <summary>
/// DTO for updating an existing question.
/// Per NetworkingAPISpec §1(1)(d1)(iii).
/// </summary>
public record InternalUpdateQuestionDto(
    Guid Id,
    Guid MaterialId,
    QuestionType QuestionType,
    string QuestionText,
    List<string>? Options = null,
    int? CorrectAnswerIndex = null,         // For MULTIPLE_CHOICE (null = auto-marking disabled)
    string? CorrectAnswer = null,           // For WRITTEN_ANSWER (null = auto-marking disabled)
    int? MaxScore = null);
