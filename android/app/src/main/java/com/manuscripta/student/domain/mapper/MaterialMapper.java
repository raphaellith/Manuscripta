package com.manuscripta.student.domain.mapper;

import androidx.annotation.NonNull;

import com.manuscripta.student.data.model.MaterialEntity;
import com.manuscripta.student.domain.model.Material;

/**
 * Mapper class to convert between MaterialEntity (data layer) and Material (domain layer).
 * Provides bidirectional mapping for Clean Architecture separation.
 */
public final class MaterialMapper {

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
}
