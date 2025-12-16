package com.manuscripta.student.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.manuscripta.student.data.local.SessionDao;
import com.manuscripta.student.data.model.SessionEntity;
import com.manuscripta.student.data.model.SessionStatus;
import com.manuscripta.student.domain.mapper.SessionMapper;
import com.manuscripta.student.domain.model.Session;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of {@link SessionRepository} that manages learning sessions
 * with local storage and session lifecycle management.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Local persistence via Room DAO</li>
 *   <li>Active session tracking (only one active at a time)</li>
 *   <li>Session lifecycle management (start, pause, resume, complete, cancel)</li>
 * </ul>
 *
 * <p>
 * <b>Session Lifecycle:</b><br>
 * Sessions begin in the <code>RECEIVED</code> state when created, transition to <code>ACTIVE</code> when started,
 * and may then move to <code>PAUSED</code>, <code>COMPLETED</code>, or <code>CANCELLED</code> as appropriate.<br>
 * <br>
 * <code>RECEIVED → ACTIVE → PAUSED/COMPLETED/CANCELLED</code>
 * </p>
 */
@Singleton
public class SessionRepositoryImpl implements SessionRepository {

    /** The DAO for session persistence. */
    private final SessionDao sessionDao;

    /**
     * Creates a new SessionRepositoryImpl with the given DAO.
     *
     * @param sessionDao The DAO for session persistence
     * @throws IllegalArgumentException if sessionDao is null
     */
    @Inject
    public SessionRepositoryImpl(@NonNull SessionDao sessionDao) {
        if (sessionDao == null) {
            throw new IllegalArgumentException("SessionDao cannot be null");
        }
        this.sessionDao = sessionDao;
    }

    /**
     * Starts a new session for the given material and device.
     * If an active session exists, it will be paused automatically.
     *
     * @param materialId The ID of the material for the session
     * @param deviceId   The ID of the device starting the session
     * @return The newly created session
     * @throws IllegalArgumentException if materialId or deviceId is null or empty
     */
    @Override
    @NonNull
    public Session startSession(@NonNull String materialId, @NonNull String deviceId) {
        validateNotEmpty(materialId, "Material ID");
        validateNotEmpty(deviceId, "Device ID");

        // Pause any existing active session first (per Session Interaction §5(4)(c))
        SessionEntity activeSession = sessionDao.getActiveSession();
        if (activeSession != null) {
            sessionDao.endSession(activeSession.getId(), System.currentTimeMillis(),
                    SessionStatus.PAUSED);
        }

        // Create and save new session
        Session newSession = Session.create(materialId, deviceId);
        SessionEntity entity = SessionMapper.toEntity(newSession);
        sessionDao.insert(entity);

        return newSession;
    }

    /**
     * Retrieves the currently active session.
     *
     * @return The active session or null if no active session exists
     */
    @Override
    @Nullable
    public Session getActiveSession() {
        SessionEntity entity = sessionDao.getActiveSession();
        if (entity == null) {
            return null;
        }
        return SessionMapper.toDomain(entity);
    }

    /**
     * Checks whether an active session exists.
     *
     * @return true if an active session exists, false otherwise
     */
    @Override
    public boolean hasActiveSession() {
        return sessionDao.getActiveSession() != null;
    }

    /**
     * Pauses the currently active session.
     *
     * @throws IllegalStateException if no active session exists
     */
    @Override
    public void pauseSession() {
        SessionEntity activeSession = sessionDao.getActiveSession();
        if (activeSession == null) {
            throw new IllegalStateException("No active session to pause");
        }
        sessionDao.updateStatus(activeSession.getId(), SessionStatus.PAUSED);
    }

    /**
     * Resumes a paused session. If another session is active, it will be paused first.
     *
     * @param sessionId The ID of the session to resume
     * @throws IllegalArgumentException if sessionId is null, empty, or does not exist
     * @throws IllegalStateException    if the session is not in PAUSED state
     */
    @Override
    public void resumeSession(@NonNull String sessionId) {
        validateNotEmpty(sessionId, "Session ID");

        SessionEntity session = sessionDao.getById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        if (session.getStatus() != SessionStatus.PAUSED) {
            throw new IllegalStateException("Cannot resume session that is not paused. "
                    + "Current status: " + session.getStatus());
        }

        // Pause any other active session first (per Session Interaction §5(4)(c))
        SessionEntity currentActive = sessionDao.getActiveSession();
        if (currentActive != null) {
            sessionDao.endSession(currentActive.getId(), System.currentTimeMillis(),
                    SessionStatus.PAUSED);
        }

        sessionDao.updateStatus(sessionId, SessionStatus.ACTIVE);
    }

    /**
     * Activates a session that is in RECEIVED state.
     *
     * @param sessionId The ID of the session to activate
     * @throws IllegalArgumentException if sessionId is null, empty, or does not exist
     * @throws IllegalStateException    if the session is not in RECEIVED state
     */
    @Override
    public void activateSession(@NonNull String sessionId) {
        validateNotEmpty(sessionId, "Session ID");

        SessionEntity session = sessionDao.getById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        if (session.getStatus() != SessionStatus.RECEIVED) {
            throw new IllegalStateException("Cannot activate session that is not in RECEIVED state. "
                    + "Current status: " + session.getStatus());
        }

        sessionDao.activateSession(sessionId, System.currentTimeMillis());
    }

