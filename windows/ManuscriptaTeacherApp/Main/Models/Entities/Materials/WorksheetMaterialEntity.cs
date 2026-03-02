using System.Text.Json.Nodes;
using Main.Models.Enums;

namespace Main.Models.Entities.Materials;

/// <summary>
/// Represents a worksheet with a mix of question types.
/// </summary>
public class WorksheetMaterialEntity : MaterialEntity
{
    private WorksheetMaterialEntity() : base() { }

    public WorksheetMaterialEntity(Guid id, Guid lessonId, string title, string content, DateTime? timestamp = null, string? metadata = null, JsonArray? vocabularyTerms = null, int? readingAge = null, int? actualAge = null)
        : base(id, lessonId, title, content, MaterialType.WORKSHEET, timestamp, metadata, vocabularyTerms, readingAge, actualAge)
    {
    }
}
