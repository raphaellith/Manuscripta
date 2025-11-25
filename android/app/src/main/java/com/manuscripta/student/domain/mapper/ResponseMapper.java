package com.manuscripta.student.domain.mapper;

import androidx.annotation.NonNull;

import com.manuscripta.student.data.model.ResponseEntity;
import com.manuscripta.student.domain.model.Response;

/**
 * Mapper class to convert between ResponseEntity (data layer) and Response (domain layer).
 * Provides bidirectional mapping for Clean Architecture separation.
 */
public class ResponseMapper {

    /**
     * Converts a ResponseEntity to a Response domain model.
     *
     * @param entity The ResponseEntity to convert
     * @return Response domain model
     */
    @NonNull
    public static Response toDomain(@NonNull ResponseEntity entity) {
        return new Response(
                entity.getId(),
                entity.getQuestionId(),
                entity.getSelectedAnswer(),
                entity.isCorrect(),
                entity.getTimestamp(),
                entity.isSynced()
        );
    }

    /**
     * Converts a Response domain model to a ResponseEntity.
     *
     * @param domain The Response domain model to convert
     * @return ResponseEntity for persistence
     */
    @NonNull
    public static ResponseEntity toEntity(@NonNull Response domain) {
        return new ResponseEntity(
                domain.getId(),
                domain.getQuestionId(),
                domain.getSelectedAnswer(),
                domain.isCorrect(),
                domain.getTimestamp(),
                domain.isSynced()
        );
    }
}
