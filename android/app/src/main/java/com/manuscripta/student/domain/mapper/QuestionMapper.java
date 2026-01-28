package com.manuscripta.student.domain.mapper;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.manuscripta.student.data.model.QuestionEntity;
import com.manuscripta.student.data.model.QuestionType;
import com.manuscripta.student.domain.model.Question;
import com.manuscripta.student.network.dto.QuestionDto;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Mapper class to convert between QuestionEntity (data layer), Question (domain layer),
 * and QuestionDto (network layer).
 * Provides bidirectional mapping for Clean Architecture separation.
 *
 * <p>Important: DTOs must preserve entity IDs exactly as received from the Windows teacher
 * application, without modification or regeneration.</p>
 */
public final class QuestionMapper {

    /**
     * Gson instance for JSON serialization/deserialization of options.
     * Uses default configuration which serializes all fields and handles nulls appropriately.
     */
    private static final Gson GSON = new Gson();

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

    /**
     * Converts a QuestionDto to a Question domain model.
     * Preserves the ID exactly as received from the API.
     *
     * @param dto The QuestionDto to convert
     * @return Question domain model
     * @throws IllegalArgumentException if required fields are null
     */
    @NonNull
    public static Question fromDto(@NonNull QuestionDto dto) {
        String id = dto.getId();
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("QuestionDto id cannot be null or empty");
        }

        String materialId = dto.getMaterialId();
        if (materialId == null || materialId.trim().isEmpty()) {
            throw new IllegalArgumentException("QuestionDto materialId cannot be null or empty");
        }

        String questionText = dto.getQuestionText();
        if (questionText == null || questionText.trim().isEmpty()) {
            throw new IllegalArgumentException("QuestionDto questionText cannot be null or empty");
        }

        String questionTypeStr = dto.getQuestionType();
        QuestionType questionType;
        try {
            questionType = questionTypeStr != null
                    ? QuestionType.valueOf(questionTypeStr.trim().toUpperCase(Locale.ROOT))
                    : QuestionType.WRITTEN_ANSWER;
        } catch (IllegalArgumentException e) {
            // Unknown type - silently default to WRITTEN_ANSWER
            questionType = QuestionType.WRITTEN_ANSWER;
        }

        // Convert options list to JSON array string
        String optionsJson = convertOptionsToJson(dto.getOptions());

        String correctAnswer = dto.getCorrectAnswer() != null ? dto.getCorrectAnswer() : "";

        return new Question(id, materialId, questionText, questionType, optionsJson, correctAnswer);
    }

    /**
     * Converts a Question domain model to a QuestionDto.
     * Preserves the ID exactly for API communication.
     *
     * @param domain The Question domain model to convert
     * @return QuestionDto for API communication
     */
    @NonNull
    public static QuestionDto toDto(@NonNull Question domain) {
        List<String> options = convertJsonToOptions(domain.getOptions());

        return new QuestionDto(
                domain.getId(),
                domain.getMaterialId(),
                domain.getQuestionType().name(),
                domain.getQuestionText(),
                options,
                domain.getCorrectAnswer(),
                null // maxScore not stored in domain model
        );
    }

    /**
     * Converts a QuestionDto directly to a QuestionEntity for persistence.
     * Preserves the ID exactly as received from the API.
     *
     * @param dto The QuestionDto to convert
     * @return QuestionEntity for persistence
     * @throws IllegalArgumentException if required fields are null
     */
    @NonNull
    public static QuestionEntity dtoToEntity(@NonNull QuestionDto dto) {
        return toEntity(fromDto(dto));
    }

    /**
     * Converts a QuestionEntity to a QuestionDto for API communication.
     *
     * @param entity The QuestionEntity to convert
     * @return QuestionDto for API communication
     */
    @NonNull
    public static QuestionDto entityToDto(@NonNull QuestionEntity entity) {
        return toDto(toDomain(entity));
    }

    /**
     * Converts a list of options to a JSON array string.
     *
     * @param options The list of options
     * @return JSON array string representation
     */
    @NonNull
    private static String convertOptionsToJson(List<String> options) {
        if (options == null || options.isEmpty()) {
            return "[]";
        }
        return GSON.toJson(options);
    }

    /**
     * Converts a JSON array string to a list of options.
     * Invalid JSON will result in an empty list being returned silently.
     *
     * @param json The JSON array string
     * @return List of options, or empty list if parsing fails
     */
    @NonNull
    private static List<String> convertJsonToOptions(String json) {
        if (json == null || json.isEmpty() || "[]".equals(json)) {
            return Collections.emptyList();
        }
        try {
            String[] options = GSON.fromJson(json, String[].class);
            return options != null ? Arrays.asList(options) : Collections.emptyList();
        } catch (JsonSyntaxException e) {
            // Invalid JSON - silently return empty list
            return Collections.emptyList();
        }
    }
}
