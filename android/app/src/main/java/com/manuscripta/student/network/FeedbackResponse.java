package com.manuscripta.student.network;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response wrapper for the GET /feedback/{deviceId} endpoint.
 * Contains an array of FeedbackDto objects.
 *
 * <p>Per API Contract §2.6, the response format is:
 * <pre>
 * {
 *   "feedback": [
 *     // Array of FeedbackEntity objects
 *   ]
 * }
 * </pre>
 */
public class FeedbackResponse {

    /**
     * The list of feedback items.
     */
    @SerializedName("feedback")
    private List<FeedbackDto> feedback;

    /**
     * Default constructor for Gson.
     */
    public FeedbackResponse() {
    }

    /**
     * Constructor with feedback list.
     *
     * @param feedback The list of feedback items
     */
    public FeedbackResponse(List<FeedbackDto> feedback) {
        this.feedback = feedback;
    }

    /**
     * Returns the list of feedback items.
     *
     * @return The feedback list
     */
    public List<FeedbackDto> getFeedback() {
        return feedback;
    }

    /**
     * Sets the list of feedback items.
     *
     * @param feedback The feedback list
     */
    public void setFeedback(List<FeedbackDto> feedback) {
        this.feedback = feedback;
    }
}
