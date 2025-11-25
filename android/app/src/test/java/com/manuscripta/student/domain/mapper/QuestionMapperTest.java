package com.manuscripta.student.domain.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.manuscripta.student.data.model.QuestionEntity;
import com.manuscripta.student.data.model.QuestionType;
import com.manuscripta.student.domain.model.Question;

import org.junit.Test;

/**
 * Unit tests for {@link QuestionMapper}.
 * Tests bidirectional mapping between QuestionEntity and Question domain model.
 */
public class QuestionMapperTest {

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
        java.lang.reflect.Constructor<QuestionMapper> constructor = QuestionMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            throw new AssertionError("Expected constructor to throw AssertionError");
        } catch (java.lang.reflect.InvocationTargetException e) {
            // Verify the cause is AssertionError
            assertEquals(AssertionError.class, e.getCause().getClass());
        }
    }
}
