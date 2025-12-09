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

    @Override
    @NonNull
    public Session startSession(@NonNull String materialId, @NonNull String deviceId) {
        validateNotEmpty(materialId, "Material ID");
        validateNotEmpty(deviceId, "Device ID");

        // Complete any existing active session first
        SessionEntity activeSession = sessionDao.getActiveSession();
        if (activeSession != null) {
            sessionDao.endSession(activeSession.getId(), System.currentTimeMillis(),
                    SessionStatus.COMPLETED);
        }

        // Create and save new session
        Session newSession = Session.create(materialId, deviceId);
        SessionEntity entity = SessionMapper.toEntity(newSession);
        sessionDao.insert(entity);

        return newSession;
    }

    @Override
    @Nullable
    public Session getActiveSession() {
        SessionEntity entity = sessionDao.getActiveSession();
        if (entity == null) {
            return null;
        }
        return SessionMapper.toDomain(entity);
    }

    @Override
    public boolean hasActiveSession() {
        return sessionDao.getActiveSession() != null;
    }

    @Override
    public void pauseSession() {
        SessionEntity activeSession = sessionDao.getActiveSession();
        if (activeSession == null) {
            throw new IllegalStateException("No active session to pause");
        }
        sessionDao.updateStatus(activeSession.getId(), SessionStatus.PAUSED);
    }

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

        // Complete any other active session first
        SessionEntity currentActive = sessionDao.getActiveSession();
        if (currentActive != null) {
            sessionDao.endSession(currentActive.getId(), System.currentTimeMillis(),
                    SessionStatus.COMPLETED);
        }

        sessionDao.updateStatus(sessionId, SessionStatus.ACTIVE);
    }

    @Override
    public void completeSession() {
        SessionEntity activeSession = sessionDao.getActiveSession();
        if (activeSession == null) {
            throw new IllegalStateException("No active session to complete");
        }
        sessionDao.endSession(activeSession.getId(), System.currentTimeMillis(),
                SessionStatus.COMPLETED);
    }

    @Override
    public void cancelSession() {
        SessionEntity activeSession = sessionDao.getActiveSession();
        if (activeSession == null) {
            throw new IllegalStateException("No active session to cancel");
        }
        sessionDao.endSession(activeSession.getId(), System.currentTimeMillis(),
                SessionStatus.CANCELLED);
    }

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

    @Override
    @NonNull
    public List<Session> getSessionsByMaterialId(@NonNull String materialId) {
        validateNotEmpty(materialId, "Material ID");

        List<SessionEntity> entities = sessionDao.getByMaterialId(materialId);
        return mapEntitiesToDomain(entities);
    }

    @Override
    @NonNull
    public List<Session> getSessionsByStatus(@NonNull SessionStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        List<SessionEntity> entities = sessionDao.getByStatus(status);
        return mapEntitiesToDomain(entities);
    }

    @Override
    @NonNull
    public List<Session> getAllSessions() {
        List<SessionEntity> entities = sessionDao.getAll();
        return mapEntitiesToDomain(entities);
    }

    @Override
    @NonNull
    public List<Session> getSessionsByDeviceId(@NonNull String deviceId) {
        validateNotEmpty(deviceId, "Device ID");

        List<SessionEntity> entities = sessionDao.getByDeviceId(deviceId);
        return mapEntitiesToDomain(entities);
    }

    @Override
    public void deleteSession(@NonNull String id) {
        validateNotEmpty(id, "Session ID");
        sessionDao.deleteById(id);
    }

    @Override
    public void deleteSessionsByMaterialId(@NonNull String materialId) {
        validateNotEmpty(materialId, "Material ID");
        sessionDao.deleteByMaterialId(materialId);
    }

    @Override
    public void deleteAllSessions() {
        sessionDao.deleteAll();
    }

    @Override
    public int getSessionCount() {
        return sessionDao.getCount();
    }

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
