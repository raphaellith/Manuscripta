package com.manuscripta.student.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.manuscripta.student.data.model.MaterialEntity;
import com.manuscripta.student.data.model.MaterialType;
import com.manuscripta.student.data.model.SessionEntity;
import com.manuscripta.student.data.model.SessionStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link SessionDao}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class SessionDaoTest {

    private ManuscriptaDatabase database;
    private MaterialDao materialDao;
    private SessionDao sessionDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, ManuscriptaDatabase.class)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();
        materialDao = database.materialDao();
        sessionDao = database.sessionDao();

        // Insert a parent material for foreign key constraint
        MaterialEntity material = new MaterialEntity(
                "mat-1",
                MaterialType.QUIZ,
                "Test Quiz",
                "Content",
                "{}",
                "[]",
                System.currentTimeMillis()
        );
        materialDao.insert(material);
    }

    @After
    public void tearDown() {
        if (database != null) {
            database.close();
        }
    }

    private SessionEntity createSession(String id, String materialId) {
        // Use Room constructor to force specific ID
        return new SessionEntity(
            id,
            materialId,
            System.currentTimeMillis(),
            0,
            SessionStatus.ACTIVE,
            "device-1"
        );
    }

    @Test
    public void testInsertAndGetById() {
        SessionEntity session = createSession("s-1", "mat-1");
        sessionDao.insert(session);

        SessionEntity retrieved = sessionDao.getById("s-1");
        assertNotNull(retrieved);
        assertEquals("s-1", retrieved.getId());
        assertEquals("mat-1", retrieved.getMaterialId());
        assertEquals(SessionStatus.ACTIVE, retrieved.getStatus());
    }

    @Test
    public void testGetAll() {
        sessionDao.insert(createSession("s-1", "mat-1"));
        sessionDao.insert(createSession("s-2", "mat-1"));

        List<SessionEntity> sessions = sessionDao.getAll();
        assertEquals(2, sessions.size());
    }

    @Test
    public void testGetByMaterialId() {
        // Add another material
        materialDao.insert(new MaterialEntity(
                "mat-2",
                MaterialType.READING,
                "Reading",
                "Content",
                "{}",
                "[]",
                System.currentTimeMillis()
        ));

        sessionDao.insert(createSession("s-1", "mat-1"));
        sessionDao.insert(createSession("s-2", "mat-1"));
        sessionDao.insert(createSession("s-3", "mat-2"));

        List<SessionEntity> mat1Sessions = sessionDao.getByMaterialId("mat-1");
        assertEquals(2, mat1Sessions.size());

        List<SessionEntity> mat2Sessions = sessionDao.getByMaterialId("mat-2");
        assertEquals(1, mat2Sessions.size());
    }

    @Test
    public void testGetByStatus() {
        // Create sessions with specific statuses using constructor
        SessionEntity active = new SessionEntity(
            "s-1", "mat-1", System.currentTimeMillis(), 0, SessionStatus.ACTIVE, "device-1");
        sessionDao.insert(active);

        SessionEntity completed = new SessionEntity(
            "s-2", "mat-1", System.currentTimeMillis(), System.currentTimeMillis(), SessionStatus.COMPLETED, "device-1");
        sessionDao.insert(completed);

        List<SessionEntity> activeSessions = sessionDao.getByStatus(SessionStatus.ACTIVE);
        assertEquals(1, activeSessions.size());
        assertEquals("s-1", activeSessions.get(0).getId());

        List<SessionEntity> completedSessions = sessionDao.getByStatus(SessionStatus.COMPLETED);
        assertEquals(1, completedSessions.size());
        assertEquals("s-2", completedSessions.get(0).getId());
    }

    @Test
    public void testGetActiveSession() {
        SessionEntity active = new SessionEntity(
            "s-1", "mat-1", System.currentTimeMillis(), 0, SessionStatus.ACTIVE, "device-1");
        sessionDao.insert(active);

        SessionEntity completed = new SessionEntity(
            "s-2", "mat-1", System.currentTimeMillis(), System.currentTimeMillis(), SessionStatus.COMPLETED, "device-1");
        sessionDao.insert(completed);

        SessionEntity activeSession = sessionDao.getActiveSession();
        assertNotNull(activeSession);
        assertEquals("s-1", activeSession.getId());
    }

    @Test
    public void testGetActiveSessionNone() {
        SessionEntity completed = new SessionEntity(
            "s-1", "mat-1", System.currentTimeMillis(), System.currentTimeMillis(), SessionStatus.COMPLETED, "device-1");
        sessionDao.insert(completed);

        assertNull(sessionDao.getActiveSession());
    }

    @Test
    public void testGetByDeviceId() {
        SessionEntity session1 = new SessionEntity(
            "s-1", "mat-1", System.currentTimeMillis(), 0, SessionStatus.ACTIVE, "device-1");
        sessionDao.insert(session1);

        SessionEntity session2 = new SessionEntity(
            "s-2", "mat-1", System.currentTimeMillis(), 0, SessionStatus.ACTIVE, "device-2");
        sessionDao.insert(session2);

        List<SessionEntity> device1Sessions = sessionDao.getByDeviceId("device-1");
        assertEquals(1, device1Sessions.size());
        assertEquals("s-1", device1Sessions.get(0).getId());
    }

    @Test
    public void testUpdateStatus() {
        SessionEntity session = createSession("s-1", "mat-1");
        sessionDao.insert(session);

        sessionDao.updateStatus("s-1", SessionStatus.PAUSED);

        assertEquals(SessionStatus.PAUSED, sessionDao.getById("s-1").getStatus());
    }

    @Test
    public void testEndSession() {
        SessionEntity session = createSession("s-1", "mat-1");
        sessionDao.insert(session);

        long endTime = System.currentTimeMillis();
        sessionDao.endSession("s-1", endTime, SessionStatus.COMPLETED);

        SessionEntity ended = sessionDao.getById("s-1");
        assertEquals(endTime, ended.getEndTime());
        assertEquals(SessionStatus.COMPLETED, ended.getStatus());
    }

    @Test
    public void testUpdate() {
        SessionEntity session = createSession("s-1", "mat-1");
        sessionDao.insert(session);

        // Create a new entity with updated values since entities are immutable
        SessionEntity updatedSession = new SessionEntity(
            "s-1", "mat-1", session.getStartTime(), session.getEndTime(), 
            session.getStatus(), "device-2");
        sessionDao.update(updatedSession);

        assertEquals("device-2", sessionDao.getById("s-1").getDeviceId());
    }

    @Test
    public void testDelete() {
        SessionEntity session = createSession("s-1", "mat-1");
        sessionDao.insert(session);
        sessionDao.delete(session);

        assertNull(sessionDao.getById("s-1"));
    }

    @Test
    public void testDeleteById() {
        sessionDao.insert(createSession("s-1", "mat-1"));
        sessionDao.deleteById("s-1");

        assertNull(sessionDao.getById("s-1"));
    }

    @Test
    public void testDeleteByMaterialId() {
        sessionDao.insert(createSession("s-1", "mat-1"));
        sessionDao.insert(createSession("s-2", "mat-1"));
        sessionDao.deleteByMaterialId("mat-1");

        assertEquals(0, sessionDao.getCount());
    }

    @Test
    public void testDeleteAll() {
        sessionDao.insert(createSession("s-1", "mat-1"));
        sessionDao.insert(createSession("s-2", "mat-1"));
        sessionDao.deleteAll();

        assertEquals(0, sessionDao.getCount());
    }

    @Test
    public void testGetCount() {
        assertEquals(0, sessionDao.getCount());

        sessionDao.insert(createSession("s-1", "mat-1"));
        assertEquals(1, sessionDao.getCount());

        sessionDao.insert(createSession("s-2", "mat-1"));
        assertEquals(2, sessionDao.getCount());
    }

    @Test
    public void testGetCountByStatus() {
        // Create sessions with specific statuses using constructor
        SessionEntity active = new SessionEntity(
            "s-1", "mat-1", System.currentTimeMillis(), 0, SessionStatus.ACTIVE, "device-1");
        sessionDao.insert(active);

        SessionEntity completed = new SessionEntity(
            "s-2", "mat-1", System.currentTimeMillis(), System.currentTimeMillis(), SessionStatus.COMPLETED, "device-1");
        sessionDao.insert(completed);

        assertEquals(1, sessionDao.getCountByStatus(SessionStatus.ACTIVE));
        assertEquals(1, sessionDao.getCountByStatus(SessionStatus.COMPLETED));
        assertEquals(0, sessionDao.getCountByStatus(SessionStatus.PAUSED));
    }

    @Test
    public void testInsertAll() {
        SessionEntity s1 = createSession("s-1", "mat-1");
        SessionEntity s2 = createSession("s-2", "mat-1");
        sessionDao.insertAll(Arrays.asList(s1, s2));

        assertEquals(2, sessionDao.getCount());
    }

    @Test
    public void testCascadeDeleteOnMaterialDelete() {
        sessionDao.insert(createSession("s-1", "mat-1"));
        sessionDao.insert(createSession("s-2", "mat-1"));

        // Delete the parent material
        materialDao.deleteById("mat-1");

        // Sessions should be deleted due to CASCADE
        assertEquals(0, sessionDao.getCount());
    }

    @Test
    public void testGetByIdNotFound() {
        assertNull(sessionDao.getById("nonexistent"));
    }

    @Test
    public void testGetAllEmpty() {
        List<SessionEntity> sessions = sessionDao.getAll();
        assertNotNull(sessions);
        assertTrue(sessions.isEmpty());
    }

    @Test
    public void testInsertReplaceOnConflict() {
        // Create session with device-1
        SessionEntity session = new SessionEntity(
            "s-1", "mat-1", System.currentTimeMillis(), 0, SessionStatus.ACTIVE, "device-1");
        sessionDao.insert(session);

        // Create new session with same ID but different device
        SessionEntity updated = new SessionEntity(
            "s-1", "mat-1", session.getStartTime(), 0, SessionStatus.ACTIVE, "device-2");
        sessionDao.insert(updated);

        assertEquals("device-2", sessionDao.getById("s-1").getDeviceId());
        assertEquals(1, sessionDao.getCount());
    }

    @Test
    public void testActivateSession() {
        // Create session in RECEIVED state with startTime = 0
        SessionEntity received = new SessionEntity(
            "s-1", "mat-1", 0, 0, SessionStatus.RECEIVED, "device-1");
        sessionDao.insert(received);

        // Activate the session
        long activationTime = System.currentTimeMillis();
        sessionDao.activateSession("s-1", activationTime);

        // Verify transition
        SessionEntity activated = sessionDao.getById("s-1");
        assertEquals(SessionStatus.ACTIVE, activated.getStatus());
        assertEquals(activationTime, activated.getStartTime());
    }
}
