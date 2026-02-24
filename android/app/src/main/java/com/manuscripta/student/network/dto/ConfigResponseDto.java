package com.manuscripta.student.network.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object for tablet configuration responses from the API.
 * Per API Contract §2.2, this represents the response body for GET /config/{deviceId}.
 *
 * <p>Configuration settings allow dynamic updates to tablet behaviour without
 * requiring app updates.</p>
 *
 * <p>Per Validation Rules §1(6), field names use PascalCase in JSON.</p>
 */
public class ConfigResponseDto {

    /**
     * Whether kiosk mode is enabled for the device.
     * When enabled, restricts navigation away from the app.
     */
    @SerializedName("KioskMode")
    @Nullable
    private Boolean kioskMode;

    /**
     * The text size setting for the device.
     * Expected values: "small", "medium", "large".
     */
    @SerializedName("TextSize")
    @Nullable
    private String textSize;

    /**
     * Default constructor for Gson deserialization.
     */
    public ConfigResponseDto() {
    }

    /**
     * Constructor with all fields.
     *
     * @param kioskMode Whether kiosk mode is enabled
     * @param textSize  The text size setting
     */
    public ConfigResponseDto(@Nullable Boolean kioskMode, @Nullable String textSize) {
        this.kioskMode = kioskMode;
        this.textSize = textSize;
    }

    /**
     * Gets the kiosk mode setting.
     *
     * @return Whether kiosk mode is enabled
     */
    @Nullable
    public Boolean getKioskMode() {
        return kioskMode;
    }

    /**
     * Sets the kiosk mode setting.
     *
     * @param kioskMode Whether kiosk mode is enabled
     */
    public void setKioskMode(@Nullable Boolean kioskMode) {
        this.kioskMode = kioskMode;
    }

    /**
     * Gets the text size setting.
     *
     * @return The text size setting
     */
    @Nullable
    public String getTextSize() {
        return textSize;
    }

    /**
     * Sets the text size setting.
     *
     * @param textSize The text size setting
     */
    public void setTextSize(@Nullable String textSize) {
        this.textSize = textSize;
    }

    @Override
    @NonNull
    public String toString() {
        return "ConfigResponseDto{"
                + "kioskMode=" + kioskMode
                + ", textSize='" + textSize + '\''
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
        ConfigResponseDto that = (ConfigResponseDto) o;
        if (kioskMode != null ? !kioskMode.equals(that.kioskMode) : that.kioskMode != null) {
            return false;
        }
        return textSize != null ? textSize.equals(that.textSize) : that.textSize == null;
    }

    @Override
    public int hashCode() {
        int result = kioskMode != null ? kioskMode.hashCode() : 0;
        result = 31 * result + (textSize != null ? textSize.hashCode() : 0);
        return result;
    }
}
