package com.manuscripta.student.domain.model;

import androidx.annotation.NonNull;

import com.manuscripta.student.data.model.FeedbackStyle;
import com.manuscripta.student.data.model.MascotSelection;

/**
 * Domain model representing tablet configuration in the Manuscripta system.
 * Per Validation Rules §2G, this contains all configuration fields with validation.
 *
 * <p>This is a clean domain object used in the business logic layer.</p>
 */
public class Configuration {

    /** Minimum allowed text size per Validation Rules §2G(1)(a). */
    public static final int MIN_TEXT_SIZE = 5;

    /** Maximum allowed text size per Validation Rules §2G(1)(a). */
    public static final int MAX_TEXT_SIZE = 50;

    /** Default text size. */
    public static final int DEFAULT_TEXT_SIZE = 6;

    /** Default feedback style. */
    public static final FeedbackStyle DEFAULT_FEEDBACK_STYLE = FeedbackStyle.IMMEDIATE;

    /** Default TTS enabled state. */
    public static final boolean DEFAULT_TTS_ENABLED = false;

    /** Default AI scaffolding enabled state. */
    public static final boolean DEFAULT_AI_SCAFFOLDING_ENABLED = false;

    /** Default summarisation enabled state. */
    public static final boolean DEFAULT_SUMMARISATION_ENABLED = false;

    /** Default mascot selection. */
    public static final MascotSelection DEFAULT_MASCOT_SELECTION = MascotSelection.NONE;

    /** The text size setting (5-50). */
    private final int textSize;

    /** The feedback style setting. */
    @NonNull
    private final FeedbackStyle feedbackStyle;

    /** Whether Text-to-Speech is enabled. */
    private final boolean ttsEnabled;

    /** Whether AI scaffolding is enabled. */
    private final boolean aiScaffoldingEnabled;

    /** Whether summarisation is enabled. */
    private final boolean summarisationEnabled;

    /** The selected mascot. */
    @NonNull
    private final MascotSelection mascotSelection;

    /**
     * Constructor with all fields and validation.
     *
     * @param textSize             The text size (5-50)
     * @param feedbackStyle        The feedback style
     * @param ttsEnabled           Whether TTS is enabled
     * @param aiScaffoldingEnabled Whether AI scaffolding is enabled
     * @param summarisationEnabled Whether summarisation is enabled
     * @param mascotSelection      The selected mascot
     * @throws IllegalArgumentException if textSize is outside valid range
     * @throws IllegalArgumentException if feedbackStyle is null
     * @throws IllegalArgumentException if mascotSelection is null
     */
    public Configuration(int textSize,
                         @NonNull FeedbackStyle feedbackStyle,
                         boolean ttsEnabled,
                         boolean aiScaffoldingEnabled,
                         boolean summarisationEnabled,
                         @NonNull MascotSelection mascotSelection) {
        if (textSize < MIN_TEXT_SIZE || textSize > MAX_TEXT_SIZE) {
            throw new IllegalArgumentException(
                    "TextSize must be between " + MIN_TEXT_SIZE + " and " + MAX_TEXT_SIZE
                            + ", got: " + textSize);
        }
        if (feedbackStyle == null) {
            throw new IllegalArgumentException("FeedbackStyle cannot be null");
        }
        if (mascotSelection == null) {
            throw new IllegalArgumentException("MascotSelection cannot be null");
        }

        this.textSize = textSize;
        this.feedbackStyle = feedbackStyle;
        this.ttsEnabled = ttsEnabled;
        this.aiScaffoldingEnabled = aiScaffoldingEnabled;
        this.summarisationEnabled = summarisationEnabled;
        this.mascotSelection = mascotSelection;
    }

    /**
     * Creates a default Configuration with standard values.
     *
     * @return A Configuration with default values
     */
    @NonNull
    public static Configuration createDefault() {
        return new Configuration(
                DEFAULT_TEXT_SIZE,
                DEFAULT_FEEDBACK_STYLE,
                DEFAULT_TTS_ENABLED,
                DEFAULT_AI_SCAFFOLDING_ENABLED,
                DEFAULT_SUMMARISATION_ENABLED,
                DEFAULT_MASCOT_SELECTION
        );
    }

    /**
     * Gets the text size setting.
     *
     * @return The text size (5-50)
     */
    public int getTextSize() {
        return textSize;
    }

    /**
     * Gets the feedback style setting.
     *
     * @return The feedback style
     */
    @NonNull
    public FeedbackStyle getFeedbackStyle() {
        return feedbackStyle;
    }

    /**
     * Gets whether TTS is enabled.
     *
     * @return true if TTS is enabled
     */
    public boolean isTtsEnabled() {
        return ttsEnabled;
    }

    /**
     * Gets whether AI scaffolding is enabled.
     *
     * @return true if AI scaffolding is enabled
     */
    public boolean isAiScaffoldingEnabled() {
        return aiScaffoldingEnabled;
    }

    /**
     * Gets whether summarisation is enabled.
     *
     * @return true if summarisation is enabled
     */
    public boolean isSummarisationEnabled() {
        return summarisationEnabled;
    }

    /**
     * Gets the selected mascot.
     *
     * @return The mascot selection
     */
    @NonNull
    public MascotSelection getMascotSelection() {
        return mascotSelection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Configuration that = (Configuration) o;
        return textSize == that.textSize
                && ttsEnabled == that.ttsEnabled
                && aiScaffoldingEnabled == that.aiScaffoldingEnabled
                && summarisationEnabled == that.summarisationEnabled
                && feedbackStyle == that.feedbackStyle
                && mascotSelection == that.mascotSelection;
    }

    @Override
    public int hashCode() {
        int result = textSize;
        result = 31 * result + feedbackStyle.hashCode();
        result = 31 * result + (ttsEnabled ? 1 : 0);
        result = 31 * result + (aiScaffoldingEnabled ? 1 : 0);
        result = 31 * result + (summarisationEnabled ? 1 : 0);
        result = 31 * result + mascotSelection.hashCode();
        return result;
    }

    @Override
    @NonNull
    public String toString() {
        return "Configuration{"
                + "textSize=" + textSize
                + ", feedbackStyle=" + feedbackStyle
                + ", ttsEnabled=" + ttsEnabled
                + ", aiScaffoldingEnabled=" + aiScaffoldingEnabled
                + ", summarisationEnabled=" + summarisationEnabled
                + ", mascotSelection=" + mascotSelection
                + '}';
    }
}
