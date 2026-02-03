package com.manuscripta.student.domain.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.manuscripta.student.data.model.FeedbackStyle;
import com.manuscripta.student.data.model.MascotSelection;
import com.manuscripta.student.domain.model.Configuration;
import com.manuscripta.student.network.dto.ConfigResponseDto;

import org.junit.Test;

/**
 * Unit tests for {@link ConfigurationMapper}.
 * Tests conversion between ConfigResponseDto and Configuration domain model.
 */
public class ConfigurationMapperTest {

    @Test
    public void testFromDto_withAllFields() {
        // Given
        ConfigResponseDto dto = new ConfigResponseDto(
                20,
                "IMMEDIATE",
                true,
                true,
                true,
                "MASCOT3"
        );

        // When
        Configuration config = ConfigurationMapper.fromDto(dto);

        // Then
        assertNotNull(config);
        assertEquals(20, config.getTextSize());
        assertEquals(FeedbackStyle.IMMEDIATE, config.getFeedbackStyle());
        assertTrue(config.isTtsEnabled());
        assertTrue(config.isAiScaffoldingEnabled());
        assertTrue(config.isSummarisationEnabled());
        assertEquals(MascotSelection.MASCOT3, config.getMascotSelection());
    }

    @Test
    public void testFromDto_withNeutralFeedbackStyle() {
        // Given
        ConfigResponseDto dto = new ConfigResponseDto(
                15,
                "NEUTRAL",
                false,
                false,
                false,
                "NONE"
        );

        // When
        Configuration config = ConfigurationMapper.fromDto(dto);

        // Then
        assertEquals(FeedbackStyle.NEUTRAL, config.getFeedbackStyle());
        assertFalse(config.isTtsEnabled());
        assertFalse(config.isAiScaffoldingEnabled());
        assertFalse(config.isSummarisationEnabled());
        assertEquals(MascotSelection.NONE, config.getMascotSelection());
    }

    @Test
    public void testFromDto_withNullTextSize_usesDefault() {
        // Given
        ConfigResponseDto dto = new ConfigResponseDto(
                null,
                "IMMEDIATE",
                true,
                true,
                true,
                "MASCOT1"
        );

        // When
        Configuration config = ConfigurationMapper.fromDto(dto);

        // Then
        assertEquals(Configuration.DEFAULT_TEXT_SIZE, config.getTextSize());
    }

    @Test
    public void testFromDto_withTextSizeBelowMin_clampsToMin() {
        // Given
        ConfigResponseDto dto = new ConfigResponseDto(
                1, // Below minimum of 5
                "IMMEDIATE",
                true,
                true,
                true,
                "MASCOT1"
        );

        // When
        Configuration config = ConfigurationMapper.fromDto(dto);

        // Then
        assertEquals(Configuration.MIN_TEXT_SIZE, config.getTextSize());
    }

    @Test
    public void testFromDto_withTextSizeAboveMax_clampsToMax() {
        // Given
        ConfigResponseDto dto = new ConfigResponseDto(
                100, // Above maximum of 50
                "IMMEDIATE",
                true,
                true,
                true,
                "MASCOT1"
        );

        // When
        Configuration config = ConfigurationMapper.fromDto(dto);

        // Then
        assertEquals(Configuration.MAX_TEXT_SIZE, config.getTextSize());
    }

    @Test
    public void testFromDto_withNullFeedbackStyle_usesDefault() {
        // Given
        ConfigResponseDto dto = new ConfigResponseDto(
                12,
                null,
                true,
                true,
                true,
                "MASCOT1"
        );

        // When
        Configuration config = ConfigurationMapper.fromDto(dto);

        // Then
        assertEquals(Configuration.DEFAULT_FEEDBACK_STYLE, config.getFeedbackStyle());
    }

    @Test
    public void testFromDto_withInvalidFeedbackStyle_usesDefault() {
        // Given
        ConfigResponseDto dto = new ConfigResponseDto(
                12,
                "INVALID_STYLE",
                true,
                true,
                true,
                "MASCOT1"
        );

        // When
        Configuration config = ConfigurationMapper.fromDto(dto);

        // Then
        assertEquals(Configuration.DEFAULT_FEEDBACK_STYLE, config.getFeedbackStyle());
    }

