package com.manuscripta.student.domain.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.manuscripta.student.data.model.SessionEntity;
import com.manuscripta.student.data.model.SessionStatus;
import com.manuscripta.student.domain.model.Session;

import org.junit.Test;

/**
 * Unit tests for {@link SessionMapper}.
 * Tests bidirectional mapping between SessionEntity and Session domain model.
 */
public class SessionMapperTest {

    @Test
    public void testToDomain() {
        // Given
        SessionEntity entity = new SessionEntity(
                "session-id-123",
                "material-id-456",
                1234567890L,
                1234568000L,
                SessionStatus.COMPLETED,
                "device-android-001"
        );

        // When
        Session domain = SessionMapper.toDomain(entity);

        // Then
        assertNotNull(domain);
        assertEquals(entity.getId(), domain.getId());
        assertEquals(entity.getMaterialId(), domain.getMaterialId());
        assertEquals(entity.getStartTime(), domain.getStartTime());
        assertEquals(entity.getEndTime(), domain.getEndTime());
        assertEquals(entity.getStatus(), domain.getStatus());
        assertEquals(entity.getDeviceId(), domain.getDeviceId());
    }

    @Test
    public void testToEntity() {
        // Given
        Session domain = new Session(
                "session-id-789",
                "material-id-012",
                9876543210L,
                9876544000L,
                SessionStatus.PAUSED,
                "device-android-002"
        );

        // When
        SessionEntity entity = SessionMapper.toEntity(domain);

        // Then
        assertNotNull(entity);
        assertEquals(domain.getId(), entity.getId());
        assertEquals(domain.getMaterialId(), entity.getMaterialId());
        assertEquals(domain.getStartTime(), entity.getStartTime());
        assertEquals(domain.getEndTime(), entity.getEndTime());
        assertEquals(domain.getStatus(), entity.getStatus());
        assertEquals(domain.getDeviceId(), entity.getDeviceId());
    }

    @Test
    public void testToDomain_ActiveSession() {
        // Given - Active session with endTime = 0
        SessionEntity entity = new SessionEntity(
                "active-session-id",
                "material-id-active",
                5555555555L,
                0L,
                SessionStatus.ACTIVE,
                "device-tablet-001"
        );

        // When
        Session domain = SessionMapper.toDomain(entity);

        // Then
        assertNotNull(domain);
        assertEquals(SessionStatus.ACTIVE, domain.getStatus());
        assertEquals(0L, domain.getEndTime());
        assertEquals("device-tablet-001", domain.getDeviceId());
    }

    @Test
    public void testToDomain_CancelledSession() {
        // Given
        SessionEntity entity = new SessionEntity(
                "cancelled-session-id",
                "material-id-cancelled",
                7777777777L,
                7777778000L,
                SessionStatus.CANCELLED,
                "device-phone-003"
        );

        // When
        Session domain = SessionMapper.toDomain(entity);

        // Then
        assertNotNull(domain);
        assertEquals(SessionStatus.CANCELLED, domain.getStatus());
        assertEquals(7777778000L, domain.getEndTime());
    }

    @Test
    public void testRoundTripConversion_EntityToDomainToEntity() {
        // Given
        SessionEntity originalEntity = new SessionEntity(
                "round-trip-id",
                "material-round-trip",
                1111111111L,
                1111112000L,
                SessionStatus.COMPLETED,
                "device-round-trip"
        );

        // When
        Session domain = SessionMapper.toDomain(originalEntity);
        SessionEntity resultEntity = SessionMapper.toEntity(domain);

        // Then
        assertEquals(originalEntity.getId(), resultEntity.getId());
        assertEquals(originalEntity.getMaterialId(), resultEntity.getMaterialId());
        assertEquals(originalEntity.getStartTime(), resultEntity.getStartTime());
        assertEquals(originalEntity.getEndTime(), resultEntity.getEndTime());
        assertEquals(originalEntity.getStatus(), resultEntity.getStatus());
        assertEquals(originalEntity.getDeviceId(), resultEntity.getDeviceId());
    }

    @Test
    public void testRoundTripConversion_DomainToEntityToDomain() {
        // Given
        Session originalDomain = new Session(
                "round-trip-domain-id",
                "material-domain-id",
                2222222222L,
                2222223000L,
                SessionStatus.PAUSED,
                "device-domain-id"
        );

        // When
        SessionEntity entity = SessionMapper.toEntity(originalDomain);
        Session resultDomain = SessionMapper.toDomain(entity);

        // Then
        assertEquals(originalDomain.getId(), resultDomain.getId());
        assertEquals(originalDomain.getMaterialId(), resultDomain.getMaterialId());
        assertEquals(originalDomain.getStartTime(), resultDomain.getStartTime());
        assertEquals(originalDomain.getEndTime(), resultDomain.getEndTime());
        assertEquals(originalDomain.getStatus(), resultDomain.getStatus());
        assertEquals(originalDomain.getDeviceId(), resultDomain.getDeviceId());
    }

    @Test
    public void testToDomain_AllSessionStatuses() {
        // Test ACTIVE
        SessionEntity activeEntity = new SessionEntity("id1", "mat1", 1000L, 0L,
                SessionStatus.ACTIVE, "device1");
        Session activeDomain = SessionMapper.toDomain(activeEntity);
        assertEquals(SessionStatus.ACTIVE, activeDomain.getStatus());

        // Test PAUSED
        SessionEntity pausedEntity = new SessionEntity("id2", "mat2", 2000L, 0L,
                SessionStatus.PAUSED, "device2");
        Session pausedDomain = SessionMapper.toDomain(pausedEntity);
        assertEquals(SessionStatus.PAUSED, pausedDomain.getStatus());

        // Test COMPLETED
        SessionEntity completedEntity = new SessionEntity("id3", "mat3", 3000L, 4000L,
                SessionStatus.COMPLETED, "device3");
        Session completedDomain = SessionMapper.toDomain(completedEntity);
        assertEquals(SessionStatus.COMPLETED, completedDomain.getStatus());

        // Test CANCELLED
        SessionEntity cancelledEntity = new SessionEntity("id4", "mat4", 5000L, 6000L,
                SessionStatus.CANCELLED, "device4");
        Session cancelledDomain = SessionMapper.toDomain(cancelledEntity);
        assertEquals(SessionStatus.CANCELLED, cancelledDomain.getStatus());
    }
}
