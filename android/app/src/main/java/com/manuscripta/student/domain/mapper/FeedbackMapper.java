package com.manuscripta.student.domain.mapper;

import androidx.annotation.NonNull;

import com.manuscripta.student.data.model.FeedbackEntity;
import com.manuscripta.student.domain.model.Feedback;

/**
 * Mapper class to convert between FeedbackEntity (data layer) and Feedback (domain layer).
 * Provides bidirectional mapping for Clean Architecture separation.
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
}
