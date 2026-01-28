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
 * Unit tests for {@link QuestionListDto}.
 * Tests construction, getters, setters, equals, hashCode, and toString methods.
 */
public class QuestionListDtoTest {

    private static final String TEST_QUESTION_ID_1 = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_QUESTION_ID_2 = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
    private static final String TEST_MATERIAL_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
    private static final Integer TEST_TOTAL_COUNT = 10;

    @Test
    public void testDefaultConstructor() {
        QuestionListDto dto = new QuestionListDto();

        assertNull(dto.getQuestionIds());
        assertNull(dto.getQuestions());
        assertNull(dto.getTotalCount());
    }

    @Test
    public void testConstructorWithAllFields() {
        List<String> questionIds = createTestQuestionIds();
        List<QuestionDto> questions = createTestQuestions();

        QuestionListDto dto = new QuestionListDto(questionIds, questions, TEST_TOTAL_COUNT);

        assertEquals(questionIds, dto.getQuestionIds());
        assertEquals(questions, dto.getQuestions());
        assertEquals(TEST_TOTAL_COUNT, dto.getTotalCount());
    }

    @Test
    public void testConstructorWithNullValues() {
        QuestionListDto dto = new QuestionListDto(null, null, null);

        assertNull(dto.getQuestionIds());
        assertNull(dto.getQuestions());
        assertNull(dto.getTotalCount());
    }

    @Test
    public void testSetQuestionIds() {
        QuestionListDto dto = new QuestionListDto();
        List<String> questionIds = createTestQuestionIds();

        dto.setQuestionIds(questionIds);
        assertEquals(questionIds, dto.getQuestionIds());

        dto.setQuestionIds(null);
        assertNull(dto.getQuestionIds());
    }

    @Test
    public void testSetQuestions() {
        QuestionListDto dto = new QuestionListDto();
        List<QuestionDto> questions = createTestQuestions();

        dto.setQuestions(questions);
        assertEquals(questions, dto.getQuestions());

        dto.setQuestions(null);
        assertNull(dto.getQuestions());
    }

    @Test
    public void testSetTotalCount() {
        QuestionListDto dto = new QuestionListDto();

        dto.setTotalCount(TEST_TOTAL_COUNT);
        assertEquals(TEST_TOTAL_COUNT, dto.getTotalCount());

        dto.setTotalCount(null);
        assertNull(dto.getTotalCount());
    }

