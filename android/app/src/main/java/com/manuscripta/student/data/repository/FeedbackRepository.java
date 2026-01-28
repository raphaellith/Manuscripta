package com.manuscripta.student.data.repository;

import com.manuscripta.student.domain.model.Feedback;

import java.util.List;

/**
 * Repository interface for managing feedback operations.
 * Provides methods for fetching, storing, and querying teacher feedback.
 */
public interface FeedbackRepository {

    /**
     * Fetches feedback from the server and stores it locally.
     * This method should be called when a RETURN_FEEDBACK TCP signal is received.
     *
     * @param deviceId The device ID to fetch feedback for
     * @throws Exception if the fetch operation fails
     */
    void fetchAndStoreFeedback(String deviceId) throws Exception;

    /**
     * Gets feedback for a specific response from local storage.
     *
     * @param responseId The ID of the response
     * @return The feedback for that response, or null if not found
     */
    Feedback getFeedbackForResponse(String responseId);

    /**
     * Gets all feedback from local storage.
     *
     * @return List of all feedback
     */
    List<Feedback> getAllFeedback();

    /**
     * Gets all feedback for a specific device from local storage.
     *
     * @param deviceId The device ID to filter by
     * @return List of feedback for that device
     */
    List<Feedback> getAllFeedbackByDeviceId(String deviceId);

    /**
     * Deletes all feedback from local storage.
     */
    void deleteAllFeedback();

    /**
     * Gets the count of feedback entries.
     *
     * @return The total number of feedback entries
     */
    int getFeedbackCount();
}
