package com.manuscripta.student.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Unit tests for {@link FeedbackDto}.
 * Tests construction, getters, setters, and JSON serialization behaviour.
 *
 * <p>Per Validation Rules.md §2F, feedback must have at least one of text or marks.
 * However, the DTO itself is a data transfer object and does not enforce this validation.</p>
 */
public class FeedbackDtoTest {

    private static final String TEST_ID = "fb-550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_RESPONSE_ID = "resp-7c9e6679-7425-40de-944b-e07fc1f90ae7";
    private static final String TEST_TEXT = "Excellent work on this question!";
    private static final Integer TEST_MARKS = 85;

    @Test
    public void testDefaultConstructor() {
        FeedbackDto dto = new FeedbackDto();

        assertNull(dto.getId());
        assertNull(dto.getResponseId());
        assertNull(dto.getText());
        assertNull(dto.getMarks());
    }

    @Test
    public void testConstructorWithAllFields() {
        FeedbackDto dto = new FeedbackDto(TEST_ID, TEST_RESPONSE_ID, TEST_TEXT, TEST_MARKS);

        assertEquals(TEST_ID, dto.getId());
        assertEquals(TEST_RESPONSE_ID, dto.getResponseId());
        assertEquals(TEST_TEXT, dto.getText());
        assertEquals(TEST_MARKS, dto.getMarks());
    }

    @Test
    public void testConstructorWithTextOnly() {
        FeedbackDto dto = new FeedbackDto(TEST_ID, TEST_RESPONSE_ID, TEST_TEXT, null);

        assertEquals(TEST_ID, dto.getId());
        assertEquals(TEST_RESPONSE_ID, dto.getResponseId());
        assertEquals(TEST_TEXT, dto.getText());
        assertNull(dto.getMarks());
    }

    @Test
    public void testConstructorWithMarksOnly() {
        FeedbackDto dto = new FeedbackDto(TEST_ID, TEST_RESPONSE_ID, null, TEST_MARKS);

        assertEquals(TEST_ID, dto.getId());
        assertEquals(TEST_RESPONSE_ID, dto.getResponseId());
        assertNull(dto.getText());
        assertEquals(TEST_MARKS, dto.getMarks());
    }

    @Test
    public void testConstructorWithNullValues() {
        FeedbackDto dto = new FeedbackDto(null, null, null, null);

        assertNull(dto.getId());
        assertNull(dto.getResponseId());
        assertNull(dto.getText());
        assertNull(dto.getMarks());
    }

    @Test
    public void testSetId() {
        FeedbackDto dto = new FeedbackDto();

        dto.setId(TEST_ID);
        assertEquals(TEST_ID, dto.getId());

        dto.setId(null);
        assertNull(dto.getId());
    }

    @Test
    public void testSetResponseId() {
        FeedbackDto dto = new FeedbackDto();

        dto.setResponseId(TEST_RESPONSE_ID);
        assertEquals(TEST_RESPONSE_ID, dto.getResponseId());

        dto.setResponseId(null);
        assertNull(dto.getResponseId());
    }

    @Test
    public void testSetText() {
        FeedbackDto dto = new FeedbackDto();

        dto.setText(TEST_TEXT);
        assertEquals(TEST_TEXT, dto.getText());

        dto.setText(null);
        assertNull(dto.getText());
    }

    @Test
    public void testSetMarks() {
        FeedbackDto dto = new FeedbackDto();

        dto.setMarks(TEST_MARKS);
        assertEquals(TEST_MARKS, dto.getMarks());

        dto.setMarks(null);
        assertNull(dto.getMarks());
    }

    @Test
    public void testSetMarksToZero() {
        FeedbackDto dto = new FeedbackDto();

        dto.setMarks(0);
        assertEquals(Integer.valueOf(0), dto.getMarks());
    }

    @Test
    public void testPreservesWindowsAssignedId() {
        // Per API Contract §4.1, Feedback IDs are assigned by Windows and must be preserved
        String windowsGeneratedId = "win-fb-12345-abcdef-67890";

        FeedbackDto dto = new FeedbackDto();
        dto.setId(windowsGeneratedId);

        assertEquals(windowsGeneratedId, dto.getId());

        // Re-set via constructor
        FeedbackDto dto2 = new FeedbackDto(windowsGeneratedId, TEST_RESPONSE_ID, TEST_TEXT, TEST_MARKS);
        assertEquals(windowsGeneratedId, dto2.getId());
    }

    @Test
    public void testEmptyTextString() {
        FeedbackDto dto = new FeedbackDto(TEST_ID, TEST_RESPONSE_ID, "", TEST_MARKS);

        assertNotNull(dto.getText());
        assertEquals("", dto.getText());
    }

    @Test
    public void testWhitespaceOnlyText() {
        String whitespaceText = "   \t\n  ";
        FeedbackDto dto = new FeedbackDto(TEST_ID, TEST_RESPONSE_ID, whitespaceText, TEST_MARKS);

        assertEquals(whitespaceText, dto.getText());
    }

    @Test
    public void testLongTextFeedback() {
        String longText = "This is a very long feedback message that a teacher might write "
                + "when providing detailed comments on a student's written response. "
                + "It includes multiple sentences and might span several lines. "
                + "The teacher wants to give comprehensive feedback to help the student improve.";

        FeedbackDto dto = new FeedbackDto(TEST_ID, TEST_RESPONSE_ID, longText, TEST_MARKS);

        assertEquals(longText, dto.getText());
    }

    @Test
    public void testMarksWithMaxValue() {
        Integer maxMarks = 100;
        FeedbackDto dto = new FeedbackDto(TEST_ID, TEST_RESPONSE_ID, TEST_TEXT, maxMarks);

        assertEquals(maxMarks, dto.getMarks());
    }

    @Test
    public void testTextWithSpecialCharacters() {
        String textWithSpecialChars = "Great work! 👍 Your answer was: a² + b² = c²";
        FeedbackDto dto = new FeedbackDto(TEST_ID, TEST_RESPONSE_ID, textWithSpecialChars, TEST_MARKS);

        assertEquals(textWithSpecialChars, dto.getText());
    }

    @Test
    public void testTextWithNewlines() {
        String multilineText = "Point 1: Correct\nPoint 2: Needs work\nOverall: Good effort";
        FeedbackDto dto = new FeedbackDto(TEST_ID, TEST_RESPONSE_ID, multilineText, TEST_MARKS);

        assertEquals(multilineText, dto.getText());
    }
}
