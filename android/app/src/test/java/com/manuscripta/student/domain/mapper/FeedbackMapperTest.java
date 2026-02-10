package com.manuscripta.student.domain.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.manuscripta.student.data.model.FeedbackEntity;
import com.manuscripta.student.domain.model.Feedback;
import com.manuscripta.student.network.dto.FeedbackDto;

import org.junit.Test;

/**
 * Unit tests for {@link FeedbackMapper}.
 * Tests bidirectional mapping between FeedbackEntity, Feedback domain model, and FeedbackDto.
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

    // ========== DTO Conversion Tests ==========

    @Test
    public void testFromDto_withTextAndMarks() {
        // Given
        FeedbackDto dto = new FeedbackDto(
                "dto-feedback-id",
                "dto-response-id",
                "Excellent work!",
                95
        );

        // When
        Feedback domain = FeedbackMapper.fromDto(dto);

        // Then
        assertNotNull(domain);
        assertEquals(dto.getId(), domain.getId());
        assertEquals(dto.getResponseId(), domain.getResponseId());
        assertEquals(dto.getText(), domain.getText());
        assertEquals(dto.getMarks(), domain.getMarks());
    }

    @Test
    public void testFromDto_withTextOnly() {
        // Given
        FeedbackDto dto = new FeedbackDto(
                "dto-text-only",
                "response-text",
                "Good progress!",
                null
        );

        // When
        Feedback domain = FeedbackMapper.fromDto(dto);

        // Then
        assertNotNull(domain);
        assertEquals("Good progress!", domain.getText());
        assertNull(domain.getMarks());
        assertTrue(domain.hasText());
        assertFalse(domain.hasMarks());
    }

    @Test
    public void testFromDto_withMarksOnly() {
        // Given
        FeedbackDto dto = new FeedbackDto(
                "dto-marks-only",
                "response-marks",
                null,
                88
        );

        // When
        Feedback domain = FeedbackMapper.fromDto(dto);

        // Then
        assertNotNull(domain);
        assertNull(domain.getText());
        assertEquals(Integer.valueOf(88), domain.getMarks());
        assertFalse(domain.hasText());
        assertTrue(domain.hasMarks());
    }

    @Test
    public void testFromDto_withZeroMarks() {
        // Given
        FeedbackDto dto = new FeedbackDto(
                "dto-zero-marks",
                "response-zero",
                null,
                0
        );

        // When
        Feedback domain = FeedbackMapper.fromDto(dto);

        // Then
        assertEquals(Integer.valueOf(0), domain.getMarks());
        assertTrue(domain.hasMarks());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromDto_withNullId_throwsException() {
        // Given
        FeedbackDto dto = new FeedbackDto(null, "response-id", "text", 50);

        // When/Then - expect exception
        FeedbackMapper.fromDto(dto);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromDto_withEmptyId_throwsException() {
        // Given
        FeedbackDto dto = new FeedbackDto("", "response-id", "text", 50);

        // When/Then - expect exception
        FeedbackMapper.fromDto(dto);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromDto_withWhitespaceId_throwsException() {
        // Given
        FeedbackDto dto = new FeedbackDto("   ", "response-id", "text", 50);

        // When/Then - expect exception
        FeedbackMapper.fromDto(dto);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromDto_withNullResponseId_throwsException() {
        // Given
        FeedbackDto dto = new FeedbackDto("feedback-id", null, "text", 50);

        // When/Then - expect exception
        FeedbackMapper.fromDto(dto);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromDto_withEmptyResponseId_throwsException() {
        // Given
        FeedbackDto dto = new FeedbackDto("feedback-id", "", "text", 50);

        // When/Then - expect exception
        FeedbackMapper.fromDto(dto);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromDto_withWhitespaceResponseId_throwsException() {
        // Given
        FeedbackDto dto = new FeedbackDto("feedback-id", "  \t ", "text", 50);

        // When/Then - expect exception
        FeedbackMapper.fromDto(dto);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromDto_withNullTextAndMarks_throwsException() {
        // Per Validation Rules.md §2F(1)(b), at least one must be present
        // Given
        FeedbackDto dto = new FeedbackDto("feedback-id", "response-id", null, null);

        // When/Then - expect exception from Feedback constructor
        FeedbackMapper.fromDto(dto);
    }

    @Test
    public void testToDto_withTextAndMarks() {
        // Given
        Feedback domain = new Feedback(
                "domain-feedback-id",
                "domain-response-id",
                "Well done!",
                78
        );

        // When
        FeedbackDto dto = FeedbackMapper.toDto(domain);

        // Then
        assertNotNull(dto);
        assertEquals(domain.getId(), dto.getId());
        assertEquals(domain.getResponseId(), dto.getResponseId());
        assertEquals(domain.getText(), dto.getText());
        assertEquals(domain.getMarks(), dto.getMarks());
    }

    @Test
    public void testToDto_withTextOnly() {
        // Given
        Feedback domain = new Feedback(
                "domain-text-only",
                "response-text",
                "Keep up the good work!",
                null
        );

        // When
        FeedbackDto dto = FeedbackMapper.toDto(domain);

        // Then
        assertNotNull(dto);
        assertEquals("Keep up the good work!", dto.getText());
        assertNull(dto.getMarks());
    }

    @Test
    public void testToDto_withMarksOnly() {
        // Given
        Feedback domain = new Feedback(
                "domain-marks-only",
                "response-marks",
                null,
                60
        );

        // When
        FeedbackDto dto = FeedbackMapper.toDto(domain);

        // Then
        assertNotNull(dto);
        assertNull(dto.getText());
        assertEquals(Integer.valueOf(60), dto.getMarks());
    }

    @Test
    public void testDtoToEntity_withTextAndMarks() {
        // Given
        FeedbackDto dto = new FeedbackDto(
                "dto-to-entity-id",
                "dto-response-id",
                "Great improvement!",
                82
        );

        // When
        FeedbackEntity entity = FeedbackMapper.dtoToEntity(dto);

        // Then
        assertNotNull(entity);
        assertEquals(dto.getId(), entity.getId());
        assertEquals(dto.getResponseId(), entity.getResponseId());
        assertEquals(dto.getText(), entity.getText());
        assertEquals(dto.getMarks(), entity.getMarks());
    }

    @Test
    public void testDtoToEntity_withTextOnly() {
        // Given
        FeedbackDto dto = new FeedbackDto(
                "dto-entity-text",
                "response-id",
                "Needs more detail",
                null
        );

        // When
        FeedbackEntity entity = FeedbackMapper.dtoToEntity(dto);

        // Then
        assertEquals("Needs more detail", entity.getText());
        assertNull(entity.getMarks());
    }

    @Test
    public void testDtoToEntity_withMarksOnly() {
        // Given
        FeedbackDto dto = new FeedbackDto(
                "dto-entity-marks",
                "response-id",
                null,
                100
        );

        // When
        FeedbackEntity entity = FeedbackMapper.dtoToEntity(dto);

        // Then
        assertNull(entity.getText());
        assertEquals(Integer.valueOf(100), entity.getMarks());
    }

    @Test
    public void testEntityToDto_withTextAndMarks() {
        // Given
        FeedbackEntity entity = new FeedbackEntity(
                "entity-to-dto-id",
                "entity-response-id",
                "Outstanding!",
                99
        );

        // When
        FeedbackDto dto = FeedbackMapper.entityToDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(entity.getId(), dto.getId());
        assertEquals(entity.getResponseId(), dto.getResponseId());
        assertEquals(entity.getText(), dto.getText());
        assertEquals(entity.getMarks(), dto.getMarks());
    }

    @Test
    public void testEntityToDto_withTextOnly() {
        // Given
        FeedbackEntity entity = new FeedbackEntity(
                "entity-dto-text",
                "response-id",
                "Please review chapter 5",
                null
        );

        // When
        FeedbackDto dto = FeedbackMapper.entityToDto(entity);

        // Then
        assertEquals("Please review chapter 5", dto.getText());
        assertNull(dto.getMarks());
    }

    @Test
    public void testEntityToDto_withMarksOnly() {
        // Given
        FeedbackEntity entity = new FeedbackEntity(
                "entity-dto-marks",
                "response-id",
                null,
                42
        );

        // When
        FeedbackDto dto = FeedbackMapper.entityToDto(entity);

        // Then
        assertNull(dto.getText());
        assertEquals(Integer.valueOf(42), dto.getMarks());
    }

    @Test
    public void testRoundTripConversion_DtoToDomainToDto() {
        // Given
        FeedbackDto originalDto = new FeedbackDto(
                "round-trip-dto-id",
                "round-trip-response-id",
                "Round trip from DTO",
                77
        );

        // When
        Feedback domain = FeedbackMapper.fromDto(originalDto);
        FeedbackDto resultDto = FeedbackMapper.toDto(domain);

        // Then
        assertEquals(originalDto.getId(), resultDto.getId());
        assertEquals(originalDto.getResponseId(), resultDto.getResponseId());
        assertEquals(originalDto.getText(), resultDto.getText());
        assertEquals(originalDto.getMarks(), resultDto.getMarks());
    }

    @Test
    public void testRoundTripConversion_DtoToEntityToDto() {
        // Given
        FeedbackDto originalDto = new FeedbackDto(
                "full-round-trip-id",
                "full-round-trip-response",
                "Complete round trip",
                93
        );

        // When
        FeedbackEntity entity = FeedbackMapper.dtoToEntity(originalDto);
        FeedbackDto resultDto = FeedbackMapper.entityToDto(entity);

        // Then
        assertEquals(originalDto.getId(), resultDto.getId());
        assertEquals(originalDto.getResponseId(), resultDto.getResponseId());
        assertEquals(originalDto.getText(), resultDto.getText());
        assertEquals(originalDto.getMarks(), resultDto.getMarks());
    }

    @Test
    public void testFromDto_preservesWindowsAssignedId() {
        // Per API Contract §4.1 and issue #203, Windows-assigned IDs must be preserved
        String windowsId = "win-fb-7c9e6679-7425-40de-944b-e07fc1f90ae7";
        FeedbackDto dto = new FeedbackDto(windowsId, "response-id", "Feedback", 50);

        Feedback domain = FeedbackMapper.fromDto(dto);
        FeedbackEntity entity = FeedbackMapper.dtoToEntity(dto);

        assertEquals(windowsId, domain.getId());
        assertEquals(windowsId, entity.getId());
    }
}
