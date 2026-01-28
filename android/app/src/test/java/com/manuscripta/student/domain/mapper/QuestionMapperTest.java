package com.manuscripta.student.domain.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.manuscripta.student.data.model.QuestionEntity;
import com.manuscripta.student.data.model.QuestionType;
import com.manuscripta.student.domain.model.Question;
import com.manuscripta.student.network.dto.QuestionDto;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link QuestionMapper}.
 * Tests bidirectional mapping between QuestionEntity, Question domain model, and QuestionDto.
 */
public class QuestionMapperTest {

    private static final String TEST_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_MATERIAL_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";

    @Test
    public void testToDomain() {
        // Given
        QuestionEntity entity = new QuestionEntity(
                "question-id-123",
                "material-id-456",
                "What is 2+2?",
                QuestionType.MULTIPLE_CHOICE,
                "[\"2\",\"3\",\"4\",\"5\"]",
                "4"
        );

        // When
        Question domain = QuestionMapper.toDomain(entity);

        // Then
        assertNotNull(domain);
        assertEquals(entity.getId(), domain.getId());
        assertEquals(entity.getMaterialId(), domain.getMaterialId());
        assertEquals(entity.getQuestionText(), domain.getQuestionText());
        assertEquals(entity.getQuestionType(), domain.getQuestionType());
        assertEquals(entity.getOptions(), domain.getOptions());
        assertEquals(entity.getCorrectAnswer(), domain.getCorrectAnswer());
    }

    @Test
    public void testToEntity() {
        // Given
        Question domain = new Question(
                "question-id-789",
                "material-id-012",
                "Is the sky blue?",
                QuestionType.TRUE_FALSE,
                "[\"true\",\"false\"]",
                "true"
        );

        // When
        QuestionEntity entity = QuestionMapper.toEntity(domain);

        // Then
        assertNotNull(entity);
        assertEquals(domain.getId(), entity.getId());
        assertEquals(domain.getMaterialId(), entity.getMaterialId());
        assertEquals(domain.getQuestionText(), entity.getQuestionText());
        assertEquals(domain.getQuestionType(), entity.getQuestionType());
        assertEquals(domain.getOptions(), entity.getOptions());
        assertEquals(domain.getCorrectAnswer(), entity.getCorrectAnswer());
    }

    @Test
    public void testToDomain_WrittenAnswerQuestion() {
        // Given
        QuestionEntity entity = new QuestionEntity(
                "written-answer-id",
                "material-id-999",
                "Explain photosynthesis",
                QuestionType.WRITTEN_ANSWER,
                "",
                "Process where plants convert sunlight to energy"
        );

        // When
        Question domain = QuestionMapper.toDomain(entity);

        // Then
        assertNotNull(domain);
        assertEquals(entity.getId(), domain.getId());
        assertEquals(entity.getMaterialId(), domain.getMaterialId());
        assertEquals(entity.getQuestionText(), domain.getQuestionText());
        assertEquals(QuestionType.WRITTEN_ANSWER, domain.getQuestionType());
        assertEquals("", domain.getOptions());
        assertEquals(entity.getCorrectAnswer(), domain.getCorrectAnswer());
    }

    @Test
    public void testRoundTripConversion_EntityToDomainToEntity() {
        // Given
        QuestionEntity originalEntity = new QuestionEntity(
                "round-trip-id",
                "material-round-trip",
                "Choose the correct answer",
                QuestionType.MULTIPLE_CHOICE,
                "[\"A\",\"B\",\"C\",\"D\"]",
                "B"
        );

        // When
        Question domain = QuestionMapper.toDomain(originalEntity);
        QuestionEntity resultEntity = QuestionMapper.toEntity(domain);

        // Then
        assertEquals(originalEntity.getId(), resultEntity.getId());
        assertEquals(originalEntity.getMaterialId(), resultEntity.getMaterialId());
        assertEquals(originalEntity.getQuestionText(), resultEntity.getQuestionText());
        assertEquals(originalEntity.getQuestionType(), resultEntity.getQuestionType());
        assertEquals(originalEntity.getOptions(), resultEntity.getOptions());
        assertEquals(originalEntity.getCorrectAnswer(), resultEntity.getCorrectAnswer());
    }

    @Test
    public void testRoundTripConversion_DomainToEntityToDomain() {
        // Given
        Question originalDomain = new Question(
                "round-trip-domain-id",
                "material-domain-id",
                "True or False question",
                QuestionType.TRUE_FALSE,
                "[\"true\",\"false\"]",
                "false"
        );

        // When
        QuestionEntity entity = QuestionMapper.toEntity(originalDomain);
        Question resultDomain = QuestionMapper.toDomain(entity);

        // Then
        assertEquals(originalDomain.getId(), resultDomain.getId());
        assertEquals(originalDomain.getMaterialId(), resultDomain.getMaterialId());
        assertEquals(originalDomain.getQuestionText(), resultDomain.getQuestionText());
        assertEquals(originalDomain.getQuestionType(), resultDomain.getQuestionType());
        assertEquals(originalDomain.getOptions(), resultDomain.getOptions());
        assertEquals(originalDomain.getCorrectAnswer(), resultDomain.getCorrectAnswer());
    }

