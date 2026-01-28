package com.manuscripta.student.network.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Data Transfer Object for Question API communication.
 * Questions are linked to a parent Material and define interactive elements
 * within worksheets, polls, or quizzes.
 *
 * <p>Entity IDs are assigned by the Windows teacher application and must be preserved
 * exactly as received, without modification or regeneration.</p>
 *
 * <p>Related requirements: Section 2B of Validation Rules</p>
 */
public class QuestionDto {

    /**
     * Unique identifier for the question (UUID format).
     * Assigned by the Windows teacher application and preserved across services.
     */
    @SerializedName("Id")
    @Nullable
    private String id;

    /**
     * The ID of the material this question belongs to (UUID format).
     */
    @SerializedName("MaterialId")
    @Nullable
    private String materialId;

    /**
     * The type of question (e.g., "MULTIPLE_CHOICE", "WRITTEN_ANSWER").
     */
    @SerializedName("QuestionType")
    @Nullable
    private String questionType;

    /**
     * The text of the question displayed to the student.
     */
    @SerializedName("QuestionText")
    @Nullable
    private String questionText;

    /**
     * List of options for multiple choice questions.
     * May be null or empty for written answer questions.
     */
    @SerializedName("Options")
    @Nullable
    private List<String> options;

    /**
     * The correct answer for the question.
     * For multiple choice questions, this stores the value used by the teacher
     * application to identify the correct option (for example "A", "2", or the
     * full option text), and is not guaranteed to be a numeric index into
     * {@link #options}.
     * For written answer questions, this is the expected answer text.
     */
    @SerializedName("CorrectAnswer")
    @Nullable
    private String correctAnswer;

    /**
     * The maximum number of marks available for this question.
     */
    @SerializedName("MaxScore")
    @Nullable
    private Integer maxScore;

    /**
     * Default constructor for Gson deserialization.
     */
    public QuestionDto() {
    }

    /**
     * Constructor with all fields.
     *
     * @param id            Unique identifier (UUID)
     * @param materialId    UUID of the parent material
     * @param questionType  Type of question
     * @param questionText  The question text
     * @param options       List of options for multiple choice
     * @param correctAnswer The correct answer
     * @param maxScore      Maximum marks available
     */
    public QuestionDto(@Nullable String id,
                       @Nullable String materialId,
                       @Nullable String questionType,
                       @Nullable String questionText,
                       @Nullable List<String> options,
                       @Nullable String correctAnswer,
                       @Nullable Integer maxScore) {
        this.id = id;
        this.materialId = materialId;
        this.questionType = questionType;
        this.questionText = questionText;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.maxScore = maxScore;
    }

    /**
     * Gets the unique identifier.
     *
     * @return The question ID
     */
    @Nullable
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier.
     *
     * @param id The question ID
     */
    public void setId(@Nullable String id) {
        this.id = id;
    }

    /**
     * Gets the material ID.
     *
     * @return The parent material ID
     */
    @Nullable
    public String getMaterialId() {
        return materialId;
    }

    /**
     * Sets the material ID.
     *
     * @param materialId The parent material ID
     */
    public void setMaterialId(@Nullable String materialId) {
        this.materialId = materialId;
    }

    /**
     * Gets the question type.
     *
     * @return The type (e.g., "MULTIPLE_CHOICE", "WRITTEN_ANSWER")
     */
    @Nullable
    public String getQuestionType() {
        return questionType;
    }

    /**
     * Sets the question type.
     *
     * @param questionType The type (e.g., "MULTIPLE_CHOICE", "WRITTEN_ANSWER")
     */
    public void setQuestionType(@Nullable String questionType) {
        this.questionType = questionType;
    }

    /**
     * Gets the question text.
     *
     * @return The question text
     */
    @Nullable
    public String getQuestionText() {
        return questionText;
    }

    /**
     * Sets the question text.
     *
     * @param questionText The question text
     */
    public void setQuestionText(@Nullable String questionText) {
        this.questionText = questionText;
    }

    /**
     * Gets the options list.
     *
     * @return The list of options
     */
    @Nullable
    public List<String> getOptions() {
        return options;
    }

    /**
     * Sets the options list.
     *
     * @param options The list of options
     */
    public void setOptions(@Nullable List<String> options) {
        this.options = options;
    }

    /**
     * Gets the correct answer.
     *
     * @return The correct answer
     */
    @Nullable
    public String getCorrectAnswer() {
        return correctAnswer;
    }

    /**
     * Sets the correct answer.
     *
     * @param correctAnswer The correct answer
     */
    public void setCorrectAnswer(@Nullable String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    /**
     * Gets the maximum score.
     *
     * @return The maximum marks available
     */
    @Nullable
    public Integer getMaxScore() {
        return maxScore;
    }

    /**
     * Sets the maximum score.
     *
     * @param maxScore The maximum marks available
     */
    public void setMaxScore(@Nullable Integer maxScore) {
        this.maxScore = maxScore;
    }

    @Override
    @NonNull
    public String toString() {
        return "QuestionDto{"
                + "id='" + id + '\''
                + ", materialId='" + materialId + '\''
                + ", questionType='" + questionType + '\''
                + ", questionText='" + questionText + '\''
                + ", options=" + options
                + ", correctAnswer='" + correctAnswer + '\''
                + ", maxScore=" + maxScore
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QuestionDto that = (QuestionDto) o;
        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (materialId != null ? !materialId.equals(that.materialId) : that.materialId != null) {
            return false;
        }
        if (questionType != null ? !questionType.equals(that.questionType)
                : that.questionType != null) {
            return false;
        }
        if (questionText != null ? !questionText.equals(that.questionText)
                : that.questionText != null) {
            return false;
        }
        if (options != null ? !options.equals(that.options) : that.options != null) {
            return false;
        }
        if (correctAnswer != null ? !correctAnswer.equals(that.correctAnswer)
                : that.correctAnswer != null) {
            return false;
        }
        return maxScore != null ? maxScore.equals(that.maxScore) : that.maxScore == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (materialId != null ? materialId.hashCode() : 0);
        result = 31 * result + (questionType != null ? questionType.hashCode() : 0);
        result = 31 * result + (questionText != null ? questionText.hashCode() : 0);
        result = 31 * result + (options != null ? options.hashCode() : 0);
        result = 31 * result + (correctAnswer != null ? correctAnswer.hashCode() : 0);
        result = 31 * result + (maxScore != null ? maxScore.hashCode() : 0);
        return result;
    }
}
