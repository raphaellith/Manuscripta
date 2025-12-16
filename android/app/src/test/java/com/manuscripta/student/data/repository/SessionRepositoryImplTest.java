package com.manuscripta.student.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.manuscripta.student.data.local.SessionDao;
import com.manuscripta.student.data.model.SessionEntity;
import com.manuscripta.student.data.model.SessionStatus;
import com.manuscripta.student.domain.model.Session;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link SessionRepositoryImpl}.
 */
public class SessionRepositoryImplTest {

    private SessionDao mockDao;
    private SessionRepositoryImpl repository;

    private static final String TEST_ID = "test-session-id";
    private static final String TEST_MATERIAL_ID = "test-material-id";
    private static final String TEST_DEVICE_ID = "test-device-id";

    @Before
    public void setUp() {
        mockDao = mock(SessionDao.class);
        repository = new SessionRepositoryImpl(mockDao);
    }

    // ==================== Constructor Tests ====================

    @Test
    public void testConstructor_validDao_createsInstance() {
        SessionRepositoryImpl repo = new SessionRepositoryImpl(mockDao);
        assertNotNull(repo);
    }

    @Test
    public void testConstructor_nullDao_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SessionRepositoryImpl(null)
        );
        assertEquals("SessionDao cannot be null", exception.getMessage());
    }

    // ==================== startSession Tests ====================

    @Test
    public void testStartSession_noActiveSession_createsNewSession() {
        when(mockDao.getActiveSession()).thenReturn(null);

        Session session = repository.startSession(TEST_MATERIAL_ID, TEST_DEVICE_ID);

        assertNotNull(session);
        assertEquals(TEST_MATERIAL_ID, session.getMaterialId());
        assertEquals(TEST_DEVICE_ID, session.getDeviceId());
        assertEquals(SessionStatus.RECEIVED, session.getStatus());
        assertEquals(0, session.getStartTime()); // Not set until first interaction
        verify(mockDao).insert(any(SessionEntity.class));
    }

    @Test
    public void testStartSession_hasActiveSession_completesExistingAndCreatesNew() {
        SessionEntity activeEntity = createTestEntity(TEST_ID, SessionStatus.ACTIVE);
        when(mockDao.getActiveSession()).thenReturn(activeEntity);

        Session session = repository.startSession(TEST_MATERIAL_ID, TEST_DEVICE_ID);

        assertNotNull(session);
        verify(mockDao).endSession(eq(TEST_ID), anyLong(), eq(SessionStatus.COMPLETED));
        verify(mockDao).insert(any(SessionEntity.class));
    }

    @Test
    public void testStartSession_nullMaterialId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.startSession(null, TEST_DEVICE_ID)
        );
        assertEquals("Material ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testStartSession_emptyMaterialId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.startSession("", TEST_DEVICE_ID)
        );
        assertEquals("Material ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testStartSession_blankMaterialId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.startSession("   ", TEST_DEVICE_ID)
        );
        assertEquals("Material ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testStartSession_nullDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.startSession(TEST_MATERIAL_ID, null)
        );
        assertEquals("Device ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testStartSession_emptyDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.startSession(TEST_MATERIAL_ID, "")
        );
        assertEquals("Device ID cannot be null or empty", exception.getMessage());
    }

    // ==================== getActiveSession Tests ====================

    @Test
    public void testGetActiveSession_hasActive_returnsSession() {
        SessionEntity entity = createTestEntity(TEST_ID, SessionStatus.ACTIVE);
        when(mockDao.getActiveSession()).thenReturn(entity);

        Session result = repository.getActiveSession();

        assertNotNull(result);
        assertEquals(TEST_ID, result.getId());
    }

    @Test
    public void testGetActiveSession_noActive_returnsNull() {
        when(mockDao.getActiveSession()).thenReturn(null);

        Session result = repository.getActiveSession();

        assertNull(result);
    }

    // ==================== hasActiveSession Tests ====================

    @Test
    public void testHasActiveSession_hasActive_returnsTrue() {
        SessionEntity entity = createTestEntity(TEST_ID, SessionStatus.ACTIVE);
        when(mockDao.getActiveSession()).thenReturn(entity);

        assertTrue(repository.hasActiveSession());
    }

    @Test
    public void testHasActiveSession_noActive_returnsFalse() {
        when(mockDao.getActiveSession()).thenReturn(null);

        assertFalse(repository.hasActiveSession());
    }

    // ==================== pauseSession Tests ====================

    @Test
    public void testPauseSession_hasActive_updatesStatus() {
        SessionEntity entity = createTestEntity(TEST_ID, SessionStatus.ACTIVE);
        when(mockDao.getActiveSession()).thenReturn(entity);

        repository.pauseSession();

        verify(mockDao).updateStatus(TEST_ID, SessionStatus.PAUSED);
    }

    @Test
    public void testPauseSession_noActive_throwsException() {
        when(mockDao.getActiveSession()).thenReturn(null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> repository.pauseSession()
        );
        assertEquals("No active session to pause", exception.getMessage());
    }

    // ==================== resumeSession Tests ====================

    @Test
    public void testResumeSession_pausedSession_updatesStatus() {
        SessionEntity pausedEntity = createTestEntity(TEST_ID, SessionStatus.PAUSED);
        when(mockDao.getById(TEST_ID)).thenReturn(pausedEntity);
        when(mockDao.getActiveSession()).thenReturn(null);

        repository.resumeSession(TEST_ID);

        verify(mockDao).updateStatus(TEST_ID, SessionStatus.ACTIVE);
    }

    @Test
    public void testResumeSession_pausedSessionWithActiveSession_completesActiveFirst() {
        SessionEntity pausedEntity = createTestEntity(TEST_ID, SessionStatus.PAUSED);
        SessionEntity activeEntity = createTestEntity("active-id", SessionStatus.ACTIVE);
        when(mockDao.getById(TEST_ID)).thenReturn(pausedEntity);
        when(mockDao.getActiveSession()).thenReturn(activeEntity);

        repository.resumeSession(TEST_ID);

        verify(mockDao).endSession(eq("active-id"), anyLong(), eq(SessionStatus.COMPLETED));
        verify(mockDao).updateStatus(TEST_ID, SessionStatus.ACTIVE);
    }

    @Test
    public void testResumeSession_nullId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.resumeSession(null)
        );
        assertEquals("Session ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testResumeSession_emptyId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.resumeSession("")
        );
        assertEquals("Session ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testResumeSession_sessionNotFound_throwsException() {
        when(mockDao.getById(anyString())).thenReturn(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.resumeSession("non-existent")
        );
        assertTrue(exception.getMessage().contains("Session not found"));
    }

    @Test
    public void testResumeSession_notPaused_throwsException() {
        SessionEntity activeEntity = createTestEntity(TEST_ID, SessionStatus.ACTIVE);
        when(mockDao.getById(TEST_ID)).thenReturn(activeEntity);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> repository.resumeSession(TEST_ID)
        );
        assertTrue(exception.getMessage().contains("Cannot resume session that is not paused"));
    }

    // ==================== completeSession Tests ====================

    @Test
    public void testCompleteSession_hasActive_endsSession() {
        SessionEntity entity = createTestEntity(TEST_ID, SessionStatus.ACTIVE);
        when(mockDao.getActiveSession()).thenReturn(entity);

        repository.completeSession();

        verify(mockDao).endSession(eq(TEST_ID), anyLong(), eq(SessionStatus.COMPLETED));
    }

    @Test
    public void testCompleteSession_noActive_throwsException() {
        when(mockDao.getActiveSession()).thenReturn(null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> repository.completeSession()
        );
        assertEquals("No active session to complete", exception.getMessage());
    }

    // ==================== cancelSession Tests ====================

    @Test
    public void testCancelSession_hasActive_endsSession() {
        SessionEntity entity = createTestEntity(TEST_ID, SessionStatus.ACTIVE);
        when(mockDao.getActiveSession()).thenReturn(entity);

        repository.cancelSession();

        verify(mockDao).endSession(eq(TEST_ID), anyLong(), eq(SessionStatus.CANCELLED));
    }

    @Test
    public void testCancelSession_noActive_throwsException() {
        when(mockDao.getActiveSession()).thenReturn(null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> repository.cancelSession()
        );
        assertEquals("No active session to cancel", exception.getMessage());
    }

    // ==================== endSession Tests ====================

    @Test
    public void testEndSession_completedStatus_endsSession() {
        repository.endSession(TEST_ID, SessionStatus.COMPLETED);

        verify(mockDao).endSession(eq(TEST_ID), anyLong(), eq(SessionStatus.COMPLETED));
    }

    @Test
    public void testEndSession_cancelledStatus_endsSession() {
        repository.endSession(TEST_ID, SessionStatus.CANCELLED);

        verify(mockDao).endSession(eq(TEST_ID), anyLong(), eq(SessionStatus.CANCELLED));
    }

    @Test
    public void testEndSession_nullId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.endSession(null, SessionStatus.COMPLETED)
        );
        assertEquals("Session ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testEndSession_emptyId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.endSession("", SessionStatus.COMPLETED)
        );
        assertEquals("Session ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testEndSession_nullStatus_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.endSession(TEST_ID, null)
        );
        assertEquals("Status cannot be null", exception.getMessage());
    }

    @Test
    public void testEndSession_activeStatus_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.endSession(TEST_ID, SessionStatus.ACTIVE)
        );
        assertTrue(exception.getMessage().contains("must be COMPLETED or CANCELLED"));
    }

    @Test
    public void testEndSession_pausedStatus_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.endSession(TEST_ID, SessionStatus.PAUSED)
        );
        assertTrue(exception.getMessage().contains("must be COMPLETED or CANCELLED"));
    }

    // ==================== getSessionById Tests ====================

    @Test
    public void testGetSessionById_existingId_returnsSession() {
        SessionEntity entity = createTestEntity(TEST_ID, SessionStatus.ACTIVE);
        when(mockDao.getById(TEST_ID)).thenReturn(entity);

        Session result = repository.getSessionById(TEST_ID);

        assertNotNull(result);
        assertEquals(TEST_ID, result.getId());
    }

    @Test
    public void testGetSessionById_nonExistent_returnsNull() {
        when(mockDao.getById(anyString())).thenReturn(null);

        Session result = repository.getSessionById("non-existent");

        assertNull(result);
    }

    @Test
    public void testGetSessionById_nullId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.getSessionById(null)
        );
        assertEquals("Session ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testGetSessionById_emptyId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.getSessionById("")
        );
        assertEquals("Session ID cannot be null or empty", exception.getMessage());
    }

    // ==================== getSessionsByMaterialId Tests ====================

    @Test
    public void testGetSessionsByMaterialId_hasSessions_returnsList() {
        List<SessionEntity> entities = Arrays.asList(
                createTestEntity("id1", SessionStatus.ACTIVE),
                createTestEntity("id2", SessionStatus.COMPLETED)
        );
        when(mockDao.getByMaterialId(TEST_MATERIAL_ID)).thenReturn(entities);

        List<Session> result = repository.getSessionsByMaterialId(TEST_MATERIAL_ID);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testGetSessionsByMaterialId_noSessions_returnsEmptyList() {
        when(mockDao.getByMaterialId(anyString())).thenReturn(Collections.emptyList());

        List<Session> result = repository.getSessionsByMaterialId(TEST_MATERIAL_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetSessionsByMaterialId_nullId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.getSessionsByMaterialId(null)
        );
        assertEquals("Material ID cannot be null or empty", exception.getMessage());
    }

    // ==================== getSessionsByStatus Tests ====================

    @Test
    public void testGetSessionsByStatus_hasSessions_returnsList() {
        List<SessionEntity> entities = Arrays.asList(
                createTestEntity("id1", SessionStatus.ACTIVE),
                createTestEntity("id2", SessionStatus.ACTIVE)
        );
        when(mockDao.getByStatus(SessionStatus.ACTIVE)).thenReturn(entities);

        List<Session> result = repository.getSessionsByStatus(SessionStatus.ACTIVE);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testGetSessionsByStatus_nullStatus_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.getSessionsByStatus(null)
        );
        assertEquals("Status cannot be null", exception.getMessage());
    }

    // ==================== getAllSessions Tests ====================

    @Test
    public void testGetAllSessions_hasSessions_returnsList() {
        List<SessionEntity> entities = Arrays.asList(
                createTestEntity("id1", SessionStatus.ACTIVE),
                createTestEntity("id2", SessionStatus.COMPLETED)
        );
        when(mockDao.getAll()).thenReturn(entities);

        List<Session> result = repository.getAllSessions();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testGetAllSessions_noSessions_returnsEmptyList() {
        when(mockDao.getAll()).thenReturn(Collections.emptyList());

        List<Session> result = repository.getAllSessions();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== getSessionsByDeviceId Tests ====================

    @Test
    public void testGetSessionsByDeviceId_hasSessions_returnsList() {
        List<SessionEntity> entities = Arrays.asList(
                createTestEntity("id1", SessionStatus.ACTIVE),
                createTestEntity("id2", SessionStatus.COMPLETED)
        );
        when(mockDao.getByDeviceId(TEST_DEVICE_ID)).thenReturn(entities);

        List<Session> result = repository.getSessionsByDeviceId(TEST_DEVICE_ID);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testGetSessionsByDeviceId_nullId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.getSessionsByDeviceId(null)
        );
        assertEquals("Device ID cannot be null or empty", exception.getMessage());
    }

    // ==================== deleteSession Tests ====================

    @Test
    public void testDeleteSession_validId_deletesSession() {
        repository.deleteSession(TEST_ID);

        verify(mockDao).deleteById(TEST_ID);
    }

    @Test
    public void testDeleteSession_nullId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.deleteSession(null)
        );
        assertEquals("Session ID cannot be null or empty", exception.getMessage());
    }

    // ==================== deleteSessionsByMaterialId Tests ====================

    @Test
    public void testDeleteSessionsByMaterialId_validId_deletesSessions() {
        repository.deleteSessionsByMaterialId(TEST_MATERIAL_ID);

        verify(mockDao).deleteByMaterialId(TEST_MATERIAL_ID);
    }

    @Test
    public void testDeleteSessionsByMaterialId_nullId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.deleteSessionsByMaterialId(null)
        );
        assertEquals("Material ID cannot be null or empty", exception.getMessage());
    }

    // ==================== deleteAllSessions Tests ====================

    @Test
    public void testDeleteAllSessions_deletesSessions() {
        repository.deleteAllSessions();

        verify(mockDao).deleteAll();
    }

    // ==================== getSessionCount Tests ====================

    @Test
    public void testGetSessionCount_hasSessions_returnsCount() {
        when(mockDao.getCount()).thenReturn(5);

        int result = repository.getSessionCount();

        assertEquals(5, result);
    }

    // ==================== getSessionCountByStatus Tests ====================

    @Test
    public void testGetSessionCountByStatus_hasSessions_returnsCount() {
        when(mockDao.getCountByStatus(SessionStatus.ACTIVE)).thenReturn(3);

        int result = repository.getSessionCountByStatus(SessionStatus.ACTIVE);

        assertEquals(3, result);
    }

    @Test
    public void testGetSessionCountByStatus_nullStatus_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.getSessionCountByStatus(null)
        );
        assertEquals("Status cannot be null", exception.getMessage());
    }

    // ==================== Helper Methods ====================

    private SessionEntity createTestEntity(String id, SessionStatus status) {
        return new SessionEntity(
                id,
                TEST_MATERIAL_ID,
                System.currentTimeMillis(),
                0,
                status,
                TEST_DEVICE_ID
        );
    }
}
