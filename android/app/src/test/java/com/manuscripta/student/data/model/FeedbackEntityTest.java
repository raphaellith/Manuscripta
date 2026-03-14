package com.manuscripta.student.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Unit tests for {@link FeedbackEntity} entity.
 * Tests immutable entity construction and getters.
 */
public class FeedbackEntityTest {

    @Test
    public void testConstructorWithTextOnly() {
        FeedbackEntity feedback = new FeedbackEntity(
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
    }

    @Test
    public void testConstructorWithMarksOnly() {
        FeedbackEntity feedback = new FeedbackEntity(
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
    }

    @Test
    public void testConstructorWithTextAndMarks() {
        FeedbackEntity feedback = new FeedbackEntity(
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
    }

    @Test
    public void testConstructorWithBothNull() {
        // Entity allows both null - validation is in domain model
        FeedbackEntity feedback = new FeedbackEntity(
                "feedback-4",
                "response-4",
                null,
                null
        );

        assertNotNull(feedback);
        assertEquals("feedback-4", feedback.getId());
        assertEquals("response-4", feedback.getResponseId());
        assertNull(feedback.getText());
        assertNull(feedback.getMarks());
    }

    @Test
    public void testGetters() {
        FeedbackEntity feedback = new FeedbackEntity(
                "feedback-id-123",
                "response-id-456",
                "Well done!",
                75
        );

        assertEquals("feedback-id-123", feedback.getId());
        assertEquals("response-id-456", feedback.getResponseId());
        assertEquals("Well done!", feedback.getText());
        assertEquals(Integer.valueOf(75), feedback.getMarks());
    }

    @Test
    public void testMarksWithZeroValue() {
        FeedbackEntity feedback = new FeedbackEntity(
                "feedback-zero",
                "response-zero",
                null,
                0
        );

        assertEquals(Integer.valueOf(0), feedback.getMarks());
    }

    @Test
    public void testMarksWithMaxValue() {
        FeedbackEntity feedback = new FeedbackEntity(
                "feedback-max",
                "response-max",
                null,
                100
        );

        assertEquals(Integer.valueOf(100), feedback.getMarks());
    }

    @Test
    public void testTextWithEmptyString() {
        FeedbackEntity feedback = new FeedbackEntity(
                "feedback-empty-text",
                "response-empty",
                "",
                50
        );

        assertEquals("", feedback.getText());
    }

    @Test
    public void testTextWithWhitespace() {
        FeedbackEntity feedback = new FeedbackEntity(
                "feedback-whitespace",
                "response-whitespace",
                "   ",
                60
        );

        assertEquals("   ", feedback.getText());
    }

    @Test
    public void testTextWithSpecialCharacters() {
        String specialText = "Great work! 👍 Keep it up — you're doing well.";
        FeedbackEntity feedback = new FeedbackEntity(
                "feedback-special",
                "response-special",
                specialText,
                null
        );

        assertEquals(specialText, feedback.getText());
    }

    @Test
    public void testTextWithMultilineContent() {
        String multilineText = "Good work!\nHowever, please review:\n- Point 1\n- Point 2";
        FeedbackEntity feedback = new FeedbackEntity(
                "feedback-multiline",
                "response-multiline",
                multilineText,
                null
        );

        assertEquals(multilineText, feedback.getText());
    }
}
