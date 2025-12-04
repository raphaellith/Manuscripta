using Main.Models.Entities;
using Main.Models.Entities.Sessions;

namespace Main.Models.Mappings;

/// <summary>
/// Provides mapping methods between SessionEntity (polymorphic) and SessionDataEntity (persistence).
/// </summary>
public static class SessionEntityMapper
{
    /// <summary>
    /// Maps a SessionEntity to a SessionDataEntity for persistence.
    /// </summary>
    public static SessionDataEntity ToDataEntity(SessionEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        return new SessionDataEntity(
            id: entity.Id,
            materialId: entity.MaterialId,
            startTime: entity.StartTime,
            sessionStatus: entity.SessionStatus,
            deviceId: entity.DeviceId,
            endTime: entity.EndTime
        );
    }

    /// <summary>
    /// Maps a SessionDataEntity to a SessionEntity.
    /// </summary>
    public static SessionEntity ToEntity(SessionDataEntity dataEntity)
    {
        if (dataEntity == null)
            throw new ArgumentNullException(nameof(dataEntity));

        return new SessionEntity(
            id: dataEntity.Id,
            materialId: dataEntity.MaterialId,
            startTime: dataEntity.StartTime,
            sessionStatus: dataEntity.SessionStatus,
            deviceId: dataEntity.DeviceId,
            endTime: dataEntity.EndTime
        );
    }
}
