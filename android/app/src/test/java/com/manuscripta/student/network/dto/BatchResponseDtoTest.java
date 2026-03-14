package com.manuscripta.student.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link BatchResponseDto}.
 * Tests construction, getters, setters, utility methods, equals, hashCode, and toString methods.
 */
public class BatchResponseDtoTest {

    @Test
    public void testDefaultConstructor() {
        BatchResponseDto dto = new BatchResponseDto();

        assertNull(dto.getResponses());
        assertEquals(0, dto.size());
        assertTrue(dto.isEmpty());
    }

    @Test
    public void testConstructorWithResponses() {
        List<ResponseDto> responses = createTestResponsesList();

        BatchResponseDto dto = new BatchResponseDto(responses);

        assertEquals(responses, dto.getResponses());
        assertEquals(2, dto.size());
        assertFalse(dto.isEmpty());
    }

    @Test
    public void testConstructorWithNullResponses() {
        BatchResponseDto dto = new BatchResponseDto(null);

        assertNull(dto.getResponses());
        assertEquals(0, dto.size());
        assertTrue(dto.isEmpty());
    }

    @Test
    public void testConstructorWithEmptyList() {
        BatchResponseDto dto = new BatchResponseDto(new ArrayList<>());

        assertNotNull(dto.getResponses());
        assertEquals(0, dto.size());
        assertTrue(dto.isEmpty());
    }

    @Test
    public void testSetResponses() {
        BatchResponseDto dto = new BatchResponseDto();
        List<ResponseDto> responses = createTestResponsesList();

        dto.setResponses(responses);

        assertEquals(responses, dto.getResponses());
        assertEquals(2, dto.size());
    }

    @Test
    public void testSetResponsesNull() {
        BatchResponseDto dto = new BatchResponseDto(createTestResponsesList());

        dto.setResponses(null);

        assertNull(dto.getResponses());
        assertEquals(0, dto.size());
        assertTrue(dto.isEmpty());
    }

    @Test
    public void testAddResponseToNullList() {
        BatchResponseDto dto = new BatchResponseDto();
        ResponseDto response = createTestResponseDto("id-1");

        dto.addResponse(response);

        assertNotNull(dto.getResponses());
        assertEquals(1, dto.size());
        assertEquals(response, dto.getResponses().get(0));
    }

    @Test
    public void testAddResponseToExistingList() {
        List<ResponseDto> initialResponses = new ArrayList<>();
        initialResponses.add(createTestResponseDto("id-1"));
        BatchResponseDto dto = new BatchResponseDto(initialResponses);

        ResponseDto newResponse = createTestResponseDto("id-2");
        dto.addResponse(newResponse);

        assertEquals(2, dto.size());
        assertEquals(newResponse, dto.getResponses().get(1));
    }

    @Test
    public void testAddMultipleResponses() {
        BatchResponseDto dto = new BatchResponseDto();

        dto.addResponse(createTestResponseDto("id-1"));
        dto.addResponse(createTestResponseDto("id-2"));
        dto.addResponse(createTestResponseDto("id-3"));

        assertEquals(3, dto.size());
    }

    @Test
    public void testSizeWithResponses() {
        BatchResponseDto dto = new BatchResponseDto(createTestResponsesList());

        assertEquals(2, dto.size());
    }

    @Test
    public void testSizeWithNullResponses() {
        BatchResponseDto dto = new BatchResponseDto(null);

        assertEquals(0, dto.size());
    }

    @Test
    public void testSizeWithEmptyList() {
        BatchResponseDto dto = new BatchResponseDto(new ArrayList<>());

        assertEquals(0, dto.size());
    }

    @Test
    public void testIsEmptyWithResponses() {
        BatchResponseDto dto = new BatchResponseDto(createTestResponsesList());

        assertFalse(dto.isEmpty());
    }

    @Test
    public void testIsEmptyWithNullResponses() {
        BatchResponseDto dto = new BatchResponseDto(null);

        assertTrue(dto.isEmpty());
    }

    @Test
    public void testIsEmptyWithEmptyList() {
        BatchResponseDto dto = new BatchResponseDto(new ArrayList<>());

        assertTrue(dto.isEmpty());
    }

