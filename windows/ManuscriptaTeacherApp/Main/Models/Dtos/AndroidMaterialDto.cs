using Main.Models.Enums;
using Main.Models.Entities.Materials;
using System.Text.Json.Nodes;

namespace Main.Models.Dtos;

/// <summary>
/// DTO for MaterialEntity when communicating with Android clients.
/// Per AdditionalValidationRules s1A(2): Excludes Windows-only fields (LessonId, ReadingAge, ActualAge).
/// Per AdditionalValidationRules s1A(3): Flat composition-like structure matching Validation Rules §2A.
/// </summary>
public record AndroidMaterialDto(
    Guid Id,
    MaterialType MaterialType,
    string Title,
    string Content,
    long Timestamp,
    string? Metadata,
    JsonArray? VocabularyTerms)
{
    /// <summary>
    /// Maps a polymorphic MaterialEntity to an AndroidMaterialDto.
    /// Excludes Windows-only fields per s1A(2).
    /// </summary>
    public static AndroidMaterialDto FromEntity(MaterialEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        // Convert DateTime to Unix timestamp (long) per Validation Rules §2A(1)(d)
        var unixTimestamp = new DateTimeOffset(entity.Timestamp).ToUnixTimeSeconds();

        return new AndroidMaterialDto(
            Id: entity.Id,
            MaterialType: entity.MaterialType,
            Title: entity.Title,
            Content: entity.Content,
            Timestamp: unixTimestamp,
            Metadata: entity.Metadata,
            VocabularyTerms: entity.VocabularyTerms
        );
        // Note: LessonId, ReadingAge, ActualAge excluded per s1A(2)
    }
}
