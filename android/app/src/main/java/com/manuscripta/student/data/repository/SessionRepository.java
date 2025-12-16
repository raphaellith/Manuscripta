package com.manuscripta.student.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.manuscripta.student.data.model.SessionStatus;
import com.manuscripta.student.domain.model.Session;

import java.util.List;

/**
 * Repository interface for managing learning sessions.
 * Provides abstraction over local storage and session lifecycle management.
 */
public interface SessionRepository {

    /**
     * Creates a new session for the given material in the RECEIVED state.
     * If there's already an active session, it will be completed first.
     * <p>
     * Note: The session is initially created in the {@link SessionStatus#RECEIVED} state and is not
     * considered "started" (i.e., not {@link SessionStatus#ACTIVE}) until the first interaction occurs.
     *
     * @param materialId The ID of the material to start a session for
     * @param deviceId   The ID of the device starting the session
     * @return The newly created session in RECEIVED state
     */
    @NonNull
    Session startSession(@NonNull String materialId, @NonNull String deviceId);

    /**
     * Gets the currently active session, if any.
     *
     * @return The active session, or null if no session is active
     */
    @Nullable
    Session getActiveSession();

    /**
     * Checks if there is currently an active session.
     *
     * @return true if there is an active session, false otherwise
     */
    boolean hasActiveSession();

    /**
     * Pauses the currently active session.
     *
     * @throws IllegalStateException if no session is currently active
     */
    void pauseSession();

    /**
     * Resumes a paused session.
     *
     * @param sessionId The ID of the session to resume
     * @throws IllegalArgumentException if sessionId is null or empty
     * @throws IllegalStateException if the session is not in PAUSED status
     */
    void resumeSession(@NonNull String sessionId);

    /**
     * Completes the currently active session.
     *
     * @throws IllegalStateException if no session is currently active
     */
    void completeSession();

    /**
     * Cancels the currently active session.
     *
     * @throws IllegalStateException if no session is currently active
     */
    void cancelSession();

    /**
     * Ends a specific session with the given status.
     *
     * @param sessionId The ID of the session to end
     * @param status    The final status (COMPLETED or CANCELLED)
     * @throws IllegalArgumentException if sessionId is null or empty
     * @throws IllegalArgumentException if status is not COMPLETED or CANCELLED
     */
    void endSession(@NonNull String sessionId, @NonNull SessionStatus status);

    /**
     * Gets a session by its unique identifier.
     *
     * @param id The UUID of the session
     * @return The session, or null if not found
     */
    @Nullable
    Session getSessionById(@NonNull String id);

    /**
     * Gets all sessions for a specific material.
     *
     * @param materialId The UUID of the material
     * @return List of sessions for that material
     */
    @NonNull
    List<Session> getSessionsByMaterialId(@NonNull String materialId);

    /**
     * Gets all sessions with a specific status.
     *
     * @param status The session status to filter by
     * @return List of sessions with that status
     */
    @NonNull
    List<Session> getSessionsByStatus(@NonNull SessionStatus status);

    /**
     * Gets all sessions.
     *
     * @return List of all sessions
     */
    @NonNull
    List<Session> getAllSessions();

    /**
     * Gets all sessions for a specific device.
     *
     * @param deviceId The device identifier
     * @return List of sessions for that device
     */
    @NonNull
    List<Session> getSessionsByDeviceId(@NonNull String deviceId);

    /**
     * Deletes a session by its ID.
     *
     * @param id The UUID of the session to delete
     */
    void deleteSession(@NonNull String id);

    /**
     * Deletes all sessions for a specific material.
     *
     * @param materialId The UUID of the material
     */
    void deleteSessionsByMaterialId(@NonNull String materialId);

    /**
     * Deletes all sessions.
     */
    void deleteAllSessions();

    /**
     * Gets the total count of sessions.
     *
     * @return The total number of sessions
     */
    int getSessionCount();

    /**
     * Gets the count of sessions with a specific status.
     *
     * @param status The session status to count
     * @return The number of sessions with that status
     */
    int getSessionCountByStatus(@NonNull SessionStatus status);
}
