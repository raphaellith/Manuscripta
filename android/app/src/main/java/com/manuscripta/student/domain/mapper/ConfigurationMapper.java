package com.manuscripta.student.domain.mapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.manuscripta.student.data.model.FeedbackStyle;
import com.manuscripta.student.data.model.MascotSelection;
import com.manuscripta.student.domain.model.Configuration;
import com.manuscripta.student.network.dto.ConfigResponseDto;

/**
 * Mapper class to convert between ConfigResponseDto (network layer) and Configuration (domain).
 * Provides bidirectional mapping for Clean Architecture separation.
 *
 * <p>This mapper handles the conversion of configuration settings received from the
 * Windows teacher application, applying default values for missing or invalid fields.</p>
 */
public final class ConfigurationMapper {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private ConfigurationMapper() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Converts a ConfigResponseDto to a Configuration domain model.
     * Missing or invalid values are replaced with defaults.
     *
     * @param dto The ConfigResponseDto to convert
     * @return Configuration domain model with validated values
     */
    @NonNull
    public static Configuration fromDto(@NonNull ConfigResponseDto dto) {
        int textSize = parseTextSize(dto.getTextSize());
        FeedbackStyle feedbackStyle = parseFeedbackStyle(dto.getFeedbackStyle());
        boolean ttsEnabled = parseBoolean(dto.getTtsEnabled(), Configuration.DEFAULT_TTS_ENABLED);
        boolean aiScaffoldingEnabled = parseBoolean(dto.getAiScaffoldingEnabled(),
                Configuration.DEFAULT_AI_SCAFFOLDING_ENABLED);
        boolean summarisationEnabled = parseBoolean(dto.getSummarisationEnabled(),
                Configuration.DEFAULT_SUMMARISATION_ENABLED);
        MascotSelection mascotSelection = parseMascotSelection(dto.getMascotSelection());

        return new Configuration(
                textSize,
                feedbackStyle,
                ttsEnabled,
                aiScaffoldingEnabled,
                summarisationEnabled,
                mascotSelection
        );
    }

    /**
     * Converts a Configuration domain model to a ConfigResponseDto.
     *
     * @param config The Configuration domain model to convert
     * @return ConfigResponseDto for API communication
     */
    @NonNull
    public static ConfigResponseDto toDto(@NonNull Configuration config) {
        return new ConfigResponseDto(
                config.getTextSize(),
                config.getFeedbackStyle().name(),
                config.isTtsEnabled(),
                config.isAiScaffoldingEnabled(),
                config.isSummarisationEnabled(),
                config.getMascotSelection().name()
        );
    }

    /**
     * Parses and validates the text size value.
     *
     * @param textSize The text size from the DTO, may be null
     * @return A valid text size within the allowed range, or default if invalid
     */
    private static int parseTextSize(@Nullable Integer textSize) {
        if (textSize == null) {
            return Configuration.DEFAULT_TEXT_SIZE;
        }
        if (textSize < Configuration.MIN_TEXT_SIZE) {
            return Configuration.MIN_TEXT_SIZE;
        }
        if (textSize > Configuration.MAX_TEXT_SIZE) {
            return Configuration.MAX_TEXT_SIZE;
        }
        return textSize;
    }

    /**
     * Parses and validates the feedback style value.
     *
     * @param feedbackStyle The feedback style string from the DTO, may be null
     * @return A valid FeedbackStyle, or default if invalid
     */
    @NonNull
    private static FeedbackStyle parseFeedbackStyle(@Nullable String feedbackStyle) {
        if (feedbackStyle == null) {
            return Configuration.DEFAULT_FEEDBACK_STYLE;
        }
        try {
            return FeedbackStyle.valueOf(feedbackStyle.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Configuration.DEFAULT_FEEDBACK_STYLE;
        }
    }

    /**
     * Parses a Boolean value with a default.
     *
     * @param value        The Boolean value, may be null
     * @param defaultValue The default value to use if null
     * @return The boolean value or default
     */
    private static boolean parseBoolean(@Nullable Boolean value, boolean defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Parses and validates the mascot selection value.
     *
     * @param mascotSelection The mascot selection string from the DTO, may be null
     * @return A valid MascotSelection, or default if invalid
     */
    @NonNull
    private static MascotSelection parseMascotSelection(@Nullable String mascotSelection) {
        if (mascotSelection == null) {
            return Configuration.DEFAULT_MASCOT_SELECTION;
        }
        try {
            return MascotSelection.valueOf(mascotSelection.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Configuration.DEFAULT_MASCOT_SELECTION;
        }
    }
}
