using System.Text.Json.Nodes;
using Main.Models.Enums;

namespace Main.Models.Entities.Materials;

/// <summary>
/// Represents a quiz with multiple choice questions only.
/// Intended for assessment and scoring.
/// </summary>
public class QuizMaterialEntity : MaterialEntity
{
    private QuizMaterialEntity() : base() { }

    public QuizMaterialEntity(Guid id, Guid lessonId, string title, string content, DateTime? timestamp = null, string? metadata = null, JsonArray? vocabularyTerms = null, int? readingAge = null, int? actualAge = null)
        : base(id, lessonId, title, content, MaterialType.QUIZ, timestamp, metadata, vocabularyTerms, readingAge, actualAge)
    {
    }
}
