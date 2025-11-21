package com.manuscripta.student.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.manuscripta.student.data.model.ResponseEntity;

import java.util.List;

/**
 * Data Access Object for {@link ResponseEntity}.
 * Provides methods for CRUD operations and sync status tracking.
 */
@Dao
public interface ResponseDao {

    /**
     * Get all responses from the database.
     *
     * @return List of all responses
     */
    @Query("SELECT * FROM responses ORDER BY timestamp DESC")
    List<ResponseEntity> getAll();

    /**
     * Get a response by its unique identifier.
     *
     * @param id The UUID of the response
     * @return The response entity, or null if not found
     */
    @Query("SELECT * FROM responses WHERE id = :id")
    ResponseEntity getById(String id);

    /**
     * Get all responses for a specific question.
     *
     * @param questionId The UUID of the parent question
     * @return List of responses for that question
     */
    @Query("SELECT * FROM responses WHERE questionId = :questionId ORDER BY timestamp DESC")
    List<ResponseEntity> getByQuestionId(String questionId);

    /**
     * Get all unsynced responses (need to be sent to teacher app).
     *
     * @return List of responses where synced = false
     */
    @Query("SELECT * FROM responses WHERE synced = 0 ORDER BY timestamp ASC")
    List<ResponseEntity> getUnsynced();

    /**
     * Get count of unsynced responses.
     *
     * @return Number of responses pending sync
     */
    @Query("SELECT COUNT(*) FROM responses WHERE synced = 0")
    int getUnsyncedCount();

    /**
     * Mark a response as synced.
     *
     * @param id The UUID of the response to mark as synced
     */
    @Query("UPDATE responses SET synced = 1 WHERE id = :id")
    void markSynced(String id);

    /**
     * Mark multiple responses as synced.
     *
     * @param ids List of response UUIDs to mark as synced
     */
    @Query("UPDATE responses SET synced = 1 WHERE id IN (:ids)")
    void markAllSynced(List<String> ids);

    /**
     * Insert a new response into the database.
     * If a response with the same ID already exists, it will be replaced.
     *
     * @param response The response to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ResponseEntity response);

    /**
     * Insert multiple responses into the database.
     *
     * @param responses The responses to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ResponseEntity> responses);

    /**
     * Update an existing response in the database.
     *
     * @param response The response with updated values
     */
    @Update
    void update(ResponseEntity response);

    /**
     * Delete a response from the database.
     *
     * @param response The response to delete
     */
    @Delete
    void delete(ResponseEntity response);

    /**
     * Delete a response by its unique identifier.
     *
     * @param id The UUID of the response to delete
     */
    @Query("DELETE FROM responses WHERE id = :id")
    void deleteById(String id);

    /**
     * Delete all responses for a specific question.
     *
     * @param questionId The UUID of the parent question
     */
    @Query("DELETE FROM responses WHERE questionId = :questionId")
    void deleteByQuestionId(String questionId);

    /**
     * Delete all responses from the database.
     */
    @Query("DELETE FROM responses")
    void deleteAll();

    /**
     * Get the count of all responses.
     *
     * @return The total number of responses
     */
    @Query("SELECT COUNT(*) FROM responses")
    int getCount();

    /**
     * Get the count of responses for a specific question.
     *
     * @param questionId The UUID of the parent question
     * @return The number of responses for that question
     */
    @Query("SELECT COUNT(*) FROM responses WHERE questionId = :questionId")
    int getCountByQuestionId(String questionId);
}
