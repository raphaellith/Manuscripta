package com.manuscripta.student.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
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

    @Test
    public void testConstructorWithZeroTimestamps() {
        // Zero timestamps are valid (e.g., active session with no end time)
        Session s = new Session(
                "id",
                "mat-id",
                0L,
                0L,
                SessionStatus.ACTIVE,
                "dev-id"
        );
        assertEquals(0L, s.getStartTime());
        assertEquals(0L, s.getEndTime());
    }

    @Test
    public void testConstructor_nullId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Session(
                        null,
                        "material-id",
                        0L,
                        0L,
                        SessionStatus.ACTIVE,
                        "device-id"
                )
        );
        assertEquals("Session id cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_emptyId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Session(
                        "",
                        "material-id",
                        0L,
                        0L,
                        SessionStatus.ACTIVE,
                        "device-id"
                )
        );
        assertEquals("Session id cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_blankId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Session(
                        "   ",
                        "material-id",
                        0L,
                        0L,
                        SessionStatus.ACTIVE,
                        "device-id"
                )
        );
        assertEquals("Session id cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_nullMaterialId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Session(
                        "id",
                        null,
                        0L,
                        0L,
                        SessionStatus.ACTIVE,
                        "device-id"
                )
        );
        assertEquals("Session materialId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_emptyMaterialId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Session(
                        "id",
                        "",
                        0L,
                        0L,
                        SessionStatus.ACTIVE,
                        "device-id"
                )
        );
        assertEquals("Session materialId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_blankMaterialId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Session(
                        "id",
                        "   ",
                        0L,
                        0L,
                        SessionStatus.ACTIVE,
                        "device-id"
                )
        );
        assertEquals("Session materialId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_negativeStartTime_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Session(
                        "id",
                        "material-id",
                        -1L,
                        0L,
                        SessionStatus.ACTIVE,
                        "device-id"
                )
        );
        assertEquals("Session startTime cannot be negative", exception.getMessage());
    }

    @Test
    public void testConstructor_negativeEndTime_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Session(
                        "id",
                        "material-id",
                        0L,
                        -1L,
                        SessionStatus.ACTIVE,
                        "device-id"
                )
        );
        assertEquals("Session endTime cannot be negative", exception.getMessage());
    }

    @Test
    public void testConstructor_nullStatus_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Session(
                        "id",
                        "material-id",
                        0L,
                        0L,
                        null,
                        "device-id"
                )
        );
        assertEquals("Session status cannot be null", exception.getMessage());
    }

    @Test
    public void testConstructor_nullDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Session(
                        "id",
                        "material-id",
                        0L,
                        0L,
                        SessionStatus.ACTIVE,
                        null
                )
        );
        assertEquals("Session deviceId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_emptyDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Session(
                        "id",
                        "material-id",
                        0L,
                        0L,
                        SessionStatus.ACTIVE,
                        ""
                )
        );
        assertEquals("Session deviceId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_blankDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Session(
                        "id",
                        "material-id",
                        0L,
                        0L,
                        SessionStatus.ACTIVE,
                        "   "
                )
        );
        assertEquals("Session deviceId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testCreate_nullMaterialId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Session.create(null, "device-id")
        );
        assertEquals("Session materialId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testCreate_emptyMaterialId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Session.create("", "device-id")
        );
        assertEquals("Session materialId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testCreate_blankMaterialId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Session.create("   ", "device-id")
        );
        assertEquals("Session materialId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testCreate_nullDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Session.create("material-id", null)
        );
        assertEquals("Session deviceId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testCreate_emptyDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Session.create("material-id", "")
        );
        assertEquals("Session deviceId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testCreate_blankDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Session.create("material-id", "   ")
        );
        assertEquals("Session deviceId cannot be null or empty", exception.getMessage());
    }
}
