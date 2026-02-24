package com.manuscripta.student.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link ConfigResponseDto}.
 * Tests the DTO for tablet configuration responses per API Contract §2.2.
 */
public class ConfigResponseDtoTest {

    private static final Boolean TEST_KIOSK_MODE = true;
    private static final String TEST_TEXT_SIZE = "medium";

    @Test
    public void testDefaultConstructor() {
        ConfigResponseDto dto = new ConfigResponseDto();

        assertNull(dto.getKioskMode());
        assertNull(dto.getTextSize());
    }

    @Test
    public void testConstructorWithAllFields() {
        ConfigResponseDto dto = new ConfigResponseDto(TEST_KIOSK_MODE, TEST_TEXT_SIZE);

        assertEquals(TEST_KIOSK_MODE, dto.getKioskMode());
        assertEquals(TEST_TEXT_SIZE, dto.getTextSize());
    }

    @Test
    public void testConstructorWithNullValues() {
        ConfigResponseDto dto = new ConfigResponseDto(null, null);

        assertNull(dto.getKioskMode());
        assertNull(dto.getTextSize());
    }

    @Test
    public void testSetKioskMode() {
        ConfigResponseDto dto = new ConfigResponseDto();

        dto.setKioskMode(true);
        assertEquals(Boolean.TRUE, dto.getKioskMode());

        dto.setKioskMode(false);
        assertEquals(Boolean.FALSE, dto.getKioskMode());

        dto.setKioskMode(null);
        assertNull(dto.getKioskMode());
    }

    @Test
    public void testSetTextSize() {
        ConfigResponseDto dto = new ConfigResponseDto();

        dto.setTextSize("small");
        assertEquals("small", dto.getTextSize());

        dto.setTextSize("medium");
        assertEquals("medium", dto.getTextSize());

        dto.setTextSize("large");
        assertEquals("large", dto.getTextSize());

        dto.setTextSize(null);
        assertNull(dto.getTextSize());
    }

    @Test
    public void testToString() {
        ConfigResponseDto dto = new ConfigResponseDto(TEST_KIOSK_MODE, TEST_TEXT_SIZE);

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("ConfigResponseDto"));
        assertTrue(result.contains(TEST_KIOSK_MODE.toString()));
        assertTrue(result.contains(TEST_TEXT_SIZE));
    }

    @Test
    public void testToStringWithNullValues() {
        ConfigResponseDto dto = new ConfigResponseDto();

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("ConfigResponseDto"));
        assertTrue(result.contains("null"));
    }

    @Test
    public void testEqualsSameObject() {
        ConfigResponseDto dto = new ConfigResponseDto(TEST_KIOSK_MODE, TEST_TEXT_SIZE);

        assertTrue(dto.equals(dto));
    }

    @Test
    public void testEqualsNull() {
        ConfigResponseDto dto = new ConfigResponseDto(TEST_KIOSK_MODE, TEST_TEXT_SIZE);

        assertFalse(dto.equals(null));
    }

    @Test
    public void testEqualsDifferentClass() {
        ConfigResponseDto dto = new ConfigResponseDto(TEST_KIOSK_MODE, TEST_TEXT_SIZE);

        assertFalse(dto.equals("not a dto"));
    }

    @Test
    public void testEqualsSameValues() {
        ConfigResponseDto dto1 = new ConfigResponseDto(TEST_KIOSK_MODE, TEST_TEXT_SIZE);
        ConfigResponseDto dto2 = new ConfigResponseDto(TEST_KIOSK_MODE, TEST_TEXT_SIZE);

        assertTrue(dto1.equals(dto2));
        assertTrue(dto2.equals(dto1));
    }

    @Test
    public void testEqualsDifferentKioskMode() {
        ConfigResponseDto dto1 = new ConfigResponseDto(true, TEST_TEXT_SIZE);
        ConfigResponseDto dto2 = new ConfigResponseDto(false, TEST_TEXT_SIZE);

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentTextSize() {
        ConfigResponseDto dto1 = new ConfigResponseDto(TEST_KIOSK_MODE, "small");
        ConfigResponseDto dto2 = new ConfigResponseDto(TEST_KIOSK_MODE, "large");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullKioskMode() {
        ConfigResponseDto dto1 = new ConfigResponseDto(null, TEST_TEXT_SIZE);
        ConfigResponseDto dto2 = new ConfigResponseDto(null, TEST_TEXT_SIZE);

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullVsNonNullKioskMode() {
        ConfigResponseDto dto1 = new ConfigResponseDto(null, TEST_TEXT_SIZE);
        ConfigResponseDto dto2 = new ConfigResponseDto(TEST_KIOSK_MODE, TEST_TEXT_SIZE);

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullTextSize() {
        ConfigResponseDto dto1 = new ConfigResponseDto(TEST_KIOSK_MODE, null);
        ConfigResponseDto dto2 = new ConfigResponseDto(TEST_KIOSK_MODE, null);

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullVsNonNullTextSize() {
        ConfigResponseDto dto1 = new ConfigResponseDto(TEST_KIOSK_MODE, null);
        ConfigResponseDto dto2 = new ConfigResponseDto(TEST_KIOSK_MODE, TEST_TEXT_SIZE);

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsAllNullValues() {
        ConfigResponseDto dto1 = new ConfigResponseDto();
        ConfigResponseDto dto2 = new ConfigResponseDto();

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testHashCodeConsistency() {
        ConfigResponseDto dto = new ConfigResponseDto(TEST_KIOSK_MODE, TEST_TEXT_SIZE);

        int hashCode1 = dto.hashCode();
        int hashCode2 = dto.hashCode();

        assertEquals(hashCode1, hashCode2);
    }

    @Test
    public void testHashCodeEquality() {
        ConfigResponseDto dto1 = new ConfigResponseDto(TEST_KIOSK_MODE, TEST_TEXT_SIZE);
        ConfigResponseDto dto2 = new ConfigResponseDto(TEST_KIOSK_MODE, TEST_TEXT_SIZE);

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testHashCodeWithNullValues() {
        ConfigResponseDto dto1 = new ConfigResponseDto();
        ConfigResponseDto dto2 = new ConfigResponseDto();

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testKioskModeEnabled() {
        ConfigResponseDto dto = new ConfigResponseDto(true, "medium");

        assertTrue(dto.getKioskMode());
    }

    @Test
    public void testKioskModeDisabled() {
        ConfigResponseDto dto = new ConfigResponseDto(false, "medium");

        assertFalse(dto.getKioskMode());
    }

    @Test
    public void testAllTextSizeValues() {
        ConfigResponseDto small = new ConfigResponseDto(true, "small");
        ConfigResponseDto medium = new ConfigResponseDto(true, "medium");
        ConfigResponseDto large = new ConfigResponseDto(true, "large");

        assertEquals("small", small.getTextSize());
        assertEquals("medium", medium.getTextSize());
        assertEquals("large", large.getTextSize());
    }
}
