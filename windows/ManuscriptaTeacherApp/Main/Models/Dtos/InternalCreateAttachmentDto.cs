namespace Main.Models.Dtos;

/// <summary>
/// DTO for creating an attachment entity via hub.
/// Per NetworkingAPISpec §1(1)(l)(i).
/// </summary>
public record InternalCreateAttachmentDto(
    Guid MaterialId,
    string FileBaseName,
    string FileExtension
);
