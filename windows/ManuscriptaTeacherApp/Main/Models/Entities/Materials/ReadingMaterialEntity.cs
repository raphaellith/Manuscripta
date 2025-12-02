using System.Text.Json.Nodes;
using Main.Models.Enums;

namespace Main.Models.Entities.Materials;

/// <summary>
/// Represents reading material without questions.
/// Pure content for students to read and study.
/// </summary>
public class ReadingMaterialEntity : MaterialEntity
{
    private ReadingMaterialEntity() : base() { }

    public ReadingMaterialEntity(Guid id, string title, string content, DateTime? timestamp = null, string? metadata = null, JsonArray? vocabularyTerms = null)
        : base(id, title, content, MaterialType.READING, timestamp, metadata, vocabularyTerms)
    {
    }
}
