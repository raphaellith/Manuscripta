package com.manuscripta.student.domain.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.manuscripta.student.data.model.DeviceStatusEntity;
import com.manuscripta.student.domain.model.DeviceStatus;
import com.manuscripta.student.network.dto.DeviceStatusDto;

import org.junit.Test;

/**
 * Unit tests for {@link DeviceStatusMapper}.
 * Tests bidirectional mapping between DeviceStatusEntity, DeviceStatus domain model,
 * and DeviceStatusDto.
 */
public class DeviceStatusMapperTest {

    @Test
    public void testToDomain() {
        // Given
        DeviceStatusEntity entity = new DeviceStatusEntity(
                "device-id-123",
                com.manuscripta.student.data.model.DeviceStatus.ON_TASK,
                85,
                "material-id-456",
                "StudentView",
                1234567890L
        );

        // When
        DeviceStatus domain = DeviceStatusMapper.toDomain(entity);

        // Then
        assertNotNull(domain);
        assertEquals(entity.getDeviceId(), domain.getDeviceId());
        assertEquals(entity.getStatus(), domain.getStatus());
        assertEquals(entity.getBatteryLevel(), domain.getBatteryLevel());
        assertEquals(entity.getCurrentMaterialId(), domain.getCurrentMaterialId());
        assertEquals(entity.getStudentView(), domain.getStudentView());
        assertEquals(entity.getLastUpdated(), domain.getLastUpdated());
    }

    @Test
    public void testToEntity() {
        // Given
        DeviceStatus domain = new DeviceStatus(
                "device-id-789",
                com.manuscripta.student.data.model.DeviceStatus.IDLE,
                50,
                "material-id-012",
                "AnotherView",
                9876543210L
        );

        // When
        DeviceStatusEntity entity = DeviceStatusMapper.toEntity(domain);

        // Then
        assertNotNull(entity);
        assertEquals(domain.getDeviceId(), entity.getDeviceId());
        assertEquals(domain.getStatus(), entity.getStatus());
        assertEquals(domain.getBatteryLevel(), entity.getBatteryLevel());
        assertEquals(domain.getCurrentMaterialId(), entity.getCurrentMaterialId());
        assertEquals(domain.getStudentView(), entity.getStudentView());
        assertEquals(domain.getLastUpdated(), entity.getLastUpdated());
    }

    @Test
    public void testToDomain_WithNullFields() {
        // Given - Entity with null optional fields
        DeviceStatusEntity entity = new DeviceStatusEntity(
                "device-id-null",
                com.manuscripta.student.data.model.DeviceStatus.DISCONNECTED,
                0,
                null,
                null,
                5555555555L
        );

        // When
        DeviceStatus domain = DeviceStatusMapper.toDomain(entity);

        // Then
        assertNotNull(domain);
        assertEquals(com.manuscripta.student.data.model.DeviceStatus.DISCONNECTED, domain.getStatus());
        assertNull(domain.getCurrentMaterialId());
        assertNull(domain.getStudentView());
    }

    @Test
    public void testRoundTripConversion_EntityToDomainToEntity() {
        // Given
        DeviceStatusEntity originalEntity = new DeviceStatusEntity(
                "round-trip-id",
                com.manuscripta.student.data.model.DeviceStatus.LOCKED,
                75,
                "material-round-trip",
                "view-round-trip",
                1111111111L
        );

        // When
        DeviceStatus domain = DeviceStatusMapper.toDomain(originalEntity);
        DeviceStatusEntity resultEntity = DeviceStatusMapper.toEntity(domain);

        // Then
        assertEquals(originalEntity.getDeviceId(), resultEntity.getDeviceId());
        assertEquals(originalEntity.getStatus(), resultEntity.getStatus());
        assertEquals(originalEntity.getBatteryLevel(), resultEntity.getBatteryLevel());
        assertEquals(originalEntity.getCurrentMaterialId(), resultEntity.getCurrentMaterialId());
        assertEquals(originalEntity.getStudentView(), resultEntity.getStudentView());
        assertEquals(originalEntity.getLastUpdated(), resultEntity.getLastUpdated());
    }

