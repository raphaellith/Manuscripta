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
        this.sessionEntity = new SessionEntity();
    }

    @Test
    public void testDefaultConstructor() {
        assertNotNull(sessionEntity);
        assertEquals("", sessionEntity.getId());
        assertEquals("", sessionEntity.getMaterialId());
        assertEquals(SessionStatus.ACTIVE, sessionEntity.getStatus());
        assertEquals("", sessionEntity.getDeviceId());
        assertEquals(0, sessionEntity.getEndTime());
    }

    @Test
    public void testSettersAndGetters() {
        sessionEntity.setId("session-1");
        sessionEntity.setMaterialId("mat-1");
        sessionEntity.setStartTime(1000L);
        sessionEntity.setEndTime(2000L);
        sessionEntity.setStatus(SessionStatus.COMPLETED);
        sessionEntity.setDeviceId("device-1");

        assertEquals("session-1", sessionEntity.getId());
        assertEquals("mat-1", sessionEntity.getMaterialId());
        assertEquals(1000L, sessionEntity.getStartTime());
        assertEquals(2000L, sessionEntity.getEndTime());
        assertEquals(SessionStatus.COMPLETED, sessionEntity.getStatus());
        assertEquals("device-1", sessionEntity.getDeviceId());
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
