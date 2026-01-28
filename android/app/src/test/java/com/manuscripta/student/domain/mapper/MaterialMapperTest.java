package com.manuscripta.student.domain.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.manuscripta.student.data.model.MaterialEntity;
import com.manuscripta.student.data.model.MaterialType;
import com.manuscripta.student.domain.model.Material;
import com.manuscripta.student.network.dto.MaterialDto;
import com.manuscripta.student.network.dto.VocabularyTermDto;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link MaterialMapper}.
 * Tests bidirectional mapping between MaterialEntity, Material domain model, and MaterialDto.
 */
public class MaterialMapperTest {

    private static final String TEST_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_TITLE = "Test Material Title";
    private static final String TEST_CONTENT = "Test material content";
    private static final String TEST_METADATA = "{\"author\":\"Teacher\"}";
    private static final long TEST_TIMESTAMP = 1234567890L;

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
                MaterialType.READING,
                "Test Reading Title",
                "Test reading content about science",
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

    @Test
    public void testFromDtoBasic() {
        // Given
        MaterialDto dto = new MaterialDto(
                TEST_ID,
                "QUIZ",
                TEST_TITLE,
                TEST_CONTENT,
                TEST_METADATA,
                null,
                TEST_TIMESTAMP
        );

        // When
        Material domain = MaterialMapper.fromDto(dto);

        // Then
        assertNotNull(domain);
        assertEquals(TEST_ID, domain.getId());
        assertEquals(MaterialType.QUIZ, domain.getType());
        assertEquals(TEST_TITLE, domain.getTitle());
        assertEquals(TEST_CONTENT, domain.getContent());
        assertEquals(TEST_METADATA, domain.getMetadata());
        assertEquals("[]", domain.getVocabularyTerms());
        assertEquals(TEST_TIMESTAMP, domain.getTimestamp());
    }

    @Test
    public void testFromDtoWithVocabularyTerms() {
        // Given
        List<VocabularyTermDto> vocabTerms = Arrays.asList(
                new VocabularyTermDto("algebra", "A branch of mathematics"),
                new VocabularyTermDto("equation", "A mathematical statement")
        );
        MaterialDto dto = new MaterialDto(
                TEST_ID,
                "READING",
                TEST_TITLE,
                TEST_CONTENT,
                TEST_METADATA,
                vocabTerms,
                TEST_TIMESTAMP
        );

        // When
        Material domain = MaterialMapper.fromDto(dto);

        // Then
        assertNotNull(domain);
        assertTrue(domain.getVocabularyTerms().contains("algebra"));
        assertTrue(domain.getVocabularyTerms().contains("equation"));
    }

    @Test
    public void testFromDtoPreservesWindowsId() {
        // Given - ID generated by Windows teacher app
        String windowsId = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
        MaterialDto dto = new MaterialDto(
                windowsId,
                "WORKSHEET",
                TEST_TITLE,
                TEST_CONTENT,
                TEST_METADATA,
                null,
                TEST_TIMESTAMP
        );

        // When
        Material domain = MaterialMapper.fromDto(dto);

        // Then - ID must be preserved exactly
        assertEquals(windowsId, domain.getId());
    }

    @Test
    public void testFromDtoWithNullContent() {
        // Given
        MaterialDto dto = new MaterialDto(
                TEST_ID,
                "QUIZ",
                TEST_TITLE,
                null,
                null,
                null,
                null
        );

        // When
        Material domain = MaterialMapper.fromDto(dto);

        // Then
        assertEquals("", domain.getContent());
        assertEquals("{}", domain.getMetadata());
        assertEquals(0L, domain.getTimestamp());
    }

    @Test
    public void testFromDtoWithLowercaseType() {
        // Given
        MaterialDto dto = new MaterialDto(
                TEST_ID,
                "quiz",
                TEST_TITLE,
                TEST_CONTENT,
                TEST_METADATA,
                null,
                TEST_TIMESTAMP
        );

        // When
        Material domain = MaterialMapper.fromDto(dto);

        // Then
        assertEquals(MaterialType.QUIZ, domain.getType());
    }

    @Test
    public void testFromDtoWithInvalidType() {
        // Given
        MaterialDto dto = new MaterialDto(
                TEST_ID,
                "INVALID_TYPE",
                TEST_TITLE,
                TEST_CONTENT,
                TEST_METADATA,
                null,
                TEST_TIMESTAMP
        );

        // When
        Material domain = MaterialMapper.fromDto(dto);

        // Then - defaults to READING
        assertEquals(MaterialType.READING, domain.getType());
    }

