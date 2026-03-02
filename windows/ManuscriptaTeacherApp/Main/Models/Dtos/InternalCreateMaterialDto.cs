using System.Text.Json.Nodes;
using Main.Models.Enums;

namespace Main.Models.Dtos;

/// <summary>
/// DTO for creating a new material without an assigned UUID.
/// Per NetworkingAPISpec §1(1)(d)(i).
/// </summary>
public record InternalCreateMaterialDto(
    Guid LessonId,
    string Title,
    string Content,
    MaterialType MaterialType,
    string? Metadata = null,
    JsonArray? VocabularyTerms = null,
    int? ReadingAge = null,
    int? ActualAge = null);
