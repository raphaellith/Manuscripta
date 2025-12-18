package com.manuscripta.student.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.manuscripta.student.data.local.DeviceStatusDao;
import com.manuscripta.student.data.model.DeviceStatus;
import com.manuscripta.student.data.model.DeviceStatusEntity;
import com.manuscripta.student.domain.mapper.DeviceStatusMapper;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of {@link DeviceStatusRepository} that manages device status
 * with local persistence and observable state.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Local persistence via Room DAO</li>
 *   <li>Observable status via LiveData for UI updates</li>
 *   <li>Battery level tracking</li>
 *   <li>Thread-safe operations</li>
 * </ul>
 */
@Singleton
public class DeviceStatusRepositoryImpl implements DeviceStatusRepository {

    /** Tag for logging. */
    private static final String TAG = "DeviceStatusRepository";

    /** Default battery level when not yet monitored. */
    private static final int DEFAULT_BATTERY_LEVEL = 100;

    /** The DAO for device status persistence. */
    private final DeviceStatusDao deviceStatusDao;

    /** Lock object for thread-safe operations. */
    private final Object lock = new Object();

    /** LiveData for observable device status. */
    private final MutableLiveData<com.manuscripta.student.domain.model.DeviceStatus> statusLiveData;

    /** The current device ID being tracked. */
    @Nullable
    private String currentDeviceId;

    /** The current battery level. */
    private int currentBatteryLevel = DEFAULT_BATTERY_LEVEL;

    /**
     * Creates a new DeviceStatusRepositoryImpl with the given DAO.
     *
     * @param deviceStatusDao The DAO for device status persistence
     * @throws IllegalArgumentException if deviceStatusDao is null
     */
    @Inject
    public DeviceStatusRepositoryImpl(@NonNull DeviceStatusDao deviceStatusDao) {
        if (deviceStatusDao == null) {
            throw new IllegalArgumentException("DeviceStatusDao cannot be null");
        }
        this.deviceStatusDao = deviceStatusDao;
        this.statusLiveData = new MutableLiveData<>();
    }

    @Override
    @Nullable
    public com.manuscripta.student.domain.model.DeviceStatus getDeviceStatus(
            @NonNull String deviceId) {
        validateNotEmpty(deviceId, "Device ID");

        synchronized (lock) {
            DeviceStatusEntity entity = deviceStatusDao.getById(deviceId);
            if (entity == null) {
                return null;
            }
            return DeviceStatusMapper.toDomain(entity);
        }
    }

    @Override
    @NonNull
    public LiveData<com.manuscripta.student.domain.model.DeviceStatus> getDeviceStatusLiveData() {
        return statusLiveData;
    }

    @Override
    public void updateStatus(@NonNull String deviceId,
                             @NonNull DeviceStatus status,
                             @Nullable String currentMaterialId,
                             @Nullable String studentView) {
        validateNotEmpty(deviceId, "Device ID");
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        synchronized (lock) {
            com.manuscripta.student.domain.model.DeviceStatus domainStatus =
                    com.manuscripta.student.domain.model.DeviceStatus.create(
                            deviceId,
                            status,
                            currentBatteryLevel,
                            currentMaterialId,
                            studentView
                    );

            DeviceStatusEntity entity = DeviceStatusMapper.toEntity(domainStatus);
            deviceStatusDao.insert(entity);

            currentDeviceId = deviceId;
            statusLiveData.postValue(domainStatus);

            Log.d(TAG, "Updated status for device " + deviceId + " to " + status);
        }
    }

    @Override
    public void updateBatteryLevel(int batteryLevel) {
        if (batteryLevel < com.manuscripta.student.domain.model.DeviceStatus.MIN_BATTERY_LEVEL
                || batteryLevel > com.manuscripta.student.domain.model.DeviceStatus.MAX_BATTERY_LEVEL) {
            throw new IllegalArgumentException(
                    "Battery level must be between "
                            + com.manuscripta.student.domain.model.DeviceStatus.MIN_BATTERY_LEVEL
                            + " and "
                            + com.manuscripta.student.domain.model.DeviceStatus.MAX_BATTERY_LEVEL);
        }

        synchronized (lock) {
            currentBatteryLevel = batteryLevel;

            // Update the current device's status if we have one
            if (currentDeviceId != null) {
                DeviceStatusEntity currentEntity = deviceStatusDao.getById(currentDeviceId);
                if (currentEntity != null) {
                    com.manuscripta.student.domain.model.DeviceStatus updatedStatus =
                            com.manuscripta.student.domain.model.DeviceStatus.create(
                                    currentEntity.getDeviceId(),
                                    currentEntity.getStatus(),
                                    batteryLevel,
                                    currentEntity.getCurrentMaterialId(),
                                    currentEntity.getStudentView()
                            );

                    DeviceStatusEntity updatedEntity = DeviceStatusMapper.toEntity(updatedStatus);
                    deviceStatusDao.insert(updatedEntity);
                    statusLiveData.postValue(updatedStatus);

                    Log.d(TAG, "Updated battery level to " + batteryLevel + "%");
                }
            }
        }
    }

    @Override
    public void setOnTask(@NonNull String deviceId, @Nullable String currentMaterialId) {
        updateStatus(deviceId, DeviceStatus.ON_TASK, currentMaterialId, null);
    }

    @Override
    public void setIdle(@NonNull String deviceId) {
        updateStatus(deviceId, DeviceStatus.IDLE, null, null);
    }

    @Override
    public void setLocked(@NonNull String deviceId) {
        updateStatus(deviceId, DeviceStatus.LOCKED, null, null);
    }

    @Override
    public void setDisconnected(@NonNull String deviceId) {
        updateStatus(deviceId, DeviceStatus.DISCONNECTED, null, null);
    }

    @Override
    public void clearDeviceStatus(@NonNull String deviceId) {
        validateNotEmpty(deviceId, "Device ID");

        synchronized (lock) {
            deviceStatusDao.deleteById(deviceId);
            if (deviceId.equals(currentDeviceId)) {
                currentDeviceId = null;
                statusLiveData.postValue(null);
            }
            Log.d(TAG, "Cleared status for device " + deviceId);
        }
    }

    @Override
    public void clearAllDeviceStatus() {
        synchronized (lock) {
            deviceStatusDao.deleteAll();
            currentDeviceId = null;
            statusLiveData.postValue(null);
            Log.d(TAG, "Cleared all device status records");
        }
    }

    @Override
    public void initialiseDeviceStatus(@NonNull String deviceId) {
        validateNotEmpty(deviceId, "Device ID");

        synchronized (lock) {
            DeviceStatusEntity existing = deviceStatusDao.getById(deviceId);
            if (existing == null) {
                updateStatus(deviceId, DeviceStatus.IDLE, null, null);
                Log.d(TAG, "Initialised device status for " + deviceId);
            } else {
                currentDeviceId = deviceId;
                statusLiveData.postValue(DeviceStatusMapper.toDomain(existing));
                Log.d(TAG, "Loaded existing status for device " + deviceId);
            }
        }
    }

    @Override
    public int getCurrentBatteryLevel() {
        return currentBatteryLevel;
    }

    /**
     * Validates that a string parameter is not null or empty.
     *
     * @param value     The value to validate
     * @param fieldName The name of the field for error messages
     * @throws IllegalArgumentException if the value is null or empty
     */
    private void validateNotEmpty(@Nullable String value, @NonNull String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }
}
