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

    @PrimaryKey
    @NonNull
    private final String deviceId;

    @NonNull
    private DeviceStatus status;
    
    private int batteryLevel;

    @androidx.annotation.Nullable
    private String currentMaterialId;

    @androidx.annotation.Nullable
    private String studentView;

    private long lastUpdated;

    /**
     * Standard constructor used by Room to recreate objects from the database.
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
