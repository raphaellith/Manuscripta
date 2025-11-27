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

    public QuizMaterialEntity(Guid id, string title, string content, long? timestamp = null, string? metadata = null, JsonArray? vocabularyTerms = null)
        : base(id, title, content, MaterialType.QUIZ, timestamp, metadata, vocabularyTerms)
    {
    }
}
