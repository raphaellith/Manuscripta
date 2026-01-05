using Main.Models.Entities;
using Main.Models.Entities.Materials;
using Main.Models.Enums;

namespace Main.Models.Mappings;

/// <summary>
/// Provides mapping methods between MaterialEntity (polymorphic) and MaterialDataEntity (persistence).
/// </summary>
public static class MaterialEntityMapper
{
    /// <summary>
    /// Maps a polymorphic MaterialEntity to a MaterialDataEntity for persistence.
    /// </summary>
    public static MaterialDataEntity ToDataEntity(MaterialEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        return new MaterialDataEntity
        {
            Id = entity.Id,
            LessonId = entity.LessonId,
            MaterialType = entity.MaterialType,
            Title = entity.Title,
            Content = entity.Content,
            Metadata = entity.Metadata,
            VocabularyTerms = entity.VocabularyTerms ?? new System.Text.Json.Nodes.JsonArray(),
            Timestamp = entity.Timestamp,
            ReadingAge = entity.ReadingAge,
            ActualAge = entity.ActualAge
        };
    }

    /// <summary>
    /// Maps a MaterialDataEntity to the appropriate polymorphic MaterialEntity subclass.
    /// </summary>
    public static MaterialEntity ToEntity(MaterialDataEntity dataEntity)
    {
        if (dataEntity == null)
            throw new ArgumentNullException(nameof(dataEntity));

        return dataEntity.MaterialType switch
        {
            MaterialType.READING => new ReadingMaterialEntity(
                id: dataEntity.Id,
                lessonId: dataEntity.LessonId,
                title: dataEntity.Title ?? string.Empty,
                content: dataEntity.Content ?? string.Empty,
                timestamp: dataEntity.Timestamp,
                metadata: dataEntity.Metadata,
                vocabularyTerms: dataEntity.VocabularyTerms,
                readingAge: dataEntity.ReadingAge,
                actualAge: dataEntity.ActualAge
            ),
            MaterialType.WORKSHEET => new WorksheetMaterialEntity(
                id: dataEntity.Id,
                lessonId: dataEntity.LessonId,
                title: dataEntity.Title ?? string.Empty,
                content: dataEntity.Content ?? string.Empty,
                timestamp: dataEntity.Timestamp,
                metadata: dataEntity.Metadata,
                vocabularyTerms: dataEntity.VocabularyTerms,
                readingAge: dataEntity.ReadingAge,
                actualAge: dataEntity.ActualAge
            ),
            MaterialType.POLL => new PollMaterialEntity(
                id: dataEntity.Id,
                lessonId: dataEntity.LessonId,
                title: dataEntity.Title ?? string.Empty,
                content: dataEntity.Content ?? string.Empty,
                timestamp: dataEntity.Timestamp,
                metadata: dataEntity.Metadata,
                vocabularyTerms: dataEntity.VocabularyTerms,
                readingAge: dataEntity.ReadingAge,
                actualAge: dataEntity.ActualAge
            ),
            _ => throw new InvalidOperationException($"Unknown material type: {dataEntity.MaterialType}")
        };
    }
}
