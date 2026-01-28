package com.manuscripta.student.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link QuestionDto}.
 * Tests construction, getters, setters, equals, hashCode, and toString methods.
 */
public class QuestionDtoTest {

    private static final String TEST_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_MATERIAL_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
    private static final String TEST_QUESTION_TYPE = "MULTIPLE_CHOICE";
    private static final String TEST_QUESTION_TEXT = "What is 2+2?";
    private static final String TEST_CORRECT_ANSWER = "2";
    private static final Integer TEST_MAX_SCORE = 10;

    @Test
    public void testDefaultConstructor() {
        QuestionDto dto = new QuestionDto();

        assertNull(dto.getId());
        assertNull(dto.getMaterialId());
        assertNull(dto.getQuestionType());
        assertNull(dto.getQuestionText());
        assertNull(dto.getOptions());
        assertNull(dto.getCorrectAnswer());
        assertNull(dto.getMaxScore());
    }

    @Test
    public void testConstructorWithAllFields() {
        List<String> options = createTestOptions();

        QuestionDto dto = new QuestionDto(
                TEST_ID,
                TEST_MATERIAL_ID,
                TEST_QUESTION_TYPE,
                TEST_QUESTION_TEXT,
                options,
                TEST_CORRECT_ANSWER,
                TEST_MAX_SCORE
        );

        assertEquals(TEST_ID, dto.getId());
        assertEquals(TEST_MATERIAL_ID, dto.getMaterialId());
        assertEquals(TEST_QUESTION_TYPE, dto.getQuestionType());
        assertEquals(TEST_QUESTION_TEXT, dto.getQuestionText());
        assertEquals(options, dto.getOptions());
        assertEquals(TEST_CORRECT_ANSWER, dto.getCorrectAnswer());
        assertEquals(TEST_MAX_SCORE, dto.getMaxScore());
    }

    @Test
    public void testConstructorWithNullValues() {
        QuestionDto dto = new QuestionDto(null, null, null, null, null, null, null);

        assertNull(dto.getId());
        assertNull(dto.getMaterialId());
        assertNull(dto.getQuestionType());
        assertNull(dto.getQuestionText());
        assertNull(dto.getOptions());
        assertNull(dto.getCorrectAnswer());
        assertNull(dto.getMaxScore());
    }

    @Test
    public void testSetId() {
        QuestionDto dto = new QuestionDto();

        dto.setId(TEST_ID);
        assertEquals(TEST_ID, dto.getId());

        dto.setId(null);
        assertNull(dto.getId());
    }

    @Test
    public void testSetMaterialId() {
        QuestionDto dto = new QuestionDto();

        dto.setMaterialId(TEST_MATERIAL_ID);
        assertEquals(TEST_MATERIAL_ID, dto.getMaterialId());

        dto.setMaterialId(null);
        assertNull(dto.getMaterialId());
    }

    @Test
    public void testSetQuestionType() {
        QuestionDto dto = new QuestionDto();

        dto.setQuestionType(TEST_QUESTION_TYPE);
        assertEquals(TEST_QUESTION_TYPE, dto.getQuestionType());

        dto.setQuestionType(null);
        assertNull(dto.getQuestionType());
    }

    @Test
    public void testSetQuestionText() {
        QuestionDto dto = new QuestionDto();

        dto.setQuestionText(TEST_QUESTION_TEXT);
        assertEquals(TEST_QUESTION_TEXT, dto.getQuestionText());

        dto.setQuestionText(null);
        assertNull(dto.getQuestionText());
    }

    @Test
    public void testSetOptions() {
        QuestionDto dto = new QuestionDto();
        List<String> options = createTestOptions();

        dto.setOptions(options);
        assertEquals(options, dto.getOptions());

        dto.setOptions(null);
        assertNull(dto.getOptions());
    }

    @Test
    public void testSetCorrectAnswer() {
        QuestionDto dto = new QuestionDto();

        dto.setCorrectAnswer(TEST_CORRECT_ANSWER);
        assertEquals(TEST_CORRECT_ANSWER, dto.getCorrectAnswer());

        dto.setCorrectAnswer(null);
        assertNull(dto.getCorrectAnswer());
    }

    @Test
    public void testSetMaxScore() {
        QuestionDto dto = new QuestionDto();

        dto.setMaxScore(TEST_MAX_SCORE);
        assertEquals(TEST_MAX_SCORE, dto.getMaxScore());

        dto.setMaxScore(null);
        assertNull(dto.getMaxScore());
    }