    @Test
    public void testPrivateConstructorThrowsException() throws Exception {
        // Use reflection to access private constructor
        java.lang.reflect.Constructor<QuestionMapper> constructor =
                QuestionMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            throw new AssertionError("Expected constructor to throw AssertionError");
        } catch (java.lang.reflect.InvocationTargetException e) {
            // Verify the cause is AssertionError
            assertEquals(AssertionError.class, e.getCause().getClass());
        }
    }

    // DTO mapping tests

    @Test
    public void testFromDto_MultipleChoiceQuestion() {
        // Given
        List<String> options = Arrays.asList("2", "3", "4", "5");
        QuestionDto dto = new QuestionDto(
                TEST_ID,
                TEST_MATERIAL_ID,
                "MULTIPLE_CHOICE",
                "What is 2+2?",
                options,
                "2",
                10
        );

        // When
        Question domain = QuestionMapper.fromDto(dto);

        // Then
        assertNotNull(domain);
        assertEquals(TEST_ID, domain.getId());
        assertEquals(TEST_MATERIAL_ID, domain.getMaterialId());
        assertEquals("What is 2+2?", domain.getQuestionText());
        assertEquals(QuestionType.MULTIPLE_CHOICE, domain.getQuestionType());
        assertEquals("[\"2\",\"3\",\"4\",\"5\"]", domain.getOptions());
        assertEquals("2", domain.getCorrectAnswer());
    }

    @Test
    public void testFromDto_WrittenAnswerQuestion() {
        // Given
        QuestionDto dto = new QuestionDto(
                TEST_ID,
                TEST_MATERIAL_ID,
                "WRITTEN_ANSWER",
                "Explain your reasoning",
                null,
                "Expected answer text",
                5
        );

        // When
        Question domain = QuestionMapper.fromDto(dto);

        // Then
        assertNotNull(domain);
        assertEquals(TEST_ID, domain.getId());
        assertEquals(TEST_MATERIAL_ID, domain.getMaterialId());
        assertEquals("Explain your reasoning", domain.getQuestionText());
        assertEquals(QuestionType.WRITTEN_ANSWER, domain.getQuestionType());
        assertEquals("[]", domain.getOptions());
        assertEquals("Expected answer text", domain.getCorrectAnswer());
    }

    @Test
    public void testFromDto_NullQuestionTypeDefaultsToWrittenAnswer() {
        // Given
        QuestionDto dto = new QuestionDto(
                TEST_ID,
                TEST_MATERIAL_ID,
                null,
                "What is the answer?",
                null,
                "",
                null
        );

        // When
        Question domain = QuestionMapper.fromDto(dto);

        // Then
        assertEquals(QuestionType.WRITTEN_ANSWER, domain.getQuestionType());
    }

    @Test
    public void testFromDto_UnknownQuestionTypeDefaultsToWrittenAnswer() {
        // Given
        QuestionDto dto = new QuestionDto(
                TEST_ID,
                TEST_MATERIAL_ID,
                "UNKNOWN_TYPE",
                "What is the answer?",
                null,
                "",
                null
        );

        // When
        Question domain = QuestionMapper.fromDto(dto);

        // Then
        assertEquals(QuestionType.WRITTEN_ANSWER, domain.getQuestionType());
    }

    @Test
    public void testFromDto_CaseInsensitiveQuestionType() {
        // Given - lowercase type name
        QuestionDto dto = new QuestionDto(
                TEST_ID,
                TEST_MATERIAL_ID,
                "multiple_choice",
                "What is 2+2?",
                Arrays.asList("A", "B"),
                "A",
                null
        );

        // When
        Question domain = QuestionMapper.fromDto(dto);

        // Then
        assertEquals(QuestionType.MULTIPLE_CHOICE, domain.getQuestionType());
    }

    @Test
    public void testFromDto_NullCorrectAnswerDefaultsToEmptyString() {
        // Given
        QuestionDto dto = new QuestionDto(
                TEST_ID,
                TEST_MATERIAL_ID,
                "WRITTEN_ANSWER",
                "What is the answer?",
                null,
                null,
                null
        );

        // When
        Question domain = QuestionMapper.fromDto(dto);

        // Then
        assertEquals("", domain.getCorrectAnswer());
    }

    @Test
    public void testFromDto_EmptyOptionsListConvertsToEmptyJson() {
        // Given
        QuestionDto dto = new QuestionDto(
                TEST_ID,
                TEST_MATERIAL_ID,
                "WRITTEN_ANSWER",
                "Explain your answer",
                Collections.emptyList(),
                "",
                null
        );

        // When
        Question domain = QuestionMapper.fromDto(dto);

        // Then
        assertEquals("[]", domain.getOptions());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromDto_NullIdThrowsException() {
        // Given
        QuestionDto dto = new QuestionDto(
                null,
                TEST_MATERIAL_ID,
                "MULTIPLE_CHOICE",
                "What is 2+2?",
                Arrays.asList("2", "3", "4"),
                "2",
                null
        );

        // When/Then - should throw IllegalArgumentException
        QuestionMapper.fromDto(dto);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromDto_EmptyIdThrowsException() {
        // Given
        QuestionDto dto = new QuestionDto(
                "  ",
                TEST_MATERIAL_ID,
                "MULTIPLE_CHOICE",
                "What is 2+2?",
                Arrays.asList("2", "3", "4"),
                "2",
                null
        );

        // When/Then - should throw IllegalArgumentException
        QuestionMapper.fromDto(dto);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromDto_NullMaterialIdThrowsException() {
        // Given
        QuestionDto dto = new QuestionDto(
                TEST_ID,
                null,
                "MULTIPLE_CHOICE",
                "What is 2+2?",
                Arrays.asList("2", "3", "4"),
                "2",
                null
        );

        // When/Then - should throw IllegalArgumentException
        QuestionMapper.fromDto(dto);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromDto_EmptyMaterialIdThrowsException() {
        // Given
        QuestionDto dto = new QuestionDto(
                TEST_ID,
                "",
                "MULTIPLE_CHOICE",
                "What is 2+2?",
                Arrays.asList("2", "3", "4"),
                "2",
                null
        );

        // When/Then - should throw IllegalArgumentException
        QuestionMapper.fromDto(dto);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromDto_NullQuestionTextThrowsException() {
        // Given
        QuestionDto dto = new QuestionDto(
                TEST_ID,
                TEST_MATERIAL_ID,
                "MULTIPLE_CHOICE",
                null,
                Arrays.asList("2", "3", "4"),
                "2",
                null
        );

        // When/Then - should throw IllegalArgumentException
        QuestionMapper.fromDto(dto);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromDto_EmptyQuestionTextThrowsException() {
        // Given
        QuestionDto dto = new QuestionDto(
                TEST_ID,
                TEST_MATERIAL_ID,
                "MULTIPLE_CHOICE",
                "   ",
                Arrays.asList("2", "3", "4"),
                "2",
                null
        );

        // When/Then - should throw IllegalArgumentException
        QuestionMapper.fromDto(dto);
    }

    @Test
    public void testToDto_MultipleChoiceQuestion() {
        // Given
        Question domain = new Question(
                TEST_ID,
                TEST_MATERIAL_ID,
                "What is 2+2?",
                QuestionType.MULTIPLE_CHOICE,
                "[\"2\",\"3\",\"4\",\"5\"]",
                "2"
        );

        // When
        QuestionDto dto = QuestionMapper.toDto(domain);

        // Then
        assertNotNull(dto);
        assertEquals(TEST_ID, dto.getId());
        assertEquals(TEST_MATERIAL_ID, dto.getMaterialId());
        assertEquals("MULTIPLE_CHOICE", dto.getQuestionType());
        assertEquals("What is 2+2?", dto.getQuestionText());
        assertEquals(Arrays.asList("2", "3", "4", "5"), dto.getOptions());
        assertEquals("2", dto.getCorrectAnswer());
        assertNull(dto.getMaxScore()); // maxScore not stored in domain
    }

    @Test
    public void testToDto_WrittenAnswerQuestion() {
        // Given
        Question domain = new Question(
                TEST_ID,
                TEST_MATERIAL_ID,
                "Explain photosynthesis",
                QuestionType.WRITTEN_ANSWER,
                "",
                "Process where plants convert sunlight to energy"
        );

        // When
        QuestionDto dto = QuestionMapper.toDto(domain);

        // Then
        assertNotNull(dto);
        assertEquals("WRITTEN_ANSWER", dto.getQuestionType());
        assertTrue(dto.getOptions().isEmpty());
    }

    @Test
    public void testToDto_EmptyOptionsJsonConvertsToEmptyList() {
        // Given
        Question domain = new Question(
                TEST_ID,
                TEST_MATERIAL_ID,
                "Explain your answer",
                QuestionType.WRITTEN_ANSWER,
                "[]",
                ""
        );

        // When
        QuestionDto dto = QuestionMapper.toDto(domain);

        // Then
        assertNotNull(dto.getOptions());
        assertTrue(dto.getOptions().isEmpty());
    }

    @Test
    public void testToDto_InvalidOptionsJsonReturnsEmptyList() {
        // Given
        Question domain = new Question(
                TEST_ID,
                TEST_MATERIAL_ID,
                "Question with invalid options",
                QuestionType.MULTIPLE_CHOICE,
                "invalid json",
                "0"
        );

        // When
        QuestionDto dto = QuestionMapper.toDto(domain);

        // Then
        assertNotNull(dto.getOptions());
        assertTrue(dto.getOptions().isEmpty());
    }

    @Test
    public void testDtoToEntity() {
        // Given
        List<String> options = Arrays.asList("A", "B", "C", "D");
        QuestionDto dto = new QuestionDto(
                TEST_ID,
                TEST_MATERIAL_ID,
                "MULTIPLE_CHOICE",
                "Select the correct answer",
                options,
                "B",
                5
        );

        // When
        QuestionEntity entity = QuestionMapper.dtoToEntity(dto);

        // Then
        assertNotNull(entity);
        assertEquals(TEST_ID, entity.getId());
        assertEquals(TEST_MATERIAL_ID, entity.getMaterialId());
        assertEquals("Select the correct answer", entity.getQuestionText());
        assertEquals(QuestionType.MULTIPLE_CHOICE, entity.getQuestionType());
        assertEquals("[\"A\",\"B\",\"C\",\"D\"]", entity.getOptions());
        assertEquals("B", entity.getCorrectAnswer());
    }

    @Test
    public void testEntityToDto() {
        // Given
        QuestionEntity entity = new QuestionEntity(
                TEST_ID,
                TEST_MATERIAL_ID,
                "True or false: The sky is blue",
                QuestionType.TRUE_FALSE,
                "[\"True\",\"False\"]",
                "True"
        );

        // When
        QuestionDto dto = QuestionMapper.entityToDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(TEST_ID, dto.getId());
        assertEquals(TEST_MATERIAL_ID, dto.getMaterialId());
        assertEquals("TRUE_FALSE", dto.getQuestionType());
        assertEquals("True or false: The sky is blue", dto.getQuestionText());
        assertEquals(Arrays.asList("True", "False"), dto.getOptions());
        assertEquals("True", dto.getCorrectAnswer());
    }

    @Test
    public void testRoundTripConversion_DtoToDomainToDto() {
        // Given
        List<String> options = Arrays.asList("Option A", "Option B", "Option C");
        QuestionDto originalDto = new QuestionDto(
                TEST_ID,
                TEST_MATERIAL_ID,
                "MULTIPLE_CHOICE",
                "Choose one option",
                options,
                "1",
                null
        );

        // When
        Question domain = QuestionMapper.fromDto(originalDto);
        QuestionDto resultDto = QuestionMapper.toDto(domain);

        // Then
        assertEquals(originalDto.getId(), resultDto.getId());
        assertEquals(originalDto.getMaterialId(), resultDto.getMaterialId());
        assertEquals(originalDto.getQuestionType(), resultDto.getQuestionType());
        assertEquals(originalDto.getQuestionText(), resultDto.getQuestionText());
        assertEquals(originalDto.getOptions(), resultDto.getOptions());
        assertEquals(originalDto.getCorrectAnswer(), resultDto.getCorrectAnswer());
    }

    @Test
    public void testRoundTripConversion_DtoToEntityToDto() {
        // Given
        List<String> options = Arrays.asList("Yes", "No");
        QuestionDto originalDto = new QuestionDto(
                TEST_ID,
                TEST_MATERIAL_ID,
                "MULTIPLE_CHOICE",
                "Do you agree?",
                options,
                "0",
                5
        );

        // When
        QuestionEntity entity = QuestionMapper.dtoToEntity(originalDto);
        QuestionDto resultDto = QuestionMapper.entityToDto(entity);

        // Then
        assertEquals(originalDto.getId(), resultDto.getId());
        assertEquals(originalDto.getMaterialId(), resultDto.getMaterialId());
        assertEquals(originalDto.getQuestionType(), resultDto.getQuestionType());
        assertEquals(originalDto.getQuestionText(), resultDto.getQuestionText());
        assertEquals(originalDto.getOptions(), resultDto.getOptions());
        assertEquals(originalDto.getCorrectAnswer(), resultDto.getCorrectAnswer());
    }

    @Test
    public void testFromDto_PreservesWindowsAssignedId() {
        // Test that Windows teacher app IDs are preserved exactly
        String windowsGeneratedId = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
        QuestionDto dto = new QuestionDto(
                windowsGeneratedId,
                TEST_MATERIAL_ID,
                "WRITTEN_ANSWER",
                "Describe your answer",
                null,
                "",
                null
        );

        // When
        Question domain = QuestionMapper.fromDto(dto);

        // Then
        assertEquals(windowsGeneratedId, domain.getId());
    }
}
