package com.manuscripta.student.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.manuscripta.student.data.model.SessionEntity;
import com.manuscripta.student.data.model.SessionStatus;

import java.util.List;

/**
 * Data Access Object for {@link SessionEntity}.
 * Provides methods for CRUD operations and session lifecycle tracking.
 */
@Dao
public interface SessionDao {

    /**
     * Get all sessions from the database.
     *
     * @return List of all sessions ordered by start time (newest first)
     */
    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    List<SessionEntity> getAll();

    /**
     * Get a session by its unique identifier.
     *
     * @param id The UUID of the session
     * @return The session entity, or null if not found
     */
    @Query("SELECT * FROM sessions WHERE id = :id")
    SessionEntity getById(String id);

    /**
     * Get all sessions for a specific material.
     *
     * @param materialId The UUID of the material
     * @return List of sessions for that material
     */
    @Query("SELECT * FROM sessions WHERE materialId = :materialId ORDER BY startTime DESC")
    List<SessionEntity> getByMaterialId(String materialId);

    /**
     * Get all sessions with a specific status.
     *
     * @param status The session status to filter by
     * @return List of sessions with that status
     */
    @Query("SELECT * FROM sessions WHERE status = :status ORDER BY startTime DESC")
    List<SessionEntity> getByStatus(SessionStatus status);

    /**
     * Get the currently active session (if any).
     *
     * @return The active session, or null if none
     */
    @Query("SELECT * FROM sessions WHERE status = 'ACTIVE' LIMIT 1")
    SessionEntity getActiveSession();

    /**
     * Get all sessions for a specific device.
     *
     * @param deviceId The device identifier
     * @return List of sessions for that device
     */
    @Query("SELECT * FROM sessions WHERE deviceId = :deviceId ORDER BY startTime DESC")
    List<SessionEntity> getByDeviceId(String deviceId);

    /**
     * Update the status of a session.
     *
     * @param id The UUID of the session
     * @param status The new status
     */
    @Query("UPDATE sessions SET status = :status WHERE id = :id")
    void updateStatus(String id, SessionStatus status);

    /**
     * End a session by setting its end time and status.
     *
     * @param id The UUID of the session
     * @param endTime The end timestamp
     * @param status The final status (COMPLETED or CANCELLED)
     */
    @Query("UPDATE sessions SET endTime = :endTime, status = :status WHERE id = :id")
    void endSession(String id, long endTime, SessionStatus status);

    /**
     * Insert a new session into the database.
     * If a session with the same ID already exists, it will be replaced.
     *
     * @param session The session to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SessionEntity session);

    /**
     * Insert multiple sessions into the database.
     *
     * @param sessions The sessions to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SessionEntity> sessions);

    /**
     * Update an existing session in the database.
     *
     * @param session The session with updated values
     */
    @Update
    void update(SessionEntity session);

    /**
     * Delete a session from the database.
     *
     * @param session The session to delete
     */
    @Delete
    void delete(SessionEntity session);

    /**
     * Delete a session by its unique identifier.
     *
     * @param id The UUID of the session to delete
     */
    @Query("DELETE FROM sessions WHERE id = :id")
    void deleteById(String id);

    /**
     * Delete all sessions for a specific material.
     *
     * @param materialId The UUID of the material
     */
    @Query("DELETE FROM sessions WHERE materialId = :materialId")
    void deleteByMaterialId(String materialId);

    /**
     * Delete all sessions from the database.
     */
    @Query("DELETE FROM sessions")
    void deleteAll();

    /**
     * Get the count of all sessions.
     *
     * @return The total number of sessions
     */
    @Query("SELECT COUNT(*) FROM sessions")
    int getCount();

    /**
     * Get the count of sessions with a specific status.
     *
     * @param status The session status to count
     * @return The number of sessions with that status
     */
    @Query("SELECT COUNT(*) FROM sessions WHERE status = :status")
    int getCountByStatus(SessionStatus status);
}
