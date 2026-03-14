package com.manuscripta.student.domain.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.manuscripta.student.data.model.ResponseEntity;
import com.manuscripta.student.domain.model.Response;
import com.manuscripta.student.network.dto.BatchResponseDto;
import com.manuscripta.student.network.dto.ResponseDto;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link ResponseMapper}.
 * Tests bidirectional mapping between ResponseEntity, Response domain model, and ResponseDto.
 */
public class ResponseMapperTest {

    private static final String TEST_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_QUESTION_ID = "q-uuid-123";
    private static final String TEST_MATERIAL_ID = "mat-uuid-456";
    private static final String TEST_DEVICE_ID = "device-uuid-789";
    private static final String TEST_ANSWER = "3";
    private static final long TEST_TIMESTAMP = 1698400200000L; // 2023-10-27T09:50:00Z

    // ==================== Entity <-> Domain Tests ====================

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
        assertEquals(entity.getAnswer(), domain.getAnswer());
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
        assertEquals(domain.getAnswer(), entity.getAnswer());
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
        assertEquals("Answer A", domain.getAnswer());
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
        assertEquals("Wrong answer", domain.getAnswer());
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
        assertEquals(originalEntity.getAnswer(), resultEntity.getAnswer());
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
        assertEquals(originalDomain.getAnswer(), resultDomain.getAnswer());
        assertEquals(originalDomain.isCorrect(), resultDomain.isCorrect());
        assertEquals(originalDomain.getTimestamp(), resultDomain.getTimestamp());
        assertEquals(originalDomain.isSynced(), resultDomain.isSynced());
        assertEquals(originalDomain.getDeviceId(), resultDomain.getDeviceId());
    }

    // ==================== Domain -> DTO Tests ====================

    @Test
    public void testToDto() {
        // Given
        Response domain = new Response(
                TEST_ID,
                TEST_QUESTION_ID,
                TEST_ANSWER,
                false,
                TEST_TIMESTAMP,
                false,
                TEST_DEVICE_ID
        );

        // When
        ResponseDto dto = ResponseMapper.toDto(domain, TEST_MATERIAL_ID);

        // Then
        assertNotNull(dto);
        assertEquals(TEST_ID, dto.getId());
        assertEquals(TEST_QUESTION_ID, dto.getQuestionId());
        assertEquals(TEST_MATERIAL_ID, dto.getMaterialId());
        assertEquals(TEST_DEVICE_ID, dto.getDeviceId());
        assertEquals(TEST_ANSWER, dto.getAnswer());
        assertNotNull(dto.getTimestamp());
        // isCorrect is false, so should be null (only set when true)
        assertNull(dto.getIsCorrect());
    }

    @Test
    public void testToDtoWithCorrectResponse() {
        // Given - response marked as correct
        Response domain = new Response(
                TEST_ID,
                TEST_QUESTION_ID,
                TEST_ANSWER,
                true,  // Correct response
                TEST_TIMESTAMP,
                false,
                TEST_DEVICE_ID
        );

        // When
        ResponseDto dto = ResponseMapper.toDto(domain, TEST_MATERIAL_ID);

        // Then
        assertEquals(Boolean.TRUE, dto.getIsCorrect());
    }

    @Test
    public void testToDtoWithNullMaterialId() {
        // Given
        Response domain = new Response(
                TEST_ID,
                TEST_QUESTION_ID,
                TEST_ANSWER,
                false,
                TEST_TIMESTAMP,
                false,
                TEST_DEVICE_ID
        );

        // When
        ResponseDto dto = ResponseMapper.toDto(domain, null);

        // Then
        assertNotNull(dto);
        assertNull(dto.getMaterialId());
    }

    @Test
    public void testToDtoPreservesClientGeneratedId() {
        // Per API Contract §4.1, Response IDs are generated by Android
        // and must be preserved for Windows to use
        String androidGeneratedId = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
        Response domain = new Response(
                androidGeneratedId,
                TEST_QUESTION_ID,
                TEST_ANSWER,
                false,
                TEST_TIMESTAMP,
                false,
                TEST_DEVICE_ID
        );

        ResponseDto dto = ResponseMapper.toDto(domain, TEST_MATERIAL_ID);

        assertEquals(androidGeneratedId, dto.getId());
    }

    @Test
    public void testToDtoList() {
        // Given
        Response response1 = new Response(
                "resp-1",
                "q-1",
                "A",
                true,
                TEST_TIMESTAMP,
                false,
                TEST_DEVICE_ID
        );
        Response response2 = new Response(
                "resp-2",
                "q-2",
                "B",
                false,
                TEST_TIMESTAMP + 1000,
                false,
                TEST_DEVICE_ID
        );
        List<Response> responses = Arrays.asList(response1, response2);

        // When
        List<ResponseDto> dtos = ResponseMapper.toDtoList(responses, TEST_MATERIAL_ID);

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("resp-1", dtos.get(0).getId());
        assertEquals("resp-2", dtos.get(1).getId());
        assertEquals(TEST_MATERIAL_ID, dtos.get(0).getMaterialId());
        assertEquals(TEST_MATERIAL_ID, dtos.get(1).getMaterialId());
    }

    @Test
    public void testToDtoListEmpty() {
        // Given
        List<Response> emptyList = Arrays.asList();

        // When
        List<ResponseDto> dtos = ResponseMapper.toDtoList(emptyList, TEST_MATERIAL_ID);

        // Then
        assertNotNull(dtos);
        assertTrue(dtos.isEmpty());
    }

    @Test
    public void testToBatchDto() {
        // Given
        Response response1 = new Response(
                "resp-1",
                "q-1",
                "A",
                true,
                TEST_TIMESTAMP,
                false,
                TEST_DEVICE_ID
        );
        Response response2 = new Response(
                "resp-2",
                "q-2",
                "B",
                false,
                TEST_TIMESTAMP + 1000,
                false,
                TEST_DEVICE_ID
        );
        List<Response> responses = Arrays.asList(response1, response2);

        // When
        BatchResponseDto batchDto = ResponseMapper.toBatchDto(responses, TEST_MATERIAL_ID);

        // Then
        assertNotNull(batchDto);
        assertNotNull(batchDto.getResponses());
        assertEquals(2, batchDto.size());
        assertFalse(batchDto.isEmpty());
    }

    @Test
    public void testToBatchDtoEmpty() {
        // Given
        List<Response> emptyList = Arrays.asList();

        // When
        BatchResponseDto batchDto = ResponseMapper.toBatchDto(emptyList, TEST_MATERIAL_ID);

        // Then
        assertNotNull(batchDto);
        assertNotNull(batchDto.getResponses());
        assertEquals(0, batchDto.size());
        assertTrue(batchDto.isEmpty());
    }

    // ==================== Timestamp Tests ====================

    @Test
    public void testFormatTimestamp() {
        // Given - known timestamp for 2023-10-27T09:50:00Z
        long timestamp = 1698400200000L;

        // When
        String formatted = ResponseMapper.formatTimestamp(timestamp);

        // Then
        assertNotNull(formatted);
        assertEquals("2023-10-27T09:50:00Z", formatted);
    }

    @Test
    public void testFormatTimestampZero() {
        // Given
        long timestamp = 0L;

        // When
        String formatted = ResponseMapper.formatTimestamp(timestamp);

        // Then
        assertNotNull(formatted);
        assertEquals("1970-01-01T00:00:00Z", formatted);
    }

    @Test
    public void testFormatTimestampRecentDate() {
        // Given - a more recent date
        // 2024-01-15T14:30:00Z = 1705329000000L
        long timestamp = 1705329000000L;

        // When
        String formatted = ResponseMapper.formatTimestamp(timestamp);

        // Then
        assertNotNull(formatted);
        assertEquals("2024-01-15T14:30:00Z", formatted);
    }

    @Test
    public void testParseTimestamp() {
        // Given
        String isoTimestamp = "2023-10-27T09:50:00Z";

        // When
        long timestamp = ResponseMapper.parseTimestamp(isoTimestamp);

        // Then
        assertEquals(1698400200000L, timestamp);
    }

    @Test
    public void testParseTimestampNull() {
        // Given
        String isoTimestamp = null;

        // When
        long timestamp = ResponseMapper.parseTimestamp(isoTimestamp);

        // Then
        assertEquals(0L, timestamp);
    }

    @Test
    public void testParseTimestampEmpty() {
        // Given
        String isoTimestamp = "";

        // When
        long timestamp = ResponseMapper.parseTimestamp(isoTimestamp);

        // Then
        assertEquals(0L, timestamp);
    }

    @Test
    public void testParseTimestampInvalid() {
        // Given - invalid format
        String isoTimestamp = "not-a-timestamp";

        // When
        long timestamp = ResponseMapper.parseTimestamp(isoTimestamp);

        // Then
        assertEquals(0L, timestamp);
    }

    @Test
    public void testTimestampRoundTrip() {
        // Given
        long originalTimestamp = 1698400200000L;

        // When
        String formatted = ResponseMapper.formatTimestamp(originalTimestamp);
        long parsed = ResponseMapper.parseTimestamp(formatted);

        // Then
        assertEquals(originalTimestamp, parsed);
    }

    @Test
    public void testToDtoTimestampFormat() {
        // Per API Contract §4.4, timestamps are transmitted as ISO 8601 strings
        Response domain = new Response(
                TEST_ID,
                TEST_QUESTION_ID,
                TEST_ANSWER,
                false,
                1698400200000L,  // 2023-10-27T09:50:00Z
                false,
                TEST_DEVICE_ID
        );

        ResponseDto dto = ResponseMapper.toDto(domain, TEST_MATERIAL_ID);

        // Verify timestamp is in ISO 8601 format
        assertEquals("2023-10-27T09:50:00Z", dto.getTimestamp());
    }

    // ==================== Batch Submission Scenario ====================

    @Test
    public void testBatchSubmissionScenario() {
        // Simulate offline mode with responses collected over time
        Response response1 = new Response(
                "offline-1",
                "q-1",
                "A",
                true,
                1698400200000L,  // 09:50
                false,
                TEST_DEVICE_ID
        );
        Response response2 = new Response(
                "offline-2",
                "q-2",
                "Written answer text here",
                false,
                1698400260000L,  // 09:51
                false,
                TEST_DEVICE_ID
        );
        Response response3 = new Response(
                "offline-3",
                "q-3",
                "42",
                true,
                1698400320000L,  // 09:52
                false,
                TEST_DEVICE_ID
        );
        List<Response> offlineResponses = Arrays.asList(response1, response2, response3);

        BatchResponseDto batch = ResponseMapper.toBatchDto(offlineResponses, TEST_MATERIAL_ID);

        assertEquals(3, batch.size());

        // Verify each response has its original ID preserved
        assertEquals("offline-1", batch.getResponses().get(0).getId());
        assertEquals("offline-2", batch.getResponses().get(1).getId());
        assertEquals("offline-3", batch.getResponses().get(2).getId());

        // Verify timestamps are in ISO 8601 format
        assertEquals("2023-10-27T09:50:00Z", batch.getResponses().get(0).getTimestamp());
        assertEquals("2023-10-27T09:51:00Z", batch.getResponses().get(1).getTimestamp());
        assertEquals("2023-10-27T09:52:00Z", batch.getResponses().get(2).getTimestamp());
    }

    // ==================== Private Constructor Test ====================

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

