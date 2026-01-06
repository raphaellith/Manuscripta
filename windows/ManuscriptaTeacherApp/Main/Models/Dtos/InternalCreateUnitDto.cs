namespace Main.Models.Dtos;

/// <summary>
/// DTO for creating a new unit without an assigned UUID.
/// Per NetworkingAPISpec §1(1)(b)(i).
/// </summary>
public record InternalCreateUnitDto(
    Guid UnitCollectionId,
    string Title,
    List<string>? SourceDocuments = null);
