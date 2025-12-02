using System.Text.Json.Nodes;
using Main.Models.Enums;

namespace Main.Models.Entities.Materials;

/// <summary>
/// Represents a worksheet with a mix of question types.
/// Can contain MULTIPLE_CHOICE, TRUE_FALSE, and WRITTEN_ANSWER questions.
/// </summary>
public class WorksheetMaterialEntity : MaterialEntity
{
    private WorksheetMaterialEntity() : base() { }

    public WorksheetMaterialEntity(Guid id, string title, string content, DateTime? timestamp = null, string? metadata = null, JsonArray? vocabularyTerms = null)
        : base(id, title, content, MaterialType.WORKSHEET, timestamp, metadata, vocabularyTerms)
    {
    }
}
