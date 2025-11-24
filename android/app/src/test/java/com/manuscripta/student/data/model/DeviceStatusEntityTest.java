package com.manuscripta.student.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link DeviceStatusEntity} entity.
 */
public class DeviceStatusEntityTest {

    private DeviceStatusEntity deviceStatusEntity;
    private final String TEST_DEVICE_ID = "test-device-123";
    private final int TEST_BATTERY_LEVEL = 85;
    private final String TEST_MATERIAL_ID = "mat-123";
    private final String TEST_STUDENT_VIEW = "StudentView";

    @Before
    public void setUp() {
        // Use convenience constructor for general setup
        this.deviceStatusEntity = new DeviceStatusEntity(
                TEST_DEVICE_ID,
                DeviceStatus.ON_TASK,
                TEST_BATTERY_LEVEL,
                TEST_MATERIAL_ID,
                TEST_STUDENT_VIEW);
    }

    @Test
    public void testRoomConstructor() {
        // Test rehydration constructor
        long now = System.currentTimeMillis();
        DeviceStatusEntity rehydratedEntity = new DeviceStatusEntity(
                "rehydrated-device",
                DeviceStatus.DISCONNECTED,
                50,
                "mat-rehydrated",
                "view-rehydrated",
                now);

        assertEquals("rehydrated-device", rehydratedEntity.getDeviceId());
        assertEquals(DeviceStatus.DISCONNECTED, rehydratedEntity.getStatus());
        assertEquals(50, rehydratedEntity.getBatteryLevel());
        assertEquals("mat-rehydrated", rehydratedEntity.getCurrentMaterialId());
        assertEquals("view-rehydrated", rehydratedEntity.getStudentView());
        assertEquals(now, rehydratedEntity.getLastUpdated());
    }

    @Test
    public void testConvenienceConstructor() {
        DeviceStatusEntity newEntity = new DeviceStatusEntity(
                "new-device",
                DeviceStatus.HAND_RAISED,
                100,
                null,
                null);

        assertEquals("new-device", newEntity.getDeviceId());
        assertEquals(DeviceStatus.HAND_RAISED, newEntity.getStatus());
        assertEquals(100, newEntity.getBatteryLevel());
        assertNull(newEntity.getCurrentMaterialId());
        assertNull(newEntity.getStudentView());

        // Verify lastUpdated is set to current time (approx)
        long currentTime = System.currentTimeMillis();
        long diff = Math.abs(currentTime - newEntity.getLastUpdated());
        assertTrue("Last updated time should be close to current time", diff < 1000);
    }

    @Test
    public void testSettersAndGetters() {
        // Verify initial values
        assertEquals(TEST_DEVICE_ID, deviceStatusEntity.getDeviceId());
        assertEquals(DeviceStatus.ON_TASK, deviceStatusEntity.getStatus());
        assertEquals(TEST_BATTERY_LEVEL, deviceStatusEntity.getBatteryLevel());
        assertEquals(TEST_MATERIAL_ID, deviceStatusEntity.getCurrentMaterialId());
        assertEquals(TEST_STUDENT_VIEW, deviceStatusEntity.getStudentView());

        // Test setters
        deviceStatusEntity.setStatus(DeviceStatus.HAND_RAISED);
        deviceStatusEntity.setBatteryLevel(20);
        deviceStatusEntity.setCurrentMaterialId("new-mat");
        deviceStatusEntity.setStudentView("new-view");
        long newTime = System.currentTimeMillis() + 1000;
        deviceStatusEntity.setLastUpdated(newTime);

        // Verify updated values
        assertEquals(DeviceStatus.HAND_RAISED, deviceStatusEntity.getStatus());
        assertEquals(20, deviceStatusEntity.getBatteryLevel());
        assertEquals("new-mat", deviceStatusEntity.getCurrentMaterialId());
        assertEquals("new-view", deviceStatusEntity.getStudentView());
        assertEquals(newTime, deviceStatusEntity.getLastUpdated());
    }

    @Test
    public void testStatusTransitions() {
        // Test all enum values
        for (DeviceStatus status : DeviceStatus.values()) {
            deviceStatusEntity.setStatus(status);
            assertEquals(status, deviceStatusEntity.getStatus());
        }

        // Explicitly check new statuses
        deviceStatusEntity.setStatus(DeviceStatus.LOCKED);
        assertEquals(DeviceStatus.LOCKED, deviceStatusEntity.getStatus());

        deviceStatusEntity.setStatus(DeviceStatus.IDLE);
        assertEquals(DeviceStatus.IDLE, deviceStatusEntity.getStatus());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetBatteryLevelInvalidLow() {
        // battery below 0 should throw
        deviceStatusEntity.setBatteryLevel(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetBatteryLevelInvalidHigh() {
        // battery above 100 should throw
        deviceStatusEntity.setBatteryLevel(101);
    }
}
