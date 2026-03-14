using System.Text.Json.Nodes;
using Main.Models.Enums;

namespace Main.Models.Entities.Materials;

/// <summary>
/// Represents a reading material without questions.
/// </summary>
public class ReadingMaterialEntity : MaterialEntity
{
    private ReadingMaterialEntity() : base() { }

    public ReadingMaterialEntity(Guid id, Guid lessonId, string title, string content, DateTime? timestamp = null, string? metadata = null, JsonArray? vocabularyTerms = null, int? readingAge = null, int? actualAge = null)
        : base(id, lessonId, title, content, MaterialType.READING, timestamp, metadata, vocabularyTerms, readingAge, actualAge)
    {
    }
}
