package com.manuscripta.student.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link MaterialListResponseDto}.
 * Tests construction, getters, setters, equals, hashCode, and toString methods.
 */
public class MaterialListResponseDtoTest {

    @Test
    public void testDefaultConstructor() {
        MaterialListResponseDto dto = new MaterialListResponseDto();

        assertNull(dto.getMaterialIds());
        assertNull(dto.getMaterials());
        assertNull(dto.getTotalCount());
    }

    @Test
    public void testConstructorWithAllFields() {
        List<String> materialIds = Arrays.asList("id-1", "id-2", "id-3");
        List<MaterialDto> materials = createTestMaterials();
        Integer totalCount = 10;

        MaterialListResponseDto dto = new MaterialListResponseDto(materialIds, materials, totalCount);

        assertEquals(materialIds, dto.getMaterialIds());
        assertEquals(materials, dto.getMaterials());
        assertEquals(totalCount, dto.getTotalCount());
    }

    @Test
    public void testConstructorWithNullValues() {
        MaterialListResponseDto dto = new MaterialListResponseDto(null, null, null);

        assertNull(dto.getMaterialIds());
        assertNull(dto.getMaterials());
        assertNull(dto.getTotalCount());
    }

    @Test
    public void testSetMaterialIds() {
        MaterialListResponseDto dto = new MaterialListResponseDto();
        List<String> materialIds = Arrays.asList("id-1", "id-2");

        dto.setMaterialIds(materialIds);
        assertEquals(materialIds, dto.getMaterialIds());

        dto.setMaterialIds(null);
        assertNull(dto.getMaterialIds());
    }

    @Test
    public void testSetMaterials() {
        MaterialListResponseDto dto = new MaterialListResponseDto();
        List<MaterialDto> materials = createTestMaterials();

        dto.setMaterials(materials);
        assertEquals(materials, dto.getMaterials());

        dto.setMaterials(null);
        assertNull(dto.getMaterials());
    }

    @Test
    public void testSetTotalCount() {
        MaterialListResponseDto dto = new MaterialListResponseDto();

        dto.setTotalCount(25);
        assertEquals(Integer.valueOf(25), dto.getTotalCount());

        dto.setTotalCount(null);
        assertNull(dto.getTotalCount());
    }

