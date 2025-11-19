package com.manuscripta.student.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link MaterialEntity} entity.
 */
public class MaterialEntityTest {
    private MaterialEntity materialEntity;

    @Before
    public void setUp() {
        this.materialEntity = new MaterialEntity(
                "id",
                MaterialType.LESSON,
                "Title",
                "Content",
                "Metadata",
                "Vocab",
                1L
        );
    }

    @Test
    public void testConstructorAndGetters() {
        assertEquals("id", materialEntity.getId());
        assertEquals(MaterialType.LESSON, materialEntity.getType());
        assertEquals("Title", materialEntity.getTitle());
        assertEquals("Content", materialEntity.getContent());
        assertEquals("Metadata", materialEntity.getMetadata());
        assertEquals("Vocab", materialEntity.getVocabularyTerms());
        assertEquals(1L, materialEntity.getTimestamp());
    }
}
