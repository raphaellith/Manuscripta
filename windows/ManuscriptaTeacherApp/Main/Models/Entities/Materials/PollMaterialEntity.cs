using System.Text.Json.Nodes;
using Main.Models.Enums;

namespace Main.Models.Entities.Materials;

/// <summary>
/// Represents a poll with a multiple choice question whose response
/// distribution is intended to be revealed.
/// </summary>
public class PollMaterialEntity : MaterialEntity
{
    private PollMaterialEntity() : base() { }

    public PollMaterialEntity(Guid id, Guid lessonId, string title, string content, DateTime? timestamp = null, string? metadata = null, JsonArray? vocabularyTerms = null, int? readingAge = null, int? actualAge = null)
        : base(id, lessonId, title, content, MaterialType.POLL, timestamp, metadata, vocabularyTerms, readingAge, actualAge)
    {
    }
}
