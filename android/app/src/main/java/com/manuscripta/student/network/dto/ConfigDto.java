package com.manuscripta.student.network.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object for tablet configuration settings.
 * Per API Contract §2.2, contains configuration options like kiosk mode and text size.
 *
 * <p>The configuration is retrieved via {@code GET /config/{deviceId}} and can be refreshed
 * dynamically when triggered by a TCP REFRESH_CONFIG signal from the teacher server.</p>
 *
 * <p>Per Validation Rules §1(6), field names use PascalCase in JSON.</p>
 */
public class ConfigDto {

    /**
     * Whether kiosk mode is enabled for the device.
     * Kiosk mode restricts the device to only the student application.
     */
    @SerializedName("KioskMode")
    @Nullable
    private Boolean kioskMode;

    /**
     * Text size setting for the device.
     * Valid values: "small", "medium", "large"
     */
    @SerializedName("TextSize")
    @Nullable
    private String textSize;

    /**
     * Default constructor for Gson deserialization.
     */
    public ConfigDto() {
    }

    /**
     * Constructor with all fields.
     *
     * @param kioskMode Whether kiosk mode is enabled
     * @param textSize  Text size setting
     */
    public ConfigDto(@Nullable Boolean kioskMode, @Nullable String textSize) {
        this.kioskMode = kioskMode;
        this.textSize = textSize;
    }

    /**
     * Gets the kiosk mode setting.
     *
     * @return True if kiosk mode is enabled, false otherwise, or null if not set
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
     * @return Text size ("small", "medium", "large"), or null if not set
     */
    @Nullable
    public String getTextSize() {
        return textSize;
    }

    /**
     * Sets the text size setting.
     *
     * @param textSize Text size ("small", "medium", "large")
     */
    public void setTextSize(@Nullable String textSize) {
        this.textSize = textSize;
    }

    @Override
    @NonNull
    public String toString() {
        return "ConfigDto{"
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
        ConfigDto configDto = (ConfigDto) o;
        if (kioskMode != null ? !kioskMode.equals(configDto.kioskMode)
                : configDto.kioskMode != null) {
            return false;
        }
        return textSize != null ? textSize.equals(configDto.textSize) : configDto.textSize == null;
    }

    @Override
    public int hashCode() {
        int result = kioskMode != null ? kioskMode.hashCode() : 0;
        result = 31 * result + (textSize != null ? textSize.hashCode() : 0);
        return result;
    }
}
