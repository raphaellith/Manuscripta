using System.Text.Json.Nodes;
using Main.Models.Enums;

namespace Main.Models.Entities.Materials;

/// <summary>
/// Represents a poll with a single multiple choice question.
/// Response distribution is intended to be revealed to students.
/// </summary>
public class PollMaterialEntity : MaterialEntity
{
    private PollMaterialEntity() : base() { }

    public PollMaterialEntity(Guid id, string title, string content, long? timestamp = null, string? metadata = null, JsonArray? vocabularyTerms = null)
        : base(id, title, content, MaterialType.POLL, timestamp, metadata, vocabularyTerms)
    {
    }
}
