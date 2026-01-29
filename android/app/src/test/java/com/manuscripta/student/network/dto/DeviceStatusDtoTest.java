package com.manuscripta.student.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link DeviceStatusDto}.
 * Tests construction, getters, setters, equals, hashCode, and toString methods.
 */
public class DeviceStatusDtoTest {

    private static final String TEST_DEVICE_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_STATUS = "ON_TASK";
    private static final Integer TEST_BATTERY_LEVEL = 85;
    private static final String TEST_CURRENT_MATERIAL_ID = "660e8400-e29b-41d4-a716-446655440001";
    private static final String TEST_STUDENT_VIEW = "page-5";
    private static final Long TEST_TIMESTAMP = 1702147200L;

    @Test
    public void testDefaultConstructor() {
        DeviceStatusDto dto = new DeviceStatusDto();

        assertNull(dto.getDeviceId());
        assertNull(dto.getStatus());
        assertNull(dto.getBatteryLevel());
        assertNull(dto.getCurrentMaterialId());
        assertNull(dto.getStudentView());
        assertNull(dto.getTimestamp());
    }

    @Test
    public void testConstructorWithAllFields() {
        DeviceStatusDto dto = new DeviceStatusDto(
                TEST_DEVICE_ID,
                TEST_STATUS,
                TEST_BATTERY_LEVEL,
                TEST_CURRENT_MATERIAL_ID,
                TEST_STUDENT_VIEW,
                TEST_TIMESTAMP
        );

        assertEquals(TEST_DEVICE_ID, dto.getDeviceId());
        assertEquals(TEST_STATUS, dto.getStatus());
        assertEquals(TEST_BATTERY_LEVEL, dto.getBatteryLevel());
        assertEquals(TEST_CURRENT_MATERIAL_ID, dto.getCurrentMaterialId());
        assertEquals(TEST_STUDENT_VIEW, dto.getStudentView());
        assertEquals(TEST_TIMESTAMP, dto.getTimestamp());
    }

    @Test
    public void testConstructorWithNullValues() {
        DeviceStatusDto dto = new DeviceStatusDto(null, null, null, null, null, null);

        assertNull(dto.getDeviceId());
        assertNull(dto.getStatus());
        assertNull(dto.getBatteryLevel());
        assertNull(dto.getCurrentMaterialId());
        assertNull(dto.getStudentView());
        assertNull(dto.getTimestamp());
    }

    @Test
    public void testSetDeviceId() {
        DeviceStatusDto dto = new DeviceStatusDto();

        dto.setDeviceId(TEST_DEVICE_ID);
        assertEquals(TEST_DEVICE_ID, dto.getDeviceId());

        dto.setDeviceId(null);
        assertNull(dto.getDeviceId());
    }

    @Test
    public void testSetStatus() {
        DeviceStatusDto dto = new DeviceStatusDto();

        dto.setStatus(TEST_STATUS);
        assertEquals(TEST_STATUS, dto.getStatus());

        dto.setStatus("IDLE");
        assertEquals("IDLE", dto.getStatus());

        dto.setStatus(null);
        assertNull(dto.getStatus());
    }

    @Test
    public void testSetBatteryLevel() {
        DeviceStatusDto dto = new DeviceStatusDto();

        dto.setBatteryLevel(TEST_BATTERY_LEVEL);
        assertEquals(TEST_BATTERY_LEVEL, dto.getBatteryLevel());

        dto.setBatteryLevel(0);
        assertEquals(Integer.valueOf(0), dto.getBatteryLevel());

        dto.setBatteryLevel(100);
        assertEquals(Integer.valueOf(100), dto.getBatteryLevel());

        dto.setBatteryLevel(null);
        assertNull(dto.getBatteryLevel());
    }

    @Test
    public void testSetCurrentMaterialId() {
        DeviceStatusDto dto = new DeviceStatusDto();

        dto.setCurrentMaterialId(TEST_CURRENT_MATERIAL_ID);
        assertEquals(TEST_CURRENT_MATERIAL_ID, dto.getCurrentMaterialId());

        dto.setCurrentMaterialId(null);
        assertNull(dto.getCurrentMaterialId());
    }

    @Test
    public void testSetStudentView() {
        DeviceStatusDto dto = new DeviceStatusDto();

        dto.setStudentView(TEST_STUDENT_VIEW);
        assertEquals(TEST_STUDENT_VIEW, dto.getStudentView());

        dto.setStudentView(null);
        assertNull(dto.getStudentView());
    }

    @Test
    public void testSetTimestamp() {
        DeviceStatusDto dto = new DeviceStatusDto();

        dto.setTimestamp(TEST_TIMESTAMP);
        assertEquals(TEST_TIMESTAMP, dto.getTimestamp());

        dto.setTimestamp(null);
        assertNull(dto.getTimestamp());
    }

