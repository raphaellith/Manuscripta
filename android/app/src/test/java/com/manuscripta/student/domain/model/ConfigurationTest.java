package com.manuscripta.student.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.manuscripta.student.data.model.FeedbackStyle;
import com.manuscripta.student.data.model.MascotSelection;

import org.junit.Test;

/**
 * Unit tests for {@link Configuration} domain model.
 * Tests constructor validation and business logic per Validation Rules §2G.
 */
public class ConfigurationTest {

    @Test
    public void testConstructorWithValidValues() {
        Configuration config = new Configuration(
                12,
                FeedbackStyle.IMMEDIATE,
                true,
                true,
                true,
                MascotSelection.MASCOT1
        );

        assertNotNull(config);
        assertEquals(12, config.getTextSize());
        assertEquals(FeedbackStyle.IMMEDIATE, config.getFeedbackStyle());
        assertTrue(config.isTtsEnabled());
        assertTrue(config.isAiScaffoldingEnabled());
        assertTrue(config.isSummarisationEnabled());
        assertEquals(MascotSelection.MASCOT1, config.getMascotSelection());
    }

    @Test
    public void testConstructorWithMinTextSize() {
        Configuration config = new Configuration(
                Configuration.MIN_TEXT_SIZE,
                FeedbackStyle.IMMEDIATE,
                false,
                false,
                false,
                MascotSelection.NONE
        );

        assertEquals(Configuration.MIN_TEXT_SIZE, config.getTextSize());
    }

    @Test
    public void testConstructorWithMaxTextSize() {
        Configuration config = new Configuration(
                Configuration.MAX_TEXT_SIZE,
                FeedbackStyle.NEUTRAL,
                true,
                true,
                true,
                MascotSelection.MASCOT5
        );

        assertEquals(Configuration.MAX_TEXT_SIZE, config.getTextSize());
    }