    @Test
    public void testFromDto_withNullTtsEnabled_usesDefault() {
        // Given
        ConfigResponseDto dto = new ConfigResponseDto(
                12,
                "IMMEDIATE",
                null,
                true,
                true,
                "MASCOT1"
        );

        // When
        Configuration config = ConfigurationMapper.fromDto(dto);

        // Then
        assertEquals(Configuration.DEFAULT_TTS_ENABLED, config.isTtsEnabled());
    }

    @Test
    public void testFromDto_withNullAiScaffoldingEnabled_usesDefault() {
        // Given
        ConfigResponseDto dto = new ConfigResponseDto(
                12,
                "IMMEDIATE",
                true,
                null,
                true,
                "MASCOT1"
        );

        // When
        Configuration config = ConfigurationMapper.fromDto(dto);

        // Then
        assertEquals(Configuration.DEFAULT_AI_SCAFFOLDING_ENABLED, config.isAiScaffoldingEnabled());
    }

    @Test
    public void testFromDto_withNullSummarisationEnabled_usesDefault() {
        // Given
        ConfigResponseDto dto = new ConfigResponseDto(
                12,
                "IMMEDIATE",
                true,
                true,
                null,
                "MASCOT1"
        );

        // When
        Configuration config = ConfigurationMapper.fromDto(dto);

        // Then
        assertEquals(Configuration.DEFAULT_SUMMARISATION_ENABLED, config.isSummarisationEnabled());
    }

    @Test
    public void testFromDto_withNullMascotSelection_usesDefault() {
        // Given
        ConfigResponseDto dto = new ConfigResponseDto(
                12,
                "IMMEDIATE",
                true,
                true,
                true,
                null
        );

        // When
        Configuration config = ConfigurationMapper.fromDto(dto);

        // Then
        assertEquals(Configuration.DEFAULT_MASCOT_SELECTION, config.getMascotSelection());
    }

    @Test
    public void testFromDto_withInvalidMascotSelection_usesDefault() {
        // Given
        ConfigResponseDto dto = new ConfigResponseDto(
                12,
                "IMMEDIATE",
                true,
                true,
                true,
                "INVALID_MASCOT"
        );

        // When
        Configuration config = ConfigurationMapper.fromDto(dto);

        // Then
        assertEquals(Configuration.DEFAULT_MASCOT_SELECTION, config.getMascotSelection());
    }

    @Test
    public void testFromDto_withAllNullValues_usesDefaults() {
        // Given
        ConfigResponseDto dto = new ConfigResponseDto();

        // When
        Configuration config = ConfigurationMapper.fromDto(dto);

        // Then
        assertEquals(Configuration.DEFAULT_TEXT_SIZE, config.getTextSize());
        assertEquals(Configuration.DEFAULT_FEEDBACK_STYLE, config.getFeedbackStyle());
        assertEquals(Configuration.DEFAULT_TTS_ENABLED, config.isTtsEnabled());
        assertEquals(Configuration.DEFAULT_AI_SCAFFOLDING_ENABLED, config.isAiScaffoldingEnabled());
        assertEquals(Configuration.DEFAULT_SUMMARISATION_ENABLED, config.isSummarisationEnabled());
        assertEquals(Configuration.DEFAULT_MASCOT_SELECTION, config.getMascotSelection());
    }

    @Test
    public void testFromDto_withMinTextSize() {
        // Given
        ConfigResponseDto dto = new ConfigResponseDto(
                Configuration.MIN_TEXT_SIZE,
                "IMMEDIATE",
                true,
                true,
                true,
                "MASCOT1"
        );

        // When
        Configuration config = ConfigurationMapper.fromDto(dto);

        // Then
        assertEquals(Configuration.MIN_TEXT_SIZE, config.getTextSize());
    }

    @Test
    public void testFromDto_withMaxTextSize() {
        // Given
        ConfigResponseDto dto = new ConfigResponseDto(
                Configuration.MAX_TEXT_SIZE,
                "IMMEDIATE",
                true,
                true,
                true,
                "MASCOT1"
        );

        // When
        Configuration config = ConfigurationMapper.fromDto(dto);

        // Then
        assertEquals(Configuration.MAX_TEXT_SIZE, config.getTextSize());
    }

