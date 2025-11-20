package com.manuscripta.student.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Unit tests for {@link SessionStatus} enum.
 */
public class SessionStatusTest {

    @Test
    public void testEnumValues() {
        SessionStatus[] values = SessionStatus.values();
        // Verify that all expected enum values exist
        assertNotNull(SessionStatus.valueOf("ACTIVE"));
        assertNotNull(SessionStatus.valueOf("PAUSED"));
        assertNotNull(SessionStatus.valueOf("COMPLETED"));
        assertNotNull(SessionStatus.valueOf("CANCELLED"));
    }

    @Test
    public void testActiveStatus() {
        SessionStatus status = SessionStatus.ACTIVE;
        assertNotNull(status);
        assertEquals("ACTIVE", status.name());
        assertEquals(0, status.ordinal());
    }

    @Test
    public void testPausedStatus() {
        SessionStatus status = SessionStatus.PAUSED;
        assertNotNull(status);
        assertEquals("PAUSED", status.name());
        assertEquals(1, status.ordinal());
    }

    @Test
    public void testCompletedStatus() {
        SessionStatus status = SessionStatus.COMPLETED;
        assertNotNull(status);
        assertEquals("COMPLETED", status.name());
        assertEquals(2, status.ordinal());
    }

    @Test
    public void testCancelledStatus() {
        SessionStatus status = SessionStatus.CANCELLED;
        assertNotNull(status);
        assertEquals("CANCELLED", status.name());
        assertEquals(3, status.ordinal());
    }

    @Test
    public void testValueOf() {
        assertEquals(SessionStatus.ACTIVE, SessionStatus.valueOf("ACTIVE"));
        assertEquals(SessionStatus.PAUSED, SessionStatus.valueOf("PAUSED"));
        assertEquals(SessionStatus.COMPLETED, SessionStatus.valueOf("COMPLETED"));
        assertEquals(SessionStatus.CANCELLED, SessionStatus.valueOf("CANCELLED"));
    }
}
