package com.manuscripta.student.domain.model;

import androidx.annotation.NonNull;

import com.manuscripta.student.data.model.SessionStatus;

import java.util.UUID;

/**
 * Domain model representing a learning session.
 * This is a clean domain object without persistence annotations, used in the business logic layer.
 *
 * <p>Sessions track when a student is working on a material.</p>
 */
public class Session {

    @NonNull
    private final String id;

    @NonNull
    private final String materialId;

    private final long startTime;

    private final long endTime;

    @NonNull
    private final SessionStatus status;

    @NonNull
    private final String deviceId;

    /**
     * Factory method for creating a NEW session.
     * Generates a random UUID, captures the current timestamp, and sets default values.
     *
     * @param materialId UUID of the parent material
     * @param deviceId   Identifier of the device running the session
     * @return A new Session instance with generated ID and default values
     */
    @NonNull
    public static Session create(@NonNull String materialId, @NonNull String deviceId) {
        return new Session(
                UUID.randomUUID().toString(),
                materialId,
                System.currentTimeMillis(),
                0,
                SessionStatus.ACTIVE,
                deviceId
        );
    }

    /**
     * Constructor with all fields.
     *
     * @param id         Unique identifier (UUID)
     * @param materialId UUID of the parent material
     * @param startTime  Session start timestamp (Unix epoch milliseconds)
     * @param endTime    Session end timestamp (0 if still active)
     * @param status     Current status (ACTIVE, PAUSED, COMPLETED, CANCELLED)
     * @param deviceId   Identifier of the device running the session
     */
    public Session(@NonNull String id,
                   @NonNull String materialId,
                   long startTime,
                   long endTime,
                   @NonNull SessionStatus status,
                   @NonNull String deviceId) {
        this.id = id;
        this.materialId = materialId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.deviceId = deviceId;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getMaterialId() {
        return materialId;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    @NonNull
    public SessionStatus getStatus() {
        return status;
    }

    @NonNull
    public String getDeviceId() {
        return deviceId;
    }
}
