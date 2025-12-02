package com.manuscripta.student.domain.mapper;

import androidx.annotation.NonNull;

import com.manuscripta.student.data.model.SessionEntity;
import com.manuscripta.student.domain.model.Session;

/**
 * Mapper class to convert between SessionEntity (data layer) and Session (domain layer).
 * Provides bidirectional mapping for Clean Architecture separation.
 */
public final class SessionMapper {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private SessionMapper() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Converts a SessionEntity to a Session domain model.
     *
     * @param entity The SessionEntity to convert
     * @return Session domain model
     */
    @NonNull
    public static Session toDomain(@NonNull SessionEntity entity) {
        return new Session(
                entity.getId(),
                entity.getMaterialId(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getStatus(),
                entity.getDeviceId()
        );
    }

    /**
     * Converts a Session domain model to a SessionEntity.
     *
     * @param domain The Session domain model to convert
     * @return SessionEntity for persistence
     */
    @NonNull
    public static SessionEntity toEntity(@NonNull Session domain) {
        return new SessionEntity(
                domain.getId(),
                domain.getMaterialId(),
                domain.getStartTime(),
                domain.getEndTime(),
                domain.getStatus(),
                domain.getDeviceId()
        );
    }
}
