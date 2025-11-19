package com.manuscripta.student.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity representing a question within a material.
 * Questions are linked to a parent MaterialEntity via foreign key.
 *
 * <p>Like MaterialEntity, this is immutable on the client side as the content
 * is defined by the teacher's Windows application.</p>
 */
@Entity(
        tableName = "questions",
        foreignKeys = @ForeignKey(
                entity = MaterialEntity.class,
                parentColumns = "id",
                childColumns = "materialId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("materialId")
)
public class QuestionEntity {

    @PrimaryKey
    @NonNull
    private String id;

    @NonNull
    private String materialId;

    @NonNull
    private String questionText;

    @NonNull
    private QuestionType questionType;

    /**
     * JSON string representing options (e.g., for Multiple Choice).
     */
    private String options;

    /**
     * String representing the correct answer.
     */
    private String correctAnswer;

    /**
     * Constructor with required fields.
     *
     * @param id Unique identifier (UUID)
     * @param materialId UUID of the parent material
     * @param questionText The text of the question
     * @param questionType The type of question
     * @param options JSON string of options (use empty string if none)
     * @param correctAnswer The correct answer string (use empty string if none)
     */
    public QuestionEntity(@NonNull String id,
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

    // Getters Only (Immutable)

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

    public String getOptions() {
        return options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }
}