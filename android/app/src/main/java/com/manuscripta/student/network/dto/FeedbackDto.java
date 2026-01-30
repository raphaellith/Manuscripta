package com.manuscripta.student.network;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object for feedback received from the server via HTTP.
 * Per Validation Rules.md §1(6), field names use PascalCase in JSON.
 *
 * <p>Represents teacher feedback on a student's response, containing optional
 * textual comments and/or numerical marks.</p>
 */
public class FeedbackDto {

    /**
     * The unique identifier for the feedback (UUID assigned by Windows).
     */
    @SerializedName("Id")
    private String id;

    /**
     * The ID of the response this feedback is for.
     */
    @SerializedName("ResponseId")
    private String responseId;

    /**
     * Optional textual feedback from the teacher.
     */
    @SerializedName("Text")
    @Nullable
    private String text;

    /**
     * Optional numerical marks awarded.
     */
    @SerializedName("Marks")
    @Nullable
    private Integer marks;

    /**
     * Default constructor for Gson.
     */
    public FeedbackDto() {
    }

    /**
     * Constructor with all fields.
     *
     * @param id         The unique identifier for the feedback
     * @param responseId The ID of the response this feedback is for
     * @param text       Optional textual feedback
     * @param marks      Optional numerical marks
     */
    public FeedbackDto(String id, String responseId, @Nullable String text,
                       @Nullable Integer marks) {
        this.id = id;
        this.responseId = responseId;
        this.text = text;
        this.marks = marks;
    }

    /**
     * Returns the unique identifier for this feedback.
     *
     * @return The feedback ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this feedback.
     *
     * @param id The feedback ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the ID of the response this feedback is for.
     *
     * @return The response ID
     */
    public String getResponseId() {
        return responseId;
    }

    /**
     * Sets the ID of the response this feedback is for.
     *
     * @param responseId The response ID
     */
    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    /**
     * Returns the optional textual feedback.
     *
     * @return The text feedback, or null if not provided
     */
    @Nullable
    public String getText() {
        return text;
    }

    /**
     * Sets the optional textual feedback.
     *
     * @param text The text feedback
     */
    public void setText(@Nullable String text) {
        this.text = text;
    }

    /**
     * Returns the optional numerical marks.
     *
     * @return The marks awarded, or null if not provided
     */
    @Nullable
    public Integer getMarks() {
        return marks;
    }

    /**
     * Sets the optional numerical marks.
     *
     * @param marks The marks awarded
     */
    public void setMarks(@Nullable Integer marks) {
        this.marks = marks;
    }
}
