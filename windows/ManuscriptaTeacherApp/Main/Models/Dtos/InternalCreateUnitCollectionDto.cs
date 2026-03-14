namespace Main.Models.Dtos;

/// <summary>
/// DTO for creating a new unit collection without an assigned UUID.
/// Per NetworkingAPISpec §1(1)(a)(i).
/// </summary>
public record InternalCreateUnitCollectionDto(string Title);
