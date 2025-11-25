package com.manuscripta.student.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link DeviceStatus} domain model.
 */
public class DeviceStatusModelTest {
    private DeviceStatus deviceStatus;

    @Before
    public void setUp() {
        this.deviceStatus = new DeviceStatus(
                "device-id",
                com.manuscripta.student.data.model.DeviceStatus.ON_TASK,
                85,
                "material-id",
                "StudentView",
                1234567890L
        );
    }

    @Test
    public void testConstructorAndGetters() {
        assertNotNull(deviceStatus);
        assertEquals("device-id", deviceStatus.getDeviceId());
        assertEquals(com.manuscripta.student.data.model.DeviceStatus.ON_TASK, deviceStatus.getStatus());
        assertEquals(85, deviceStatus.getBatteryLevel());
        assertEquals("material-id", deviceStatus.getCurrentMaterialId());
        assertEquals("StudentView", deviceStatus.getStudentView());
        assertEquals(1234567890L, deviceStatus.getLastUpdated());
    }

    @Test
    public void testCreateFactoryMethod() {
        DeviceStatus newModel = DeviceStatus.create(
                "new-device",
                com.manuscripta.student.data.model.DeviceStatus.HAND_RAISED,
                100,
                null,
                null
        );

        assertNotNull(newModel);
        assertEquals("new-device", newModel.getDeviceId());
        assertEquals(com.manuscripta.student.data.model.DeviceStatus.HAND_RAISED, newModel.getStatus());
        assertEquals(100, newModel.getBatteryLevel());
        assertNull(newModel.getCurrentMaterialId());
        assertNull(newModel.getStudentView());

        // Verify lastUpdated is set to current time (approx)
        long currentTime = System.currentTimeMillis();
        long diff = Math.abs(currentTime - newModel.getLastUpdated());
        assertTrue("Last updated time should be close to current time", diff < 1000);
    }

    @Test
    public void testCreateFactoryMethodWithNonNullFields() {
        DeviceStatus newModel = DeviceStatus.create(
                "device-123",
                com.manuscripta.student.data.model.DeviceStatus.ON_TASK,
                50,
                "mat-123",
                "CurrentView"
        );

        assertNotNull(newModel);
        assertEquals("device-123", newModel.getDeviceId());
        assertEquals(com.manuscripta.student.data.model.DeviceStatus.ON_TASK, newModel.getStatus());
        assertEquals(50, newModel.getBatteryLevel());
        assertEquals("mat-123", newModel.getCurrentMaterialId());
        assertEquals("CurrentView", newModel.getStudentView());
    }

    @Test
    public void testConstructorWithNullableFields() {
        DeviceStatus model = new DeviceStatus(
                "device-id",
                com.manuscripta.student.data.model.DeviceStatus.DISCONNECTED,
                0,
                null,
                null,
                0L
        );

        assertNotNull(model);
        assertEquals("device-id", model.getDeviceId());
        assertEquals(com.manuscripta.student.data.model.DeviceStatus.DISCONNECTED, model.getStatus());
        assertEquals(0, model.getBatteryLevel());
        assertNull(model.getCurrentMaterialId());
        assertNull(model.getStudentView());
        assertEquals(0L, model.getLastUpdated());
    }
}
