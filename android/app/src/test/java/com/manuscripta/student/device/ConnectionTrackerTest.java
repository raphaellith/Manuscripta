package com.manuscripta.student.device;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.lifecycle.MutableLiveData;

import com.manuscripta.student.data.repository.DeviceStatusRepository;
import com.manuscripta.student.network.tcp.ConnectionState;
import com.manuscripta.student.network.tcp.TcpMessage;
import com.manuscripta.student.network.tcp.TcpProtocolException;
import com.manuscripta.student.network.tcp.TcpSocketManager;
import com.manuscripta.student.network.tcp.message.LockScreenMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link ConnectionTracker}.
 */
public class ConnectionTrackerTest {

    private static final String TEST_DEVICE_ID = "test-device-123";

    @Mock
    private DeviceStatusRepository mockRepository;

    @Mock
    private TcpSocketManager mockSocketManager;

    private ConnectionTracker tracker;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockSocketManager.getConnectionState())
                .thenReturn(new MutableLiveData<>(ConnectionState.DISCONNECTED));
        tracker = new ConnectionTracker(mockRepository, mockSocketManager);
    }

    // ========== Constructor tests ==========

    @Test
    public void testConstructor_createsInstance() {
        assertNotNull(tracker);
    }

    @Test
    public void testConstructor_nullRepository_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConnectionTracker(null, mockSocketManager));
    }

    @Test
    public void testConstructor_nullSocketManager_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConnectionTracker(mockRepository, null));
    }

    // ========== start/stop tests ==========

    @Test
    public void testStart_setsTracking() {
        tracker.start(TEST_DEVICE_ID);
        assertTrue(tracker.isTracking());
    }

    @Test
    public void testStart_registersListener() {
        tracker.start(TEST_DEVICE_ID);
        verify(mockSocketManager).addMessageListener(tracker);
    }

    @Test
    public void testStart_nullDeviceId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> tracker.start(null));
    }

    @Test
    public void testStart_emptyDeviceId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> tracker.start(""));
    }

    @Test
    public void testStart_blankDeviceId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> tracker.start("   "));
    }

    @Test
    public void testStop_clearsTracking() {
        tracker.start(TEST_DEVICE_ID);
        tracker.stop();
        assertFalse(tracker.isTracking());
    }

    @Test
    public void testStop_unregistersListener() {
        tracker.start(TEST_DEVICE_ID);
        tracker.stop();
        verify(mockSocketManager).removeMessageListener(tracker);
    }

    // ========== isConnected tests ==========

    @Test
    public void testIsConnected_delegatesToSocketManager() {
        when(mockSocketManager.isConnected()).thenReturn(true);
        assertTrue(tracker.isConnected());
    }

    @Test
    public void testIsConnected_whenNotConnected() {
        when(mockSocketManager.isConnected()).thenReturn(false);
        assertFalse(tracker.isConnected());
    }

    // ========== getConnectionState tests ==========

    @Test
    public void testGetConnectionState_returnsLiveData() {
        assertNotNull(tracker.getConnectionState());
    }

    // ========== onConnectionStateChanged tests ==========

    @Test
    public void testOnConnectionStateChanged_disconnected_setsDisconnected() {
        tracker.start(TEST_DEVICE_ID);
        tracker.onConnectionStateChanged(ConnectionState.DISCONNECTED);
        verify(mockRepository).setDisconnected(TEST_DEVICE_ID);
    }

    @Test
    public void testOnConnectionStateChanged_reconnecting_setsDisconnected() {
        tracker.start(TEST_DEVICE_ID);
        tracker.onConnectionStateChanged(ConnectionState.RECONNECTING);
        verify(mockRepository).setDisconnected(TEST_DEVICE_ID);
    }

    @Test
    public void testOnConnectionStateChanged_connected_noStatusUpdate() {
        tracker.start(TEST_DEVICE_ID);
        tracker.onConnectionStateChanged(ConnectionState.CONNECTED);
        verify(mockRepository, never()).setDisconnected(TEST_DEVICE_ID);
    }

    @Test
    public void testOnConnectionStateChanged_connecting_noStatusUpdate() {
        tracker.start(TEST_DEVICE_ID);
        tracker.onConnectionStateChanged(ConnectionState.CONNECTING);
        verify(mockRepository, never()).setDisconnected(TEST_DEVICE_ID);
    }

    @Test
    public void testOnConnectionStateChanged_notTracking_noUpdate() {
        tracker.onConnectionStateChanged(ConnectionState.DISCONNECTED);
        verify(mockRepository, never()).setDisconnected(TEST_DEVICE_ID);
    }

    @Test
    public void testOnConnectionStateChanged_afterStop_noUpdate() {
        tracker.start(TEST_DEVICE_ID);
        tracker.stop();
        tracker.onConnectionStateChanged(ConnectionState.DISCONNECTED);
        verify(mockRepository, never()).setDisconnected(TEST_DEVICE_ID);
    }

    // ========== onMessageReceived tests ==========

    @Test
    public void testOnMessageReceived_doesNothing() {
        tracker.start(TEST_DEVICE_ID);
        TcpMessage message = new LockScreenMessage();
        tracker.onMessageReceived(message);
        // Should not throw or interact with repository
    }

    // ========== onError tests ==========

    @Test
    public void testOnError_doesNotThrow() {
        TcpProtocolException error = new TcpProtocolException(
                TcpProtocolException.ErrorType.EMPTY_DATA, "test error");
        tracker.onError(error);
        // Should not throw
    }

    // ========== Constants tests ==========

    @Test
    public void testConstants_maxUnacknowledgedHeartbeats() {
        assertEquals(3, ConnectionTracker.MAX_UNACKNOWLEDGED_HEARTBEATS);
    }

    @Test
    public void testConstants_unpairTimeoutMs() {
        assertEquals(60_000L, ConnectionTracker.UNPAIR_TIMEOUT_MS);
    }
}
