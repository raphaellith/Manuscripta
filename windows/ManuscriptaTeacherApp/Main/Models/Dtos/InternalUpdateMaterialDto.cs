using System.Text.Json.Nodes;
using Main.Models.Enums;

namespace Main.Models.Dtos;

/// <summary>
/// DTO for updating an existing material.
/// Per NetworkingAPISpec §1(1)(d)(iii).
/// </summary>
public record InternalUpdateMaterialDto(
    Guid Id,
    Guid LessonId,
    string Title,
    string Content,
    MaterialType MaterialType,
    string? Metadata = null,
    JsonArray? VocabularyTerms = null,
    int? ReadingAge = null,
    int? ActualAge = null);