    @Test
    public void testConstructor_textSizeTooSmall_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Configuration(
                        Configuration.MIN_TEXT_SIZE - 1,
                        FeedbackStyle.IMMEDIATE,
                        true, true, true,
                        MascotSelection.MASCOT1
                )
        );
        assertTrue(exception.getMessage().contains("TextSize must be between"));
    }

    @Test
    public void testConstructor_textSizeTooLarge_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Configuration(
                        Configuration.MAX_TEXT_SIZE + 1,
                        FeedbackStyle.IMMEDIATE,
                        true, true, true,
                        MascotSelection.MASCOT1
                )
        );
        assertTrue(exception.getMessage().contains("TextSize must be between"));
    }

    @Test
    public void testConstructor_nullFeedbackStyle_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Configuration(
                        12,
                        null,
                        true, true, true,
                        MascotSelection.MASCOT1
                )
        );
        assertEquals("FeedbackStyle cannot be null", exception.getMessage());
    }

    @Test
    public void testConstructor_nullMascotSelection_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Configuration(
                        12,
                        FeedbackStyle.IMMEDIATE,
                        true, true, true,
                        null
                )
        );
        assertEquals("MascotSelection cannot be null", exception.getMessage());
    }

    @Test
    public void testCreateDefault() {
        Configuration config = Configuration.createDefault();

        assertNotNull(config);
        assertEquals(Configuration.DEFAULT_TEXT_SIZE, config.getTextSize());
        assertEquals(Configuration.DEFAULT_FEEDBACK_STYLE, config.getFeedbackStyle());
        assertEquals(Configuration.DEFAULT_TTS_ENABLED, config.isTtsEnabled());
        assertEquals(Configuration.DEFAULT_AI_SCAFFOLDING_ENABLED, config.isAiScaffoldingEnabled());
        assertEquals(Configuration.DEFAULT_SUMMARISATION_ENABLED, config.isSummarisationEnabled());
        assertEquals(Configuration.DEFAULT_MASCOT_SELECTION, config.getMascotSelection());
    }

    @Test
    public void testAllFeedbackStyleValues() {
        for (FeedbackStyle style : FeedbackStyle.values()) {
            Configuration config = new Configuration(
                    12, style, true, true, true, MascotSelection.MASCOT1
            );
            assertEquals(style, config.getFeedbackStyle());
        }
    }

    @Test
    public void testAllMascotSelectionValues() {
        for (MascotSelection mascot : MascotSelection.values()) {
            Configuration config = new Configuration(
                    12, FeedbackStyle.IMMEDIATE, true, true, true, mascot
            );
            assertEquals(mascot, config.getMascotSelection());
        }
    }

    @Test
    public void testEqualsSameObject() {
        Configuration config = new Configuration(
                12, FeedbackStyle.IMMEDIATE, true, true, true, MascotSelection.MASCOT1
        );

        assertTrue(config.equals(config));
    }

    @Test
    public void testEqualsNull() {
        Configuration config = new Configuration(
                12, FeedbackStyle.IMMEDIATE, true, true, true, MascotSelection.MASCOT1
        );

        assertFalse(config.equals(null));
    }

    @Test
    public void testEqualsDifferentClass() {
        Configuration config = new Configuration(
                12, FeedbackStyle.IMMEDIATE, true, true, true, MascotSelection.MASCOT1
        );

        assertFalse(config.equals("not a config"));
    }

    @Test
    public void testEqualsSameValues() {
        Configuration config1 = new Configuration(
                12, FeedbackStyle.IMMEDIATE, true, true, true, MascotSelection.MASCOT1
        );
        Configuration config2 = new Configuration(
                12, FeedbackStyle.IMMEDIATE, true, true, true, MascotSelection.MASCOT1
        );

        assertTrue(config1.equals(config2));
        assertTrue(config2.equals(config1));
    }

    @Test
    public void testEqualsDifferentTextSize() {
        Configuration config1 = new Configuration(
                12, FeedbackStyle.IMMEDIATE, true, true, true, MascotSelection.MASCOT1
        );
        Configuration config2 = new Configuration(
                20, FeedbackStyle.IMMEDIATE, true, true, true, MascotSelection.MASCOT1
        );

        assertFalse(config1.equals(config2));
    }

    @Test
    public void testEqualsDifferentFeedbackStyle() {
        Configuration config1 = new Configuration(
                12, FeedbackStyle.IMMEDIATE, true, true, true, MascotSelection.MASCOT1
        );
        Configuration config2 = new Configuration(
                12, FeedbackStyle.NEUTRAL, true, true, true, MascotSelection.MASCOT1
        );

        assertFalse(config1.equals(config2));
    }

    @Test
    public void testEqualsDifferentTtsEnabled() {
        Configuration config1 = new Configuration(
                12, FeedbackStyle.IMMEDIATE, true, true, true, MascotSelection.MASCOT1
        );
        Configuration config2 = new Configuration(
                12, FeedbackStyle.IMMEDIATE, false, true, true, MascotSelection.MASCOT1
        );

        assertFalse(config1.equals(config2));
    }

    @Test
    public void testEqualsDifferentAiScaffoldingEnabled() {
        Configuration config1 = new Configuration(
                12, FeedbackStyle.IMMEDIATE, true, true, true, MascotSelection.MASCOT1
        );
        Configuration config2 = new Configuration(
                12, FeedbackStyle.IMMEDIATE, true, false, true, MascotSelection.MASCOT1
        );

        assertFalse(config1.equals(config2));
    }

    @Test
    public void testEqualsDifferentSummarisationEnabled() {
        Configuration config1 = new Configuration(
                12, FeedbackStyle.IMMEDIATE, true, true, true, MascotSelection.MASCOT1
        );
        Configuration config2 = new Configuration(
                12, FeedbackStyle.IMMEDIATE, true, true, false, MascotSelection.MASCOT1
        );

        assertFalse(config1.equals(config2));
    }

    @Test
    public void testEqualsDifferentMascotSelection() {
        Configuration config1 = new Configuration(
                12, FeedbackStyle.IMMEDIATE, true, true, true, MascotSelection.MASCOT1
        );
        Configuration config2 = new Configuration(
                12, FeedbackStyle.IMMEDIATE, true, true, true, MascotSelection.MASCOT5
        );

        assertFalse(config1.equals(config2));
    }

    @Test
    public void testHashCodeConsistency() {
        Configuration config = new Configuration(
                12, FeedbackStyle.IMMEDIATE, true, true, true, MascotSelection.MASCOT1
        );

        int hashCode1 = config.hashCode();
        int hashCode2 = config.hashCode();

        assertEquals(hashCode1, hashCode2);
    }

    @Test
    public void testHashCodeEquality() {
        Configuration config1 = new Configuration(
                12, FeedbackStyle.IMMEDIATE, true, true, true, MascotSelection.MASCOT1
        );
        Configuration config2 = new Configuration(
                12, FeedbackStyle.IMMEDIATE, true, true, true, MascotSelection.MASCOT1
        );

        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void testToString() {
        Configuration config = new Configuration(
                12, FeedbackStyle.IMMEDIATE, true, true, true, MascotSelection.MASCOT1
        );

        String result = config.toString();

        assertNotNull(result);
        assertTrue(result.contains("Configuration"));
        assertTrue(result.contains("12"));
        assertTrue(result.contains("IMMEDIATE"));
        assertTrue(result.contains("MASCOT1"));
    }

    @Test
    public void testDefaultConstants() {
        assertEquals(5, Configuration.MIN_TEXT_SIZE);
        assertEquals(50, Configuration.MAX_TEXT_SIZE);
        assertEquals(6, Configuration.DEFAULT_TEXT_SIZE);
        assertEquals(FeedbackStyle.IMMEDIATE, Configuration.DEFAULT_FEEDBACK_STYLE);
        assertFalse(Configuration.DEFAULT_TTS_ENABLED);
        assertFalse(Configuration.DEFAULT_AI_SCAFFOLDING_ENABLED);
        assertFalse(Configuration.DEFAULT_SUMMARISATION_ENABLED);
        assertEquals(MascotSelection.NONE, Configuration.DEFAULT_MASCOT_SELECTION);
    }
}
