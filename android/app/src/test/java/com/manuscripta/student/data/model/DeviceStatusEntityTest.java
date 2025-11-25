package com.manuscripta.student.data.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link DeviceStatusEntity} entity.
 * Tests immutable entity construction and getters.
 */
public class DeviceStatusEntityTest {

    private final String TEST_DEVICE_ID = "test-device-123";
    private final int TEST_BATTERY_LEVEL = 85;
    private final String TEST_MATERIAL_ID = "mat-123";
    private final String TEST_STUDENT_VIEW = "StudentView";

    @Test
    public void testRoomConstructor() {
        // Test rehydration constructor
        long now = System.currentTimeMillis();
        DeviceStatusEntity entity = new DeviceStatusEntity(
                "rehydrated-device",
                DeviceStatus.DISCONNECTED,
                50,
                "mat-rehydrated",
                "view-rehydrated",
                now);

        assertEquals("rehydrated-device", entity.getDeviceId());
        assertEquals(DeviceStatus.DISCONNECTED, entity.getStatus());
        assertEquals(50, entity.getBatteryLevel());
        assertEquals("mat-rehydrated", entity.getCurrentMaterialId());
        assertEquals("view-rehydrated", entity.getStudentView());
        assertEquals(now, entity.getLastUpdated());
    }

    @Test
    public void testGetters() {
        long now = System.currentTimeMillis();
        DeviceStatusEntity entity = new DeviceStatusEntity(
                TEST_DEVICE_ID,
                DeviceStatus.ON_TASK,
                TEST_BATTERY_LEVEL,
                TEST_MATERIAL_ID,
                TEST_STUDENT_VIEW,
                now);

        assertEquals(TEST_DEVICE_ID, entity.getDeviceId());
        assertEquals(DeviceStatus.ON_TASK, entity.getStatus());
        assertEquals(TEST_BATTERY_LEVEL, entity.getBatteryLevel());
        assertEquals(TEST_MATERIAL_ID, entity.getCurrentMaterialId());
        assertEquals(TEST_STUDENT_VIEW, entity.getStudentView());
        assertEquals(now, entity.getLastUpdated());
    }

    @Test
    public void testStatusValues() {
        long now = System.currentTimeMillis();
        
        // Test creating entities with each status value
        for (DeviceStatus status : DeviceStatus.values()) {
            DeviceStatusEntity entity = new DeviceStatusEntity(
                    TEST_DEVICE_ID,
                    status,
                    TEST_BATTERY_LEVEL,
                    TEST_MATERIAL_ID,
                    TEST_STUDENT_VIEW,
                    now);
            assertEquals(status, entity.getStatus());
        }

        // Explicitly check specific statuses
        DeviceStatusEntity lockedEntity = new DeviceStatusEntity(
                TEST_DEVICE_ID, DeviceStatus.LOCKED, 50, "mat", "view", now);
        assertEquals(DeviceStatus.LOCKED, lockedEntity.getStatus());

        DeviceStatusEntity idleEntity = new DeviceStatusEntity(
                TEST_DEVICE_ID, DeviceStatus.IDLE, 50, "mat", "view", now);
        assertEquals(DeviceStatus.IDLE, idleEntity.getStatus());
    }

    @Test
    public void testBatteryLevelBoundary() {
        long now = System.currentTimeMillis();
        
        // Test valid boundary values
        DeviceStatusEntity lowBattery = new DeviceStatusEntity(
                TEST_DEVICE_ID, DeviceStatus.ON_TASK, 0, TEST_MATERIAL_ID, TEST_STUDENT_VIEW, now);
        assertEquals(0, lowBattery.getBatteryLevel());

        DeviceStatusEntity fullBattery = new DeviceStatusEntity(
                TEST_DEVICE_ID, DeviceStatus.ON_TASK, 100, TEST_MATERIAL_ID, TEST_STUDENT_VIEW, now);
        assertEquals(100, fullBattery.getBatteryLevel());
    }
}
