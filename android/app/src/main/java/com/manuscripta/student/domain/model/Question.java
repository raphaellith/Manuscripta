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

    @NonNull
    private final String id;

    @NonNull
    private final String materialId;

    @NonNull
    private final String questionText;

    @NonNull
    private final QuestionType questionType;

    @NonNull
    private final String options;

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
     */
    public Question(@NonNull String id,
                    @NonNull String materialId,
                    @NonNull String questionText,
                    @NonNull QuestionType questionType,
                    @NonNull String options,
                    @NonNull String correctAnswer) {
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