    @Test
    public void testToString() {
        QuestionDto dto = new QuestionDto(
                TEST_ID,
                TEST_MATERIAL_ID,
                TEST_QUESTION_TYPE,
                TEST_QUESTION_TEXT,
                createTestOptions(),
                TEST_CORRECT_ANSWER,
                TEST_MAX_SCORE
        );

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("QuestionDto"));
        assertTrue(result.contains(TEST_ID));
        assertTrue(result.contains(TEST_MATERIAL_ID));
        assertTrue(result.contains(TEST_QUESTION_TYPE));
        // Verify correctAnswer is redacted (not exposed as correctAnswer='<actual value>')
        assertFalse(result.contains("correctAnswer='" + TEST_CORRECT_ANSWER + "'"));
        assertTrue(result.contains("correctAnswer='[REDACTED]'"));
    }

    @Test
    public void testToStringWithNullValues() {
        QuestionDto dto = new QuestionDto();

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("QuestionDto"));
        assertTrue(result.contains("null"));
    }

    @Test
    public void testEqualsSameObject() {
        QuestionDto dto = createTestQuestionDto();

        assertTrue(dto.equals(dto));
    }

    @Test
    public void testEqualsNull() {
        QuestionDto dto = createTestQuestionDto();

        assertFalse(dto.equals(null));
    }

    @Test
    public void testEqualsDifferentClass() {
        QuestionDto dto = createTestQuestionDto();

        assertFalse(dto.equals("not a dto"));
    }

    @Test
    public void testEqualsSameValues() {
        QuestionDto dto1 = createTestQuestionDto();
        QuestionDto dto2 = createTestQuestionDto();

        assertTrue(dto1.equals(dto2));
        assertTrue(dto2.equals(dto1));
    }

    @Test
    public void testEqualsDifferentId() {
        QuestionDto dto1 = createTestQuestionDto();
        QuestionDto dto2 = createTestQuestionDto();
        dto2.setId("different-id");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentMaterialId() {
        QuestionDto dto1 = createTestQuestionDto();
        QuestionDto dto2 = createTestQuestionDto();
        dto2.setMaterialId("different-material-id");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentQuestionType() {
        QuestionDto dto1 = createTestQuestionDto();
        QuestionDto dto2 = createTestQuestionDto();
        dto2.setQuestionType("WRITTEN_ANSWER");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentQuestionText() {
        QuestionDto dto1 = createTestQuestionDto();
        QuestionDto dto2 = createTestQuestionDto();
        dto2.setQuestionText("Different question text?");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentOptions() {
        QuestionDto dto1 = createTestQuestionDto();
        QuestionDto dto2 = createTestQuestionDto();
        dto2.setOptions(Arrays.asList("Different", "Options"));

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentCorrectAnswer() {
        QuestionDto dto1 = createTestQuestionDto();
        QuestionDto dto2 = createTestQuestionDto();
        dto2.setCorrectAnswer("different-answer");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentMaxScore() {
        QuestionDto dto1 = createTestQuestionDto();
        QuestionDto dto2 = createTestQuestionDto();
        dto2.setMaxScore(99);

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullId() {
        QuestionDto dto1 = new QuestionDto(null, TEST_MATERIAL_ID, TEST_QUESTION_TYPE,
                TEST_QUESTION_TEXT, null, TEST_CORRECT_ANSWER, TEST_MAX_SCORE);
        QuestionDto dto2 = new QuestionDto(null, TEST_MATERIAL_ID, TEST_QUESTION_TYPE,
                TEST_QUESTION_TEXT, null, TEST_CORRECT_ANSWER, TEST_MAX_SCORE);

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullVsNonNullId() {
        QuestionDto dto1 = new QuestionDto(null, TEST_MATERIAL_ID, TEST_QUESTION_TYPE,
                TEST_QUESTION_TEXT, null, TEST_CORRECT_ANSWER, TEST_MAX_SCORE);
        QuestionDto dto2 = createTestQuestionDto();

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsAllNullValues() {
        QuestionDto dto1 = new QuestionDto();
        QuestionDto dto2 = new QuestionDto();

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testHashCodeConsistency() {
        QuestionDto dto = createTestQuestionDto();

        int hashCode1 = dto.hashCode();
        int hashCode2 = dto.hashCode();

        assertEquals(hashCode1, hashCode2);
    }

    @Test
    public void testHashCodeEquality() {
        QuestionDto dto1 = createTestQuestionDto();
        QuestionDto dto2 = createTestQuestionDto();

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testHashCodeWithNullValues() {
        QuestionDto dto1 = new QuestionDto();
        QuestionDto dto2 = new QuestionDto();

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testEmptyOptionsList() {
        QuestionDto dto = new QuestionDto(
                TEST_ID,
                TEST_MATERIAL_ID,
                "WRITTEN_ANSWER",
                "Describe your answer",
                new ArrayList<>(),
                "",
                TEST_MAX_SCORE
        );

        assertNotNull(dto.getOptions());
        assertTrue(dto.getOptions().isEmpty());
    }

    @Test
    public void testPreservesOriginalId() {
        // Test that Windows teacher app IDs are preserved exactly
        String windowsGeneratedId = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
        QuestionDto dto = new QuestionDto(
                windowsGeneratedId,
                TEST_MATERIAL_ID,
                TEST_QUESTION_TYPE,
                TEST_QUESTION_TEXT,
                createTestOptions(),
                TEST_CORRECT_ANSWER,
                TEST_MAX_SCORE
        );

        assertEquals(windowsGeneratedId, dto.getId());
    }

    @Test
    public void testWrittenAnswerQuestionType() {
        QuestionDto dto = new QuestionDto(
                TEST_ID,
                TEST_MATERIAL_ID,
                "WRITTEN_ANSWER",
                "Explain photosynthesis in your own words.",
                null,
                "Process where plants convert sunlight to energy",
                5
        );

        assertEquals("WRITTEN_ANSWER", dto.getQuestionType());
        assertNull(dto.getOptions());
    }

    @Test
    public void testMultipleChoiceWithManyOptions() {
        List<String> manyOptions = Arrays.asList("A", "B", "C", "D", "E", "F");
        QuestionDto dto = new QuestionDto(
                TEST_ID,
                TEST_MATERIAL_ID,
                "MULTIPLE_CHOICE",
                "Select all that apply",
                manyOptions,
                "0",
                1
        );

        assertEquals(6, dto.getOptions().size());
        assertEquals("A", dto.getOptions().get(0));
        assertEquals("F", dto.getOptions().get(5));
    }

    private QuestionDto createTestQuestionDto() {
        return new QuestionDto(
                TEST_ID,
                TEST_MATERIAL_ID,
                TEST_QUESTION_TYPE,
                TEST_QUESTION_TEXT,
                createTestOptions(),
                TEST_CORRECT_ANSWER,
                TEST_MAX_SCORE
        );
    }

    private List<String> createTestOptions() {
        return Arrays.asList("2", "3", "4", "5");
    }
}
