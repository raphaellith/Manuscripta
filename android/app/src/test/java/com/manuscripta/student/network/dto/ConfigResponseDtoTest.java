package com.manuscripta.student.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link ConfigResponseDto}.
 * Tests the DTO for tablet configuration responses per API Contract §2.2 and Validation Rules §2G.
 */
public class ConfigResponseDtoTest {

    private static final Integer TEST_TEXT_SIZE = 12;
    private static final String TEST_FEEDBACK_STYLE = "IMMEDIATE";
    private static final Boolean TEST_TTS_ENABLED = true;
    private static final Boolean TEST_AI_SCAFFOLDING_ENABLED = true;
    private static final Boolean TEST_SUMMARISATION_ENABLED = true;
    private static final String TEST_MASCOT_SELECTION = "MASCOT1";

    @Test
    public void testDefaultConstructor() {
        ConfigResponseDto dto = new ConfigResponseDto();

        assertNull(dto.getTextSize());
        assertNull(dto.getFeedbackStyle());
        assertNull(dto.getTtsEnabled());
        assertNull(dto.getAiScaffoldingEnabled());
        assertNull(dto.getSummarisationEnabled());
        assertNull(dto.getMascotSelection());
    }

    @Test
    public void testConstructorWithAllFields() {
        ConfigResponseDto dto = new ConfigResponseDto(
                TEST_TEXT_SIZE,
                TEST_FEEDBACK_STYLE,
                TEST_TTS_ENABLED,
                TEST_AI_SCAFFOLDING_ENABLED,
                TEST_SUMMARISATION_ENABLED,
                TEST_MASCOT_SELECTION
        );

        assertEquals(TEST_TEXT_SIZE, dto.getTextSize());
        assertEquals(TEST_FEEDBACK_STYLE, dto.getFeedbackStyle());
        assertEquals(TEST_TTS_ENABLED, dto.getTtsEnabled());
        assertEquals(TEST_AI_SCAFFOLDING_ENABLED, dto.getAiScaffoldingEnabled());
        assertEquals(TEST_SUMMARISATION_ENABLED, dto.getSummarisationEnabled());
        assertEquals(TEST_MASCOT_SELECTION, dto.getMascotSelection());
    }

    @Test
    public void testConstructorWithNullValues() {
        ConfigResponseDto dto = new ConfigResponseDto(null, null, null, null, null, null);

        assertNull(dto.getTextSize());
        assertNull(dto.getFeedbackStyle());
        assertNull(dto.getTtsEnabled());
        assertNull(dto.getAiScaffoldingEnabled());
        assertNull(dto.getSummarisationEnabled());
        assertNull(dto.getMascotSelection());
    }

    @Test
    public void testSetTextSize() {
        ConfigResponseDto dto = new ConfigResponseDto();

        dto.setTextSize(5);
        assertEquals(Integer.valueOf(5), dto.getTextSize());

        dto.setTextSize(50);
        assertEquals(Integer.valueOf(50), dto.getTextSize());

        dto.setTextSize(null);
        assertNull(dto.getTextSize());
    }

    @Test
    public void testSetFeedbackStyle() {
        ConfigResponseDto dto = new ConfigResponseDto();

        dto.setFeedbackStyle("IMMEDIATE");
        assertEquals("IMMEDIATE", dto.getFeedbackStyle());

        dto.setFeedbackStyle("NEUTRAL");
        assertEquals("NEUTRAL", dto.getFeedbackStyle());

        dto.setFeedbackStyle(null);
        assertNull(dto.getFeedbackStyle());
    }

    @Test
    public void testSetTtsEnabled() {
        ConfigResponseDto dto = new ConfigResponseDto();

        dto.setTtsEnabled(true);
        assertEquals(Boolean.TRUE, dto.getTtsEnabled());

        dto.setTtsEnabled(false);
        assertEquals(Boolean.FALSE, dto.getTtsEnabled());

        dto.setTtsEnabled(null);
        assertNull(dto.getTtsEnabled());
    }

    @Test
    public void testSetAiScaffoldingEnabled() {
        ConfigResponseDto dto = new ConfigResponseDto();

        dto.setAiScaffoldingEnabled(true);
        assertEquals(Boolean.TRUE, dto.getAiScaffoldingEnabled());

        dto.setAiScaffoldingEnabled(false);
        assertEquals(Boolean.FALSE, dto.getAiScaffoldingEnabled());

        dto.setAiScaffoldingEnabled(null);
        assertNull(dto.getAiScaffoldingEnabled());
    }

    @Test
    public void testSetSummarisationEnabled() {
        ConfigResponseDto dto = new ConfigResponseDto();

        dto.setSummarisationEnabled(true);
        assertEquals(Boolean.TRUE, dto.getSummarisationEnabled());

        dto.setSummarisationEnabled(false);
        assertEquals(Boolean.FALSE, dto.getSummarisationEnabled());

        dto.setSummarisationEnabled(null);
        assertNull(dto.getSummarisationEnabled());
    }

