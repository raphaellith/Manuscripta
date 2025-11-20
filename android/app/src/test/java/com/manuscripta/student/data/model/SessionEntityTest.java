package com.manuscripta.student.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link SessionEntity} entity.
 */
public class SessionEntityTest {
    private SessionEntity sessionEntity;

    @Before
    public void setUp() {
        // Use convenience constructor for general setup
        this.sessionEntity = new SessionEntity("mat-1", "device-1");
    }

    @Test
    public void testRoomConstructor() {
        // Test rehydration constructor
        long now = System.currentTimeMillis();
        SessionEntity rehydratedSession = new SessionEntity(
            "session-123",
            "mat-123",
            now,
            0,
            SessionStatus.ACTIVE,
            "dev-123"
        );

        assertEquals("session-123", rehydratedSession.getId());
        assertEquals("mat-123", rehydratedSession.getMaterialId());
        assertEquals(now, rehydratedSession.getStartTime());
        assertEquals(0, rehydratedSession.getEndTime());
        assertEquals(SessionStatus.ACTIVE, rehydratedSession.getStatus());
        assertEquals("dev-123", rehydratedSession.getDeviceId());
    }

    @Test
    public void testConvenienceConstructor() {
        SessionEntity newSession = new SessionEntity("mat-123", "dev-456");
        assertNotNull(newSession.getId());
        assertEquals(36, newSession.getId().length()); // UUID length
        assertEquals("mat-123", newSession.getMaterialId());
        assertEquals("dev-456", newSession.getDeviceId());
        assertEquals(SessionStatus.ACTIVE, newSession.getStatus());
        assertEquals(0, newSession.getEndTime());
        // Start time should be close to current time
        long currentTime = System.currentTimeMillis();
        long diff = Math.abs(currentTime - newSession.getStartTime());
        // Allow for small time difference (e.g., 1 second)
        assert(diff < 1000);
    }

    @Test
    public void testSettersAndGetters() {
        // ID is now final and cannot be set
        // sessionEntity.setId("session-1"); 
        
        sessionEntity.setMaterialId("mat-2");
        sessionEntity.setStartTime(1000L);
        sessionEntity.setEndTime(2000L);
        sessionEntity.setStatus(SessionStatus.COMPLETED);
        sessionEntity.setDeviceId("device-2");

        // ID should remain as generated in setUp
        assertNotNull(sessionEntity.getId());
        assertEquals("mat-2", sessionEntity.getMaterialId());
        assertEquals(1000L, sessionEntity.getStartTime());
        assertEquals(2000L, sessionEntity.getEndTime());
        assertEquals(SessionStatus.COMPLETED, sessionEntity.getStatus());
        assertEquals("device-2", sessionEntity.getDeviceId());
    }

    @Test
    public void testStatusTransitions() {
        sessionEntity.setStatus(SessionStatus.ACTIVE);
        assertEquals(SessionStatus.ACTIVE, sessionEntity.getStatus());

        sessionEntity.setStatus(SessionStatus.PAUSED);
        assertEquals(SessionStatus.PAUSED, sessionEntity.getStatus());

        sessionEntity.setStatus(SessionStatus.COMPLETED);
        assertEquals(SessionStatus.COMPLETED, sessionEntity.getStatus());

        sessionEntity.setStatus(SessionStatus.CANCELLED);
        assertEquals(SessionStatus.CANCELLED, sessionEntity.getStatus());
    }
}
