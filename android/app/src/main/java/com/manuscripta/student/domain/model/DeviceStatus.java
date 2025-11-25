package com.manuscripta.student.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Domain model representing the status of a device.
 * This is a clean domain object without persistence annotations, used in the business logic layer.
 *
 * <p>Used for reporting student status to the teacher.</p>
 */
public class DeviceStatus {

    @NonNull
    private final String deviceId;

    @NonNull
    private final com.manuscripta.student.data.model.DeviceStatus status;

    private final int batteryLevel;

    @Nullable
    private final String currentMaterialId;

    @Nullable
    private final String studentView;

    private final long lastUpdated;

    /**
     * Factory method for creating/updating device status.
     * Automatically captures the current timestamp for lastUpdated.
     *
     * @param deviceId          Identifier of the device
     * @param status            Current device status
     * @param batteryLevel      Battery level (0-100)
     * @param currentMaterialId UUID of the material currently being viewed (nullable)
     * @param studentView       Current student view (nullable)
     * @return A new DeviceStatusModel instance with current timestamp
     */
    @NonNull
    public static DeviceStatus create(@NonNull String deviceId,
                                      @NonNull com.manuscripta.student.data.model.DeviceStatus status,
                                      int batteryLevel,
                                      @Nullable String currentMaterialId,
                                      @Nullable String studentView) {
        return new DeviceStatus(
                deviceId,
                status,
                batteryLevel,
                currentMaterialId,
                studentView,
                System.currentTimeMillis()
        );
    }

    /**
     * Constructor with all fields.
     *
     * @param deviceId          Identifier of the device
     * @param status            Current device status
     * @param batteryLevel      Battery level (0-100)
     * @param currentMaterialId UUID of the material currently being viewed (nullable)
     * @param studentView       Current student view (nullable)
     * @param lastUpdated       Timestamp of the last update (Unix epoch milliseconds)
     */
    public DeviceStatus(@NonNull String deviceId,
                        @NonNull com.manuscripta.student.data.model.DeviceStatus status,
                        int batteryLevel,
                        @Nullable String currentMaterialId,
                        @Nullable String studentView,
                        long lastUpdated) {
        this.deviceId = deviceId;
        this.status = status;
        this.batteryLevel = batteryLevel;
        this.currentMaterialId = currentMaterialId;
        this.studentView = studentView;
        this.lastUpdated = lastUpdated;
    }

    @NonNull
    public String getDeviceId() {
        return deviceId;
    }

    @NonNull
    public com.manuscripta.student.data.model.DeviceStatus getStatus() {
        return status;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    @Nullable
    public String getCurrentMaterialId() {
        return currentMaterialId;
    }

    @Nullable
    public String getStudentView() {
        return studentView;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }
}
