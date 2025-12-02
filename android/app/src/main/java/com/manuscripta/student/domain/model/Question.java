package com.manuscripta.student.domain.model;

import androidx.annotation.NonNull;

import com.manuscripta.student.data.model.QuestionType;

/**
 * Domain model representing a question within a material.
 * This is a clean domain object without persistence annotations, used in the business logic layer.
 *
 * <p>Questions are linked to a parent Material.</p>
 */
public class Question {

    /** The unique identifier for the question. */
    @NonNull
    private final String id;

    /** The ID of the parent material. */
    @NonNull
    private final String materialId;

    /** The text of the question. */
    @NonNull
    private final String questionText;

    /** The type of question. */
    @NonNull
    private final QuestionType questionType;

    /** JSON string of options (empty string if none). */
    @NonNull
    private final String options;

    /** The correct answer string (empty string if none). */
    @NonNull
    private final String correctAnswer;

    /**
     * Constructor with all fields.
     *
     * @param id            Unique identifier (UUID)
     * @param materialId    UUID of the parent material
     * @param questionText  The text of the question
     * @param questionType  The type of question (MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER)
     * @param options       JSON string of options (empty string if none)
     * @param correctAnswer The correct answer string (empty string if none)
     * @throws IllegalArgumentException if id is null or empty
     * @throws IllegalArgumentException if materialId is null or empty
     * @throws IllegalArgumentException if questionText is null or empty
     * @throws IllegalArgumentException if questionType is null
     * @throws IllegalArgumentException if options is null
     * @throws IllegalArgumentException if correctAnswer is null
     */
    public Question(@NonNull String id,
                    @NonNull String materialId,
                    @NonNull String questionText,
                    @NonNull QuestionType questionType,
                    @NonNull String options,
                    @NonNull String correctAnswer) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Question id cannot be null or empty");
        }
        if (materialId == null || materialId.trim().isEmpty()) {
            throw new IllegalArgumentException("Question materialId cannot be null or empty");
        }
        if (questionText == null || questionText.trim().isEmpty()) {
            throw new IllegalArgumentException("Question questionText cannot be null or empty");
        }
        if (questionType == null) {
            throw new IllegalArgumentException("Question questionType cannot be null");
        }
        if (options == null) {
            throw new IllegalArgumentException("Question options cannot be null");
        }
        if (correctAnswer == null) {
            throw new IllegalArgumentException("Question correctAnswer cannot be null");
        }

        this.id = id;
        this.materialId = materialId;
        this.questionText = questionText;
        this.questionType = questionType;
        this.options = options;
        this.correctAnswer = correctAnswer;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getMaterialId() {
        return materialId;
    }

    @NonNull
    public String getQuestionText() {
        return questionText;
    }

    @NonNull
    public QuestionType getQuestionType() {
        return questionType;
    }

    @NonNull
    public String getOptions() {
        return options;
    }

    @NonNull
    public String getCorrectAnswer() {
        return correctAnswer;
    }
}
