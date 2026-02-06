package com.manuscripta.student.device;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.manuscripta.student.data.repository.DeviceStatusRepository;
import com.manuscripta.student.network.tcp.ConnectionState;
import com.manuscripta.student.network.tcp.TcpProtocolException;
import com.manuscripta.student.network.tcp.TcpSocketManager;
import com.manuscripta.student.network.tcp.message.LockScreenMessage;
import com.manuscripta.student.network.tcp.message.RefreshConfigMessage;
import com.manuscripta.student.network.tcp.message.UnlockScreenMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link ScreenLockManager}.
 */
public class ScreenLockManagerTest {

    private static final String TEST_DEVICE_ID = "test-device-123";

    @Mock
    private DeviceStatusRepository mockRepository;

    @Mock
    private TcpSocketManager mockSocketManager;

    @Mock
    private ScreenLockManager.ScreenLockListener mockListener;

    private ScreenLockManager manager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        manager = new ScreenLockManager(mockRepository, mockSocketManager);
    }

    // ========== Constructor tests ==========

    @Test
    public void testConstructor_createsInstance() {
        assertNotNull(manager);
    }

    @Test
    public void testConstructor_nullRepository_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScreenLockManager(null, mockSocketManager));
    }

    @Test
    public void testConstructor_nullSocketManager_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScreenLockManager(mockRepository, null));
    }

    @Test
    public void testConstructor_initiallyUnlocked() {
        assertFalse(manager.isScreenLocked());
    }

    @Test
    public void testConstructor_initiallyInactive() {
        assertFalse(manager.isActive());
    }

    // ========== start/stop tests ==========

    @Test
    public void testStart_setsActive() {
        manager.start(TEST_DEVICE_ID);
        assertTrue(manager.isActive());
    }

    @Test
    public void testStart_registersListener() {
        manager.start(TEST_DEVICE_ID);
        verify(mockSocketManager).addMessageListener(manager);
    }

    @Test
    public void testStart_nullDeviceId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.start(null));
    }

    @Test
    public void testStart_emptyDeviceId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.start(""));
    }

    @Test
    public void testStart_blankDeviceId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.start("   "));
    }

    @Test
    public void testStop_setsInactive() {
        manager.start(TEST_DEVICE_ID);
        manager.stop();
        assertFalse(manager.isActive());
    }

    @Test
    public void testStop_unregistersListener() {
        manager.start(TEST_DEVICE_ID);
        manager.stop();
        verify(mockSocketManager).removeMessageListener(manager);
    }

    @Test
    public void testStop_resetsScreenLock() {
        manager.start(TEST_DEVICE_ID);
        manager.onMessageReceived(new LockScreenMessage());
        manager.stop();
        assertFalse(manager.isScreenLocked());
    }

    // ========== onMessageReceived tests ==========

    @Test
    public void testOnMessageReceived_lockScreen_setsLocked() {
        manager.start(TEST_DEVICE_ID);
        manager.onMessageReceived(new LockScreenMessage());
        assertTrue(manager.isScreenLocked());
    }

    @Test
    public void testOnMessageReceived_lockScreen_updatesRepository() {
        manager.start(TEST_DEVICE_ID);
        manager.onMessageReceived(new LockScreenMessage());
        verify(mockRepository).setLocked(TEST_DEVICE_ID);
    }

    @Test
    public void testOnMessageReceived_lockScreen_notifiesListener() {
        manager.start(TEST_DEVICE_ID);
        manager.setScreenLockListener(mockListener);
        manager.onMessageReceived(new LockScreenMessage());
        verify(mockListener).onScreenLockChanged(true);
    }

    @Test
    public void testOnMessageReceived_unlockScreen_setsUnlocked() {
        manager.start(TEST_DEVICE_ID);
        manager.onMessageReceived(new LockScreenMessage());
        manager.onMessageReceived(new UnlockScreenMessage());
        assertFalse(manager.isScreenLocked());
    }

    @Test
    public void testOnMessageReceived_unlockScreen_updatesRepository() {
        manager.start(TEST_DEVICE_ID);
        manager.onMessageReceived(new UnlockScreenMessage());
        verify(mockRepository).setOnTask(TEST_DEVICE_ID, null);
    }

    @Test
    public void testOnMessageReceived_unlockScreen_notifiesListener() {
        manager.start(TEST_DEVICE_ID);
        manager.setScreenLockListener(mockListener);
        manager.onMessageReceived(new UnlockScreenMessage());
        verify(mockListener).onScreenLockChanged(false);
    }

    @Test
    public void testOnMessageReceived_notActive_ignoresMessage() {
        manager.onMessageReceived(new LockScreenMessage());
        assertFalse(manager.isScreenLocked());
        verify(mockRepository, never()).setLocked(TEST_DEVICE_ID);
    }

    @Test
    public void testOnMessageReceived_otherMessage_noEffect() {
        manager.start(TEST_DEVICE_ID);
        manager.onMessageReceived(new RefreshConfigMessage());
        assertFalse(manager.isScreenLocked());
    }

    @Test
    public void testOnMessageReceived_afterStop_ignoresMessage() {
        manager.start(TEST_DEVICE_ID);
        manager.stop();
        manager.onMessageReceived(new LockScreenMessage());
        assertFalse(manager.isScreenLocked());
    }

    // ========== Listener tests ==========

    @Test
    public void testSetScreenLockListener_null_doesNotThrow() {
        manager.setScreenLockListener(null);
        manager.start(TEST_DEVICE_ID);
        manager.onMessageReceived(new LockScreenMessage());
        // Should not throw
    }

    @Test
    public void testRemoveScreenLockListener_removesListener() {
        manager.start(TEST_DEVICE_ID);
        manager.setScreenLockListener(mockListener);
        manager.removeScreenLockListener();
        manager.onMessageReceived(new LockScreenMessage());
        verify(mockListener, never()).onScreenLockChanged(true);
    }

    @Test
    public void testLockUnlockSequence() {
        manager.start(TEST_DEVICE_ID);
        manager.setScreenLockListener(mockListener);

        manager.onMessageReceived(new LockScreenMessage());
        assertTrue(manager.isScreenLocked());
        verify(mockListener).onScreenLockChanged(true);

        manager.onMessageReceived(new UnlockScreenMessage());
        assertFalse(manager.isScreenLocked());
        verify(mockListener).onScreenLockChanged(false);
    }

    // ========== onConnectionStateChanged tests ==========

    @Test
    public void testOnConnectionStateChanged_doesNothing() {
        manager.onConnectionStateChanged(ConnectionState.DISCONNECTED);
        // Should not throw or interact with repository
    }

    // ========== onError tests ==========

    @Test
    public void testOnError_doesNotThrow() {
        TcpProtocolException error = new TcpProtocolException(
                TcpProtocolException.ErrorType.EMPTY_DATA, "test");
        manager.onError(error);
        // Should not throw
    }
}
