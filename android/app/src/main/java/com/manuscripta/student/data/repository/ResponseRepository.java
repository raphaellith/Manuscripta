package com.manuscripta.student.data.repository;

import androidx.annotation.NonNull;

import com.manuscripta.student.domain.common.Result;
import com.manuscripta.student.domain.model.Response;

import java.util.List;

/**
 * Repository interface for managing student responses.
 * Provides methods for CRUD operations and sync functionality for offline-first architecture.
 *
 * <p>All responses are persisted locally first, then synced to the teacher's Windows app
 * when network is available. This ensures students can submit answers even without
 * network connectivity.</p>
 */
public interface ResponseRepository {

    /**
     * Saves a new response to local storage.
     * The response will be queued for sync with the teacher's Windows app.
     *
     * @param response The response to save
     * @return Result containing the saved Response on success, or error on failure
     */
    @NonNull
    Result<Response> saveResponse(@NonNull Response response);

    /**
     * Gets a response by its unique identifier.
     *
     * @param id The UUID of the response
     * @return Result containing the Response if found, null if not found, or error on failure
     */
    @NonNull
    Result<Response> getResponseById(@NonNull String id);

    /**
     * Gets all responses for a specific question.
     *
     * @param questionId The UUID of the parent question
     * @return Result containing list of responses, or error on failure
     */
    @NonNull
    Result<List<Response>> getResponsesByQuestionId(@NonNull String questionId);

    /**
     * Gets all responses from local storage.
     *
     * @return Result containing list of all responses, or error on failure
     */
    @NonNull
    Result<List<Response>> getAllResponses();

    /**
     * Gets all unsynced responses that need to be sent to the teacher's Windows app.
     *
     * @return Result containing list of unsynced responses, or error on failure
     */
    @NonNull
    Result<List<Response>> getUnsyncedResponses();

    /**
     * Gets the count of unsynced responses.
     *
     * @return Result containing the count of pending responses, or error on failure
     */
    @NonNull
    Result<Integer> getUnsyncedCount();

    /**
     * Marks a response as synced after successful transmission to the teacher's app.
     *
     * @param id The UUID of the response to mark as synced
     * @return Result containing success (true) or failure
     */
    @NonNull
    Result<Boolean> markResponseSynced(@NonNull String id);

    /**
     * Marks multiple responses as synced after successful batch transmission.
     *
     * @param ids List of response UUIDs to mark as synced
     * @return Result containing success (true) or failure
     */
    @NonNull
    Result<Boolean> markResponsesSynced(@NonNull List<String> ids);

    /**
     * Updates an existing response.
     *
     * @param response The response with updated values
     * @return Result containing success (true) or failure
     */
    @NonNull
    Result<Boolean> updateResponse(@NonNull Response response);

    /**
     * Deletes a response by its unique identifier.
     *
     * @param id The UUID of the response to delete
     * @return Result containing success (true) or failure
     */
    @NonNull
    Result<Boolean> deleteResponse(@NonNull String id);

    /**
     * Deletes all responses for a specific question.
     *
     * @param questionId The UUID of the parent question
     * @return Result containing success (true) or failure
     */
    @NonNull
    Result<Boolean> deleteResponsesByQuestionId(@NonNull String questionId);

    /**
     * Deletes all responses from local storage.
     *
     * @return Result containing success (true) or failure
     */
    @NonNull
    Result<Boolean> deleteAllResponses();

    /**
     * Synchronizes all pending responses with the teacher's Windows app.
     * Uses exponential backoff for retry logic on network failures.
     *
     * @return Result containing the number of successfully synced responses, or error on failure
     */
    @NonNull
    Result<Integer> syncPendingResponses();

    /**
     * Adds a response to the sync queue for deferred transmission.
     * This is called automatically when a response is saved while offline.
     *
     * @param responseId The UUID of the response to queue for sync
     */
    void queueForSync(@NonNull String responseId);

    /**
     * Gets the current size of the sync queue.
     *
     * @return The number of responses waiting to be synced
     */
    int getSyncQueueSize();

    /**
     * Clears the sync queue without syncing.
     * Use with caution - this may result in data loss.
     */
    void clearSyncQueue();
}
