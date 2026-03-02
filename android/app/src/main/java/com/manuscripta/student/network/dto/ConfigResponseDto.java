package com.manuscripta.student.network.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

/**
 * Data Transfer Object for tablet configuration responses from the API.
 * Per API Contract §2.2, this represents the response body for GET /config/{deviceId}.
 *
 * <p>Configuration settings allow dynamic updates to tablet behaviour without
 * requiring app updates. Per Validation Rules §2G, this includes:</p>
 * <ul>
 *   <li>TextSize (5-50): Controls global font scaling</li>
 *   <li>FeedbackStyle (IMMEDIATE/NEUTRAL): Sets student feedback mode</li>
 *   <li>TtsEnabled: Master toggle for Text-to-Speech</li>
 *   <li>AiScaffoldingEnabled: Toggle for AI chat assistance</li>
 *   <li>SummarisationEnabled: Toggle for AI summarisation tools</li>
 *   <li>MascotSelection: Animated learning companion selection</li>
 * </ul>
 *
 * <p>Per Validation Rules §1(6), field names use PascalCase in JSON.</p>
 */
public class ConfigResponseDto {

    /**
     * The text size setting for the device.
     * Per Validation Rules §2G(1)(a), value must be between 5 and 50 (inclusive).
     */
    @SerializedName("TextSize")
    @Nullable
    private Integer textSize;

    /**
     * The feedback style setting for quiz responses.
     * Per Validation Rules §2G(1)(b), valid values are "IMMEDIATE" or "NEUTRAL".
     */
    @SerializedName("FeedbackStyle")
    @Nullable
    private String feedbackStyle;

    /**
     * Whether Text-to-Speech is enabled.
     * Per Validation Rules §2G(1)(c), controls accessibility TTS button visibility.
     */
    @SerializedName("TtsEnabled")
    @Nullable
    private Boolean ttsEnabled;

    /**
     * Whether AI scaffolding chat assistance is enabled.
     * Per Validation Rules §2G(1)(d), toggles dynamic AI chat interface (MAT5).
     */
    @SerializedName("AiScaffoldingEnabled")
    @Nullable
    private Boolean aiScaffoldingEnabled;

    /**
     * Whether AI summarisation tools are enabled.
     * Per Validation Rules §2G(1)(e), toggles Simplify/Summarise/Expand tools (MAT4).
     */
    @SerializedName("SummarisationEnabled")
    @Nullable
    private Boolean summarisationEnabled;

    /**
     * The selected mascot for the animated learning companion.
     * Per Validation Rules §2G(1)(f), valid values are "NONE" or "MASCOT1" to "MASCOT5".
     */
    @SerializedName("MascotSelection")
    @Nullable
    private String mascotSelection;

    /**
     * Default constructor for Gson deserialization.
     */
    public ConfigResponseDto() {
    }

    /**
     * Constructor with all fields.
     *
     * @param textSize             The text size setting (5-50)
     * @param feedbackStyle        The feedback style (IMMEDIATE/NEUTRAL)
     * @param ttsEnabled           Whether TTS is enabled
     * @param aiScaffoldingEnabled Whether AI scaffolding is enabled
     * @param summarisationEnabled Whether summarisation is enabled
     * @param mascotSelection      The selected mascot
     */
    public ConfigResponseDto(@Nullable Integer textSize,
                             @Nullable String feedbackStyle,
                             @Nullable Boolean ttsEnabled,
                             @Nullable Boolean aiScaffoldingEnabled,
                             @Nullable Boolean summarisationEnabled,
                             @Nullable String mascotSelection) {
        this.textSize = textSize;
        this.feedbackStyle = feedbackStyle;
        this.ttsEnabled = ttsEnabled;
        this.aiScaffoldingEnabled = aiScaffoldingEnabled;
        this.summarisationEnabled = summarisationEnabled;
        this.mascotSelection = mascotSelection;
    }

