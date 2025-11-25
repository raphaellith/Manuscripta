package com.manuscripta.student.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
                MaterialType.LESSON,
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
        assertEquals(MaterialType.LESSON, material.getType());
        assertEquals("Test Title", material.getTitle());
        assertEquals("Test Content", material.getContent());
        assertEquals("{\"meta\":\"data\"}", material.getMetadata());
        assertEquals("[\"vocab1\",\"vocab2\"]", material.getVocabularyTerms());
        assertEquals(1234567890L, material.getTimestamp());
    }
}