    @Test
    public void testToString() {
        BatchResponseDto dto = new BatchResponseDto(createTestResponsesList());

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("BatchResponseDto"));
        assertTrue(result.contains("responses="));
    }

    @Test
    public void testToStringWithNullResponses() {
        BatchResponseDto dto = new BatchResponseDto();

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("BatchResponseDto"));
        assertTrue(result.contains("null"));
    }

    @Test
    public void testEqualsSameObject() {
        BatchResponseDto dto = new BatchResponseDto(createTestResponsesList());

        assertTrue(dto.equals(dto));
    }

    @Test
    public void testEqualsNull() {
        BatchResponseDto dto = new BatchResponseDto(createTestResponsesList());

        assertFalse(dto.equals(null));
    }

    @Test
    public void testEqualsDifferentClass() {
        BatchResponseDto dto = new BatchResponseDto(createTestResponsesList());

        assertFalse(dto.equals("not a dto"));
    }

    @Test
    public void testEqualsSameValues() {
        BatchResponseDto dto1 = new BatchResponseDto(createTestResponsesList());
        BatchResponseDto dto2 = new BatchResponseDto(createTestResponsesList());

        assertTrue(dto1.equals(dto2));
        assertTrue(dto2.equals(dto1));
    }

    @Test
    public void testEqualsDifferentResponses() {
        BatchResponseDto dto1 = new BatchResponseDto(createTestResponsesList());
        BatchResponseDto dto2 = new BatchResponseDto(Arrays.asList(createTestResponseDto("different-id")));

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullVsNonNullResponses() {
        BatchResponseDto dto1 = new BatchResponseDto(null);
        BatchResponseDto dto2 = new BatchResponseDto(createTestResponsesList());

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsBothNullResponses() {
        BatchResponseDto dto1 = new BatchResponseDto(null);
        BatchResponseDto dto2 = new BatchResponseDto(null);

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testEqualsEmptyLists() {
        BatchResponseDto dto1 = new BatchResponseDto(new ArrayList<>());
        BatchResponseDto dto2 = new BatchResponseDto(new ArrayList<>());

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testHashCodeConsistency() {
        BatchResponseDto dto = new BatchResponseDto(createTestResponsesList());

        int hashCode1 = dto.hashCode();
        int hashCode2 = dto.hashCode();

        assertEquals(hashCode1, hashCode2);
    }

    @Test
    public void testHashCodeEquality() {
        BatchResponseDto dto1 = new BatchResponseDto(createTestResponsesList());
        BatchResponseDto dto2 = new BatchResponseDto(createTestResponsesList());

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testHashCodeWithNullResponses() {
        BatchResponseDto dto1 = new BatchResponseDto(null);
        BatchResponseDto dto2 = new BatchResponseDto(null);

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testHashCodeDifferentResponses() {
        BatchResponseDto dto1 = new BatchResponseDto(createTestResponsesList());
        BatchResponseDto dto2 = new BatchResponseDto(Arrays.asList(createTestResponseDto("different-id")));

        assertNotEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testBatchSubmissionScenario() {
        // Simulate reconnecting after offline mode with multiple responses
        BatchResponseDto dto = new BatchResponseDto();

        // Add responses collected while offline
        dto.addResponse(new ResponseDto(
                "offline-resp-1",
                "q-1",
                "mat-1",
                "device-123",
                "A",
                "2023-10-27T10:05:00Z",
                null
        ));
        dto.addResponse(new ResponseDto(
                "offline-resp-2",
                "q-2",
                "mat-1",
                "device-123",
                "B",
                "2023-10-27T10:06:00Z",
                null
        ));
        dto.addResponse(new ResponseDto(
                "offline-resp-3",
                "q-3",
                "mat-1",
                "device-123",
                "42",
                "2023-10-27T10:07:00Z",
                null
        ));

        assertEquals(3, dto.size());
        assertFalse(dto.isEmpty());
    }

    @Test
    public void testLargeBatch() {
        // Test with a larger number of responses
        BatchResponseDto dto = new BatchResponseDto();

        for (int i = 0; i < 100; i++) {
            dto.addResponse(createTestResponseDto("resp-" + i));
        }

        assertEquals(100, dto.size());
    }

    private List<ResponseDto> createTestResponsesList() {
        return Arrays.asList(
                createTestResponseDto("resp-uuid-1"),
                createTestResponseDto("resp-uuid-2")
        );
    }

    private ResponseDto createTestResponseDto(String id) {
        return new ResponseDto(
                id,
                "q-uuid-123",
                "mat-uuid-456",
                "device-uuid-789",
                "3",
                "2023-10-27T10:05:00Z",
                true
        );
    }
}
