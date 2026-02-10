package com.manuscripta.student.domain.mapper;

import androidx.annotation.NonNull;

import com.manuscripta.student.data.model.FeedbackEntity;
import com.manuscripta.student.domain.model.Feedback;
import com.manuscripta.student.network.dto.FeedbackDto;

/**
 * Mapper class to convert between FeedbackEntity (data layer), Feedback (domain layer),
 * and FeedbackDto (network layer).
 * Provides bidirectional mapping for Clean Architecture separation.
 *
 * <p>Important: DTOs must preserve entity IDs exactly as received from the Windows teacher
 * application, without modification or regeneration.</p>
 */
public final class FeedbackMapper {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private FeedbackMapper() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Converts a FeedbackEntity to a Feedback domain model.
     *
     * @param entity The FeedbackEntity to convert
     * @return Feedback domain model
     */
    @NonNull
    public static Feedback toDomain(@NonNull FeedbackEntity entity) {
        return new Feedback(
                entity.getId(),
                entity.getResponseId(),
                entity.getText(),
                entity.getMarks()
        );
    }

    /**
     * Converts a Feedback domain model to a FeedbackEntity.
     *
     * @param domain The Feedback domain model to convert
     * @return FeedbackEntity for persistence
     */
    @NonNull
    public static FeedbackEntity toEntity(@NonNull Feedback domain) {
        return new FeedbackEntity(
                domain.getId(),
                domain.getResponseId(),
                domain.getText(),
                domain.getMarks()
        );
    }

    /**
     * Converts a FeedbackDto to a Feedback domain model.
     * Preserves the ID exactly as received from the API.
     *
     * <p>Per Validation Rules.md §2F(1)(b), at least one of text or marks must be present.
     * This validation is performed by the Feedback domain constructor.</p>
     *
     * @param dto The FeedbackDto to convert
     * @return Feedback domain model
     * @throws IllegalArgumentException if required fields are null or validation fails
     */
    @NonNull
    public static Feedback fromDto(@NonNull FeedbackDto dto) {
        String id = dto.getId();
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("FeedbackDto id cannot be null or empty");
        }

        String responseId = dto.getResponseId();
        if (responseId == null || responseId.trim().isEmpty()) {
            throw new IllegalArgumentException("FeedbackDto responseId cannot be null or empty");
        }

        // Feedback domain constructor validates that at least one of text/marks is present
        return new Feedback(id, responseId, dto.getText(), dto.getMarks());
    }

    /**
     * Converts a Feedback domain model to a FeedbackDto.
     * Preserves the ID exactly for API communication.
     *
     * @param domain The Feedback domain model to convert
     * @return FeedbackDto for API communication
     */
    @NonNull
    public static FeedbackDto toDto(@NonNull Feedback domain) {
        return new FeedbackDto(
                domain.getId(),
                domain.getResponseId(),
                domain.getText(),
                domain.getMarks()
        );
    }

    /**
     * Converts a FeedbackDto directly to a FeedbackEntity for persistence.
     * Preserves the ID exactly as received from the API.
     *
     * @param dto The FeedbackDto to convert
     * @return FeedbackEntity for persistence
     * @throws IllegalArgumentException if required fields are null or validation fails
     */
    @NonNull
    public static FeedbackEntity dtoToEntity(@NonNull FeedbackDto dto) {
        return toEntity(fromDto(dto));
    }

    /**
     * Converts a FeedbackEntity to a FeedbackDto for API communication.
     *
     * @param entity The FeedbackEntity to convert
     * @return FeedbackDto for API communication
     */
    @NonNull
    public static FeedbackDto entityToDto(@NonNull FeedbackEntity entity) {
        return toDto(toDomain(entity));
    }
}