    @Test
    public void testToString() {
        QuestionListDto dto = new QuestionListDto(
                createTestQuestionIds(),
                createTestQuestions(),
                TEST_TOTAL_COUNT
        );

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("QuestionListDto"));
        assertTrue(result.contains(TEST_QUESTION_ID_1));
        assertTrue(result.contains(TEST_TOTAL_COUNT.toString()));
    }

    @Test
    public void testToStringWithNullValues() {
        QuestionListDto dto = new QuestionListDto();

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("QuestionListDto"));
        assertTrue(result.contains("null"));
    }

    @Test
    public void testEqualsSameObject() {
        QuestionListDto dto = createTestQuestionListDto();

        assertTrue(dto.equals(dto));
    }

    @Test
    public void testEqualsNull() {
        QuestionListDto dto = createTestQuestionListDto();

        assertFalse(dto.equals(null));
    }

    @Test
    public void testEqualsDifferentClass() {
        QuestionListDto dto = createTestQuestionListDto();

        assertFalse(dto.equals("not a dto"));
    }

    @Test
    public void testEqualsSameValues() {
        QuestionListDto dto1 = createTestQuestionListDto();
        QuestionListDto dto2 = createTestQuestionListDto();

        assertTrue(dto1.equals(dto2));
        assertTrue(dto2.equals(dto1));
    }

    @Test
    public void testEqualsDifferentQuestionIds() {
        QuestionListDto dto1 = createTestQuestionListDto();
        QuestionListDto dto2 = createTestQuestionListDto();
        dto2.setQuestionIds(Arrays.asList("different-id"));

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentQuestions() {
        QuestionListDto dto1 = createTestQuestionListDto();
        QuestionListDto dto2 = createTestQuestionListDto();
        List<QuestionDto> differentQuestions = Arrays.asList(
                new QuestionDto("diff-id", TEST_MATERIAL_ID, "WRITTEN_ANSWER",
                        "Different question?", null, "", null)
        );
        dto2.setQuestions(differentQuestions);

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentTotalCount() {
        QuestionListDto dto1 = createTestQuestionListDto();
        QuestionListDto dto2 = createTestQuestionListDto();
        dto2.setTotalCount(999);

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullQuestionIds() {
        QuestionListDto dto1 = new QuestionListDto(null, createTestQuestions(), TEST_TOTAL_COUNT);
        QuestionListDto dto2 = new QuestionListDto(null, createTestQuestions(), TEST_TOTAL_COUNT);

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullVsNonNullQuestionIds() {
        QuestionListDto dto1 = new QuestionListDto(null, createTestQuestions(), TEST_TOTAL_COUNT);
        QuestionListDto dto2 = createTestQuestionListDto();

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsAllNullValues() {
        QuestionListDto dto1 = new QuestionListDto();
        QuestionListDto dto2 = new QuestionListDto();

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testHashCodeConsistency() {
        QuestionListDto dto = createTestQuestionListDto();

        int hashCode1 = dto.hashCode();
        int hashCode2 = dto.hashCode();

        assertEquals(hashCode1, hashCode2);
    }

    @Test
    public void testHashCodeEquality() {
        QuestionListDto dto1 = createTestQuestionListDto();
        QuestionListDto dto2 = createTestQuestionListDto();

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testHashCodeWithNullValues() {
        QuestionListDto dto1 = new QuestionListDto();
        QuestionListDto dto2 = new QuestionListDto();

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testEmptyQuestionIdsList() {
        QuestionListDto dto = new QuestionListDto(
                new ArrayList<>(),
                null,
                0
        );

        assertNotNull(dto.getQuestionIds());
        assertTrue(dto.getQuestionIds().isEmpty());
    }

    @Test
    public void testEmptyQuestionsList() {
        QuestionListDto dto = new QuestionListDto(
                createTestQuestionIds(),
                new ArrayList<>(),
                TEST_TOTAL_COUNT
        );

        assertNotNull(dto.getQuestions());
        assertTrue(dto.getQuestions().isEmpty());
    }

    @Test
    public void testQuestionIdsOnlyNoFullObjects() {
        List<String> questionIds = createTestQuestionIds();

        QuestionListDto dto = new QuestionListDto(questionIds, null, 2);

        assertEquals(2, dto.getQuestionIds().size());
        assertNull(dto.getQuestions());
        assertEquals(Integer.valueOf(2), dto.getTotalCount());
    }

    @Test
    public void testQuestionsOnlyNoIds() {
        List<QuestionDto> questions = createTestQuestions();

        QuestionListDto dto = new QuestionListDto(null, questions, questions.size());

        assertNull(dto.getQuestionIds());
        assertEquals(2, dto.getQuestions().size());
        assertEquals(Integer.valueOf(2), dto.getTotalCount());
    }

    @Test
    public void testOrderPreservation() {
        List<String> questionIds = Arrays.asList("id-3", "id-1", "id-2");

        QuestionListDto dto = new QuestionListDto(questionIds, null, 3);

        assertEquals("id-3", dto.getQuestionIds().get(0));
        assertEquals("id-1", dto.getQuestionIds().get(1));
        assertEquals("id-2", dto.getQuestionIds().get(2));
    }

    private QuestionListDto createTestQuestionListDto() {
        return new QuestionListDto(
                createTestQuestionIds(),
                createTestQuestions(),
                TEST_TOTAL_COUNT
        );
    }

    private List<String> createTestQuestionIds() {
        return Arrays.asList(TEST_QUESTION_ID_1, TEST_QUESTION_ID_2);
    }

    private List<QuestionDto> createTestQuestions() {
        QuestionDto question1 = new QuestionDto(
                TEST_QUESTION_ID_1,
                TEST_MATERIAL_ID,
                "MULTIPLE_CHOICE",
                "What is 2+2?",
                Arrays.asList("2", "3", "4", "5"),
                "2",
                5
        );

        QuestionDto question2 = new QuestionDto(
                TEST_QUESTION_ID_2,
                TEST_MATERIAL_ID,
                "WRITTEN_ANSWER",
                "Explain your reasoning",
                null,
                "",
                10
        );

        return Arrays.asList(question1, question2);
    }
}