    @Test
    public void testSetMascotSelection() {
        ConfigResponseDto dto = new ConfigResponseDto();

        dto.setMascotSelection("NONE");
        assertEquals("NONE", dto.getMascotSelection());

        dto.setMascotSelection("MASCOT5");
        assertEquals("MASCOT5", dto.getMascotSelection());

        dto.setMascotSelection(null);
        assertNull(dto.getMascotSelection());
    }

    @Test
    public void testToString() {
        ConfigResponseDto dto = new ConfigResponseDto(
                TEST_TEXT_SIZE,
                TEST_FEEDBACK_STYLE,
                TEST_TTS_ENABLED,
                TEST_AI_SCAFFOLDING_ENABLED,
                TEST_SUMMARISATION_ENABLED,
                TEST_MASCOT_SELECTION
        );

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("ConfigResponseDto"));
        assertTrue(result.contains(TEST_TEXT_SIZE.toString()));
        assertTrue(result.contains(TEST_FEEDBACK_STYLE));
        assertTrue(result.contains(TEST_MASCOT_SELECTION));
    }

    @Test
    public void testToStringWithNullValues() {
        ConfigResponseDto dto = new ConfigResponseDto();

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("ConfigResponseDto"));
        assertTrue(result.contains("null"));
    }

    @Test
    public void testEqualsSameObject() {
        ConfigResponseDto dto = new ConfigResponseDto(
                TEST_TEXT_SIZE,
                TEST_FEEDBACK_STYLE,
                TEST_TTS_ENABLED,
                TEST_AI_SCAFFOLDING_ENABLED,
                TEST_SUMMARISATION_ENABLED,
                TEST_MASCOT_SELECTION
        );

        assertTrue(dto.equals(dto));
    }

    @Test
    public void testEqualsNull() {
        ConfigResponseDto dto = new ConfigResponseDto(
                TEST_TEXT_SIZE,
                TEST_FEEDBACK_STYLE,
                TEST_TTS_ENABLED,
                TEST_AI_SCAFFOLDING_ENABLED,
                TEST_SUMMARISATION_ENABLED,
                TEST_MASCOT_SELECTION
        );

        assertFalse(dto.equals(null));
    }

    @Test
    public void testEqualsDifferentClass() {
        ConfigResponseDto dto = new ConfigResponseDto(
                TEST_TEXT_SIZE,
                TEST_FEEDBACK_STYLE,
                TEST_TTS_ENABLED,
                TEST_AI_SCAFFOLDING_ENABLED,
                TEST_SUMMARISATION_ENABLED,
                TEST_MASCOT_SELECTION
        );

        assertFalse(dto.equals("not a dto"));
    }

    @Test
    public void testEqualsSameValues() {
        ConfigResponseDto dto1 = new ConfigResponseDto(
                TEST_TEXT_SIZE,
                TEST_FEEDBACK_STYLE,
                TEST_TTS_ENABLED,
                TEST_AI_SCAFFOLDING_ENABLED,
                TEST_SUMMARISATION_ENABLED,
                TEST_MASCOT_SELECTION
        );
        ConfigResponseDto dto2 = new ConfigResponseDto(
                TEST_TEXT_SIZE,
                TEST_FEEDBACK_STYLE,
                TEST_TTS_ENABLED,
                TEST_AI_SCAFFOLDING_ENABLED,
                TEST_SUMMARISATION_ENABLED,
                TEST_MASCOT_SELECTION
        );

        assertTrue(dto1.equals(dto2));
        assertTrue(dto2.equals(dto1));
    }

    @Test
    public void testEqualsDifferentTextSize() {
        ConfigResponseDto dto1 = new ConfigResponseDto(12, TEST_FEEDBACK_STYLE,
                TEST_TTS_ENABLED, TEST_AI_SCAFFOLDING_ENABLED, TEST_SUMMARISATION_ENABLED,
                TEST_MASCOT_SELECTION);
        ConfigResponseDto dto2 = new ConfigResponseDto(20, TEST_FEEDBACK_STYLE,
                TEST_TTS_ENABLED, TEST_AI_SCAFFOLDING_ENABLED, TEST_SUMMARISATION_ENABLED,
                TEST_MASCOT_SELECTION);

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentFeedbackStyle() {
        ConfigResponseDto dto1 = new ConfigResponseDto(TEST_TEXT_SIZE, "IMMEDIATE",
                TEST_TTS_ENABLED, TEST_AI_SCAFFOLDING_ENABLED, TEST_SUMMARISATION_ENABLED,
                TEST_MASCOT_SELECTION);
        ConfigResponseDto dto2 = new ConfigResponseDto(TEST_TEXT_SIZE, "NEUTRAL",
                TEST_TTS_ENABLED, TEST_AI_SCAFFOLDING_ENABLED, TEST_SUMMARISATION_ENABLED,
                TEST_MASCOT_SELECTION);

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentTtsEnabled() {
        ConfigResponseDto dto1 = new ConfigResponseDto(TEST_TEXT_SIZE, TEST_FEEDBACK_STYLE,
                true, TEST_AI_SCAFFOLDING_ENABLED, TEST_SUMMARISATION_ENABLED,
                TEST_MASCOT_SELECTION);
        ConfigResponseDto dto2 = new ConfigResponseDto(TEST_TEXT_SIZE, TEST_FEEDBACK_STYLE,
                false, TEST_AI_SCAFFOLDING_ENABLED, TEST_SUMMARISATION_ENABLED,
                TEST_MASCOT_SELECTION);

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsAllNullValues() {
        ConfigResponseDto dto1 = new ConfigResponseDto();
        ConfigResponseDto dto2 = new ConfigResponseDto();

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testHashCodeConsistency() {
        ConfigResponseDto dto = new ConfigResponseDto(
                TEST_TEXT_SIZE,
                TEST_FEEDBACK_STYLE,
                TEST_TTS_ENABLED,
                TEST_AI_SCAFFOLDING_ENABLED,
                TEST_SUMMARISATION_ENABLED,
                TEST_MASCOT_SELECTION
        );

        int hashCode1 = dto.hashCode();
        int hashCode2 = dto.hashCode();

        assertEquals(hashCode1, hashCode2);
    }

    @Test
    public void testHashCodeEquality() {
        ConfigResponseDto dto1 = new ConfigResponseDto(
                TEST_TEXT_SIZE,
                TEST_FEEDBACK_STYLE,
                TEST_TTS_ENABLED,
                TEST_AI_SCAFFOLDING_ENABLED,
                TEST_SUMMARISATION_ENABLED,
                TEST_MASCOT_SELECTION
        );
        ConfigResponseDto dto2 = new ConfigResponseDto(
                TEST_TEXT_SIZE,
                TEST_FEEDBACK_STYLE,
                TEST_TTS_ENABLED,
                TEST_AI_SCAFFOLDING_ENABLED,
                TEST_SUMMARISATION_ENABLED,
                TEST_MASCOT_SELECTION
        );

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testHashCodeWithNullValues() {
        ConfigResponseDto dto1 = new ConfigResponseDto();
        ConfigResponseDto dto2 = new ConfigResponseDto();

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testTextSizeValidRange() {
        // Test minimum valid text size
        ConfigResponseDto minDto = new ConfigResponseDto(5, TEST_FEEDBACK_STYLE,
                TEST_TTS_ENABLED, TEST_AI_SCAFFOLDING_ENABLED, TEST_SUMMARISATION_ENABLED,
                TEST_MASCOT_SELECTION);
        assertEquals(Integer.valueOf(5), minDto.getTextSize());

        // Test maximum valid text size
        ConfigResponseDto maxDto = new ConfigResponseDto(50, TEST_FEEDBACK_STYLE,
                TEST_TTS_ENABLED, TEST_AI_SCAFFOLDING_ENABLED, TEST_SUMMARISATION_ENABLED,
                TEST_MASCOT_SELECTION);
        assertEquals(Integer.valueOf(50), maxDto.getTextSize());
    }

    @Test
    public void testAllFeedbackStyleValues() {
        ConfigResponseDto immediate = new ConfigResponseDto(TEST_TEXT_SIZE, "IMMEDIATE",
                TEST_TTS_ENABLED, TEST_AI_SCAFFOLDING_ENABLED, TEST_SUMMARISATION_ENABLED,
                TEST_MASCOT_SELECTION);
        ConfigResponseDto neutral = new ConfigResponseDto(TEST_TEXT_SIZE, "NEUTRAL",
                TEST_TTS_ENABLED, TEST_AI_SCAFFOLDING_ENABLED, TEST_SUMMARISATION_ENABLED,
                TEST_MASCOT_SELECTION);

        assertEquals("IMMEDIATE", immediate.getFeedbackStyle());
        assertEquals("NEUTRAL", neutral.getFeedbackStyle());
    }

    @Test
    public void testAllMascotSelectionValues() {
        String[] mascotValues = {"NONE", "MASCOT1", "MASCOT2", "MASCOT3", "MASCOT4", "MASCOT5"};

        for (String mascot : mascotValues) {
            ConfigResponseDto dto = new ConfigResponseDto(TEST_TEXT_SIZE, TEST_FEEDBACK_STYLE,
                    TEST_TTS_ENABLED, TEST_AI_SCAFFOLDING_ENABLED, TEST_SUMMARISATION_ENABLED,
                    mascot);
            assertEquals(mascot, dto.getMascotSelection());
        }
    }

    @Test
    public void testHashCodeDifferentValues() {
        ConfigResponseDto dto1 = new ConfigResponseDto(12, "IMMEDIATE", true, true, true, "MASCOT1");
        ConfigResponseDto dto2 = new ConfigResponseDto(20, "NEUTRAL", false, false, false, "MASCOT2");

        assertNotEquals(dto1.hashCode(), dto2.hashCode());
    }
}
