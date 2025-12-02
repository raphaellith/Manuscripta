package com.manuscripta.student.domain.mapper;

import androidx.annotation.NonNull;

import com.manuscripta.student.data.model.DeviceStatusEntity;
import com.manuscripta.student.domain.model.DeviceStatus;

/**
 * Mapper class to convert between DeviceStatusEntity (data layer) and DeviceStatus (domain layer).
 * Provides bidirectional mapping for Clean Architecture separation.
 */
public final class DeviceStatusMapper {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private DeviceStatusMapper() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Converts a DeviceStatusEntity to a DeviceStatus domain model.
     *
     * @param entity The DeviceStatusEntity to convert
     * @return DeviceStatus domain model
     */
    @NonNull
    public static DeviceStatus toDomain(@NonNull DeviceStatusEntity entity) {
        return new DeviceStatus(
                entity.getDeviceId(),
                entity.getStatus(),
                entity.getBatteryLevel(),
                entity.getCurrentMaterialId(),
                entity.getStudentView(),
                entity.getLastUpdated()
        );
    }

    /**
     * Converts a DeviceStatus domain model to a DeviceStatusEntity.
     *
     * @param domain The DeviceStatus domain model to convert
     * @return DeviceStatusEntity for persistence
     */
    @NonNull
    public static DeviceStatusEntity toEntity(@NonNull DeviceStatus domain) {
        return new DeviceStatusEntity(
                domain.getDeviceId(),
                domain.getStatus(),
                domain.getBatteryLevel(),
                domain.getCurrentMaterialId(),
                domain.getStudentView(),
                domain.getLastUpdated()
        );
    }
}
