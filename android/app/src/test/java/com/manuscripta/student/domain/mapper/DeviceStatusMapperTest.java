package com.manuscripta.student.domain.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.manuscripta.student.data.model.DeviceStatusEntity;
import com.manuscripta.student.domain.model.DeviceStatus;

import org.junit.Test;

/**
 * Unit tests for {@link DeviceStatusMapper}.
 * Tests bidirectional mapping between DeviceStatusEntity and DeviceStatus domain model.
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
                com.manuscripta.student.data.model.DeviceStatus.HAND_RAISED,
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

        // Test HAND_RAISED
        DeviceStatusEntity handRaisedEntity = new DeviceStatusEntity("id2", com.manuscripta.student.data.model.DeviceStatus.HAND_RAISED,
                50, null, null, 2000L);
        DeviceStatus handRaisedDomain = DeviceStatusMapper.toDomain(handRaisedEntity);
        assertEquals(com.manuscripta.student.data.model.DeviceStatus.HAND_RAISED, handRaisedDomain.getStatus());

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
}