    @Test
    public void testFromDtoWithNullType() {
        // Given
        MaterialDto dto = new MaterialDto(
                TEST_ID,
                null,
                TEST_TITLE,
                TEST_CONTENT,
                TEST_METADATA,
                null,
                TEST_TIMESTAMP
        );

        // When
        Material domain = MaterialMapper.fromDto(dto);

        // Then - defaults to READING
        assertEquals(MaterialType.READING, domain.getType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromDtoWithNullId() {
        MaterialDto dto = new MaterialDto(
                null,
                "QUIZ",
                TEST_TITLE,
                TEST_CONTENT,
                TEST_METADATA,
                null,
                TEST_TIMESTAMP
        );

        MaterialMapper.fromDto(dto);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromDtoWithEmptyId() {
        MaterialDto dto = new MaterialDto(
                "",
                "QUIZ",
                TEST_TITLE,
                TEST_CONTENT,
                TEST_METADATA,
                null,
                TEST_TIMESTAMP
        );

        MaterialMapper.fromDto(dto);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromDtoWithNullTitle() {
        MaterialDto dto = new MaterialDto(
                TEST_ID,
                "QUIZ",
                null,
                TEST_CONTENT,
                TEST_METADATA,
                null,
                TEST_TIMESTAMP
        );

        MaterialMapper.fromDto(dto);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromDtoWithEmptyTitle() {
        MaterialDto dto = new MaterialDto(
                TEST_ID,
                "QUIZ",
                "   ",
                TEST_CONTENT,
                TEST_METADATA,
                null,
                TEST_TIMESTAMP
        );

        MaterialMapper.fromDto(dto);
    }

    @Test
    public void testToDtoBasic() {
        // Given
        Material domain = new Material(
                TEST_ID,
                MaterialType.QUIZ,
                TEST_TITLE,
                TEST_CONTENT,
                TEST_METADATA,
                "[]",
                TEST_TIMESTAMP
        );

        // When
        MaterialDto dto = MaterialMapper.toDto(domain);

        // Then
        assertNotNull(dto);
        assertEquals(TEST_ID, dto.getId());
        assertEquals("QUIZ", dto.getType());
        assertEquals(TEST_TITLE, dto.getTitle());
        assertEquals(TEST_CONTENT, dto.getContent());
        assertEquals(TEST_METADATA, dto.getMetadata());
        assertEquals(Long.valueOf(TEST_TIMESTAMP), dto.getTimestamp());
    }

    @Test
    public void testToDtoWithVocabularyTerms() {
        // Given
        String vocabJson = "[{\"Term\":\"mitosis\",\"Definition\":\"Cell division\"}]";
        Material domain = new Material(
                TEST_ID,
                MaterialType.READING,
                TEST_TITLE,
                TEST_CONTENT,
                TEST_METADATA,
                vocabJson,
                TEST_TIMESTAMP
        );

        // When
        MaterialDto dto = MaterialMapper.toDto(domain);

        // Then
        assertNotNull(dto.getVocabularyTerms());
        assertEquals(1, dto.getVocabularyTerms().size());
        assertEquals("mitosis", dto.getVocabularyTerms().get(0).getTerm());
        assertEquals("Cell division", dto.getVocabularyTerms().get(0).getDefinition());
    }

    @Test
    public void testToDtoPreservesId() {
        // Given
        String originalId = "preserved-uuid-12345";
        Material domain = new Material(
                originalId,
                MaterialType.POLL,
                TEST_TITLE,
                TEST_CONTENT,
                TEST_METADATA,
                "[]",
                TEST_TIMESTAMP
        );

        // When
        MaterialDto dto = MaterialMapper.toDto(domain);

        // Then
        assertEquals(originalId, dto.getId());
    }

    @Test
    public void testDtoToEntity() {
        // Given
        MaterialDto dto = new MaterialDto(
                TEST_ID,
                "WORKSHEET",
                TEST_TITLE,
                TEST_CONTENT,
                TEST_METADATA,
                Arrays.asList(new VocabularyTermDto("term", "def")),
                TEST_TIMESTAMP
        );

        // When
        MaterialEntity entity = MaterialMapper.dtoToEntity(dto);

        // Then
        assertNotNull(entity);
        assertEquals(TEST_ID, entity.getId());
        assertEquals(MaterialType.WORKSHEET, entity.getType());
        assertEquals(TEST_TITLE, entity.getTitle());
    }

    @Test
    public void testEntityToDto() {
        // Given
        MaterialEntity entity = new MaterialEntity(
                TEST_ID,
                MaterialType.POLL,
                TEST_TITLE,
                TEST_CONTENT,
                TEST_METADATA,
                "[{\"Term\":\"vocab\",\"Definition\":\"word\"}]",
                TEST_TIMESTAMP
        );

        // When
        MaterialDto dto = MaterialMapper.entityToDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(TEST_ID, dto.getId());
        assertEquals("POLL", dto.getType());
    }

    @Test
    public void testRoundTripConversion_DtoToDomainToDto() {
        // Given
        List<VocabularyTermDto> vocabTerms = Arrays.asList(
                new VocabularyTermDto("photosynthesis", "Process of converting sunlight")
        );
        MaterialDto originalDto = new MaterialDto(
                TEST_ID,
                "READING",
                TEST_TITLE,
                TEST_CONTENT,
                TEST_METADATA,
                vocabTerms,
                TEST_TIMESTAMP
        );

        // When
        Material domain = MaterialMapper.fromDto(originalDto);
        MaterialDto resultDto = MaterialMapper.toDto(domain);

        // Then
        assertEquals(originalDto.getId(), resultDto.getId());
        assertEquals(originalDto.getType(), resultDto.getType());
        assertEquals(originalDto.getTitle(), resultDto.getTitle());
        assertEquals(originalDto.getContent(), resultDto.getContent());
        assertEquals(originalDto.getTimestamp(), resultDto.getTimestamp());
    }

    @Test
    public void testFromDtoWithEmptyVocabularyTermsList() {
        // Given
        MaterialDto dto = new MaterialDto(
                TEST_ID,
                "QUIZ",
                TEST_TITLE,
                TEST_CONTENT,
                TEST_METADATA,
                Collections.emptyList(),
                TEST_TIMESTAMP
        );

        // When
        Material domain = MaterialMapper.fromDto(dto);

        // Then
        assertEquals("[]", domain.getVocabularyTerms());
    }

    @Test
    public void testToDtoWithInvalidVocabularyTermsJson() {
        // Given - invalid JSON should result in empty list
        Material domain = new Material(
                TEST_ID,
                MaterialType.READING,
                TEST_TITLE,
                TEST_CONTENT,
                TEST_METADATA,
                "invalid json",
                TEST_TIMESTAMP
        );

        // When
        MaterialDto dto = MaterialMapper.toDto(domain);

        // Then
        assertNotNull(dto.getVocabularyTerms());
        assertTrue(dto.getVocabularyTerms().isEmpty());
    }
}
