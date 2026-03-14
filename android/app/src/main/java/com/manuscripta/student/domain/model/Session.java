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

    /** The unique identifier for the session. */
    @NonNull
    private final String id;

    /** The ID of the parent material. */
    @NonNull
    private final String materialId;

    /** The timestamp when the session started (Unix epoch milliseconds). */
    private final long startTime;

    /** The timestamp when the session ended (0 if still active). */
    private final long endTime;

    /** The current status of the session. */
    @NonNull
    private final SessionStatus status;

    /** The identifier of the device running the session. */
    @NonNull
    private final String deviceId;

    /**
     * Factory method for creating a NEW session.
     * Generates a random UUID and sets the session to RECEIVED state.
     * StartTime is set to 0 (not yet started) until first interaction.
     *
     * @param materialId UUID of the parent material
     * @param deviceId   Identifier of the device running the session
     * @return A new Session instance with generated ID in RECEIVED state
     * @throws IllegalArgumentException if materialId is null or empty
     * @throws IllegalArgumentException if deviceId is null or empty
     */
    @NonNull
    public static Session create(@NonNull String materialId, @NonNull String deviceId) {
        if (materialId == null || materialId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session materialId cannot be null or empty");
        }
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session deviceId cannot be null or empty");
        }
        return new Session(
                UUID.randomUUID().toString(),
                materialId,
                0,
                0,
                SessionStatus.RECEIVED,
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
     * @throws IllegalArgumentException if id is null or empty
     * @throws IllegalArgumentException if materialId is null or empty
     * @throws IllegalArgumentException if startTime is negative
     * @throws IllegalArgumentException if endTime is negative
     * @throws IllegalArgumentException if status is null
     * @throws IllegalArgumentException if deviceId is null or empty
     */
    public Session(@NonNull String id,
                   @NonNull String materialId,
                   long startTime,
                   long endTime,
                   @NonNull SessionStatus status,
                   @NonNull String deviceId) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Session id cannot be null or empty");
        }
        if (materialId == null || materialId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session materialId cannot be null or empty");
        }
        if (startTime < 0) {
            throw new IllegalArgumentException("Session startTime cannot be negative");
        }
        if (endTime < 0) {
            throw new IllegalArgumentException("Session endTime cannot be negative");
        }
        if (status == null) {
            throw new IllegalArgumentException("Session status cannot be null");
        }
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session deviceId cannot be null or empty");
        }

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