    @Test
    public void testRoundTripConversion_DomainToEntityToDomain() {
        // Given
        DeviceStatus originalDomain = new DeviceStatus(
                "round-trip-domain-id",
                com.manuscripta.student.data.model.DeviceStatus.IDLE,
                25,
                "material-domain-id",
                "view-domain-id",
                2222222222L
        );

        // When
        DeviceStatusEntity entity = DeviceStatusMapper.toEntity(originalDomain);
        DeviceStatus resultDomain = DeviceStatusMapper.toDomain(entity);

        // Then
        assertEquals(originalDomain.getDeviceId(), resultDomain.getDeviceId());
        assertEquals(originalDomain.getStatus(), resultDomain.getStatus());
        assertEquals(originalDomain.getBatteryLevel(), resultDomain.getBatteryLevel());
        assertEquals(originalDomain.getCurrentMaterialId(), resultDomain.getCurrentMaterialId());
        assertEquals(originalDomain.getStudentView(), resultDomain.getStudentView());
        assertEquals(originalDomain.getLastUpdated(), resultDomain.getLastUpdated());
    }

    @Test
    public void testToDomain_AllDeviceStatuses() {
        // Test ON_TASK
        DeviceStatusEntity onTaskEntity = new DeviceStatusEntity("id1", com.manuscripta.student.data.model.DeviceStatus.ON_TASK,
                50, null, null, 1000L);
        DeviceStatus onTaskDomain = DeviceStatusMapper.toDomain(onTaskEntity);
        assertEquals(com.manuscripta.student.data.model.DeviceStatus.ON_TASK, onTaskDomain.getStatus());


        // Test DISCONNECTED
        DeviceStatusEntity disconnectedEntity = new DeviceStatusEntity("id3", com.manuscripta.student.data.model.DeviceStatus.DISCONNECTED,
                50, null, null, 3000L);
        DeviceStatus disconnectedDomain = DeviceStatusMapper.toDomain(disconnectedEntity);
        assertEquals(com.manuscripta.student.data.model.DeviceStatus.DISCONNECTED, disconnectedDomain.getStatus());

        // Test LOCKED
        DeviceStatusEntity lockedEntity = new DeviceStatusEntity("id4", com.manuscripta.student.data.model.DeviceStatus.LOCKED,
                50, null, null, 4000L);
        DeviceStatus lockedDomain = DeviceStatusMapper.toDomain(lockedEntity);
        assertEquals(com.manuscripta.student.data.model.DeviceStatus.LOCKED, lockedDomain.getStatus());

        // Test IDLE
        DeviceStatusEntity idleEntity = new DeviceStatusEntity("id5", com.manuscripta.student.data.model.DeviceStatus.IDLE,
                50, null, null, 5000L);
        DeviceStatus idleDomain = DeviceStatusMapper.toDomain(idleEntity);
        assertEquals(com.manuscripta.student.data.model.DeviceStatus.IDLE, idleDomain.getStatus());
    }

