package com.manuscripta.student.domain.mapper;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.manuscripta.student.data.model.MaterialEntity;
import com.manuscripta.student.data.model.MaterialType;
import com.manuscripta.student.domain.model.Material;
import com.manuscripta.student.network.dto.MaterialDto;
import com.manuscripta.student.network.dto.VocabularyTermDto;

import java.util.List;
import java.util.Locale;

/**
 * Mapper class to convert between MaterialEntity (data layer), Material (domain layer),
 * and MaterialDto (network layer).
 * Provides bidirectional mapping for Clean Architecture separation.
 *
 * <p>Important: DTOs must preserve entity IDs exactly as received from the Windows teacher
 * application, without modification or regeneration.</p>
 */
public final class MaterialMapper {

    /**
     * Gson instance for JSON serialization/deserialization of vocabulary terms.
     * Uses default configuration which serializes all fields and handles nulls appropriately.
     */
    private static final Gson GSON = new Gson();

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private MaterialMapper() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Converts a MaterialEntity to a Material domain model.
     *
     * @param entity The MaterialEntity to convert
     * @return Material domain model
     */
    @NonNull
    public static Material toDomain(@NonNull MaterialEntity entity) {
        return new Material(
                entity.getId(),
                entity.getType(),
                entity.getTitle(),
                entity.getContent(),
                entity.getMetadata(),
                entity.getVocabularyTerms(),
                entity.getTimestamp()
        );
    }

    /**
     * Converts a Material domain model to a MaterialEntity.
     *
     * @param domain The Material domain model to convert
     * @return MaterialEntity for persistence
     */
    @NonNull
    public static MaterialEntity toEntity(@NonNull Material domain) {
        return new MaterialEntity(
                domain.getId(),
                domain.getType(),
                domain.getTitle(),
                domain.getContent(),
                domain.getMetadata(),
                domain.getVocabularyTerms(),
                domain.getTimestamp()
        );
    }

    /**
     * Converts a MaterialDto to a Material domain model.
     * Preserves the ID exactly as received from the API.
     *
     * @param dto The MaterialDto to convert
     * @return Material domain model
     * @throws IllegalArgumentException if required fields are null
     */
    @NonNull
    public static Material fromDto(@NonNull MaterialDto dto) {
        String id = dto.getId();
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("MaterialDto id cannot be null or empty");
        }

        String typeStr = dto.getType();
        MaterialType type;
        try {
            type = typeStr != null
                    ? MaterialType.valueOf(typeStr.trim().toUpperCase(Locale.ROOT))
                    : MaterialType.READING;
        } catch (IllegalArgumentException e) {
            // Unknown type - silently default to READING
            type = MaterialType.READING;
        }

        String title = dto.getTitle();
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("MaterialDto title cannot be null or empty");
        }

        String content = dto.getContent() != null ? dto.getContent() : "";
        String metadata = dto.getMetadata() != null ? dto.getMetadata() : "{}";

        // Convert vocabulary terms list to JSON array string
        String vocabularyTermsJson = convertVocabularyTermsToJson(dto.getVocabularyTerms());

        Long timestamp = dto.getTimestamp();
        long timestampValue = timestamp != null ? timestamp : 0L;

        return new Material(id, type, title, content, metadata, vocabularyTermsJson, timestampValue);
    }

    /**
     * Converts a Material domain model to a MaterialDto.
     * Preserves the ID exactly for API communication.
     *
     * @param domain The Material domain model to convert
     * @return MaterialDto for API communication
     */
    @NonNull
    public static MaterialDto toDto(@NonNull Material domain) {
        List<VocabularyTermDto> vocabularyTerms =
                convertJsonToVocabularyTerms(domain.getVocabularyTerms());

        return new MaterialDto(
                domain.getId(),
                domain.getType().name(),
                domain.getTitle(),
                domain.getContent(),
                domain.getMetadata(),
                vocabularyTerms,
                domain.getTimestamp()
        );
    }

    /**
     * Converts a MaterialDto directly to a MaterialEntity for persistence.
     * Preserves the ID exactly as received from the API.
     *
     * @param dto The MaterialDto to convert
     * @return MaterialEntity for persistence
     * @throws IllegalArgumentException if required fields are null
     */
    @NonNull
    public static MaterialEntity dtoToEntity(@NonNull MaterialDto dto) {
        return toEntity(fromDto(dto));
    }

    /**
     * Converts a MaterialEntity to a MaterialDto for API communication.
     *
     * @param entity The MaterialEntity to convert
     * @return MaterialDto for API communication
     */
    @NonNull
    public static MaterialDto entityToDto(@NonNull MaterialEntity entity) {
        return toDto(toDomain(entity));
    }

    /**
     * Converts a list of VocabularyTermDto to a JSON array string.
     *
     * @param vocabularyTerms The list of vocabulary terms
     * @return JSON array string representation
     */
    @NonNull
    private static String convertVocabularyTermsToJson(List<VocabularyTermDto> vocabularyTerms) {
        if (vocabularyTerms == null || vocabularyTerms.isEmpty()) {
            return "[]";
        }
        return GSON.toJson(vocabularyTerms);
    }

    /**
     * Converts a JSON array string to a list of VocabularyTermDto.
     * Invalid JSON will result in an empty list being returned silently.
     *
     * @param json The JSON array string
     * @return List of VocabularyTermDto, or empty list if parsing fails
     */
    @NonNull
    private static List<VocabularyTermDto> convertJsonToVocabularyTerms(String json) {
        if (json == null || json.isEmpty() || "[]".equals(json)) {
            return java.util.Collections.emptyList();
        }
        try {
            VocabularyTermDto[] terms = GSON.fromJson(json, VocabularyTermDto[].class);
            return terms != null
                    ? new java.util.ArrayList<>(java.util.Arrays.asList(terms))
                    : java.util.Collections.emptyList();
        } catch (JsonSyntaxException e) {
            // Invalid JSON - silently return empty list
            return java.util.Collections.emptyList();
        }
    }
}
