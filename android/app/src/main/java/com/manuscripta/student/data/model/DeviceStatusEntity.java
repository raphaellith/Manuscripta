package com.manuscripta.student.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
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

    // -------------------------------------------------------------------------
    // CONSTRUCTOR 1: For Room (Rehydration)
    // -------------------------------------------------------------------------
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

    // -------------------------------------------------------------------------
    // CONSTRUCTOR 2: For the App (Creation)
    // -------------------------------------------------------------------------
    /**
     * Convenience constructor for creating/updating device status.
     * Automatically sets lastUpdated to current time.
     */
    @Ignore
    public DeviceStatusEntity(@NonNull String deviceId,
            @NonNull DeviceStatus status,
            int batteryLevel,
            String currentMaterialId,
            String studentView) {
        this.deviceId = deviceId;
        this.status = status;
        this.batteryLevel = batteryLevel;
        this.currentMaterialId = currentMaterialId;
        this.studentView = studentView;
        this.lastUpdated = System.currentTimeMillis();
    }

    // Getters and Setters

    @NonNull
    public String getDeviceId() {
        return deviceId;
    }

    // Device ID is final, no setter needed as it's the PK and set in constructor

    @NonNull
    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(@NonNull DeviceStatus status) {
        this.status = status;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        if (batteryLevel < 0 || batteryLevel > 100) {
            throw new IllegalArgumentException("Battery level must be between 0 and 100");
        }
        this.batteryLevel = batteryLevel;
    }
    public String getCurrentMaterialId() {
        return currentMaterialId;
    }

    public void setCurrentMaterialId(String currentMaterialId) {
        this.currentMaterialId = currentMaterialId;
    }

    public String getStudentView() {
        return studentView;
    }

    public void setStudentView(String studentView) {
        this.studentView = studentView;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