    @Test
    public void testToString() {
        DeviceStatusDto dto = createTestDeviceStatusDto();

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("DeviceStatusDto"));
        assertTrue(result.contains(TEST_DEVICE_ID));
        assertTrue(result.contains(TEST_STATUS));
        assertTrue(result.contains(TEST_BATTERY_LEVEL.toString()));
        assertTrue(result.contains(TEST_CURRENT_MATERIAL_ID));
        assertTrue(result.contains(TEST_STUDENT_VIEW));
        assertTrue(result.contains(TEST_TIMESTAMP.toString()));
    }

    @Test
    public void testToStringWithNullValues() {
        DeviceStatusDto dto = new DeviceStatusDto();

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("DeviceStatusDto"));
        assertTrue(result.contains("null"));
    }

    @Test
    public void testEqualsSameObject() {
        DeviceStatusDto dto = createTestDeviceStatusDto();

        assertTrue(dto.equals(dto));
    }

    @Test
    public void testEqualsNull() {
        DeviceStatusDto dto = createTestDeviceStatusDto();

        assertFalse(dto.equals(null));
    }

    @Test
    public void testEqualsDifferentClass() {
        DeviceStatusDto dto = createTestDeviceStatusDto();

        assertFalse(dto.equals("not a dto"));
    }

    @Test
    public void testEqualsSameValues() {
        DeviceStatusDto dto1 = createTestDeviceStatusDto();
        DeviceStatusDto dto2 = createTestDeviceStatusDto();

        assertTrue(dto1.equals(dto2));
        assertTrue(dto2.equals(dto1));
    }

    @Test
    public void testEqualsDifferentDeviceId() {
        DeviceStatusDto dto1 = createTestDeviceStatusDto();
        DeviceStatusDto dto2 = createTestDeviceStatusDto();
        dto2.setDeviceId("different-id");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentStatus() {
        DeviceStatusDto dto1 = createTestDeviceStatusDto();
        DeviceStatusDto dto2 = createTestDeviceStatusDto();
        dto2.setStatus("IDLE");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentBatteryLevel() {
        DeviceStatusDto dto1 = createTestDeviceStatusDto();
        DeviceStatusDto dto2 = createTestDeviceStatusDto();
        dto2.setBatteryLevel(50);

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentCurrentMaterialId() {
        DeviceStatusDto dto1 = createTestDeviceStatusDto();
        DeviceStatusDto dto2 = createTestDeviceStatusDto();
        dto2.setCurrentMaterialId("different-material-id");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentStudentView() {
        DeviceStatusDto dto1 = createTestDeviceStatusDto();
        DeviceStatusDto dto2 = createTestDeviceStatusDto();
        dto2.setStudentView("different-view");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentTimestamp() {
        DeviceStatusDto dto1 = createTestDeviceStatusDto();
        DeviceStatusDto dto2 = createTestDeviceStatusDto();
        dto2.setTimestamp(9999999999L);

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullDeviceId() {
        DeviceStatusDto dto1 = new DeviceStatusDto(null, TEST_STATUS, TEST_BATTERY_LEVEL,
                TEST_CURRENT_MATERIAL_ID, TEST_STUDENT_VIEW, TEST_TIMESTAMP);
        DeviceStatusDto dto2 = new DeviceStatusDto(null, TEST_STATUS, TEST_BATTERY_LEVEL,
                TEST_CURRENT_MATERIAL_ID, TEST_STUDENT_VIEW, TEST_TIMESTAMP);

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullVsNonNullDeviceId() {
        DeviceStatusDto dto1 = new DeviceStatusDto(null, TEST_STATUS, TEST_BATTERY_LEVEL,
                TEST_CURRENT_MATERIAL_ID, TEST_STUDENT_VIEW, TEST_TIMESTAMP);
        DeviceStatusDto dto2 = createTestDeviceStatusDto();

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsAllNullValues() {
        DeviceStatusDto dto1 = new DeviceStatusDto();
        DeviceStatusDto dto2 = new DeviceStatusDto();

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testHashCodeConsistency() {
        DeviceStatusDto dto = createTestDeviceStatusDto();

        int hashCode1 = dto.hashCode();
        int hashCode2 = dto.hashCode();

        assertEquals(hashCode1, hashCode2);
    }

    @Test
    public void testHashCodeEquality() {
        DeviceStatusDto dto1 = createTestDeviceStatusDto();
        DeviceStatusDto dto2 = createTestDeviceStatusDto();

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testHashCodeWithNullValues() {
        DeviceStatusDto dto1 = new DeviceStatusDto();
        DeviceStatusDto dto2 = new DeviceStatusDto();

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testAllStatusValues() {
        // Test ON_TASK
        DeviceStatusDto onTaskDto = new DeviceStatusDto(TEST_DEVICE_ID, "ON_TASK",
                TEST_BATTERY_LEVEL, null, null, TEST_TIMESTAMP);
        assertEquals("ON_TASK", onTaskDto.getStatus());

        // Test IDLE
        DeviceStatusDto idleDto = new DeviceStatusDto(TEST_DEVICE_ID, "IDLE",
                TEST_BATTERY_LEVEL, null, null, TEST_TIMESTAMP);
        assertEquals("IDLE", idleDto.getStatus());

        // Test LOCKED
        DeviceStatusDto lockedDto = new DeviceStatusDto(TEST_DEVICE_ID, "LOCKED",
                TEST_BATTERY_LEVEL, null, null, TEST_TIMESTAMP);
        assertEquals("LOCKED", lockedDto.getStatus());

        // Test DISCONNECTED
        DeviceStatusDto disconnectedDto = new DeviceStatusDto(TEST_DEVICE_ID, "DISCONNECTED",
                TEST_BATTERY_LEVEL, null, null, TEST_TIMESTAMP);
        assertEquals("DISCONNECTED", disconnectedDto.getStatus());
    }

    @Test
    public void testBatteryLevelBoundaryValues() {
        // Test minimum battery level
        DeviceStatusDto minDto = new DeviceStatusDto(TEST_DEVICE_ID, TEST_STATUS, 0,
                TEST_CURRENT_MATERIAL_ID, TEST_STUDENT_VIEW, TEST_TIMESTAMP);
        assertEquals(Integer.valueOf(0), minDto.getBatteryLevel());

        // Test maximum battery level
        DeviceStatusDto maxDto = new DeviceStatusDto(TEST_DEVICE_ID, TEST_STATUS, 100,
                TEST_CURRENT_MATERIAL_ID, TEST_STUDENT_VIEW, TEST_TIMESTAMP);
        assertEquals(Integer.valueOf(100), maxDto.getBatteryLevel());
    }

    @Test
    public void testEqualsNullStatus() {
        DeviceStatusDto dto1 = new DeviceStatusDto(TEST_DEVICE_ID, null, TEST_BATTERY_LEVEL,
                TEST_CURRENT_MATERIAL_ID, TEST_STUDENT_VIEW, TEST_TIMESTAMP);
        DeviceStatusDto dto2 = new DeviceStatusDto(TEST_DEVICE_ID, null, TEST_BATTERY_LEVEL,
                TEST_CURRENT_MATERIAL_ID, TEST_STUDENT_VIEW, TEST_TIMESTAMP);

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullBatteryLevel() {
        DeviceStatusDto dto1 = new DeviceStatusDto(TEST_DEVICE_ID, TEST_STATUS, null,
                TEST_CURRENT_MATERIAL_ID, TEST_STUDENT_VIEW, TEST_TIMESTAMP);
        DeviceStatusDto dto2 = new DeviceStatusDto(TEST_DEVICE_ID, TEST_STATUS, null,
                TEST_CURRENT_MATERIAL_ID, TEST_STUDENT_VIEW, TEST_TIMESTAMP);

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullCurrentMaterialId() {
        DeviceStatusDto dto1 = new DeviceStatusDto(TEST_DEVICE_ID, TEST_STATUS, TEST_BATTERY_LEVEL,
                null, TEST_STUDENT_VIEW, TEST_TIMESTAMP);
        DeviceStatusDto dto2 = new DeviceStatusDto(TEST_DEVICE_ID, TEST_STATUS, TEST_BATTERY_LEVEL,
                null, TEST_STUDENT_VIEW, TEST_TIMESTAMP);

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullStudentView() {
        DeviceStatusDto dto1 = new DeviceStatusDto(TEST_DEVICE_ID, TEST_STATUS, TEST_BATTERY_LEVEL,
                TEST_CURRENT_MATERIAL_ID, null, TEST_TIMESTAMP);
        DeviceStatusDto dto2 = new DeviceStatusDto(TEST_DEVICE_ID, TEST_STATUS, TEST_BATTERY_LEVEL,
                TEST_CURRENT_MATERIAL_ID, null, TEST_TIMESTAMP);

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullTimestamp() {
        DeviceStatusDto dto1 = new DeviceStatusDto(TEST_DEVICE_ID, TEST_STATUS, TEST_BATTERY_LEVEL,
                TEST_CURRENT_MATERIAL_ID, TEST_STUDENT_VIEW, null);
        DeviceStatusDto dto2 = new DeviceStatusDto(TEST_DEVICE_ID, TEST_STATUS, TEST_BATTERY_LEVEL,
                TEST_CURRENT_MATERIAL_ID, TEST_STUDENT_VIEW, null);

        assertTrue(dto1.equals(dto2));
    }

    private DeviceStatusDto createTestDeviceStatusDto() {
        return new DeviceStatusDto(
                TEST_DEVICE_ID,
                TEST_STATUS,
                TEST_BATTERY_LEVEL,
                TEST_CURRENT_MATERIAL_ID,
                TEST_STUDENT_VIEW,
                TEST_TIMESTAMP
        );
    }
}
