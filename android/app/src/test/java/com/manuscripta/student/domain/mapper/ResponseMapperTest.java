package com.manuscripta.student.domain.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.manuscripta.student.data.model.ResponseEntity;
import com.manuscripta.student.domain.model.Response;

import org.junit.Test;

/**
 * Unit tests for {@link ResponseMapper}.
 * Tests bidirectional mapping between ResponseEntity and Response domain model.
 */
public class ResponseMapperTest {

    @Test
    public void testToDomain() {
        // Given
        ResponseEntity entity = new ResponseEntity(
                "response-id-123",
                "question-id-456",
                "4",
                true,
                1234567890L,
                false,
                "device-id-abc"
        );

        // When
        Response domain = ResponseMapper.toDomain(entity);

        // Then
        assertNotNull(domain);
        assertEquals(entity.getId(), domain.getId());
        assertEquals(entity.getQuestionId(), domain.getQuestionId());
        assertEquals(entity.getSelectedAnswer(), domain.getSelectedAnswer());
        assertEquals(entity.isCorrect(), domain.isCorrect());
        assertEquals(entity.getTimestamp(), domain.getTimestamp());
        assertEquals(entity.isSynced(), domain.isSynced());
        assertEquals(entity.getDeviceId(), domain.getDeviceId());
    }

    @Test
    public void testToEntity() {
        // Given
        Response domain = new Response(
                "response-id-789",
                "question-id-012",
                "true",
                false,
                9876543210L,
                true,
                "device-id-xyz"
        );

        // When
        ResponseEntity entity = ResponseMapper.toEntity(domain);

        // Then
        assertNotNull(entity);
        assertEquals(domain.getId(), entity.getId());
        assertEquals(domain.getQuestionId(), entity.getQuestionId());
        assertEquals(domain.getSelectedAnswer(), entity.getSelectedAnswer());
        assertEquals(domain.isCorrect(), entity.isCorrect());
        assertEquals(domain.getTimestamp(), entity.getTimestamp());
        assertEquals(domain.isSynced(), entity.isSynced());
        assertEquals(domain.getDeviceId(), entity.getDeviceId());
    }

    @Test
    public void testToDomain_CorrectAndSynced() {
        // Given
        ResponseEntity entity = new ResponseEntity(
                "response-correct-synced",
                "question-id-999",
                "Answer A",
                true,
                5555555555L,
                true,
                "device-id-synced"
        );

        // When
        Response domain = ResponseMapper.toDomain(entity);

        // Then
        assertNotNull(domain);
        assertTrue(domain.isCorrect());
        assertTrue(domain.isSynced());
        assertEquals("Answer A", domain.getSelectedAnswer());
        assertEquals("device-id-synced", domain.getDeviceId());
    }

    @Test
    public void testToDomain_IncorrectAndNotSynced() {
        // Given
        ResponseEntity entity = new ResponseEntity(
                "response-incorrect-unsynced",
                "question-id-888",
                "Wrong answer",
                false,
                7777777777L,
                false,
                "device-id-unsynced"
        );

        // When
        Response domain = ResponseMapper.toDomain(entity);

        // Then
        assertNotNull(domain);
        assertFalse(domain.isCorrect());
        assertFalse(domain.isSynced());
        assertEquals("Wrong answer", domain.getSelectedAnswer());
        assertEquals("device-id-unsynced", domain.getDeviceId());
    }

    @Test
    public void testRoundTripConversion_EntityToDomainToEntity() {
        // Given
        ResponseEntity originalEntity = new ResponseEntity(
                "round-trip-id",
                "question-round-trip",
                "Selected option C",
                true,
                1111111111L,
                false,
                "device-round-trip"
        );

        // When
        Response domain = ResponseMapper.toDomain(originalEntity);
        ResponseEntity resultEntity = ResponseMapper.toEntity(domain);

        // Then
        assertEquals(originalEntity.getId(), resultEntity.getId());
        assertEquals(originalEntity.getQuestionId(), resultEntity.getQuestionId());
        assertEquals(originalEntity.getSelectedAnswer(), resultEntity.getSelectedAnswer());
        assertEquals(originalEntity.isCorrect(), resultEntity.isCorrect());
        assertEquals(originalEntity.getTimestamp(), resultEntity.getTimestamp());
        assertEquals(originalEntity.isSynced(), resultEntity.isSynced());
        assertEquals(originalEntity.getDeviceId(), resultEntity.getDeviceId());
    }

    @Test
    public void testRoundTripConversion_DomainToEntityToDomain() {
        // Given
        Response originalDomain = new Response(
                "round-trip-domain-id",
                "question-domain-id",
                "My short answer response",
                false,
                2222222222L,
                true,
                "device-domain-trip"
        );

        // When
        ResponseEntity entity = ResponseMapper.toEntity(originalDomain);
        Response resultDomain = ResponseMapper.toDomain(entity);

        // Then
        assertEquals(originalDomain.getId(), resultDomain.getId());
        assertEquals(originalDomain.getQuestionId(), resultDomain.getQuestionId());
        assertEquals(originalDomain.getSelectedAnswer(), resultDomain.getSelectedAnswer());
        assertEquals(originalDomain.isCorrect(), resultDomain.isCorrect());
        assertEquals(originalDomain.getTimestamp(), resultDomain.getTimestamp());
        assertEquals(originalDomain.isSynced(), resultDomain.isSynced());
        assertEquals(originalDomain.getDeviceId(), resultDomain.getDeviceId());
    }

    @Test
    public void testPrivateConstructorThrowsException() throws Exception {
        // Use reflection to access private constructor
        java.lang.reflect.Constructor<ResponseMapper> constructor = ResponseMapper.class.getDeclaredConstructor();
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
