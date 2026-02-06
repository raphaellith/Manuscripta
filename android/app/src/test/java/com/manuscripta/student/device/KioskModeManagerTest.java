package com.manuscripta.student.device;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link KioskModeManager}.
 */
public class KioskModeManagerTest {

    @Mock
    private KioskModeManager.KioskModeListener mockListener;

    private KioskModeManager manager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        manager = new KioskModeManager();
    }

    // ========== Constructor tests ==========

    @Test
    public void testConstructor_createsInstance() {
        assertNotNull(manager);
    }

    @Test
    public void testConstructor_initiallyDisabled() {
        assertFalse(manager.isKioskModeEnabled());
    }

    // ========== enableKioskMode tests ==========

    @Test
    public void testEnableKioskMode_returnsTrue() {
        assertTrue(manager.enableKioskMode());
    }

    @Test
    public void testEnableKioskMode_setsEnabled() {
        manager.enableKioskMode();
        assertTrue(manager.isKioskModeEnabled());
    }

    @Test
    public void testEnableKioskMode_alreadyEnabled_returnsTrue() {
        manager.enableKioskMode();
        assertTrue(manager.enableKioskMode());
    }

    @Test
    public void testEnableKioskMode_notifiesListener() {
        manager.setKioskModeListener(mockListener);
        manager.enableKioskMode();
        verify(mockListener).onKioskModeChanged(true);
    }

    @Test
    public void testEnableKioskMode_alreadyEnabled_doesNotNotify() {
        manager.enableKioskMode();
        manager.setKioskModeListener(mockListener);
        manager.enableKioskMode();
        verify(mockListener, never()).onKioskModeChanged(true);
    }

    // ========== disableKioskMode tests ==========

    @Test
    public void testDisableKioskMode_returnsTrue() {
        manager.enableKioskMode();
        assertTrue(manager.disableKioskMode());
    }

    @Test
    public void testDisableKioskMode_setsDisabled() {
        manager.enableKioskMode();
        manager.disableKioskMode();
        assertFalse(manager.isKioskModeEnabled());
    }

    @Test
    public void testDisableKioskMode_alreadyDisabled_returnsTrue() {
        assertTrue(manager.disableKioskMode());
    }

    @Test
    public void testDisableKioskMode_notifiesListener() {
        manager.enableKioskMode();
        manager.setKioskModeListener(mockListener);
        manager.disableKioskMode();
        verify(mockListener).onKioskModeChanged(false);
    }

    @Test
    public void testDisableKioskMode_alreadyDisabled_doesNotNotify() {
        manager.setKioskModeListener(mockListener);
        manager.disableKioskMode();
        verify(mockListener, never()).onKioskModeChanged(false);
    }

    // ========== isKioskModeEnabled tests ==========

    @Test
    public void testIsKioskModeEnabled_afterEnable() {
        manager.enableKioskMode();
        assertTrue(manager.isKioskModeEnabled());
    }

    @Test
    public void testIsKioskModeEnabled_afterDisable() {
        manager.enableKioskMode();
        manager.disableKioskMode();
        assertFalse(manager.isKioskModeEnabled());
    }

    // ========== Listener tests ==========

    @Test
    public void testSetKioskModeListener_null_doesNotThrow() {
        manager.setKioskModeListener(null);
        manager.enableKioskMode();
        // Should not throw
    }

    @Test
    public void testRemoveKioskModeListener_removesListener() {
        manager.setKioskModeListener(mockListener);
        manager.removeKioskModeListener();
        manager.enableKioskMode();
        verify(mockListener, never()).onKioskModeChanged(true);
    }

    @Test
    public void testListener_enableDisableSequence() {
        manager.setKioskModeListener(mockListener);
        manager.enableKioskMode();
        manager.disableKioskMode();
        verify(mockListener).onKioskModeChanged(true);
        verify(mockListener).onKioskModeChanged(false);
    }

    @Test
    public void testEnableDisable_toggleMultipleTimes() {
        manager.enableKioskMode();
        assertTrue(manager.isKioskModeEnabled());
        manager.disableKioskMode();
        assertFalse(manager.isKioskModeEnabled());
        manager.enableKioskMode();
        assertTrue(manager.isKioskModeEnabled());
    }
}
