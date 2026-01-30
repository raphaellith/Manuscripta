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
 * Unit tests for {@link MaterialDto}.
 * Tests construction, getters, setters, equals, hashCode, and toString methods.
 */
public class MaterialDtoTest {

    private static final String TEST_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_TYPE = "QUIZ";
    private static final String TEST_TITLE = "Math Quiz Chapter 5";
    private static final String TEST_CONTENT = "Question 1: What is 2+2?";
    private static final String TEST_METADATA = "{\"author\":\"Teacher\",\"subject\":\"Math\"}";
    private static final Long TEST_TIMESTAMP = 1234567890L;

    @Test
    public void testDefaultConstructor() {
        MaterialDto dto = new MaterialDto();

        assertNull(dto.getId());
        assertNull(dto.getType());
        assertNull(dto.getTitle());
        assertNull(dto.getContent());
        assertNull(dto.getMetadata());
        assertNull(dto.getVocabularyTerms());
        assertNull(dto.getTimestamp());
    }

    @Test
    public void testConstructorWithAllFields() {
        List<VocabularyTermDto> vocabTerms = createTestVocabularyTerms();

        MaterialDto dto = new MaterialDto(
                TEST_ID,
                TEST_TYPE,
                TEST_TITLE,
                TEST_CONTENT,
                TEST_METADATA,
                vocabTerms,
                TEST_TIMESTAMP
        );

        assertEquals(TEST_ID, dto.getId());
        assertEquals(TEST_TYPE, dto.getType());
        assertEquals(TEST_TITLE, dto.getTitle());
        assertEquals(TEST_CONTENT, dto.getContent());
        assertEquals(TEST_METADATA, dto.getMetadata());
        assertEquals(vocabTerms, dto.getVocabularyTerms());
        assertEquals(TEST_TIMESTAMP, dto.getTimestamp());
    }

    @Test
    public void testConstructorWithNullValues() {
        MaterialDto dto = new MaterialDto(null, null, null, null, null, null, null);

        assertNull(dto.getId());
        assertNull(dto.getType());
        assertNull(dto.getTitle());
        assertNull(dto.getContent());
        assertNull(dto.getMetadata());
        assertNull(dto.getVocabularyTerms());
        assertNull(dto.getTimestamp());
    }

    @Test
    public void testSetId() {
        MaterialDto dto = new MaterialDto();

        dto.setId(TEST_ID);
        assertEquals(TEST_ID, dto.getId());

        dto.setId(null);
        assertNull(dto.getId());
    }

    @Test
    public void testSetType() {
        MaterialDto dto = new MaterialDto();

        dto.setType(TEST_TYPE);
        assertEquals(TEST_TYPE, dto.getType());

        dto.setType(null);
        assertNull(dto.getType());
    }

    @Test
    public void testSetTitle() {
        MaterialDto dto = new MaterialDto();

        dto.setTitle(TEST_TITLE);
        assertEquals(TEST_TITLE, dto.getTitle());

        dto.setTitle(null);
        assertNull(dto.getTitle());
    }

    @Test
    public void testSetContent() {
        MaterialDto dto = new MaterialDto();

        dto.setContent(TEST_CONTENT);
        assertEquals(TEST_CONTENT, dto.getContent());

        dto.setContent(null);
        assertNull(dto.getContent());
    }

    @Test
    public void testSetMetadata() {
        MaterialDto dto = new MaterialDto();

        dto.setMetadata(TEST_METADATA);
        assertEquals(TEST_METADATA, dto.getMetadata());

        dto.setMetadata(null);
        assertNull(dto.getMetadata());
    }

    @Test
    public void testSetVocabularyTerms() {
        MaterialDto dto = new MaterialDto();
        List<VocabularyTermDto> vocabTerms = createTestVocabularyTerms();

        dto.setVocabularyTerms(vocabTerms);
        assertEquals(vocabTerms, dto.getVocabularyTerms());

        dto.setVocabularyTerms(null);
        assertNull(dto.getVocabularyTerms());
    }

    @Test
    public void testSetTimestamp() {
        MaterialDto dto = new MaterialDto();

        dto.setTimestamp(TEST_TIMESTAMP);
        assertEquals(TEST_TIMESTAMP, dto.getTimestamp());

        dto.setTimestamp(null);
        assertNull(dto.getTimestamp());
    }