    /**
     * Gets the text size setting.
     *
     * @return The text size value (5-50), or null if not set
     */
    @Nullable
    public Integer getTextSize() {
        return textSize;
    }

    /**
     * Sets the text size setting.
     *
     * @param textSize The text size value (5-50)
     */
    public void setTextSize(@Nullable Integer textSize) {
        this.textSize = textSize;
    }

    /**
     * Gets the feedback style setting.
     *
     * @return The feedback style (IMMEDIATE/NEUTRAL), or null if not set
     */
    @Nullable
    public String getFeedbackStyle() {
        return feedbackStyle;
    }

    /**
     * Sets the feedback style setting.
     *
     * @param feedbackStyle The feedback style (IMMEDIATE/NEUTRAL)
     */
    public void setFeedbackStyle(@Nullable String feedbackStyle) {
        this.feedbackStyle = feedbackStyle;
    }

    /**
     * Gets whether TTS is enabled.
     *
     * @return Whether TTS is enabled, or null if not set
     */
    @Nullable
    public Boolean getTtsEnabled() {
        return ttsEnabled;
    }

    /**
     * Sets whether TTS is enabled.
     *
     * @param ttsEnabled Whether TTS is enabled
     */
    public void setTtsEnabled(@Nullable Boolean ttsEnabled) {
        this.ttsEnabled = ttsEnabled;
    }

    /**
     * Gets whether AI scaffolding is enabled.
     *
     * @return Whether AI scaffolding is enabled, or null if not set
     */
    @Nullable
    public Boolean getAiScaffoldingEnabled() {
        return aiScaffoldingEnabled;
    }

    /**
     * Sets whether AI scaffolding is enabled.
     *
     * @param aiScaffoldingEnabled Whether AI scaffolding is enabled
     */
    public void setAiScaffoldingEnabled(@Nullable Boolean aiScaffoldingEnabled) {
        this.aiScaffoldingEnabled = aiScaffoldingEnabled;
    }

    /**
     * Gets whether summarisation is enabled.
     *
     * @return Whether summarisation is enabled, or null if not set
     */
    @Nullable
    public Boolean getSummarisationEnabled() {
        return summarisationEnabled;
    }

    /**
     * Sets whether summarisation is enabled.
     *
     * @param summarisationEnabled Whether summarisation is enabled
     */
    public void setSummarisationEnabled(@Nullable Boolean summarisationEnabled) {
        this.summarisationEnabled = summarisationEnabled;
    }

    /**
     * Gets the selected mascot.
     *
     * @return The mascot selection, or null if not set
     */
    @Nullable
    public String getMascotSelection() {
        return mascotSelection;
    }

    /**
     * Sets the selected mascot.
     *
     * @param mascotSelection The mascot selection (NONE, MASCOT1-MASCOT5)
     */
    public void setMascotSelection(@Nullable String mascotSelection) {
        this.mascotSelection = mascotSelection;
    }

    @Override
    @NonNull
    public String toString() {
        return "ConfigResponseDto{"
                + "textSize=" + textSize
                + ", feedbackStyle='" + feedbackStyle + '\''
                + ", ttsEnabled=" + ttsEnabled
                + ", aiScaffoldingEnabled=" + aiScaffoldingEnabled
                + ", summarisationEnabled=" + summarisationEnabled
                + ", mascotSelection='" + mascotSelection + '\''
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
        return Objects.equals(textSize, that.textSize)
                && Objects.equals(feedbackStyle, that.feedbackStyle)
                && Objects.equals(ttsEnabled, that.ttsEnabled)
                && Objects.equals(aiScaffoldingEnabled, that.aiScaffoldingEnabled)
                && Objects.equals(summarisationEnabled, that.summarisationEnabled)
                && Objects.equals(mascotSelection, that.mascotSelection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(textSize, feedbackStyle, ttsEnabled, aiScaffoldingEnabled,
                summarisationEnabled, mascotSelection);
    }
}
