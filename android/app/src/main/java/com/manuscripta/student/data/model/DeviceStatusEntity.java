package com.manuscripta.student.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room entity representing the status of a device.
 * Used for reporting student status to the teacher.
 */
@Entity(tableName = "device_status")
public class DeviceStatusEntity {

    /** The unique identifier for the device. */
    @PrimaryKey
    @NonNull
    private final String deviceId;

    /** The current status of the device. */
    @NonNull
    private final DeviceStatus status;

    /** The battery level percentage (0-100). */
    private final int batteryLevel;

    /** The ID of the material currently being viewed (nullable). */
    @androidx.annotation.Nullable
    private final String currentMaterialId;

    /** The current student view state (nullable). */
    @androidx.annotation.Nullable
    private final String studentView;

    /** The timestamp of the last update (Unix epoch milliseconds). */
    private final long lastUpdated;

    /**
     * Standard constructor used by Room to recreate objects from the database.
     *
     * @param deviceId          The unique identifier for the device
     * @param status            The current status of the device
     * @param batteryLevel      The battery level percentage (0-100)
     * @param currentMaterialId The ID of the material currently being viewed (nullable)
     * @param studentView       The current student view state (nullable)
     * @param lastUpdated       The timestamp of the last update (Unix epoch milliseconds)
     */
    public DeviceStatusEntity(@NonNull String deviceId,
            @NonNull DeviceStatus status,
            int batteryLevel,
            String currentMaterialId,
            String studentView,
            long lastUpdated) {
        this.deviceId = deviceId;
        this.status = status;
        this.batteryLevel = batteryLevel;
        this.currentMaterialId = currentMaterialId;
        this.studentView = studentView;
        this.lastUpdated = lastUpdated;
    }

    // Getters

    @NonNull
    public String getDeviceId() {
        return deviceId;
    }

    @NonNull
    public DeviceStatus getStatus() {
        return status;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public String getCurrentMaterialId() {
        return currentMaterialId;
    }

    public String getStudentView() {
        return studentView;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }
}