    @Test
    public void testPrivateConstructorThrowsException() throws Exception {
        // Use reflection to access private constructor
        java.lang.reflect.Constructor<DeviceStatusMapper> constructor = DeviceStatusMapper.class.getDeclaredConstructor();
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
    public void testToDto() {
        // Given
        DeviceStatus domain = new DeviceStatus(
                "device-id-dto",
                com.manuscripta.student.data.model.DeviceStatus.ON_TASK,
                85,
                "material-id-dto",
                "page-5",
                1702147200000L  // milliseconds
        );

        // When
        DeviceStatusDto dto = DeviceStatusMapper.toDto(domain);

        // Then
        assertNotNull(dto);
        assertEquals(domain.getDeviceId(), dto.getDeviceId());
        assertEquals("ON_TASK", dto.getStatus());
        assertEquals(Integer.valueOf(domain.getBatteryLevel()), dto.getBatteryLevel());
        assertEquals(domain.getCurrentMaterialId(), dto.getCurrentMaterialId());
        assertEquals(domain.getStudentView(), dto.getStudentView());
        // Timestamp should be converted from milliseconds to seconds
        assertEquals(Long.valueOf(1702147200L), dto.getTimestamp());
    }

    @Test
    public void testToDto_WithNullOptionalFields() {
        // Given
        DeviceStatus domain = new DeviceStatus(
                "device-id-null-dto",
                com.manuscripta.student.data.model.DeviceStatus.IDLE,
                50,
                null,
                null,
                1000000L
        );

        // When
        DeviceStatusDto dto = DeviceStatusMapper.toDto(domain);

        // Then
        assertNotNull(dto);
        assertNull(dto.getCurrentMaterialId());
        assertNull(dto.getStudentView());
    }

    @Test
    public void testToDto_AllStatusTypes() {
        // Test ON_TASK
        DeviceStatus onTask = new DeviceStatus("id1",
                com.manuscripta.student.data.model.DeviceStatus.ON_TASK, 50, null, null, 1000L);
        assertEquals("ON_TASK", DeviceStatusMapper.toDto(onTask).getStatus());

        // Test IDLE
        DeviceStatus idle = new DeviceStatus("id2",
                com.manuscripta.student.data.model.DeviceStatus.IDLE, 50, null, null, 1000L);
        assertEquals("IDLE", DeviceStatusMapper.toDto(idle).getStatus());

        // Test LOCKED
        DeviceStatus locked = new DeviceStatus("id3",
                com.manuscripta.student.data.model.DeviceStatus.LOCKED, 50, null, null, 1000L);
        assertEquals("LOCKED", DeviceStatusMapper.toDto(locked).getStatus());

        // Test DISCONNECTED
        DeviceStatus disconnected = new DeviceStatus("id4",
                com.manuscripta.student.data.model.DeviceStatus.DISCONNECTED, 50, null, null, 1000L);
        assertEquals("DISCONNECTED", DeviceStatusMapper.toDto(disconnected).getStatus());
    }

    @Test
    public void testFromDto() {
        // Given
        DeviceStatusDto dto = new DeviceStatusDto(
                "device-id-from-dto",
                "ON_TASK",
                85,
                "material-id-from-dto",
                "page-10",
                1702147200L  // seconds
        );

        // When
        DeviceStatus domain = DeviceStatusMapper.fromDto(dto);

        // Then
        assertNotNull(domain);
        assertEquals(dto.getDeviceId(), domain.getDeviceId());
        assertEquals(com.manuscripta.student.data.model.DeviceStatus.ON_TASK, domain.getStatus());
        assertEquals(dto.getBatteryLevel().intValue(), domain.getBatteryLevel());
        assertEquals(dto.getCurrentMaterialId(), domain.getCurrentMaterialId());
        assertEquals(dto.getStudentView(), domain.getStudentView());
        // Timestamp should be converted from seconds to milliseconds
        assertEquals(1702147200000L, domain.getLastUpdated());
    }

    @Test
    public void testFromDto_WithNullOptionalFields() {
        // Given
        DeviceStatusDto dto = new DeviceStatusDto(
                "device-id-null-from",
                "IDLE",
                50,
                null,
                null,
                1000L
        );

        // When
        DeviceStatus domain = DeviceStatusMapper.fromDto(dto);

        // Then
        assertNotNull(domain);
        assertNull(domain.getCurrentMaterialId());
        assertNull(domain.getStudentView());
    }

    @Test
    public void testFromDto_AllStatusTypes() {
        // Test ON_TASK
        DeviceStatusDto onTaskDto = new DeviceStatusDto("id1", "ON_TASK", 50, null, null, 1000L);
        assertEquals(com.manuscripta.student.data.model.DeviceStatus.ON_TASK,
                DeviceStatusMapper.fromDto(onTaskDto).getStatus());

        // Test IDLE
        DeviceStatusDto idleDto = new DeviceStatusDto("id2", "IDLE", 50, null, null, 1000L);
        assertEquals(com.manuscripta.student.data.model.DeviceStatus.IDLE,
                DeviceStatusMapper.fromDto(idleDto).getStatus());

        // Test LOCKED
        DeviceStatusDto lockedDto = new DeviceStatusDto("id3", "LOCKED", 50, null, null, 1000L);
        assertEquals(com.manuscripta.student.data.model.DeviceStatus.LOCKED,
                DeviceStatusMapper.fromDto(lockedDto).getStatus());

        // Test DISCONNECTED
        DeviceStatusDto disconnectedDto = new DeviceStatusDto("id4", "DISCONNECTED",
                50, null, null, 1000L);
        assertEquals(com.manuscripta.student.data.model.DeviceStatus.DISCONNECTED,
                DeviceStatusMapper.fromDto(disconnectedDto).getStatus());
    }

    @Test
    public void testFromDto_NullDeviceId_ThrowsException() {
        // Given
        DeviceStatusDto dto = new DeviceStatusDto(null, "ON_TASK", 50, null, null, 1000L);

        // When/Then
        try {
            DeviceStatusMapper.fromDto(dto);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("DeviceStatusDto deviceId cannot be null", e.getMessage());
        }
    }

    @Test
    public void testFromDto_NullStatus_ThrowsException() {
        // Given
        DeviceStatusDto dto = new DeviceStatusDto("device-id", null, 50, null, null, 1000L);

        // When/Then
        try {
            DeviceStatusMapper.fromDto(dto);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("DeviceStatusDto status cannot be null", e.getMessage());
        }
    }

    @Test
    public void testFromDto_NullBatteryLevel_ThrowsException() {
        // Given
        DeviceStatusDto dto = new DeviceStatusDto("device-id", "ON_TASK", null, null, null, 1000L);

        // When/Then
        try {
            DeviceStatusMapper.fromDto(dto);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("DeviceStatusDto batteryLevel cannot be null", e.getMessage());
        }
    }

    @Test
    public void testFromDto_NullTimestamp_ThrowsException() {
        // Given
        DeviceStatusDto dto = new DeviceStatusDto("device-id", "ON_TASK", 50, null, null, null);

        // When/Then
        try {
            DeviceStatusMapper.fromDto(dto);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("DeviceStatusDto timestamp cannot be null", e.getMessage());
        }
    }

    @Test
    public void testFromDto_InvalidStatus_ThrowsException() {
        // Given
        DeviceStatusDto dto = new DeviceStatusDto("device-id", "INVALID_STATUS",
                50, null, null, 1000L);

        // When/Then
        try {
            DeviceStatusMapper.fromDto(dto);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("DeviceStatusDto status is invalid: INVALID_STATUS", e.getMessage());
        }
    }

    @Test
    public void testRoundTrip_DomainToDtoToDomain() {
        // Given
        DeviceStatus originalDomain = new DeviceStatus(
                "round-trip-dto-id",
                com.manuscripta.student.data.model.DeviceStatus.LOCKED,
                75,
                "material-round-trip-dto",
                "view-round-trip-dto",
                1702147200000L  // milliseconds
        );

        // When
        DeviceStatusDto dto = DeviceStatusMapper.toDto(originalDomain);
        DeviceStatus resultDomain = DeviceStatusMapper.fromDto(dto);

        // Then
        assertEquals(originalDomain.getDeviceId(), resultDomain.getDeviceId());
        assertEquals(originalDomain.getStatus(), resultDomain.getStatus());
        assertEquals(originalDomain.getBatteryLevel(), resultDomain.getBatteryLevel());
        assertEquals(originalDomain.getCurrentMaterialId(), resultDomain.getCurrentMaterialId());
        assertEquals(originalDomain.getStudentView(), resultDomain.getStudentView());
        // Note: timestamp loses millisecond precision in round-trip due to seconds conversion
        assertEquals(originalDomain.getLastUpdated() / 1000 * 1000, resultDomain.getLastUpdated());
    }

    @Test
    public void testTimestampConversion_MillisecondsToSeconds() {
        // Given: timestamp in milliseconds
        DeviceStatus domain = new DeviceStatus(
                "device-id",
                com.manuscripta.student.data.model.DeviceStatus.ON_TASK,
                50,
                null,
                null,
                1702147200123L  // milliseconds with sub-second precision
        );

        // When
        DeviceStatusDto dto = DeviceStatusMapper.toDto(domain);

        // Then: DTO timestamp should be in seconds (truncated)
        assertEquals(Long.valueOf(1702147200L), dto.getTimestamp());
    }

    @Test
    public void testTimestampConversion_SecondsToMilliseconds() {
        // Given: timestamp in seconds
        DeviceStatusDto dto = new DeviceStatusDto(
                "device-id",
                "ON_TASK",
                50,
                null,
                null,
                1702147200L  // seconds
        );

        // When
        DeviceStatus domain = DeviceStatusMapper.fromDto(dto);

        // Then: domain timestamp should be in milliseconds
        assertEquals(1702147200000L, domain.getLastUpdated());
    }
}
