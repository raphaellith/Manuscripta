package com.manuscripta.student.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

    @Before
    public void setUp() {
        // Use convenience constructor for general setup
        this.deviceStatusEntity = new DeviceStatusEntity(
            TEST_DEVICE_ID,
            DeviceStatus.ON_TASK,
            TEST_BATTERY_LEVEL
        );
    }

    @Test
    public void testRoomConstructor() {
        // Test rehydration constructor
        long now = System.currentTimeMillis();
        DeviceStatusEntity rehydratedEntity = new DeviceStatusEntity(
            "rehydrated-device",
            DeviceStatus.DISCONNECTED,
            50,
            now
        );

        assertEquals("rehydrated-device", rehydratedEntity.getDeviceId());
        assertEquals(DeviceStatus.DISCONNECTED, rehydratedEntity.getStatus());
        assertEquals(50, rehydratedEntity.getBatteryLevel());
        assertEquals(now, rehydratedEntity.getLastUpdated());
    }

    @Test
    public void testConvenienceConstructor() {
        DeviceStatusEntity newEntity = new DeviceStatusEntity(
            "new-device",
            DeviceStatus.NEEDS_HELP,
            100
        );

        assertEquals("new-device", newEntity.getDeviceId());
        assertEquals(DeviceStatus.NEEDS_HELP, newEntity.getStatus());
        assertEquals(100, newEntity.getBatteryLevel());
        
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

        // Test setters
        deviceStatusEntity.setStatus(DeviceStatus.NEEDS_HELP);
        deviceStatusEntity.setBatteryLevel(20);
        long newTime = System.currentTimeMillis() + 1000;
        deviceStatusEntity.setLastUpdated(newTime);

        // Verify updated values
        assertEquals(DeviceStatus.NEEDS_HELP, deviceStatusEntity.getStatus());
        assertEquals(20, deviceStatusEntity.getBatteryLevel());
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
}
