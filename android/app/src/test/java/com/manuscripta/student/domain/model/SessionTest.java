package com.manuscripta.student.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.manuscripta.student.data.model.SessionStatus;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link Session} domain model.
 */
public class SessionTest {
    private Session session;

    @Before
    public void setUp() {
        this.session = new Session(
                "session-id",
                "material-id",
                1234567890L,
                1234568000L,
                SessionStatus.COMPLETED,
                "device-123"
        );
    }

    @Test
    public void testConstructorAndGetters() {
        assertNotNull(session);
        assertEquals("session-id", session.getId());
        assertEquals("material-id", session.getMaterialId());
        assertEquals(1234567890L, session.getStartTime());
        assertEquals(1234568000L, session.getEndTime());
        assertEquals(SessionStatus.COMPLETED, session.getStatus());
        assertEquals("device-123", session.getDeviceId());
    }

    @Test
    public void testCreateFactoryMethod() {
        Session newSession = Session.create("mat-123", "dev-456");

        assertNotNull(newSession);
        assertNotNull(newSession.getId());
        assertEquals(36, newSession.getId().length()); // UUID length
        assertEquals("mat-123", newSession.getMaterialId());
        assertEquals("dev-456", newSession.getDeviceId());
        assertEquals(SessionStatus.ACTIVE, newSession.getStatus());
        assertEquals(0, newSession.getEndTime());
        // Start time should be close to current time
        long currentTime = System.currentTimeMillis();
        long diff = Math.abs(currentTime - newSession.getStartTime());
        assertTrue("Start time should be within 1 second of current time", diff < 1000);
    }

    @Test
    public void testCreateFactoryMethodGeneratesUniqueIds() {
        Session session1 = Session.create("mat-1", "dev-1");
        Session session2 = Session.create("mat-1", "dev-1");

        assertNotEquals(session1.getId(), session2.getId());
    }
}
