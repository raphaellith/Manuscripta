package com.manuscripta.student.domain.mapper;

import androidx.annotation.NonNull;

import com.manuscripta.student.data.model.DeviceStatusEntity;
import com.manuscripta.student.domain.model.DeviceStatus;
import com.manuscripta.student.network.dto.DeviceStatusDto;

/**
 * Mapper class to convert between DeviceStatusEntity (data layer), DeviceStatus (domain layer),
 * and DeviceStatusDto (network layer).
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

    /**
     * Converts a DeviceStatus domain model to a DeviceStatusDto for network transmission.
     * The timestamp is converted from milliseconds to seconds for API compatibility.
     *
     * @param domain The DeviceStatus domain model to convert
     * @return DeviceStatusDto for network transmission
     */
    @NonNull
    public static DeviceStatusDto toDto(@NonNull DeviceStatus domain) {
        return new DeviceStatusDto(
                domain.getDeviceId(),
                domain.getStatus().name(),
                domain.getBatteryLevel(),
                domain.getCurrentMaterialId(),
                domain.getStudentView(),
                domain.getLastUpdated() / 1000  // Convert milliseconds to seconds
        );
    }

    /**
     * Converts a DeviceStatusDto from network to a DeviceStatus domain model.
     * The timestamp is converted from seconds to milliseconds for internal use.
     *
     * @param dto The DeviceStatusDto to convert
     * @return DeviceStatus domain model
     * @throws IllegalArgumentException if required fields are null or invalid
     */
    @NonNull
    public static DeviceStatus fromDto(@NonNull DeviceStatusDto dto) {
        if (dto.getDeviceId() == null) {
            throw new IllegalArgumentException("DeviceStatusDto deviceId cannot be null");
        }
        if (dto.getStatus() == null) {
            throw new IllegalArgumentException("DeviceStatusDto status cannot be null");
        }
        if (dto.getBatteryLevel() == null) {
            throw new IllegalArgumentException("DeviceStatusDto batteryLevel cannot be null");
        }
        if (dto.getTimestamp() == null) {
            throw new IllegalArgumentException("DeviceStatusDto timestamp cannot be null");
        }

        com.manuscripta.student.data.model.DeviceStatus status;
        try {
            status = com.manuscripta.student.data.model.DeviceStatus.valueOf(dto.getStatus());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "DeviceStatusDto status is invalid: " + dto.getStatus(), e);
        }

        return new DeviceStatus(
                dto.getDeviceId(),
                status,
                dto.getBatteryLevel(),
                dto.getCurrentMaterialId(),
                dto.getStudentView(),
                dto.getTimestamp() * 1000  // Convert seconds to milliseconds
        );
    }
}
