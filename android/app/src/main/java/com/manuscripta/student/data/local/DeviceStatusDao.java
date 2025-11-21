package com.manuscripta.student.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.manuscripta.student.data.model.DeviceStatusEntity;

import java.util.List;

/**
 * Data Access Object for {@link DeviceStatusEntity}.
 * Provides methods for managing device status records.
 */
@Dao
public interface DeviceStatusDao {

    /**
     * Insert a new device status into the database.
     * If a status for the same device ID already exists, it will be replaced.
     *
     * @param deviceStatus The device status to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DeviceStatusEntity deviceStatus);

    /**
     * Get a device status by its unique device identifier.
     *
     * @param deviceId The unique identifier of the device
     * @return The device status entity, or null if not found
     */
    @Query("SELECT * FROM device_status WHERE deviceId = :deviceId")
    DeviceStatusEntity getById(String deviceId);

    /**
     * Get all device status records.
     *
     * @return List of all device status records
     */
    @Query("SELECT * FROM device_status")
    List<DeviceStatusEntity> getAll();

    /**
     * Update an existing device status.
     *
     * @param deviceStatus The device status with updated values
     */
    @Update
    void update(DeviceStatusEntity deviceStatus);

    /**
     * Delete a device status by its unique identifier.
     *
     * @param deviceId The unique identifier of the device to delete
     */
    @Query("DELETE FROM device_status WHERE deviceId = :deviceId")
    void deleteById(String deviceId);
}
