package com.manuscripta.student.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
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
                com.manuscripta.student.data.model.DeviceStatus.IDLE,
                100,
                null,
                null
        );

        assertNotNull(newModel);
        assertEquals("new-device", newModel.getDeviceId());
        assertEquals(com.manuscripta.student.data.model.DeviceStatus.IDLE, newModel.getStatus());
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

    @Test
    public void testConstructorWithBoundaryBatteryLevels() {
        // Test minimum battery level (0)
        DeviceStatus minBattery = new DeviceStatus(
                "device-id",
                com.manuscripta.student.data.model.DeviceStatus.ON_TASK,
                0,
                null,
                null,
                0L
        );
        assertEquals(0, minBattery.getBatteryLevel());

        // Test maximum battery level (100)
        DeviceStatus maxBattery = new DeviceStatus(
                "device-id",
                com.manuscripta.student.data.model.DeviceStatus.ON_TASK,
                100,
                null,
                null,
                0L
        );
        assertEquals(100, maxBattery.getBatteryLevel());
    }

    @Test
    public void testConstructor_nullDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DeviceStatus(
                        null,
                        com.manuscripta.student.data.model.DeviceStatus.ON_TASK,
                        50,
                        null,
                        null,
                        0L
                )
        );
        assertEquals("DeviceStatus deviceId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_emptyDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DeviceStatus(
                        "",
                        com.manuscripta.student.data.model.DeviceStatus.ON_TASK,
                        50,
                        null,
                        null,
                        0L
                )
        );
        assertEquals("DeviceStatus deviceId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_blankDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DeviceStatus(
                        "   ",
                        com.manuscripta.student.data.model.DeviceStatus.ON_TASK,
                        50,
                        null,
                        null,
                        0L
                )
        );
        assertEquals("DeviceStatus deviceId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_nullStatus_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DeviceStatus(
                        "device-id",
                        null,
                        50,
                        null,
                        null,
                        0L
                )
        );
        assertEquals("DeviceStatus status cannot be null", exception.getMessage());
    }

    @Test
    public void testConstructor_negativeBatteryLevel_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DeviceStatus(
                        "device-id",
                        com.manuscripta.student.data.model.DeviceStatus.ON_TASK,
                        -1,
                        null,
                        null,
                        0L
                )
        );
        assertEquals("DeviceStatus batteryLevel must be between 0 and 100", exception.getMessage());
    }

    @Test
    public void testConstructor_batteryLevelAbove100_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DeviceStatus(
                        "device-id",
                        com.manuscripta.student.data.model.DeviceStatus.ON_TASK,
                        101,
                        null,
                        null,
                        0L
                )
        );
        assertEquals("DeviceStatus batteryLevel must be between 0 and 100", exception.getMessage());
    }

    @Test
    public void testConstructor_negativeLastUpdated_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DeviceStatus(
                        "device-id",
                        com.manuscripta.student.data.model.DeviceStatus.ON_TASK,
                        50,
                        null,
                        null,
                        -1L
                )
        );
        assertEquals("DeviceStatus lastUpdated cannot be negative", exception.getMessage());
    }

    @Test
    public void testCreate_nullDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> DeviceStatus.create(
                        null,
                        com.manuscripta.student.data.model.DeviceStatus.ON_TASK,
                        50,
                        null,
                        null
                )
        );
        assertEquals("DeviceStatus deviceId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testCreate_emptyDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> DeviceStatus.create(
                        "",
                        com.manuscripta.student.data.model.DeviceStatus.ON_TASK,
                        50,
                        null,
                        null
                )
        );
        assertEquals("DeviceStatus deviceId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testCreate_blankDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> DeviceStatus.create(
                        "   ",
                        com.manuscripta.student.data.model.DeviceStatus.ON_TASK,
                        50,
                        null,
                        null
                )
        );
        assertEquals("DeviceStatus deviceId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testCreate_nullStatus_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> DeviceStatus.create(
                        "device-id",
                        null,
                        50,
                        null,
                        null
                )
        );
        assertEquals("DeviceStatus status cannot be null", exception.getMessage());
    }

    @Test
    public void testCreate_negativeBatteryLevel_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> DeviceStatus.create(
                        "device-id",
                        com.manuscripta.student.data.model.DeviceStatus.ON_TASK,
                        -1,
                        null,
                        null
                )
        );
        assertEquals("DeviceStatus batteryLevel must be between 0 and 100", exception.getMessage());
    }

    @Test
    public void testCreate_batteryLevelAbove100_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> DeviceStatus.create(
                        "device-id",
                        com.manuscripta.student.data.model.DeviceStatus.ON_TASK,
                        101,
                        null,
                        null
                )
        );
        assertEquals("DeviceStatus batteryLevel must be between 0 and 100", exception.getMessage());
    }

    @Test
    public void testBatteryLevelConstants() {
        assertEquals(0, DeviceStatus.MIN_BATTERY_LEVEL);
        assertEquals(100, DeviceStatus.MAX_BATTERY_LEVEL);
    }
}
