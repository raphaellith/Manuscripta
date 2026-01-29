package com.manuscripta.student.network.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object for response results (feedback) received from the server.
 * Represents the result or feedback for a previously submitted response.
 *
 * <p>Per Validation Rules §1(6), field names use PascalCase in JSON.</p>
 */
public class ResponseResultDto {

    /**
     * The ID of the response this result is for.
     */
    @SerializedName("ResponseId")
    @Nullable
    private String responseId;

    /**
     * Whether the response was correct.
     */
    @SerializedName("IsCorrect")
    @Nullable
    private Boolean isCorrect;

    /**
     * Optional textual feedback from the teacher.
     */
    @SerializedName("Feedback")
    @Nullable
    private String feedback;

    /**
     * Optional numerical score or marks awarded.
     */
    @SerializedName("Score")
    @Nullable
    private Integer score;

    /**
     * The correct answer, if applicable and revealed.
     */
    @SerializedName("CorrectAnswer")
    @Nullable
    private String correctAnswer;

    /**
     * Default constructor for Gson deserialization.
     */
    public ResponseResultDto() {
    }

    /**
     * Constructor with all fields.
     *
     * @param responseId    The ID of the response this result is for
     * @param isCorrect     Whether the response was correct
     * @param feedback      Optional textual feedback
     * @param score         Optional numerical score
     * @param correctAnswer The correct answer if revealed
     */
    public ResponseResultDto(@Nullable String responseId,
                             @Nullable Boolean isCorrect,
                             @Nullable String feedback,
                             @Nullable Integer score,
                             @Nullable String correctAnswer) {
        this.responseId = responseId;
        this.isCorrect = isCorrect;
        this.feedback = feedback;
        this.score = score;
        this.correctAnswer = correctAnswer;
    }

    /**
     * Gets the response ID.
     *
     * @return The ID of the response this result is for
     */
    @Nullable
    public String getResponseId() {
        return responseId;
    }

    /**
     * Sets the response ID.
     *
     * @param responseId The ID of the response this result is for
     */
    public void setResponseId(@Nullable String responseId) {
        this.responseId = responseId;
    }

    /**
     * Gets the correctness indicator.
     *
     * @return Whether the response was correct
     */
    @Nullable
    public Boolean getIsCorrect() {
        return isCorrect;
    }

    /**
     * Sets the correctness indicator.
     *
     * @param isCorrect Whether the response was correct
     */
    public void setIsCorrect(@Nullable Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    /**
     * Gets the textual feedback.
     *
     * @return Optional textual feedback from the teacher
     */
    @Nullable
    public String getFeedback() {
        return feedback;
    }

    /**
     * Sets the textual feedback.
     *
     * @param feedback Textual feedback from the teacher
     */
    public void setFeedback(@Nullable String feedback) {
        this.feedback = feedback;
    }

    /**
     * Gets the score.
     *
     * @return Optional numerical score or marks
     */
    @Nullable
    public Integer getScore() {
        return score;
    }

    /**
     * Sets the score.
     *
     * @param score Numerical score or marks
     */
    public void setScore(@Nullable Integer score) {
        this.score = score;
    }

    /**
     * Gets the correct answer.
     *
     * @return The correct answer if revealed
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

    @Override
    @NonNull
    public String toString() {
        return "ResponseResultDto{"
                + "responseId='" + responseId + '\''
                + ", isCorrect=" + isCorrect
                + ", feedback='" + feedback + '\''
                + ", score=" + score
                + ", correctAnswer='" + correctAnswer + '\''
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
        ResponseResultDto that = (ResponseResultDto) o;
        if (responseId != null ? !responseId.equals(that.responseId) : that.responseId != null) {
            return false;
        }
        if (isCorrect != null ? !isCorrect.equals(that.isCorrect) : that.isCorrect != null) {
            return false;
        }
        if (feedback != null ? !feedback.equals(that.feedback) : that.feedback != null) {
            return false;
        }
        if (score != null ? !score.equals(that.score) : that.score != null) {
            return false;
        }
        return correctAnswer != null ? correctAnswer.equals(that.correctAnswer)
                : that.correctAnswer == null;
    }

    @Override
    public int hashCode() {
        int result = responseId != null ? responseId.hashCode() : 0;
        result = 31 * result + (isCorrect != null ? isCorrect.hashCode() : 0);
        result = 31 * result + (feedback != null ? feedback.hashCode() : 0);
        result = 31 * result + (score != null ? score.hashCode() : 0);
        result = 31 * result + (correctAnswer != null ? correctAnswer.hashCode() : 0);
        return result;
    }
}
