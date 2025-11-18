package com.manuscripta.student.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity representing a question within a material.
 * Questions are linked to a parent MaterialEntity via foreign key.
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

    private String options;

    private String correctAnswer;

    // Default constructor
    public QuestionEntity() {
        this.id = "";
        this.materialId = "";
        this.questionText = "";
        this.questionType = QuestionType.MULTIPLE_CHOICE;
        this.options = "";
        this.correctAnswer = "";
    }

    // Getters and Setters

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(@NonNull String materialId) {
        this.materialId = materialId;
    }

    @NonNull
    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(@NonNull String questionText) {
        this.questionText = questionText;
    }

    @NonNull
    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(@NonNull QuestionType questionType) {
        this.questionType = questionType;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }
}