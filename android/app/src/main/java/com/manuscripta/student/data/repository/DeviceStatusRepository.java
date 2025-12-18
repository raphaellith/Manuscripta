package com.manuscripta.student.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.manuscripta.student.data.model.DeviceStatus;

/**
 * Repository interface for managing device status.
 * Provides abstraction over local storage and status reporting.
 *
 * <p>This repository acts as a facade for:</p>
 * <ul>
 *   <li>Local persistence of device status via Room</li>
 *   <li>Observable status state for UI via LiveData</li>
 *   <li>Battery level monitoring integration</li>
 * </ul>
 */
public interface DeviceStatusRepository {

    /**
     * Gets the current device status for the specified device.
     *
     * @param deviceId The unique identifier of the device
     * @return The current device status, or null if not found
     */
    @Nullable
    com.manuscripta.student.domain.model.DeviceStatus getDeviceStatus(@NonNull String deviceId);

    /**
     * Gets the currently tracked device status as an observable LiveData.
     * The LiveData emits updates whenever the tracked device's status changes.
     * Use {@link #initialiseDeviceStatus(String)} or
     * {@link #updateStatus(String, DeviceStatus, String, String)}
     * to set which device is being tracked.
     *
     * @return LiveData containing the current status of the tracked device
     */
    @NonNull
    LiveData<com.manuscripta.student.domain.model.DeviceStatus> getDeviceStatusLiveData();

    /**
     * Updates the device status.
     *
     * @param deviceId          The unique identifier of the device
     * @param status            The new device status
     * @param currentMaterialId The ID of the material currently being viewed (nullable)
     * @param studentView       The current student view state (nullable)
     */
    void updateStatus(@NonNull String deviceId,
                      @NonNull DeviceStatus status,
                      @Nullable String currentMaterialId,
                      @Nullable String studentView);

    /**
     * Updates the battery level for the currently tracked device.
     *
     * <p>The "current device" is the one set by the most recent call to
     * {@link #updateStatus}, {@link #setOnTask}, {@link #setIdle},
     * {@link #setLocked}, {@link #setDisconnected}, or {@link #initialiseDeviceStatus}.</p>
     *
     * <p>If no device is currently tracked, the battery level is stored in memory
     * but not persisted to the database until a device is set. The stored battery
     * level will be used in subsequent status update calls.</p>
     *
     * @param batteryLevel The battery level percentage (0-100)
     * @throws IllegalArgumentException if batteryLevel is outside 0-100 range
     */
    void updateBatteryLevel(int batteryLevel);

    /**
     * Sets the device to ON_TASK status.
     *
     * @param deviceId          The unique identifier of the device
     * @param currentMaterialId The ID of the material being worked on
     */
    void setOnTask(@NonNull String deviceId, @Nullable String currentMaterialId);

    /**
     * Sets the device to IDLE status.
     *
     * @param deviceId The unique identifier of the device
     */
    void setIdle(@NonNull String deviceId);

    /**
     * Sets the device to LOCKED status.
     *
     * @param deviceId The unique identifier of the device
     */
    void setLocked(@NonNull String deviceId);

    /**
     * Sets the device to DISCONNECTED status.
     *
     * @param deviceId The unique identifier of the device
     */
    void setDisconnected(@NonNull String deviceId);

    /**
     * Clears the device status from local storage.
     *
     * @param deviceId The unique identifier of the device to clear
     */
    void clearDeviceStatus(@NonNull String deviceId);

    /**
     * Clears all device status records from local storage.
     */
    void clearAllDeviceStatus();

    /**
     * Initialises or loads the device status.
     *
     * <p>If the device already has a status in local storage, loads it and sets
     * this device as the currently tracked device. Otherwise, creates a new status
     * with IDLE state and default values.</p>
     *
     * <p>Should be called when the app starts or when a device is first registered.</p>
     *
     * @param deviceId The unique identifier of the device
     * @throws IllegalArgumentException if deviceId is null or empty
     */
    void initialiseDeviceStatus(@NonNull String deviceId);

    /**
     * Gets the current battery level being tracked.
     *
     * @return The current battery level percentage (0-100)
     */
    int getCurrentBatteryLevel();
}
