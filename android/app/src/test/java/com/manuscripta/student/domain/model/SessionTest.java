package com.manuscripta.student.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
}