    @Test
    public void testToString() {
        MaterialDto dto = new MaterialDto(
                TEST_ID,
                TEST_TYPE,
                TEST_TITLE,
                TEST_CONTENT,
                TEST_METADATA,
                createTestVocabularyTerms(),
                TEST_TIMESTAMP
        );

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("MaterialDto"));
        assertTrue(result.contains(TEST_ID));
        assertTrue(result.contains(TEST_TYPE));
        assertTrue(result.contains(TEST_TITLE));
    }

    @Test
    public void testToStringWithNullValues() {
        MaterialDto dto = new MaterialDto();

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("MaterialDto"));
        assertTrue(result.contains("null"));
    }

    @Test
    public void testEqualsSameObject() {
        MaterialDto dto = createTestMaterialDto();

        assertTrue(dto.equals(dto));
    }

    @Test
    public void testEqualsNull() {
        MaterialDto dto = createTestMaterialDto();

        assertFalse(dto.equals(null));
    }

    @Test
    public void testEqualsDifferentClass() {
        MaterialDto dto = createTestMaterialDto();

        assertFalse(dto.equals("not a dto"));
    }

    @Test
    public void testEqualsSameValues() {
        MaterialDto dto1 = createTestMaterialDto();
        MaterialDto dto2 = createTestMaterialDto();

        assertTrue(dto1.equals(dto2));
        assertTrue(dto2.equals(dto1));
    }

    @Test
    public void testEqualsDifferentId() {
        MaterialDto dto1 = createTestMaterialDto();
        MaterialDto dto2 = createTestMaterialDto();
        dto2.setId("different-id");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentType() {
        MaterialDto dto1 = createTestMaterialDto();
        MaterialDto dto2 = createTestMaterialDto();
        dto2.setType("READING");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentTitle() {
        MaterialDto dto1 = createTestMaterialDto();
        MaterialDto dto2 = createTestMaterialDto();
        dto2.setTitle("Different Title");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentContent() {
        MaterialDto dto1 = createTestMaterialDto();
        MaterialDto dto2 = createTestMaterialDto();
        dto2.setContent("Different content");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentMetadata() {
        MaterialDto dto1 = createTestMaterialDto();
        MaterialDto dto2 = createTestMaterialDto();
        dto2.setMetadata("{\"different\":\"metadata\"}");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentVocabularyTerms() {
        MaterialDto dto1 = createTestMaterialDto();
        MaterialDto dto2 = createTestMaterialDto();
        dto2.setVocabularyTerms(Arrays.asList(new VocabularyTermDto("different", "term")));

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentTimestamp() {
        MaterialDto dto1 = createTestMaterialDto();
        MaterialDto dto2 = createTestMaterialDto();
        dto2.setTimestamp(9999999999L);

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullId() {
        MaterialDto dto1 = new MaterialDto(null, TEST_TYPE, TEST_TITLE, TEST_CONTENT,
                TEST_METADATA, null, TEST_TIMESTAMP);
        MaterialDto dto2 = new MaterialDto(null, TEST_TYPE, TEST_TITLE, TEST_CONTENT,
                TEST_METADATA, null, TEST_TIMESTAMP);

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullVsNonNullId() {
        MaterialDto dto1 = new MaterialDto(null, TEST_TYPE, TEST_TITLE, TEST_CONTENT,
                TEST_METADATA, null, TEST_TIMESTAMP);
        MaterialDto dto2 = createTestMaterialDto();

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsAllNullValues() {
        MaterialDto dto1 = new MaterialDto();
        MaterialDto dto2 = new MaterialDto();

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testHashCodeConsistency() {
        MaterialDto dto = createTestMaterialDto();

        int hashCode1 = dto.hashCode();
        int hashCode2 = dto.hashCode();

        assertEquals(hashCode1, hashCode2);
    }

    @Test
    public void testHashCodeEquality() {
        MaterialDto dto1 = createTestMaterialDto();
        MaterialDto dto2 = createTestMaterialDto();

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testHashCodeWithNullValues() {
        MaterialDto dto1 = new MaterialDto();
        MaterialDto dto2 = new MaterialDto();

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testContentWithAttachmentReferences() {
        String contentWithAttachments = "Read the PDF: /attachments/doc-123 and image: /attachments/img-456";
        MaterialDto dto = new MaterialDto(
                TEST_ID,
                "READING",
                "Reading Material",
                contentWithAttachments,
                TEST_METADATA,
                null,
                TEST_TIMESTAMP
        );

        assertEquals(contentWithAttachments, dto.getContent());
    }

    @Test
    public void testEmptyVocabularyTermsList() {
        MaterialDto dto = new MaterialDto(
                TEST_ID,
                TEST_TYPE,
                TEST_TITLE,
                TEST_CONTENT,
                TEST_METADATA,
                new ArrayList<>(),
                TEST_TIMESTAMP
        );

        assertNotNull(dto.getVocabularyTerms());
        assertTrue(dto.getVocabularyTerms().isEmpty());
    }

    @Test
    public void testPreservesOriginalId() {
        // Test that Windows teacher app IDs are preserved exactly
        String windowsGeneratedId = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
        MaterialDto dto = new MaterialDto(
                windowsGeneratedId,
                TEST_TYPE,
                TEST_TITLE,
                TEST_CONTENT,
                TEST_METADATA,
                null,
                TEST_TIMESTAMP
        );

        assertEquals(windowsGeneratedId, dto.getId());
    }

    private MaterialDto createTestMaterialDto() {
        return new MaterialDto(
                TEST_ID,
                TEST_TYPE,
                TEST_TITLE,
                TEST_CONTENT,
                TEST_METADATA,
                createTestVocabularyTerms(),
                TEST_TIMESTAMP
        );
    }

    private List<VocabularyTermDto> createTestVocabularyTerms() {
        return Arrays.asList(
                new VocabularyTermDto("algebra", "A branch of mathematics"),
                new VocabularyTermDto("equation", "A mathematical statement")
        );
    }
}
