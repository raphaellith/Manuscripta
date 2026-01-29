package com.manuscripta.student.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link ResponseResultDto}.
 * Tests construction, getters, setters, equals, hashCode, and toString methods.
 */
public class ResponseResultDtoTest {

    private static final String TEST_RESPONSE_ID = "resp-uuid-123";
    private static final Boolean TEST_IS_CORRECT = true;
    private static final String TEST_FEEDBACK = "Well done!";
    private static final Integer TEST_SCORE = 85;
    private static final String TEST_CORRECT_ANSWER = "42";

    @Test
    public void testDefaultConstructor() {
        ResponseResultDto dto = new ResponseResultDto();

        assertNull(dto.getResponseId());
        assertNull(dto.getIsCorrect());
        assertNull(dto.getFeedback());
        assertNull(dto.getScore());
        assertNull(dto.getCorrectAnswer());
    }

    @Test
    public void testConstructorWithAllFields() {
        ResponseResultDto dto = new ResponseResultDto(
                TEST_RESPONSE_ID,
                TEST_IS_CORRECT,
                TEST_FEEDBACK,
                TEST_SCORE,
                TEST_CORRECT_ANSWER
        );

        assertEquals(TEST_RESPONSE_ID, dto.getResponseId());
        assertEquals(TEST_IS_CORRECT, dto.getIsCorrect());
        assertEquals(TEST_FEEDBACK, dto.getFeedback());
        assertEquals(TEST_SCORE, dto.getScore());
        assertEquals(TEST_CORRECT_ANSWER, dto.getCorrectAnswer());
    }

    @Test
    public void testConstructorWithNullValues() {
        ResponseResultDto dto = new ResponseResultDto(null, null, null, null, null);

        assertNull(dto.getResponseId());
        assertNull(dto.getIsCorrect());
        assertNull(dto.getFeedback());
        assertNull(dto.getScore());
        assertNull(dto.getCorrectAnswer());
    }

    @Test
    public void testSetResponseId() {
        ResponseResultDto dto = new ResponseResultDto();

        dto.setResponseId(TEST_RESPONSE_ID);
        assertEquals(TEST_RESPONSE_ID, dto.getResponseId());

        dto.setResponseId(null);
        assertNull(dto.getResponseId());
    }

    @Test
    public void testSetIsCorrect() {
        ResponseResultDto dto = new ResponseResultDto();

        dto.setIsCorrect(true);
        assertEquals(Boolean.TRUE, dto.getIsCorrect());

        dto.setIsCorrect(false);
        assertEquals(Boolean.FALSE, dto.getIsCorrect());

        dto.setIsCorrect(null);
        assertNull(dto.getIsCorrect());
    }

    @Test
    public void testSetFeedback() {
        ResponseResultDto dto = new ResponseResultDto();

        dto.setFeedback(TEST_FEEDBACK);
        assertEquals(TEST_FEEDBACK, dto.getFeedback());

        dto.setFeedback(null);
        assertNull(dto.getFeedback());
    }

    @Test
    public void testSetScore() {
        ResponseResultDto dto = new ResponseResultDto();

        dto.setScore(TEST_SCORE);
        assertEquals(TEST_SCORE, dto.getScore());

        dto.setScore(0);
        assertEquals(Integer.valueOf(0), dto.getScore());

        dto.setScore(null);
        assertNull(dto.getScore());
    }

    @Test
    public void testSetCorrectAnswer() {
        ResponseResultDto dto = new ResponseResultDto();

        dto.setCorrectAnswer(TEST_CORRECT_ANSWER);
        assertEquals(TEST_CORRECT_ANSWER, dto.getCorrectAnswer());

        dto.setCorrectAnswer(null);
        assertNull(dto.getCorrectAnswer());
    }

