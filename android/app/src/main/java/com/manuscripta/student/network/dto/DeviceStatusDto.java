package com.manuscripta.student.network.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object for device status reporting via TCP.
 * Per Validation Rules.md §1(6), field names use PascalCase in JSON.
 *
 * <p>Used for STATUS_UPDATE (0x10) TCP messages as defined in API Contract §3.6.
 * This DTO represents the JSON payload sent from client to server.</p>
 *
 * <p>Related requirements: NET2 (receiving student tablet status changes)</p>
 */
public class DeviceStatusDto {

    /**
     * The unique identifier for the device (UUID format).
     * Per Validation Rules.md §2E(1)(a), references the device the status is linked to.
     */
    @SerializedName("DeviceId")
    @Nullable
    private String deviceId;

    /**
     * The current status of the device.
     * Per Validation Rules.md §2E(1)(b), possible values are: ON_TASK, IDLE, LOCKED, DISCONNECTED.
     */
    @SerializedName("Status")
    @Nullable
    private String status;

    /**
     * The battery level percentage (0-100).
     * Per Validation Rules.md §2E(1)(c) and §2E(2)(b), must be between 0 and 100.
     */
    @SerializedName("BatteryLevel")
    @Nullable
    private Integer batteryLevel;

    /**
     * The ID of the material currently being viewed (UUID format).
     * Per Validation Rules.md §2E(1)(d), references a valid material.
     */
    @SerializedName("CurrentMaterialId")
    @Nullable
    private String currentMaterialId;

    /**
     * Describes the location which the student is viewing.
     * Per Validation Rules.md §2E(1)(e), describes the student's current view state.
     */
    @SerializedName("StudentView")
    @Nullable
    private String studentView;

    /**
     * The timestamp when the status was recorded (Unix epoch seconds).
     * Per Validation Rules.md §2E(1)(f), the time at which the device status is correct to.
     */
    @SerializedName("Timestamp")
    @Nullable
    private Long timestamp;

    /**
     * Default constructor for Gson deserialization.
     */
    public DeviceStatusDto() {
    }

    /**
     * Constructor with all fields.
     *
     * @param deviceId          The unique identifier for the device
     * @param status            The current status of the device
     * @param batteryLevel      The battery level percentage (0-100)
     * @param currentMaterialId The ID of the material currently being viewed
     * @param studentView       Describes the location which the student is viewing
     * @param timestamp         The timestamp when the status was recorded
     */
    public DeviceStatusDto(@Nullable String deviceId,
                           @Nullable String status,
                           @Nullable Integer batteryLevel,
                           @Nullable String currentMaterialId,
                           @Nullable String studentView,
                           @Nullable Long timestamp) {
        this.deviceId = deviceId;
        this.status = status;
        this.batteryLevel = batteryLevel;
        this.currentMaterialId = currentMaterialId;
        this.studentView = studentView;
        this.timestamp = timestamp;
    }

    /**
     * Gets the device identifier.
     *
     * @return The device ID
     */
    @Nullable
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Sets the device identifier.
     *
     * @param deviceId The device ID
     */
    public void setDeviceId(@Nullable String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Gets the device status.
     *
     * @return The status (e.g., "ON_TASK", "IDLE", "LOCKED", "DISCONNECTED")
     */
    @Nullable
    public String getStatus() {
        return status;
    }

    /**
     * Sets the device status.
     *
     * @param status The status (e.g., "ON_TASK", "IDLE", "LOCKED", "DISCONNECTED")
     */
    public void setStatus(@Nullable String status) {
        this.status = status;
    }

    /**
     * Gets the battery level.
     *
     * @return The battery level percentage (0-100)
     */
    @Nullable
    public Integer getBatteryLevel() {
        return batteryLevel;
    }

    /**
     * Sets the battery level.
     *
     * @param batteryLevel The battery level percentage (0-100)
     */
    public void setBatteryLevel(@Nullable Integer batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    /**
     * Gets the current material identifier.
     *
     * @return The ID of the material currently being viewed
     */
    @Nullable
    public String getCurrentMaterialId() {
        return currentMaterialId;
    }

    /**
     * Sets the current material identifier.
     *
     * @param currentMaterialId The ID of the material currently being viewed
     */
    public void setCurrentMaterialId(@Nullable String currentMaterialId) {
        this.currentMaterialId = currentMaterialId;
    }

    /**
     * Gets the student view description.
     *
     * @return The description of the location the student is viewing
     */
    @Nullable
    public String getStudentView() {
        return studentView;
    }

    /**
     * Sets the student view description.
     *
     * @param studentView The description of the location the student is viewing
     */
    public void setStudentView(@Nullable String studentView) {
        this.studentView = studentView;
    }

    /**
     * Gets the timestamp.
     *
     * @return The timestamp (Unix epoch seconds)
     */
    @Nullable
    public Long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp.
     *
     * @param timestamp The timestamp (Unix epoch seconds)
     */
    public void setTimestamp(@Nullable Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    @NonNull
    public String toString() {
        return "DeviceStatusDto{"
                + "deviceId='" + deviceId + '\''
                + ", status='" + status + '\''
                + ", batteryLevel=" + batteryLevel
                + ", currentMaterialId='" + currentMaterialId + '\''
                + ", studentView='" + studentView + '\''
                + ", timestamp=" + timestamp
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeviceStatusDto that = (DeviceStatusDto) o;
        if (deviceId != null ? !deviceId.equals(that.deviceId) : that.deviceId != null) {
            return false;
        }
        if (status != null ? !status.equals(that.status) : that.status != null) {
            return false;
        }
        if (batteryLevel != null ? !batteryLevel.equals(that.batteryLevel)
                : that.batteryLevel != null) {
            return false;
        }
        if (currentMaterialId != null ? !currentMaterialId.equals(that.currentMaterialId)
                : that.currentMaterialId != null) {
            return false;
        }
        if (studentView != null ? !studentView.equals(that.studentView)
                : that.studentView != null) {
            return false;
        }
        return timestamp != null ? timestamp.equals(that.timestamp) : that.timestamp == null;
    }

    @Override
    public int hashCode() {
        int result = deviceId != null ? deviceId.hashCode() : 0;
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (batteryLevel != null ? batteryLevel.hashCode() : 0);
        result = 31 * result + (currentMaterialId != null ? currentMaterialId.hashCode() : 0);
        result = 31 * result + (studentView != null ? studentView.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }
}
