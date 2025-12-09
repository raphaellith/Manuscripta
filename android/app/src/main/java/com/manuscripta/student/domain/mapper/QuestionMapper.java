package com.manuscripta.student.domain.mapper;

import androidx.annotation.NonNull;

import com.manuscripta.student.data.model.QuestionEntity;
import com.manuscripta.student.domain.model.Question;

/**
 * Mapper class to convert between QuestionEntity (data layer) and Question (domain layer).
 * Provides bidirectional mapping for Clean Architecture separation.
 */
public final class QuestionMapper {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private QuestionMapper() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Converts a QuestionEntity to a Question domain model.
     *
     * @param entity The QuestionEntity to convert
     * @return Question domain model
     */
    @NonNull
    public static Question toDomain(@NonNull QuestionEntity entity) {
        return new Question(
                entity.getId(),
                entity.getMaterialId(),
                entity.getQuestionText(),
                entity.getQuestionType(),
                entity.getOptions(),
                entity.getCorrectAnswer()
        );
    }

    /**
     * Converts a Question domain model to a QuestionEntity.
     *
     * @param domain The Question domain model to convert
     * @return QuestionEntity for persistence
     */
    @NonNull
    public static QuestionEntity toEntity(@NonNull Question domain) {
        return new QuestionEntity(
                domain.getId(),
                domain.getMaterialId(),
                domain.getQuestionText(),
                domain.getQuestionType(),
                domain.getOptions(),
                domain.getCorrectAnswer()
        );
    }
}
