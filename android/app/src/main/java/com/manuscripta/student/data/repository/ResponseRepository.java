package com.manuscripta.student.data.repository;

import androidx.annotation.NonNull;

import com.manuscripta.student.domain.model.Response;

import java.util.List;

/**
 * Repository interface for managing student responses.
 * Provides abstraction over local storage and network synchronization.
 */
public interface ResponseRepository {

    /**
     * Saves a response locally and queues it for sync.
     *
     * @param response The response to save
     */
    void saveResponse(@NonNull Response response);

    /**
     * Gets a response by its unique identifier.
     *
     * @param id The UUID of the response
     * @return The response, or null if not found
     */
    Response getResponseById(@NonNull String id);

    /**
     * Gets all responses for a specific question.
     *
     * @param questionId The UUID of the question
     * @return List of responses for that question
     */
    @NonNull
    List<Response> getResponsesByQuestionId(@NonNull String questionId);

    /**
     * Gets all responses.
     *
     * @return List of all responses
     */
    @NonNull
    List<Response> getAllResponses();

    /**
     * Gets all responses that have not been synced yet.
     *
     * @return List of unsynced responses
     */
    @NonNull
    List<Response> getUnsyncedResponses();

    /**
     * Gets the count of responses pending sync.
     *
     * @return Number of unsynced responses
     */
    int getUnsyncedCount();

    /**
     * Deletes a response by its ID.
     *
     * @param id The UUID of the response to delete
     */
    void deleteResponse(@NonNull String id);

    /**
     * Deletes all responses for a specific question.
     *
     * @param questionId The UUID of the question
     */
    void deleteResponsesByQuestionId(@NonNull String questionId);

    /**
     * Deletes all responses.
     */
    void deleteAllResponses();

    /**
     * Triggers synchronization of all pending responses.
     * This method attempts to sync all unsynced responses to the server
     * with exponential backoff retry logic.
     */
    void syncPendingResponses();

    /**
     * Callback interface for sync operations.
     */
    interface SyncCallback {
        /**
         * Called when a response is successfully synced.
         *
         * @param responseId The ID of the synced response
         */
        void onSyncSuccess(@NonNull String responseId);

        /**
         * Called when a response sync fails.
         *
         * @param responseId The ID of the response that failed to sync
         * @param error      The error message
         */
        void onSyncFailure(@NonNull String responseId, @NonNull String error);

        /**
         * Called when all pending responses have been processed.
         *
         * @param successCount Number of successfully synced responses
         * @param failureCount Number of failed syncs
         */
        void onSyncComplete(int successCount, int failureCount);
    }

    /**
     * Triggers synchronization with a callback for progress updates.
     *
     * @param callback The callback to receive sync updates
     */
    void syncPendingResponses(SyncCallback callback);
}
