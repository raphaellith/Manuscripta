package com.manuscripta.student.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link ResponseDto}.
 * Tests construction, getters, setters, equals, hashCode, and toString methods.
 */
public class ResponseDtoTest {

    private static final String TEST_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_QUESTION_ID = "q-uuid-123";
    private static final String TEST_MATERIAL_ID = "mat-uuid-456";
    private static final String TEST_STUDENT_ID = "device-uuid-789";
    private static final String TEST_ANSWER = "3";
    private static final String TEST_TIMESTAMP = "2023-10-27T10:05:00Z";
    private static final Boolean TEST_IS_CORRECT = true;

    @Test
    public void testDefaultConstructor() {
        ResponseDto dto = new ResponseDto();

        assertNull(dto.getId());
        assertNull(dto.getQuestionId());
        assertNull(dto.getMaterialId());
        assertNull(dto.getStudentId());
        assertNull(dto.getAnswer());
        assertNull(dto.getTimestamp());
        assertNull(dto.getIsCorrect());
    }

    @Test
    public void testConstructorWithAllFields() {
        ResponseDto dto = new ResponseDto(
                TEST_ID,
                TEST_QUESTION_ID,
                TEST_MATERIAL_ID,
                TEST_STUDENT_ID,
                TEST_ANSWER,
                TEST_TIMESTAMP,
                TEST_IS_CORRECT
        );

        assertEquals(TEST_ID, dto.getId());
        assertEquals(TEST_QUESTION_ID, dto.getQuestionId());
        assertEquals(TEST_MATERIAL_ID, dto.getMaterialId());
        assertEquals(TEST_STUDENT_ID, dto.getStudentId());
        assertEquals(TEST_ANSWER, dto.getAnswer());
        assertEquals(TEST_TIMESTAMP, dto.getTimestamp());
        assertEquals(TEST_IS_CORRECT, dto.getIsCorrect());
    }

    @Test
    public void testConstructorWithNullValues() {
        ResponseDto dto = new ResponseDto(null, null, null, null, null, null, null);

        assertNull(dto.getId());
        assertNull(dto.getQuestionId());
        assertNull(dto.getMaterialId());
        assertNull(dto.getStudentId());
        assertNull(dto.getAnswer());
        assertNull(dto.getTimestamp());
        assertNull(dto.getIsCorrect());
    }

    @Test
    public void testSetId() {
        ResponseDto dto = new ResponseDto();

        dto.setId(TEST_ID);
        assertEquals(TEST_ID, dto.getId());

        dto.setId(null);
        assertNull(dto.getId());
    }

    @Test
    public void testSetQuestionId() {
        ResponseDto dto = new ResponseDto();

        dto.setQuestionId(TEST_QUESTION_ID);
        assertEquals(TEST_QUESTION_ID, dto.getQuestionId());

        dto.setQuestionId(null);
        assertNull(dto.getQuestionId());
    }

    @Test
    public void testSetMaterialId() {
        ResponseDto dto = new ResponseDto();

        dto.setMaterialId(TEST_MATERIAL_ID);
        assertEquals(TEST_MATERIAL_ID, dto.getMaterialId());

        dto.setMaterialId(null);
        assertNull(dto.getMaterialId());
    }

    @Test
    public void testSetStudentId() {
        ResponseDto dto = new ResponseDto();

        dto.setStudentId(TEST_STUDENT_ID);
        assertEquals(TEST_STUDENT_ID, dto.getStudentId());

        dto.setStudentId(null);
        assertNull(dto.getStudentId());
    }

    @Test
    public void testSetAnswer() {
        ResponseDto dto = new ResponseDto();

        dto.setAnswer(TEST_ANSWER);
        assertEquals(TEST_ANSWER, dto.getAnswer());

        dto.setAnswer(null);
        assertNull(dto.getAnswer());
    }

    @Test
    public void testSetTimestamp() {
        ResponseDto dto = new ResponseDto();

        dto.setTimestamp(TEST_TIMESTAMP);
        assertEquals(TEST_TIMESTAMP, dto.getTimestamp());

        dto.setTimestamp(null);
        assertNull(dto.getTimestamp());
    }

    @Test
    public void testSetIsCorrect() {
        ResponseDto dto = new ResponseDto();

        dto.setIsCorrect(true);
        assertEquals(Boolean.TRUE, dto.getIsCorrect());

        dto.setIsCorrect(false);
        assertEquals(Boolean.FALSE, dto.getIsCorrect());

        dto.setIsCorrect(null);
        assertNull(dto.getIsCorrect());
    }

