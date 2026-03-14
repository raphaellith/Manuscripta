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

    /** Minimum valid battery level. */
    public static final int MIN_BATTERY_LEVEL = 0;

    /** Maximum valid battery level. */
    public static final int MAX_BATTERY_LEVEL = 100;

    /** The unique identifier for the device. */
    @NonNull
    private final String deviceId;

    /** The current status of the device. */
    @NonNull
    private final com.manuscripta.student.data.model.DeviceStatus status;

    /** The battery level percentage (0-100). */
    private final int batteryLevel;

    /** The ID of the material currently being viewed (nullable). */
    @Nullable
    private final String currentMaterialId;

    /** The current student view state (nullable). */
    @Nullable
    private final String studentView;

    /** The timestamp of the last update (Unix epoch milliseconds). */
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
     * @return A new DeviceStatus instance with current timestamp
     * @throws IllegalArgumentException if deviceId is null or empty
     * @throws IllegalArgumentException if status is null
     * @throws IllegalArgumentException if batteryLevel is not between 0 and 100
     */
    @NonNull
    public static DeviceStatus create(@NonNull String deviceId,
                                      @NonNull com.manuscripta.student.data.model.DeviceStatus status,
                                      int batteryLevel,
                                      @Nullable String currentMaterialId,
                                      @Nullable String studentView) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("DeviceStatus deviceId cannot be null or empty");
        }
        if (status == null) {
            throw new IllegalArgumentException("DeviceStatus status cannot be null");
        }
        if (batteryLevel < MIN_BATTERY_LEVEL || batteryLevel > MAX_BATTERY_LEVEL) {
            throw new IllegalArgumentException(
                    "DeviceStatus batteryLevel must be between "
                            + MIN_BATTERY_LEVEL + " and " + MAX_BATTERY_LEVEL);
        }
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
     * @throws IllegalArgumentException if deviceId is null or empty
     * @throws IllegalArgumentException if status is null
     * @throws IllegalArgumentException if batteryLevel is not between 0 and 100
     * @throws IllegalArgumentException if lastUpdated is negative
     */
    public DeviceStatus(@NonNull String deviceId,
                        @NonNull com.manuscripta.student.data.model.DeviceStatus status,
                        int batteryLevel,
                        @Nullable String currentMaterialId,
                        @Nullable String studentView,
                        long lastUpdated) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("DeviceStatus deviceId cannot be null or empty");
        }
        if (status == null) {
            throw new IllegalArgumentException("DeviceStatus status cannot be null");
        }
        if (batteryLevel < MIN_BATTERY_LEVEL || batteryLevel > MAX_BATTERY_LEVEL) {
            throw new IllegalArgumentException(
                    "DeviceStatus batteryLevel must be between "
                            + MIN_BATTERY_LEVEL + " and " + MAX_BATTERY_LEVEL);
        }
        if (lastUpdated < 0) {
            throw new IllegalArgumentException("DeviceStatus lastUpdated cannot be negative");
        }

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