    /**
     * Marks the currently active session as completed.
     *
     * @throws IllegalStateException if no active session exists
     */
    @Override
    public void completeSession() {
        SessionEntity activeSession = sessionDao.getActiveSession();
        if (activeSession == null) {
            throw new IllegalStateException("No active session to complete");
        }
        sessionDao.endSession(activeSession.getId(), System.currentTimeMillis(),
                SessionStatus.COMPLETED);
    }

    /**
     * Marks the currently active session as cancelled.
     *
     * @throws IllegalStateException if no active session exists
     */
    @Override
    public void cancelSession() {
        SessionEntity activeSession = sessionDao.getActiveSession();
        if (activeSession == null) {
            throw new IllegalStateException("No active session to cancel");
        }
        sessionDao.endSession(activeSession.getId(), System.currentTimeMillis(),
                SessionStatus.CANCELLED);
    }

    /**
     * Ends a session with the specified status.
     *
     * @param sessionId The ID of the session to end
     * @param status    The final status (must be COMPLETED or CANCELLED)
     * @throws IllegalArgumentException if sessionId is null, empty, or status is invalid
     */
    @Override
    public void endSession(@NonNull String sessionId, @NonNull SessionStatus status) {
        validateNotEmpty(sessionId, "Session ID");
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        if (status != SessionStatus.COMPLETED && status != SessionStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "End session status must be COMPLETED or CANCELLED, got: " + status);
        }

        sessionDao.endSession(sessionId, System.currentTimeMillis(), status);
    }

    /**
     * Retrieves a session by its ID.
     *
     * @param id The session ID
     * @return The session or null if not found
     * @throws IllegalArgumentException if id is null or empty
     */
    @Override
    @Nullable
    public Session getSessionById(@NonNull String id) {
        validateNotEmpty(id, "Session ID");

        SessionEntity entity = sessionDao.getById(id);
        if (entity == null) {
            return null;
        }
        return SessionMapper.toDomain(entity);
    }

    /**
     * Retrieves all sessions associated with the given material.
     *
     * @param materialId The material ID
     * @return List of sessions for the material (empty if none found)
     * @throws IllegalArgumentException if materialId is null or empty
     */
    @Override
    @NonNull
    public List<Session> getSessionsByMaterialId(@NonNull String materialId) {
        validateNotEmpty(materialId, "Material ID");

        List<SessionEntity> entities = sessionDao.getByMaterialId(materialId);
        return mapEntitiesToDomain(entities);
    }

    /**
     * Retrieves all sessions with the specified status.
     *
     * @param status The session status to filter by
     * @return List of sessions with the given status (empty if none found)
     * @throws IllegalArgumentException if status is null
     */
    @Override
    @NonNull
    public List<Session> getSessionsByStatus(@NonNull SessionStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        List<SessionEntity> entities = sessionDao.getByStatus(status);
        return mapEntitiesToDomain(entities);
    }

    /**
     * Retrieves all sessions.
     *
     * @return List of all sessions (empty if none found)
     */
    @Override
    @NonNull
    public List<Session> getAllSessions() {
        List<SessionEntity> entities = sessionDao.getAll();
        return mapEntitiesToDomain(entities);
    }

    /**
     * Retrieves all sessions associated with the given device.
     *
     * @param deviceId The device ID
     * @return List of sessions for the device (empty if none found)
     * @throws IllegalArgumentException if deviceId is null or empty
     */
    @Override
    @NonNull
    public List<Session> getSessionsByDeviceId(@NonNull String deviceId) {
        validateNotEmpty(deviceId, "Device ID");

        List<SessionEntity> entities = sessionDao.getByDeviceId(deviceId);
        return mapEntitiesToDomain(entities);
    }

    /**
     * Deletes a session by its ID.
     *
     * @param id The session ID
     * @throws IllegalArgumentException if id is null or empty
     */
    @Override
    public void deleteSession(@NonNull String id) {
        validateNotEmpty(id, "Session ID");
        sessionDao.deleteById(id);
    }

    /**
     * Deletes all sessions associated with the given material.
     *
     * @param materialId The material ID
     * @throws IllegalArgumentException if materialId is null or empty
     */
    @Override
    public void deleteSessionsByMaterialId(@NonNull String materialId) {
        validateNotEmpty(materialId, "Material ID");
        sessionDao.deleteByMaterialId(materialId);
    }

    /**
     * Deletes all sessions.
     */
    @Override
    public void deleteAllSessions() {
        sessionDao.deleteAll();
    }

    /**
     * Gets the total number of sessions.
     *
     * @return The count of all sessions
     */
    @Override
    public int getSessionCount() {
        return sessionDao.getCount();
    }

    /**
     * Gets the count of sessions with the specified status.
     *
     * @param status The session status to filter by
     * @return The count of sessions with the given status
     * @throws IllegalArgumentException if status is null
     */
    @Override
    public int getSessionCountByStatus(@NonNull SessionStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        return sessionDao.getCountByStatus(status);
    }

    /**
     * Validates that a string parameter is not null or empty.
     *
     * @param value     The value to validate
     * @param fieldName The name of the field for error messages
     * @throws IllegalArgumentException if the value is null or empty
     */
    private void validateNotEmpty(@Nullable String value, @NonNull String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }

    /**
     * Maps a list of SessionEntity objects to Session domain objects.
     *
     * @param entities The list of entities to map
     * @return List of domain objects
     */
    @NonNull
    private List<Session> mapEntitiesToDomain(@NonNull List<SessionEntity> entities) {
        List<Session> sessions = new ArrayList<>(entities.size());
        for (SessionEntity entity : entities) {
            sessions.add(SessionMapper.toDomain(entity));
        }
        return sessions;
    }
}