    @Test
    public void testToString() {
        ResponseDto dto = createTestResponseDto();

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("ResponseDto"));
        assertTrue(result.contains(TEST_ID));
        assertTrue(result.contains(TEST_QUESTION_ID));
        assertTrue(result.contains(TEST_MATERIAL_ID));
        assertTrue(result.contains(TEST_STUDENT_ID));
        assertTrue(result.contains(TEST_ANSWER));
        assertTrue(result.contains(TEST_TIMESTAMP));
    }

    @Test
    public void testToStringWithNullValues() {
        ResponseDto dto = new ResponseDto();

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("ResponseDto"));
        assertTrue(result.contains("null"));
    }

    @Test
    public void testEqualsSameObject() {
        ResponseDto dto = createTestResponseDto();

        assertTrue(dto.equals(dto));
    }

    @Test
    public void testEqualsNull() {
        ResponseDto dto = createTestResponseDto();

        assertFalse(dto.equals(null));
    }

    @Test
    public void testEqualsDifferentClass() {
        ResponseDto dto = createTestResponseDto();

        assertFalse(dto.equals("not a dto"));
    }

    @Test
    public void testEqualsSameValues() {
        ResponseDto dto1 = createTestResponseDto();
        ResponseDto dto2 = createTestResponseDto();

        assertTrue(dto1.equals(dto2));
        assertTrue(dto2.equals(dto1));
    }

    @Test
    public void testEqualsDifferentId() {
        ResponseDto dto1 = createTestResponseDto();
        ResponseDto dto2 = createTestResponseDto();
        dto2.setId("different-id");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentQuestionId() {
        ResponseDto dto1 = createTestResponseDto();
        ResponseDto dto2 = createTestResponseDto();
        dto2.setQuestionId("different-question-id");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentMaterialId() {
        ResponseDto dto1 = createTestResponseDto();
        ResponseDto dto2 = createTestResponseDto();
        dto2.setMaterialId("different-material-id");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentStudentId() {
        ResponseDto dto1 = createTestResponseDto();
        ResponseDto dto2 = createTestResponseDto();
        dto2.setStudentId("different-student-id");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentAnswer() {
        ResponseDto dto1 = createTestResponseDto();
        ResponseDto dto2 = createTestResponseDto();
        dto2.setAnswer("different-answer");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentTimestamp() {
        ResponseDto dto1 = createTestResponseDto();
        ResponseDto dto2 = createTestResponseDto();
        dto2.setTimestamp("2024-01-01T00:00:00Z");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentIsCorrect() {
        ResponseDto dto1 = createTestResponseDto();
        ResponseDto dto2 = createTestResponseDto();
        dto2.setIsCorrect(false);

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullVsNonNullId() {
        ResponseDto dto1 = new ResponseDto(null, TEST_QUESTION_ID, TEST_MATERIAL_ID,
                TEST_STUDENT_ID, TEST_ANSWER, TEST_TIMESTAMP, TEST_IS_CORRECT);
        ResponseDto dto2 = createTestResponseDto();

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsAllNullValues() {
        ResponseDto dto1 = new ResponseDto();
        ResponseDto dto2 = new ResponseDto();

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testHashCodeConsistency() {
        ResponseDto dto = createTestResponseDto();

        int hashCode1 = dto.hashCode();
        int hashCode2 = dto.hashCode();

        assertEquals(hashCode1, hashCode2);
    }

    @Test
    public void testHashCodeEquality() {
        ResponseDto dto1 = createTestResponseDto();
        ResponseDto dto2 = createTestResponseDto();

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testHashCodeWithNullValues() {
        ResponseDto dto1 = new ResponseDto();
        ResponseDto dto2 = new ResponseDto();

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testHashCodeDifferentValues() {
        ResponseDto dto1 = createTestResponseDto();
        ResponseDto dto2 = createTestResponseDto();
        dto2.setId("different-id");

        assertNotEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testPreservesClientGeneratedId() {
        // Test that Android client-generated IDs are preserved exactly
        String androidGeneratedId = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
        ResponseDto dto = new ResponseDto(
                androidGeneratedId,
                TEST_QUESTION_ID,
                TEST_MATERIAL_ID,
                TEST_STUDENT_ID,
                TEST_ANSWER,
                TEST_TIMESTAMP,
                TEST_IS_CORRECT
        );

        assertEquals(androidGeneratedId, dto.getId());
    }

    @Test
    public void testMultipleChoiceAnswer() {
        // Test response with multiple choice index answer
        ResponseDto dto = new ResponseDto(
                TEST_ID,
                TEST_QUESTION_ID,
                TEST_MATERIAL_ID,
                TEST_STUDENT_ID,
                "2",  // Index of selected option
                TEST_TIMESTAMP,
                true
        );

        assertEquals("2", dto.getAnswer());
    }

    @Test
    public void testWrittenAnswer() {
        // Test response with written answer text
        String writtenAnswer = "The answer to the question is 42.";
        ResponseDto dto = new ResponseDto(
                TEST_ID,
                TEST_QUESTION_ID,
                TEST_MATERIAL_ID,
                TEST_STUDENT_ID,
                writtenAnswer,
                TEST_TIMESTAMP,
                null  // Not evaluated yet
        );

        assertEquals(writtenAnswer, dto.getAnswer());
        assertNull(dto.getIsCorrect());
    }

    @Test
    public void testIso8601TimestampFormat() {
        // Verify ISO 8601 format is accepted
        String isoTimestamp = "2023-10-27T10:05:00Z";
        ResponseDto dto = new ResponseDto(
                TEST_ID,
                TEST_QUESTION_ID,
                TEST_MATERIAL_ID,
                TEST_STUDENT_ID,
                TEST_ANSWER,
                isoTimestamp,
                TEST_IS_CORRECT
        );

        assertEquals(isoTimestamp, dto.getTimestamp());
    }

    private ResponseDto createTestResponseDto() {
        return new ResponseDto(
                TEST_ID,
                TEST_QUESTION_ID,
                TEST_MATERIAL_ID,
                TEST_STUDENT_ID,
                TEST_ANSWER,
                TEST_TIMESTAMP,
                TEST_IS_CORRECT
        );
    }
}
