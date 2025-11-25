package com.manuscripta.student.domain.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.manuscripta.student.data.model.MaterialEntity;
import com.manuscripta.student.data.model.MaterialType;
import com.manuscripta.student.domain.model.Material;

import org.junit.Test;

/**
 * Unit tests for {@link MaterialMapper}.
 * Tests bidirectional mapping between MaterialEntity and Material domain model.
 */
public class MaterialMapperTest {

    @Test
    public void testToDomain() {
        // Given
        MaterialEntity entity = new MaterialEntity(
                "test-id-123",
                MaterialType.QUIZ,
                "Test Quiz Title",
                "Test quiz content with questions",
                "{\"author\":\"Teacher\",\"subject\":\"Math\"}",
                "[\"algebra\",\"geometry\"]",
                1234567890L
        );

        // When
        Material domain = MaterialMapper.toDomain(entity);

        // Then
        assertNotNull(domain);
        assertEquals(entity.getId(), domain.getId());
        assertEquals(entity.getType(), domain.getType());
        assertEquals(entity.getTitle(), domain.getTitle());
        assertEquals(entity.getContent(), domain.getContent());
        assertEquals(entity.getMetadata(), domain.getMetadata());
        assertEquals(entity.getVocabularyTerms(), domain.getVocabularyTerms());
        assertEquals(entity.getTimestamp(), domain.getTimestamp());
    }

    @Test
    public void testToEntity() {
        // Given
        Material domain = new Material(
                "test-id-456",
                MaterialType.LESSON,
                "Test Lesson Title",
                "Test lesson content about science",
                "{\"author\":\"Professor\",\"subject\":\"Science\"}",
                "[\"physics\",\"chemistry\"]",
                9876543210L
        );

        // When
        MaterialEntity entity = MaterialMapper.toEntity(domain);

        // Then
        assertNotNull(entity);
        assertEquals(domain.getId(), entity.getId());
        assertEquals(domain.getType(), entity.getType());
        assertEquals(domain.getTitle(), entity.getTitle());
        assertEquals(domain.getContent(), entity.getContent());
        assertEquals(domain.getMetadata(), entity.getMetadata());
        assertEquals(domain.getVocabularyTerms(), entity.getVocabularyTerms());
        assertEquals(domain.getTimestamp(), entity.getTimestamp());
    }

    @Test
    public void testRoundTripConversion_EntityToDomainToEntity() {
        // Given
        MaterialEntity originalEntity = new MaterialEntity(
                "round-trip-id",
                MaterialType.WORKSHEET,
                "Worksheet Title",
                "Worksheet content",
                "{\"metadata\":\"value\"}",
                "[\"term1\",\"term2\"]",
                5555555555L
        );

        // When
        Material domain = MaterialMapper.toDomain(originalEntity);
        MaterialEntity resultEntity = MaterialMapper.toEntity(domain);

        // Then
        assertEquals(originalEntity.getId(), resultEntity.getId());
        assertEquals(originalEntity.getType(), resultEntity.getType());
        assertEquals(originalEntity.getTitle(), resultEntity.getTitle());
        assertEquals(originalEntity.getContent(), resultEntity.getContent());
        assertEquals(originalEntity.getMetadata(), resultEntity.getMetadata());
        assertEquals(originalEntity.getVocabularyTerms(), resultEntity.getVocabularyTerms());
        assertEquals(originalEntity.getTimestamp(), resultEntity.getTimestamp());
    }

    @Test
    public void testRoundTripConversion_DomainToEntityToDomain() {
        // Given
        Material originalDomain = new Material(
                "round-trip-domain-id",
                MaterialType.POLL,
                "Poll Title",
                "Poll content",
                "{\"poll\":\"data\"}",
                "[\"vocab\"]",
                7777777777L
        );

        // When
        MaterialEntity entity = MaterialMapper.toEntity(originalDomain);
        Material resultDomain = MaterialMapper.toDomain(entity);

        // Then
        assertEquals(originalDomain.getId(), resultDomain.getId());
        assertEquals(originalDomain.getType(), resultDomain.getType());
        assertEquals(originalDomain.getTitle(), resultDomain.getTitle());
        assertEquals(originalDomain.getContent(), resultDomain.getContent());
        assertEquals(originalDomain.getMetadata(), resultDomain.getMetadata());
        assertEquals(originalDomain.getVocabularyTerms(), resultDomain.getVocabularyTerms());
        assertEquals(originalDomain.getTimestamp(), resultDomain.getTimestamp());
    }

    @Test(expected = AssertionError.class)
    public void testPrivateConstructorThrowsException() throws Exception {
        // Use reflection to access private constructor
        java.lang.reflect.Constructor<MaterialMapper> constructor = MaterialMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
    }
}