    @Test
    public void testFromDto_caseInsensitiveFeedbackStyle() {
        // Given
        ConfigResponseDto dto = new ConfigResponseDto(
                12,
                "immediate", // lowercase
                true,
                true,
                true,
                "MASCOT1"
        );

        // When
        Configuration config = ConfigurationMapper.fromDto(dto);

        // Then
        assertEquals(FeedbackStyle.IMMEDIATE, config.getFeedbackStyle());
    }

    @Test
    public void testFromDto_caseInsensitiveMascotSelection() {
        // Given
        ConfigResponseDto dto = new ConfigResponseDto(
                12,
                "IMMEDIATE",
                true,
                true,
                true,
                "mascot5" // lowercase
        );

        // When
        Configuration config = ConfigurationMapper.fromDto(dto);

        // Then
        assertEquals(MascotSelection.MASCOT5, config.getMascotSelection());
    }

    @Test
    public void testToDto_withAllFields() {
        // Given
        Configuration config = new Configuration(
                25,
                FeedbackStyle.NEUTRAL,
                false,
                true,
                false,
                MascotSelection.MASCOT2
        );

        // When
        ConfigResponseDto dto = ConfigurationMapper.toDto(config);

        // Then
        assertNotNull(dto);
        assertEquals(Integer.valueOf(25), dto.getTextSize());
        assertEquals("NEUTRAL", dto.getFeedbackStyle());
        assertFalse(dto.getTtsEnabled());
        assertTrue(dto.getAiScaffoldingEnabled());
        assertFalse(dto.getSummarisationEnabled());
        assertEquals("MASCOT2", dto.getMascotSelection());
    }

    @Test
    public void testRoundTripConversion_DtoToConfigToDto() {
        // Given
        ConfigResponseDto originalDto = new ConfigResponseDto(
                30,
                "IMMEDIATE",
                true,
                false,
                true,
                "MASCOT4"
        );

        // When
        Configuration config = ConfigurationMapper.fromDto(originalDto);
        ConfigResponseDto resultDto = ConfigurationMapper.toDto(config);

        // Then
        assertEquals(originalDto.getTextSize(), resultDto.getTextSize());
        assertEquals(originalDto.getFeedbackStyle(), resultDto.getFeedbackStyle());
        assertEquals(originalDto.getTtsEnabled(), resultDto.getTtsEnabled());
        assertEquals(originalDto.getAiScaffoldingEnabled(), resultDto.getAiScaffoldingEnabled());
        assertEquals(originalDto.getSummarisationEnabled(), resultDto.getSummarisationEnabled());
        assertEquals(originalDto.getMascotSelection(), resultDto.getMascotSelection());
    }

    @Test
    public void testAllMascotSelections() {
        // Test all mascot selection values are properly mapped
        for (MascotSelection mascot : MascotSelection.values()) {
            ConfigResponseDto dto = new ConfigResponseDto(
                    12, "IMMEDIATE", true, true, true, mascot.name()
            );
            Configuration config = ConfigurationMapper.fromDto(dto);
            assertEquals(mascot, config.getMascotSelection());
        }
    }

    @Test
    public void testAllFeedbackStyles() {
        // Test all feedback style values are properly mapped
        for (FeedbackStyle style : FeedbackStyle.values()) {
            ConfigResponseDto dto = new ConfigResponseDto(
                    12, style.name(), true, true, true, "MASCOT1"
            );
            Configuration config = ConfigurationMapper.fromDto(dto);
            assertEquals(style, config.getFeedbackStyle());
        }
    }

    @Test
    public void testPrivateConstructorThrowsException() throws Exception {
        // Use reflection to access private constructor
        java.lang.reflect.Constructor<ConfigurationMapper> constructor =
                ConfigurationMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            throw new AssertionError("Expected constructor to throw AssertionError");
        } catch (java.lang.reflect.InvocationTargetException e) {
            // Verify the cause is AssertionError
            assertEquals(AssertionError.class, e.getCause().getClass());
        }
    }
}
