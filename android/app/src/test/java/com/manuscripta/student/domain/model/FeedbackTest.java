package com.manuscripta.student.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link Feedback} domain model.
 * Tests constructor validation and business logic.
 */
public class FeedbackTest {

    @Test
    public void testConstructorWithTextOnly() {
        Feedback feedback = new Feedback(
                "feedback-1",
                "response-1",
                "Good work!",
                null
        );

        assertNotNull(feedback);
        assertEquals("feedback-1", feedback.getId());
        assertEquals("response-1", feedback.getResponseId());
        assertEquals("Good work!", feedback.getText());
        assertNull(feedback.getMarks());
        assertTrue(feedback.hasText());
        assertFalse(feedback.hasMarks());
    }

    @Test
    public void testConstructorWithMarksOnly() {
        Feedback feedback = new Feedback(
                "feedback-2",
                "response-2",
                null,
                85
        );

        assertNotNull(feedback);
        assertEquals("feedback-2", feedback.getId());
        assertEquals("response-2", feedback.getResponseId());
        assertNull(feedback.getText());
        assertEquals(Integer.valueOf(85), feedback.getMarks());
        assertFalse(feedback.hasText());
        assertTrue(feedback.hasMarks());
    }

    @Test
    public void testConstructorWithTextAndMarks() {
        Feedback feedback = new Feedback(
                "feedback-3",
                "response-3",
                "Excellent effort!",
                95
        );

        assertNotNull(feedback);
        assertEquals("feedback-3", feedback.getId());
        assertEquals("response-3", feedback.getResponseId());
        assertEquals("Excellent effort!", feedback.getText());
        assertEquals(Integer.valueOf(95), feedback.getMarks());
        assertTrue(feedback.hasText());
        assertTrue(feedback.hasMarks());
    }

    @Test
    public void testConstructor_nullId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Feedback(null, "response-1", "Text", 50)
        );
        assertEquals("Feedback id cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_emptyId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Feedback("", "response-1", "Text", 50)
        );
        assertEquals("Feedback id cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_blankId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Feedback("   ", "response-1", "Text", 50)
        );
        assertEquals("Feedback id cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_nullResponseId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Feedback("feedback-1", null, "Text", 50)
        );
        assertEquals("Feedback responseId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_emptyResponseId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Feedback("feedback-1", "", "Text", 50)
        );
        assertEquals("Feedback responseId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_blankResponseId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Feedback("feedback-1", "   ", "Text", 50)
        );
        assertEquals("Feedback responseId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_bothTextAndMarksNull_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Feedback("feedback-1", "response-1", null, null)
        );
        assertEquals("Feedback must have at least one of text or marks", exception.getMessage());
    }

    @Test
    public void testConstructor_emptyText_withoutMarks_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Feedback("feedback-1", "response-1", "", null)
        );
        assertEquals("Feedback must have at least one of text or marks", exception.getMessage());
    }

    @Test
    public void testConstructor_blankText_withoutMarks_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Feedback("feedback-1", "response-1", "   ", null)
        );
        assertEquals("Feedback must have at least one of text or marks", exception.getMessage());
    }

    @Test
    public void testConstructor_negativeMarks_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Feedback("feedback-1", "response-1", null, -1)
        );
        assertEquals("Feedback marks cannot be negative", exception.getMessage());
    }

    @Test
    public void testConstructorWithZeroMarks() {
        Feedback feedback = new Feedback(
                "feedback-zero",
                "response-zero",
                null,
                0
        );

        assertEquals(Integer.valueOf(0), feedback.getMarks());
        assertTrue(feedback.hasMarks());
    }

    @Test
    public void testConstructorWithEmptyTextAndMarks() {
        // Empty text with marks should be valid (marks is the required field)
        Feedback feedback = new Feedback(
                "feedback-empty-text",
                "response-1",
                "",
                50
        );

        assertEquals("", feedback.getText());
        assertFalse(feedback.hasText()); // Empty string is not considered having text
        assertTrue(feedback.hasMarks());
    }

    @Test
    public void testConstructorWithBlankTextAndMarks() {
        // Blank text with marks should be valid (marks is the required field)
        Feedback feedback = new Feedback(
                "feedback-blank-text",
                "response-1",
                "   ",
                60
        );

        assertEquals("   ", feedback.getText());
        assertFalse(feedback.hasText()); // Whitespace-only is not considered having text
        assertTrue(feedback.hasMarks());
    }

    @Test
    public void testHasText_withNonEmptyText() {
        Feedback feedback = new Feedback("f-1", "r-1", "Some feedback", null);
        assertTrue(feedback.hasText());
    }

    @Test
    public void testHasText_withNullText() {
        Feedback feedback = new Feedback("f-1", "r-1", null, 50);
        assertFalse(feedback.hasText());
    }

    @Test
    public void testHasText_withEmptyText() {
        Feedback feedback = new Feedback("f-1", "r-1", "", 50);
        assertFalse(feedback.hasText());
    }

    @Test
    public void testHasText_withWhitespaceOnlyText() {
        Feedback feedback = new Feedback("f-1", "r-1", "   ", 50);
        assertFalse(feedback.hasText());
    }

    @Test
    public void testHasMarks_withMarks() {
        Feedback feedback = new Feedback("f-1", "r-1", null, 75);
        assertTrue(feedback.hasMarks());
    }

    @Test
    public void testHasMarks_withZeroMarks() {
        Feedback feedback = new Feedback("f-1", "r-1", null, 0);
        assertTrue(feedback.hasMarks());
    }

    @Test
    public void testHasMarks_withNullMarks() {
        Feedback feedback = new Feedback("f-1", "r-1", "Some text", null);
        assertFalse(feedback.hasMarks());
    }

    @Test
    public void testTextWithSpecialCharacters() {
        String specialText = "Great work! 👍 Keep it up — you're doing well.";
        Feedback feedback = new Feedback("f-special", "r-1", specialText, null);

        assertEquals(specialText, feedback.getText());
        assertTrue(feedback.hasText());
    }

    @Test
    public void testTextWithMultilineContent() {
        String multilineText = "Good work!\nHowever, please review:\n- Point 1\n- Point 2";
        Feedback feedback = new Feedback("f-multiline", "r-1", multilineText, null);

        assertEquals(multilineText, feedback.getText());
        assertTrue(feedback.hasText());
    }

    @Test
    public void testHighMarksValue() {
        Feedback feedback = new Feedback("f-high", "r-1", null, Integer.MAX_VALUE);

        assertEquals(Integer.valueOf(Integer.MAX_VALUE), feedback.getMarks());
    }
}
