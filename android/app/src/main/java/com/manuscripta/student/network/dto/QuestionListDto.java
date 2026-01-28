package com.manuscripta.student.network.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Data Transfer Object for question list responses from the API.
 * Contains an ordered list of question IDs and optional full question objects.
 *
 * <p>The order of IDs in the list represents the intended display order
 * as determined by the teacher application.</p>
 */
public class QuestionListDto {

    /**
     * Ordered list of question IDs.
     * The order represents the intended display sequence.
     */
    @SerializedName("QuestionIds")
    @Nullable
    private List<String> questionIds;

    /**
     * Optional list of full question objects.
     * May be included for convenience to avoid additional API calls.
     */
    @SerializedName("Questions")
    @Nullable
    private List<QuestionDto> questions;

    /**
     * Total count of questions available.
     * May differ from the returned list if pagination is used.
     */
    @SerializedName("TotalCount")
    @Nullable
    private Integer totalCount;

    /**
     * Default constructor for Gson deserialization.
     */
    public QuestionListDto() {
    }

    /**
     * Constructor with all fields.
     *
     * @param questionIds Ordered list of question IDs
     * @param questions   Optional list of full question objects
     * @param totalCount  Total count of questions available
     */
    public QuestionListDto(@Nullable List<String> questionIds,
                           @Nullable List<QuestionDto> questions,
                           @Nullable Integer totalCount) {
        this.questionIds = questionIds;
        this.questions = questions;
        this.totalCount = totalCount;
    }

    /**
     * Gets the ordered list of question IDs.
     *
     * @return The list of question IDs
     */
    @Nullable
    public List<String> getQuestionIds() {
        return questionIds;
    }

    /**
     * Sets the ordered list of question IDs.
     *
     * @param questionIds The list of question IDs
     */
    public void setQuestionIds(@Nullable List<String> questionIds) {
        this.questionIds = questionIds;
    }

    /**
     * Gets the list of full question objects.
     *
     * @return The list of questions
     */
    @Nullable
    public List<QuestionDto> getQuestions() {
        return questions;
    }

    /**
     * Sets the list of full question objects.
     *
     * @param questions The list of questions
     */
    public void setQuestions(@Nullable List<QuestionDto> questions) {
        this.questions = questions;
    }

    /**
     * Gets the total count of questions.
     *
     * @return The total count
     */
    @Nullable
    public Integer getTotalCount() {
        return totalCount;
    }

    /**
     * Sets the total count of questions.
     *
     * @param totalCount The total count
     */
    public void setTotalCount(@Nullable Integer totalCount) {
        this.totalCount = totalCount;
    }

    @Override
    @NonNull
    public String toString() {
        return "QuestionListDto{"
                + "questionIds=" + questionIds
                + ", questions=" + questions
                + ", totalCount=" + totalCount
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
        QuestionListDto that = (QuestionListDto) o;
        if (questionIds != null ? !questionIds.equals(that.questionIds)
                : that.questionIds != null) {
            return false;
        }
        if (questions != null ? !questions.equals(that.questions) : that.questions != null) {
            return false;
        }
        return totalCount != null ? totalCount.equals(that.totalCount) : that.totalCount == null;
    }

    @Override
    public int hashCode() {
        int result = questionIds != null ? questionIds.hashCode() : 0;
        result = 31 * result + (questions != null ? questions.hashCode() : 0);
        result = 31 * result + (totalCount != null ? totalCount.hashCode() : 0);
        return result;
    }
}
