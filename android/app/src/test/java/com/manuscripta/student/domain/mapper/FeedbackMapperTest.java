package com.manuscripta.student.domain.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.manuscripta.student.data.model.FeedbackEntity;
import com.manuscripta.student.domain.model.Feedback;

import org.junit.Test;

/**
 * Unit tests for {@link FeedbackMapper}.
 * Tests bidirectional mapping between FeedbackEntity and Feedback domain model.
 */
public class FeedbackMapperTest {

    @Test
    public void testToDomain_withTextAndMarks() {
        // Given
        FeedbackEntity entity = new FeedbackEntity(
                "feedback-id-123",
                "response-id-456",
                "Great work!",
                85
        );

        // When
        Feedback domain = FeedbackMapper.toDomain(entity);

        // Then
        assertNotNull(domain);
        assertEquals(entity.getId(), domain.getId());
        assertEquals(entity.getResponseId(), domain.getResponseId());
        assertEquals(entity.getText(), domain.getText());
        assertEquals(entity.getMarks(), domain.getMarks());
    }

    @Test
    public void testToDomain_withTextOnly() {
        // Given
        FeedbackEntity entity = new FeedbackEntity(
                "feedback-text-only",
                "response-id",
                "Excellent effort!",
                null
        );

        // When
        Feedback domain = FeedbackMapper.toDomain(entity);

        // Then
        assertNotNull(domain);
        assertEquals("Excellent effort!", domain.getText());
        assertNull(domain.getMarks());
        assertTrue(domain.hasText());
        assertFalse(domain.hasMarks());
    }

    @Test
    public void testToDomain_withMarksOnly() {
        // Given
        FeedbackEntity entity = new FeedbackEntity(
                "feedback-marks-only",
                "response-id",
                null,
                90
        );

        // When
        Feedback domain = FeedbackMapper.toDomain(entity);

        // Then
        assertNotNull(domain);
        assertNull(domain.getText());
        assertEquals(Integer.valueOf(90), domain.getMarks());
        assertFalse(domain.hasText());
        assertTrue(domain.hasMarks());
    }

    @Test
    public void testToEntity_withTextAndMarks() {
        // Given
        Feedback domain = new Feedback(
                "feedback-id-789",
                "response-id-012",
                "Well done!",
                75
        );

        // When
        FeedbackEntity entity = FeedbackMapper.toEntity(domain);

        // Then
        assertNotNull(entity);
        assertEquals(domain.getId(), entity.getId());
        assertEquals(domain.getResponseId(), entity.getResponseId());
        assertEquals(domain.getText(), entity.getText());
        assertEquals(domain.getMarks(), entity.getMarks());
    }

    @Test
    public void testToEntity_withTextOnly() {
        // Given
        Feedback domain = new Feedback(
                "feedback-text-only",
                "response-id",
                "Good feedback",
                null
        );

        // When
        FeedbackEntity entity = FeedbackMapper.toEntity(domain);

        // Then
        assertNotNull(entity);
        assertEquals("Good feedback", entity.getText());
        assertNull(entity.getMarks());
    }

    @Test
    public void testToEntity_withMarksOnly() {
        // Given
        Feedback domain = new Feedback(
                "feedback-marks-only",
                "response-id",
                null,
                80
        );

        // When
        FeedbackEntity entity = FeedbackMapper.toEntity(domain);

        // Then
        assertNotNull(entity);
        assertNull(entity.getText());
        assertEquals(Integer.valueOf(80), entity.getMarks());
    }

    @Test
    public void testRoundTripConversion_EntityToDomainToEntity() {
        // Given
        FeedbackEntity originalEntity = new FeedbackEntity(
                "round-trip-id",
                "response-round-trip",
                "Round trip feedback",
                55
        );

        // When
        Feedback domain = FeedbackMapper.toDomain(originalEntity);
        FeedbackEntity resultEntity = FeedbackMapper.toEntity(domain);

        // Then
        assertEquals(originalEntity.getId(), resultEntity.getId());
        assertEquals(originalEntity.getResponseId(), resultEntity.getResponseId());
        assertEquals(originalEntity.getText(), resultEntity.getText());
        assertEquals(originalEntity.getMarks(), resultEntity.getMarks());
    }

    @Test
    public void testRoundTripConversion_DomainToEntityToDomain() {
        // Given
        Feedback originalDomain = new Feedback(
                "round-trip-domain-id",
                "response-domain-id",
                "Domain feedback text",
                65
        );

        // When
        FeedbackEntity entity = FeedbackMapper.toEntity(originalDomain);
        Feedback resultDomain = FeedbackMapper.toDomain(entity);

        // Then
        assertEquals(originalDomain.getId(), resultDomain.getId());
        assertEquals(originalDomain.getResponseId(), resultDomain.getResponseId());
        assertEquals(originalDomain.getText(), resultDomain.getText());
        assertEquals(originalDomain.getMarks(), resultDomain.getMarks());
    }

    @Test
    public void testToDomain_withZeroMarks() {
        // Given
        FeedbackEntity entity = new FeedbackEntity(
                "feedback-zero",
                "response-zero",
                null,
                0
        );

        // When
        Feedback domain = FeedbackMapper.toDomain(entity);

        // Then
        assertEquals(Integer.valueOf(0), domain.getMarks());
        assertTrue(domain.hasMarks());
    }

    @Test
    public void testPrivateConstructorThrowsException() throws Exception {
        // Use reflection to access private constructor
        java.lang.reflect.Constructor<FeedbackMapper> constructor =
                FeedbackMapper.class.getDeclaredConstructor();
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
