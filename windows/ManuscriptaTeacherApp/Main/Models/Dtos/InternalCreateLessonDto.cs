namespace Main.Models.Dtos;

/// <summary>
/// DTO for creating a new lesson without an assigned UUID.
/// Per NetworkingAPISpec §1(1)(c)(i).
/// </summary>
public record InternalCreateLessonDto(
    Guid UnitId,
    string Title,
    string Description);
