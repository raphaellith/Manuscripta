package com.manuscripta.student.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link DeviceInfoDto}.
 * Tests construction, getters, setters, equals, hashCode, and toString methods.
 */
public class DeviceInfoDtoTest {

    private static final String TEST_DEVICE_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_NAME = "Student Tablet 1";

    @Test
    public void testDefaultConstructor() {
        DeviceInfoDto dto = new DeviceInfoDto();

        assertNull(dto.getDeviceId());
        assertNull(dto.getName());
    }

    @Test
    public void testConstructorWithAllFields() {
        DeviceInfoDto dto = new DeviceInfoDto(TEST_DEVICE_ID, TEST_NAME);

        assertEquals(TEST_DEVICE_ID, dto.getDeviceId());
        assertEquals(TEST_NAME, dto.getName());
    }

    @Test
    public void testConstructorWithNullValues() {
        DeviceInfoDto dto = new DeviceInfoDto(null, null);

        assertNull(dto.getDeviceId());
        assertNull(dto.getName());
    }

    @Test
    public void testSetDeviceId() {
        DeviceInfoDto dto = new DeviceInfoDto();

        dto.setDeviceId(TEST_DEVICE_ID);
        assertEquals(TEST_DEVICE_ID, dto.getDeviceId());

        dto.setDeviceId(null);
        assertNull(dto.getDeviceId());
    }

    @Test
    public void testSetName() {
        DeviceInfoDto dto = new DeviceInfoDto();

        dto.setName(TEST_NAME);
        assertEquals(TEST_NAME, dto.getName());

        dto.setName("Different Name");
        assertEquals("Different Name", dto.getName());

        dto.setName(null);
        assertNull(dto.getName());
    }

    @Test
    public void testToString() {
        DeviceInfoDto dto = new DeviceInfoDto(TEST_DEVICE_ID, TEST_NAME);

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("DeviceInfoDto"));
        assertTrue(result.contains(TEST_DEVICE_ID));
        assertTrue(result.contains(TEST_NAME));
    }

    @Test
    public void testToStringWithNullValues() {
        DeviceInfoDto dto = new DeviceInfoDto();

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("DeviceInfoDto"));
        assertTrue(result.contains("null"));
    }

    @Test
    public void testEqualsSameObject() {
        DeviceInfoDto dto = new DeviceInfoDto(TEST_DEVICE_ID, TEST_NAME);

        assertTrue(dto.equals(dto));
    }

    @Test
    public void testEqualsNull() {
        DeviceInfoDto dto = new DeviceInfoDto(TEST_DEVICE_ID, TEST_NAME);

        assertFalse(dto.equals(null));
    }

    @Test
    public void testEqualsDifferentClass() {
        DeviceInfoDto dto = new DeviceInfoDto(TEST_DEVICE_ID, TEST_NAME);

        assertFalse(dto.equals("not a dto"));
    }

    @Test
    public void testEqualsSameValues() {
        DeviceInfoDto dto1 = new DeviceInfoDto(TEST_DEVICE_ID, TEST_NAME);
        DeviceInfoDto dto2 = new DeviceInfoDto(TEST_DEVICE_ID, TEST_NAME);

        assertTrue(dto1.equals(dto2));
        assertTrue(dto2.equals(dto1));
    }

    @Test
    public void testEqualsDifferentDeviceId() {
        DeviceInfoDto dto1 = new DeviceInfoDto(TEST_DEVICE_ID, TEST_NAME);
        DeviceInfoDto dto2 = new DeviceInfoDto("different-id", TEST_NAME);

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentName() {
        DeviceInfoDto dto1 = new DeviceInfoDto(TEST_DEVICE_ID, TEST_NAME);
        DeviceInfoDto dto2 = new DeviceInfoDto(TEST_DEVICE_ID, "Different Name");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullDeviceId() {
        DeviceInfoDto dto1 = new DeviceInfoDto(null, TEST_NAME);
        DeviceInfoDto dto2 = new DeviceInfoDto(null, TEST_NAME);

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullName() {
        DeviceInfoDto dto1 = new DeviceInfoDto(TEST_DEVICE_ID, null);
        DeviceInfoDto dto2 = new DeviceInfoDto(TEST_DEVICE_ID, null);

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullVsNonNullDeviceId() {
        DeviceInfoDto dto1 = new DeviceInfoDto(null, TEST_NAME);
        DeviceInfoDto dto2 = new DeviceInfoDto(TEST_DEVICE_ID, TEST_NAME);

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullVsNonNullName() {
        DeviceInfoDto dto1 = new DeviceInfoDto(TEST_DEVICE_ID, null);
        DeviceInfoDto dto2 = new DeviceInfoDto(TEST_DEVICE_ID, TEST_NAME);

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsAllNullValues() {
        DeviceInfoDto dto1 = new DeviceInfoDto();
        DeviceInfoDto dto2 = new DeviceInfoDto();

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testHashCodeConsistency() {
        DeviceInfoDto dto = new DeviceInfoDto(TEST_DEVICE_ID, TEST_NAME);

        int hashCode1 = dto.hashCode();
        int hashCode2 = dto.hashCode();

        assertEquals(hashCode1, hashCode2);
    }

    @Test
    public void testHashCodeEquality() {
        DeviceInfoDto dto1 = new DeviceInfoDto(TEST_DEVICE_ID, TEST_NAME);
        DeviceInfoDto dto2 = new DeviceInfoDto(TEST_DEVICE_ID, TEST_NAME);

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testHashCodeWithNullValues() {
        DeviceInfoDto dto1 = new DeviceInfoDto();
        DeviceInfoDto dto2 = new DeviceInfoDto();

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testPreservesDeviceId() {
        // Test that Android-generated device IDs are preserved exactly
        String androidGeneratedId = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
        DeviceInfoDto dto = new DeviceInfoDto(androidGeneratedId, TEST_NAME);

        assertEquals(androidGeneratedId, dto.getDeviceId());
    }

    @Test
    public void testEmptyName() {
        DeviceInfoDto dto = new DeviceInfoDto(TEST_DEVICE_ID, "");

        assertEquals("", dto.getName());
    }

    @Test
    public void testNameWithSpecialCharacters() {
        String specialName = "Student's Tablet (John) #1";
        DeviceInfoDto dto = new DeviceInfoDto(TEST_DEVICE_ID, specialName);

        assertEquals(specialName, dto.getName());
    }

    @Test
    public void testNameWithUnicodeCharacters() {
        String unicodeName = "学生平板电脑 1";
        DeviceInfoDto dto = new DeviceInfoDto(TEST_DEVICE_ID, unicodeName);

        assertEquals(unicodeName, dto.getName());
    }
}
