package com.manuscripta.student.data.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link SessionEntity} entity.
 * Tests immutable entity construction and getters.
 */
public class SessionEntityTest {

    @Test
    public void testRoomConstructor() {
        // Test rehydration constructor
        long now = System.currentTimeMillis();
        SessionEntity session = new SessionEntity(
            "session-123",
            "mat-123",
            now,
            0,
            SessionStatus.ACTIVE,
            "dev-123"
        );

        assertEquals("session-123", session.getId());
        assertEquals("mat-123", session.getMaterialId());
        assertEquals(now, session.getStartTime());
        assertEquals(0, session.getEndTime());
        assertEquals(SessionStatus.ACTIVE, session.getStatus());
        assertEquals("dev-123", session.getDeviceId());
    }

    @Test
    public void testGetters() {
        // Test all getters with different values
        SessionEntity session = new SessionEntity(
            "session-1",
            "mat-2",
            1000L,
            2000L,
            SessionStatus.COMPLETED,
            "device-2"
        );

        assertEquals("session-1", session.getId());
        assertEquals("mat-2", session.getMaterialId());
        assertEquals(1000L, session.getStartTime());
        assertEquals(2000L, session.getEndTime());
        assertEquals(SessionStatus.COMPLETED, session.getStatus());
        assertEquals("device-2", session.getDeviceId());
    }
}