    @Test
    public void testToString() {
        ResponseResultDto dto = createTestResponseResultDto();

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("ResponseResultDto"));
        assertTrue(result.contains(TEST_RESPONSE_ID));
        assertTrue(result.contains(TEST_FEEDBACK));
        assertTrue(result.contains(TEST_CORRECT_ANSWER));
    }

    @Test
    public void testToStringWithNullValues() {
        ResponseResultDto dto = new ResponseResultDto();

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("ResponseResultDto"));
        assertTrue(result.contains("null"));
    }

    @Test
    public void testEqualsSameObject() {
        ResponseResultDto dto = createTestResponseResultDto();

        assertTrue(dto.equals(dto));
    }

    @Test
    public void testEqualsNull() {
        ResponseResultDto dto = createTestResponseResultDto();

        assertFalse(dto.equals(null));
    }

    @Test
    public void testEqualsDifferentClass() {
        ResponseResultDto dto = createTestResponseResultDto();

        assertFalse(dto.equals("not a dto"));
    }

    @Test
    public void testEqualsSameValues() {
        ResponseResultDto dto1 = createTestResponseResultDto();
        ResponseResultDto dto2 = createTestResponseResultDto();

        assertTrue(dto1.equals(dto2));
        assertTrue(dto2.equals(dto1));
    }

    @Test
    public void testEqualsDifferentResponseId() {
        ResponseResultDto dto1 = createTestResponseResultDto();
        ResponseResultDto dto2 = createTestResponseResultDto();
        dto2.setResponseId("different-id");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentIsCorrect() {
        ResponseResultDto dto1 = createTestResponseResultDto();
        ResponseResultDto dto2 = createTestResponseResultDto();
        dto2.setIsCorrect(false);

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentFeedback() {
        ResponseResultDto dto1 = createTestResponseResultDto();
        ResponseResultDto dto2 = createTestResponseResultDto();
        dto2.setFeedback("Different feedback");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentScore() {
        ResponseResultDto dto1 = createTestResponseResultDto();
        ResponseResultDto dto2 = createTestResponseResultDto();
        dto2.setScore(50);

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentCorrectAnswer() {
        ResponseResultDto dto1 = createTestResponseResultDto();
        ResponseResultDto dto2 = createTestResponseResultDto();
        dto2.setCorrectAnswer("100");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullVsNonNullResponseId() {
        ResponseResultDto dto1 = new ResponseResultDto(null, TEST_IS_CORRECT, TEST_FEEDBACK,
                TEST_SCORE, TEST_CORRECT_ANSWER);
        ResponseResultDto dto2 = createTestResponseResultDto();

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsAllNullValues() {
        ResponseResultDto dto1 = new ResponseResultDto();
        ResponseResultDto dto2 = new ResponseResultDto();

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testHashCodeConsistency() {
        ResponseResultDto dto = createTestResponseResultDto();

        int hashCode1 = dto.hashCode();
        int hashCode2 = dto.hashCode();

        assertEquals(hashCode1, hashCode2);
    }

    @Test
    public void testHashCodeEquality() {
        ResponseResultDto dto1 = createTestResponseResultDto();
        ResponseResultDto dto2 = createTestResponseResultDto();

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testHashCodeWithNullValues() {
        ResponseResultDto dto1 = new ResponseResultDto();
        ResponseResultDto dto2 = new ResponseResultDto();

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testHashCodeDifferentValues() {
        ResponseResultDto dto1 = createTestResponseResultDto();
        ResponseResultDto dto2 = createTestResponseResultDto();
        dto2.setResponseId("different-id");

        assertNotEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testCorrectResponseWithFeedback() {
        // Test a fully correct response with positive feedback
        ResponseResultDto dto = new ResponseResultDto(
                TEST_RESPONSE_ID,
                true,
                "Excellent work! Full marks.",
                100,
                "A"
        );

        assertTrue(dto.getIsCorrect());
        assertEquals(Integer.valueOf(100), dto.getScore());
        assertEquals("A", dto.getCorrectAnswer());
    }

    @Test
    public void testIncorrectResponseWithFeedback() {
        // Test an incorrect response with corrective feedback
        ResponseResultDto dto = new ResponseResultDto(
                TEST_RESPONSE_ID,
                false,
                "Not quite. Please review the material.",
                0,
                "B"
        );

        assertFalse(dto.getIsCorrect());
        assertEquals(Integer.valueOf(0), dto.getScore());
        assertEquals("B", dto.getCorrectAnswer());
    }

    @Test
    public void testPartialCreditResponse() {
        // Test a partially correct response
        ResponseResultDto dto = new ResponseResultDto(
                TEST_RESPONSE_ID,
                null,  // Neither fully correct nor incorrect
                "Good attempt. Partial credit awarded.",
                60,
                null   // Correct answer not revealed for partial credit
        );

        assertNull(dto.getIsCorrect());
        assertEquals(Integer.valueOf(60), dto.getScore());
        assertNull(dto.getCorrectAnswer());
    }

    @Test
    public void testFeedbackOnlyResult() {
        // Test result with only textual feedback, no score
        ResponseResultDto dto = new ResponseResultDto(
                TEST_RESPONSE_ID,
                null,
                "Thank you for your response. The teacher will review it.",
                null,
                null
        );

        assertNull(dto.getIsCorrect());
        assertNotNull(dto.getFeedback());
        assertNull(dto.getScore());
    }

    @Test
    public void testZeroScore() {
        // Test that zero score is handled correctly
        ResponseResultDto dto = new ResponseResultDto(
                TEST_RESPONSE_ID,
                false,
                "Incorrect answer.",
                0,
                "C"
        );

        assertEquals(Integer.valueOf(0), dto.getScore());
    }

    private ResponseResultDto createTestResponseResultDto() {
        return new ResponseResultDto(
                TEST_RESPONSE_ID,
                TEST_IS_CORRECT,
                TEST_FEEDBACK,
                TEST_SCORE,
                TEST_CORRECT_ANSWER
        );
    }
}