    @Test
    public void testToString() {
        List<String> materialIds = Arrays.asList("id-1", "id-2");
        MaterialListResponseDto dto = new MaterialListResponseDto(materialIds, null, 2);

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("MaterialListResponseDto"));
        assertTrue(result.contains("id-1"));
        assertTrue(result.contains("id-2"));
    }

    @Test
    public void testToStringWithNullValues() {
        MaterialListResponseDto dto = new MaterialListResponseDto();

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("MaterialListResponseDto"));
        assertTrue(result.contains("null"));
    }

    @Test
    public void testEqualsSameObject() {
        MaterialListResponseDto dto = createTestDto();

        assertTrue(dto.equals(dto));
    }

    @Test
    public void testEqualsNull() {
        MaterialListResponseDto dto = createTestDto();

        assertFalse(dto.equals(null));
    }

    @Test
    public void testEqualsDifferentClass() {
        MaterialListResponseDto dto = createTestDto();

        assertFalse(dto.equals("not a dto"));
    }

    @Test
    public void testEqualsSameValues() {
        MaterialListResponseDto dto1 = createTestDto();
        MaterialListResponseDto dto2 = createTestDto();

        assertTrue(dto1.equals(dto2));
        assertTrue(dto2.equals(dto1));
    }

    @Test
    public void testEqualsDifferentMaterialIds() {
        MaterialListResponseDto dto1 = createTestDto();
        MaterialListResponseDto dto2 = createTestDto();
        dto2.setMaterialIds(Arrays.asList("different-id"));

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentMaterials() {
        MaterialListResponseDto dto1 = createTestDto();
        MaterialListResponseDto dto2 = createTestDto();
        dto2.setMaterials(Arrays.asList(new MaterialDto("diff", "QUIZ", "Diff", "", "", null, 0L)));

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentTotalCount() {
        MaterialListResponseDto dto1 = createTestDto();
        MaterialListResponseDto dto2 = createTestDto();
        dto2.setTotalCount(999);

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullMaterialIds() {
        MaterialListResponseDto dto1 = new MaterialListResponseDto(null, createTestMaterials(), 2);
        MaterialListResponseDto dto2 = new MaterialListResponseDto(null, createTestMaterials(), 2);

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullVsNonNullMaterialIds() {
        MaterialListResponseDto dto1 = new MaterialListResponseDto(null, createTestMaterials(), 2);
        MaterialListResponseDto dto2 = createTestDto();

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsAllNullValues() {
        MaterialListResponseDto dto1 = new MaterialListResponseDto();
        MaterialListResponseDto dto2 = new MaterialListResponseDto();

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullVsNonNullMaterials() {
        MaterialListResponseDto dto1 = new MaterialListResponseDto(
                Arrays.asList("id-1"), null, 1);
        MaterialListResponseDto dto2 = new MaterialListResponseDto(
                Arrays.asList("id-1"), createTestMaterials(), 1);

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullVsNonNullTotalCount() {
        MaterialListResponseDto dto1 = new MaterialListResponseDto(
                Arrays.asList("id-1"), null, null);
        MaterialListResponseDto dto2 = new MaterialListResponseDto(
                Arrays.asList("id-1"), null, 5);

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testHashCodeConsistency() {
        MaterialListResponseDto dto = createTestDto();

        int hashCode1 = dto.hashCode();
        int hashCode2 = dto.hashCode();

        assertEquals(hashCode1, hashCode2);
    }

    @Test
    public void testHashCodeEquality() {
        MaterialListResponseDto dto1 = createTestDto();
        MaterialListResponseDto dto2 = createTestDto();

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testHashCodeWithNullValues() {
        MaterialListResponseDto dto1 = new MaterialListResponseDto();
        MaterialListResponseDto dto2 = new MaterialListResponseDto();

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testOrderPreservation() {
        // Test that the order of material IDs is preserved
        List<String> orderedIds = Arrays.asList("first-id", "second-id", "third-id");
        MaterialListResponseDto dto = new MaterialListResponseDto(orderedIds, null, 3);

        List<String> retrievedIds = dto.getMaterialIds();
        assertEquals("first-id", retrievedIds.get(0));
        assertEquals("second-id", retrievedIds.get(1));
        assertEquals("third-id", retrievedIds.get(2));
    }

    @Test
    public void testEmptyLists() {
        MaterialListResponseDto dto = new MaterialListResponseDto(
                new ArrayList<>(),
                new ArrayList<>(),
                0
        );

        assertNotNull(dto.getMaterialIds());
        assertTrue(dto.getMaterialIds().isEmpty());
        assertNotNull(dto.getMaterials());
        assertTrue(dto.getMaterials().isEmpty());
        assertEquals(Integer.valueOf(0), dto.getTotalCount());
    }

    @Test
    public void testPaginationScenario() {
        // Simulate a paginated response where totalCount > returned items
        List<String> materialIds = Arrays.asList("id-1", "id-2", "id-3");
        MaterialListResponseDto dto = new MaterialListResponseDto(materialIds, null, 100);

        assertEquals(3, dto.getMaterialIds().size());
        assertEquals(Integer.valueOf(100), dto.getTotalCount());
    }

    private MaterialListResponseDto createTestDto() {
        return new MaterialListResponseDto(
                Arrays.asList("id-1", "id-2"),
                createTestMaterials(),
                2
        );
    }

    private List<MaterialDto> createTestMaterials() {
        return Arrays.asList(
                new MaterialDto("id-1", "QUIZ", "Quiz 1", "Content 1", "{}", null, 1000L),
                new MaterialDto("id-2", "READING", "Reading 1", "Content 2", "{}", null, 2000L)
        );
    }
}
