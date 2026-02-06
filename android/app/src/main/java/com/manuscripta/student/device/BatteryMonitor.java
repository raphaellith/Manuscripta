package com.manuscripta.student.device;

import androidx.annotation.NonNull;

import com.manuscripta.student.data.repository.DeviceStatusRepository;

/**
 * Monitors device battery level and reports changes.
 *
 * <p>Battery levels are validated per {@code Validation Rules.md} §2E(1)(c)
 * to be between 0 and 100 inclusive. Low battery is defined as below
 * {@link #LOW_BATTERY_THRESHOLD}%, supporting requirement CON5 (devices
 * requiring attention due to low battery).</p>
 */
public class BatteryMonitor {

    /** Threshold below which a device is considered low on battery. */
    public static final int LOW_BATTERY_THRESHOLD = 15;

    /** Minimum valid battery level. */
    public static final int MIN_BATTERY_LEVEL = 0;

    /** Maximum valid battery level. */
    public static final int MAX_BATTERY_LEVEL = 100;

    /** The repository for persisting device status. */
    private final DeviceStatusRepository deviceStatusRepository;

    /**
     * Creates a new BatteryMonitor.
     *
     * @param deviceStatusRepository The repository for device status updates
     * @throws IllegalArgumentException if deviceStatusRepository is null
     */
    public BatteryMonitor(@NonNull DeviceStatusRepository deviceStatusRepository) {
        if (deviceStatusRepository == null) {
            throw new IllegalArgumentException(
                    "DeviceStatusRepository cannot be null");
        }
        this.deviceStatusRepository = deviceStatusRepository;
    }

    /**
     * Called when the device battery level changes.
     * Validates the level and updates the repository.
     *
     * @param level The new battery level percentage (0-100)
     * @throws IllegalArgumentException if level is outside 0-100 range
     */
    public void onBatteryLevelChanged(int level) {
        validateBatteryLevel(level);
        deviceStatusRepository.updateBatteryLevel(level);
    }

    /**
     * Checks whether the specified battery level is considered low.
     *
     * @param level The battery level to check (0-100)
     * @return true if the level is below {@link #LOW_BATTERY_THRESHOLD}
     * @throws IllegalArgumentException if level is outside 0-100 range
     */
    public boolean isLowBattery(int level) {
        validateBatteryLevel(level);
        return level < LOW_BATTERY_THRESHOLD;
    }

    /**
     * Checks whether the current device battery is considered low.
     *
     * @return true if the current battery level is below
     *         {@link #LOW_BATTERY_THRESHOLD}
     */
    public boolean isLowBattery() {
        return getCurrentBatteryLevel() < LOW_BATTERY_THRESHOLD;
    }

    /**
     * Returns the current battery level from the repository.
     *
     * @return The current battery level percentage (0-100)
     */
    public int getCurrentBatteryLevel() {
        return deviceStatusRepository.getCurrentBatteryLevel();
    }

    /**
     * Validates that a battery level is within the valid range.
     *
     * @param level The battery level to validate
     * @throws IllegalArgumentException if level is outside 0-100 range
     */
    private void validateBatteryLevel(int level) {
        if (level < MIN_BATTERY_LEVEL || level > MAX_BATTERY_LEVEL) {
            throw new IllegalArgumentException(
                    "Battery level must be between " + MIN_BATTERY_LEVEL
                            + " and " + MAX_BATTERY_LEVEL);
        }
    }
}
