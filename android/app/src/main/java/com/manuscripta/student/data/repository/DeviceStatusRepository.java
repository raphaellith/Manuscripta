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
     * Gets the current device status as an observable LiveData.
     * The LiveData emits updates whenever the status changes.
     *
     * @return LiveData containing the current device status
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
     * Updates the battery level for the current device.
     *
     * @param batteryLevel The battery level percentage (0-100)
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
     * Initialises the device status with default values.
     * Should be called when the app starts or when a device is first registered.
     *
     * @param deviceId The unique identifier of the device
     */
    void initialiseDeviceStatus(@NonNull String deviceId);
}
