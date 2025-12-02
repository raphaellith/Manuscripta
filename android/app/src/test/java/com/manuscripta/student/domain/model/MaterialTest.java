package com.manuscripta.student.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import com.manuscripta.student.data.model.MaterialType;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link Material} domain model.
 */
public class MaterialTest {
    private Material material;

    @Before
    public void setUp() {
        this.material = new Material(
                "test-id",
                MaterialType.READING,
                "Test Title",
                "Test Content",
                "{\"meta\":\"data\"}",
                "[\"vocab1\",\"vocab2\"]",
                1234567890L
        );
    }

    @Test
    public void testConstructorAndGetters() {
        assertNotNull(material);
        assertEquals("test-id", material.getId());
        assertEquals(MaterialType.READING, material.getType());
        assertEquals("Test Title", material.getTitle());
        assertEquals("Test Content", material.getContent());
        assertEquals("{\"meta\":\"data\"}", material.getMetadata());
        assertEquals("[\"vocab1\",\"vocab2\"]", material.getVocabularyTerms());
        assertEquals(1234567890L, material.getTimestamp());
    }

    @Test
    public void testConstructorWithEmptyContent() {
        // Empty content is valid (e.g., placeholder material)
        Material mat = new Material(
                "id",
                MaterialType.QUIZ,
                "Title",
                "",
                "{}",
                "[]",
                0L
        );
        assertEquals("", mat.getContent());
    }

    @Test
    public void testConstructorWithEmptyMetadataAndVocabulary() {
        // Empty metadata and vocabulary are valid
        Material mat = new Material(
                "id",
                MaterialType.WORKSHEET,
                "Title",
                "Content",
                "",
                "",
                100L
        );
        assertEquals("", mat.getMetadata());
        assertEquals("", mat.getVocabularyTerms());
    }

    @Test
    public void testConstructorWithZeroTimestamp() {
        // Zero timestamp is valid (e.g., draft material not yet timestamped)
        Material mat = new Material(
                "id",
                MaterialType.POLL,
                "Title",
                "Content",
                "{}",
                "[]",
                0L
        );
        assertEquals(0L, mat.getTimestamp());
    }

    @Test
    public void testConstructor_nullId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Material(
                        null,
                        MaterialType.READING,
                        "Title",
                        "Content",
                        "{}",
                        "[]",
                        0L
                )
        );
        assertEquals("Material id cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_emptyId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Material(
                        "",
                        MaterialType.READING,
                        "Title",
                        "Content",
                        "{}",
                        "[]",
                        0L
                )
        );
        assertEquals("Material id cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_blankId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Material(
                        "   ",
                        MaterialType.READING,
                        "Title",
                        "Content",
                        "{}",
                        "[]",
                        0L
                )
        );
        assertEquals("Material id cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_nullType_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Material(
                        "id",
                        null,
                        "Title",
                        "Content",
                        "{}",
                        "[]",
                        0L
                )
        );
        assertEquals("Material type cannot be null", exception.getMessage());
    }

    @Test
    public void testConstructor_nullTitle_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Material(
                        "id",
                        MaterialType.READING,
                        null,
                        "Content",
                        "{}",
                        "[]",
                        0L
                )
        );
        assertEquals("Material title cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_emptyTitle_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Material(
                        "id",
                        MaterialType.READING,
                        "",
                        "Content",
                        "{}",
                        "[]",
                        0L
                )
        );
        assertEquals("Material title cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_blankTitle_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Material(
                        "id",
                        MaterialType.READING,
                        "   ",
                        "Content",
                        "{}",
                        "[]",
                        0L
                )
        );
        assertEquals("Material title cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_nullContent_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Material(
                        "id",
                        MaterialType.READING,
                        "Title",
                        null,
                        "{}",
                        "[]",
                        0L
                )
        );
        assertEquals("Material content cannot be null", exception.getMessage());
    }

    @Test
    public void testConstructor_nullMetadata_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Material(
                        "id",
                        MaterialType.READING,
                        "Title",
                        "Content",
                        null,
                        "[]",
                        0L
                )
        );
        assertEquals("Material metadata cannot be null", exception.getMessage());
    }

    @Test
    public void testConstructor_nullVocabularyTerms_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Material(
                        "id",
                        MaterialType.READING,
                        "Title",
                        "Content",
                        "{}",
                        null,
                        0L
                )
        );
        assertEquals("Material vocabularyTerms cannot be null", exception.getMessage());
    }

    @Test
    public void testConstructor_negativeTimestamp_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Material(
                        "id",
                        MaterialType.READING,
                        "Title",
                        "Content",
                        "{}",
                        "[]",
                        -1L
                )
        );
        assertEquals("Material timestamp cannot be negative", exception.getMessage());
    }
}
