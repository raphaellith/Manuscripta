namespace Main.Models.Dtos;

/// <summary>
/// DTO for creating a source document via the hub.
/// UUID is assigned during creation.
/// Per NetworkingAPISpec §1(1)(k)(i).
/// </summary>
public class InternalCreateSourceDocumentDto
{
    /// <summary>
    /// References the unit collection to which this source document is imported.
    /// Per AdditionalValidationRules.md §3A(1)(a).
    /// </summary>
    public Guid UnitCollectionId { get; set; }

    /// <summary>
    /// A textual transcript of the source document contents.
    /// Per AdditionalValidationRules.md §3A(1)(b).
    /// </summary>
    public string Transcript { get; set; } = string.Empty;
}
