package com.manuscripta.student.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.manuscripta.student.data.model.FeedbackEntity;

import java.util.List;

/**
 * Data Access Object for {@link FeedbackEntity}.
 * Provides methods for CRUD operations on teacher feedback.
 */
@Dao
public interface FeedbackDao {

    /**
     * Get all feedback from the database.
     *
     * @return List of all feedback
     */
    @Query("SELECT * FROM feedback")
    List<FeedbackEntity> getAll();

    /**
     * Get a feedback by its unique identifier.
     *
     * @param id The UUID of the feedback
     * @return The feedback entity, or null if not found
     */
    @Query("SELECT * FROM feedback WHERE id = :id")
    FeedbackEntity getById(String id);

    /**
     * Get feedback for a specific response.
     *
     * @param responseId The UUID of the parent response
     * @return The feedback entity for that response, or null if not found
     */
    @Query("SELECT * FROM feedback WHERE responseId = :responseId")
    FeedbackEntity getByResponseId(String responseId);

    /**
     * Get all feedback for responses belonging to a specific device.
     * This performs a JOIN with the responses table to filter by device ID.
     *
     * @param deviceId The device ID to filter by
     * @return List of feedback for responses from that device
     */
    @Query("SELECT f.* FROM feedback f "
            + "INNER JOIN responses r ON f.responseId = r.id "
            + "WHERE r.deviceId = :deviceId")
    List<FeedbackEntity> getAllByDeviceId(String deviceId);

    /**
     * Insert a new feedback into the database.
     * If a feedback with the same ID already exists, it will be replaced.
     *
     * @param feedback The feedback to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FeedbackEntity feedback);

    /**
     * Insert multiple feedback entries into the database.
     *
     * @param feedbackList The list of feedback to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FeedbackEntity> feedbackList);

    /**
     * Delete a feedback from the database.
     *
     * @param feedback The feedback to delete
     */
    @Delete
    void delete(FeedbackEntity feedback);

    /**
     * Delete feedback by its unique identifier.
     *
     * @param id The UUID of the feedback to delete
     */
    @Query("DELETE FROM feedback WHERE id = :id")
    void deleteById(String id);

    /**
     * Delete feedback for a specific response.
     *
     * @param responseId The UUID of the parent response
     */
    @Query("DELETE FROM feedback WHERE responseId = :responseId")
    void deleteByResponseId(String responseId);

    /**
     * Delete all feedback from the database.
     */
    @Query("DELETE FROM feedback")
    void deleteAll();

    /**
     * Get the count of all feedback entries.
     *
     * @return The total number of feedback entries
     */
    @Query("SELECT COUNT(*) FROM feedback")
    int getCount();
}
