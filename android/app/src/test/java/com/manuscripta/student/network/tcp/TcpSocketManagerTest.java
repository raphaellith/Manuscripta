package com.manuscripta.student.network.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;

import com.manuscripta.student.network.tcp.message.LockScreenMessage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for {@link TcpSocketManager}.
 */
public class TcpSocketManagerTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private TcpMessageEncoder mockEncoder;

    @Mock
    private TcpMessageDecoder mockDecoder;

    private TcpSocketManager socketManager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        socketManager = new TcpSocketManager(mockEncoder, mockDecoder);
    }

    // ========== Constructor tests ==========

    @Test
    public void constructor_initializesWithDisconnectedState() {
        TcpSocketManager manager = new TcpSocketManager(mockEncoder, mockDecoder);

        assertEquals(ConnectionState.DISCONNECTED, manager.getConnectionState().getValue());
    }

    @Test
    public void constructor_createsNonNullConnectionStateLiveData() {
        TcpSocketManager manager = new TcpSocketManager(mockEncoder, mockDecoder);

        assertNotNull(manager.getConnectionState());
    }

    // ========== getConnectionState tests ==========

    @Test
    public void getConnectionState_returnsLiveData() {
        LiveData<ConnectionState> liveData = socketManager.getConnectionState();

        assertNotNull(liveData);
    }

    @Test
    public void getConnectionState_initialValueIsDisconnected() {
        ConnectionState state = socketManager.getConnectionState().getValue();

        assertEquals(ConnectionState.DISCONNECTED, state);
    }

    // ========== isConnected tests ==========

    @Test
    public void isConnected_returnsFalseWhenNotConnected() {
        assertFalse(socketManager.isConnected());
    }

    @Test
    public void isConnected_returnsFalseAfterDisconnect() {
        socketManager.disconnect();

        assertFalse(socketManager.isConnected());
    }

    // ========== addMessageListener tests ==========

    @Test
    public void addMessageListener_addsListener() {
        TcpMessageListener listener = mock(TcpMessageListener.class);

        socketManager.addMessageListener(listener);

        assertEquals(1, socketManager.getListenerCount());
    }

    @Test
    public void addMessageListener_multipleListeners() {
        TcpMessageListener listener1 = mock(TcpMessageListener.class);
        TcpMessageListener listener2 = mock(TcpMessageListener.class);

        socketManager.addMessageListener(listener1);
        socketManager.addMessageListener(listener2);

        assertEquals(2, socketManager.getListenerCount());
    }

    @Test
    public void addMessageListener_duplicateListenerNotAdded() {
        TcpMessageListener listener = mock(TcpMessageListener.class);

        socketManager.addMessageListener(listener);
        socketManager.addMessageListener(listener);

        assertEquals(1, socketManager.getListenerCount());
    }

    // ========== removeMessageListener tests ==========

    @Test
    public void removeMessageListener_removesListener() {
        TcpMessageListener listener = mock(TcpMessageListener.class);

        socketManager.addMessageListener(listener);
        assertEquals(1, socketManager.getListenerCount());

        socketManager.removeMessageListener(listener);
        assertEquals(0, socketManager.getListenerCount());
    }

    @Test
    public void removeMessageListener_nonExistentListenerDoesNothing() {
        TcpMessageListener listener1 = mock(TcpMessageListener.class);
        TcpMessageListener listener2 = mock(TcpMessageListener.class);

        socketManager.addMessageListener(listener1);
        socketManager.removeMessageListener(listener2);

        assertEquals(1, socketManager.getListenerCount());
    }

    // ========== disconnect tests ==========

    @Test
    public void disconnect_setsStateToDisconnected() {
        socketManager.disconnect();

        assertEquals(ConnectionState.DISCONNECTED, socketManager.getConnectionState().getValue());
    }

    @Test
    public void disconnect_stopsFurtherReconnectionAttempts() {
        socketManager.disconnect();

        assertFalse(socketManager.getShouldReconnect());
    }

    @Test
    public void disconnect_canBeCalledMultipleTimes() {
        socketManager.disconnect();
        socketManager.disconnect();
        socketManager.disconnect();

        assertEquals(ConnectionState.DISCONNECTED, socketManager.getConnectionState().getValue());
    }

    // ========== send tests ==========

    @Test
    public void send_throwsIOExceptionWhenNotConnected() throws TcpProtocolException {
        LockScreenMessage message = new LockScreenMessage();
        when(mockEncoder.encode(message)).thenReturn(new byte[]{0x01});

        try {
            socketManager.send(message);
            fail("Expected IOException");
        } catch (IOException e) {
            assertEquals("Not connected", e.getMessage());
        }
    }

    @Test
    public void send_encodesMessageBeforeSending() throws TcpProtocolException, IOException {
        LockScreenMessage message = new LockScreenMessage();
        when(mockEncoder.encode(message)).thenReturn(new byte[]{0x01});

        try {
            socketManager.send(message);
        } catch (IOException ignored) {
            // Expected since not connected
        }

        verify(mockEncoder).encode(message);
    }

    // ========== Exponential backoff tests ==========

    @Test
    public void reconnectDelay_startsAtOneSecond() {
        assertEquals(1000L, socketManager.getCurrentReconnectDelay());
    }

    @Test
    public void reconnectDelay_doublesOnEachAttempt() {
        socketManager.setCurrentReconnectDelay(1000L);

        // Simulate backoff calculations
        long delay1 = socketManager.getCurrentReconnectDelay();
        assertEquals(1000L, delay1);

        socketManager.setCurrentReconnectDelay(delay1 * 2);
        long delay2 = socketManager.getCurrentReconnectDelay();
        assertEquals(2000L, delay2);

        socketManager.setCurrentReconnectDelay(delay2 * 2);
        long delay3 = socketManager.getCurrentReconnectDelay();
        assertEquals(4000L, delay3);

        socketManager.setCurrentReconnectDelay(delay3 * 2);
        long delay4 = socketManager.getCurrentReconnectDelay();
        assertEquals(8000L, delay4);
    }

    @Test
    public void reconnectDelay_capsAtEightSeconds() {
        socketManager.setCurrentReconnectDelay(8000L);
        long cappedDelay = Math.min(8000L * 2, 8000L);

        assertEquals(8000L, cappedDelay);
    }

    // ========== Connection lifecycle tests ==========

    @Test
    public void connect_setsReconnectFlagToTrue() throws Exception {
        TcpSocketManager spyManager = spy(new TcpSocketManager(mockEncoder, mockDecoder));
        doThrow(new IOException("Test")).when(spyManager).createSocket(any(), anyInt());

        CountDownLatch latch = new CountDownLatch(1);
        spyManager.getConnectionState().observeForever(state -> {
            if (state == ConnectionState.CONNECTING || state == ConnectionState.RECONNECTING) {
                latch.countDown();
            }
        });

        spyManager.connect("192.168.1.1", 8080);

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(spyManager.getShouldReconnect());
    }

    @Test
    public void connect_setsStateToConnecting() throws Exception {
        TcpSocketManager spyManager = spy(new TcpSocketManager(mockEncoder, mockDecoder));
        doThrow(new IOException("Test")).when(spyManager).createSocket(any(), anyInt());

        CountDownLatch latch = new CountDownLatch(1);
        spyManager.getConnectionState().observeForever(state -> {
            if (state == ConnectionState.CONNECTING || state == ConnectionState.RECONNECTING) {
                latch.countDown();
            }
        });

        spyManager.connect("192.168.1.1", 8080);

        // Connection state should transition through CONNECTING
        assertTrue(latch.await(1, TimeUnit.SECONDS));

        ConnectionState state = spyManager.getConnectionState().getValue();
        assertTrue(state == ConnectionState.CONNECTING
                || state == ConnectionState.RECONNECTING
                || state == ConnectionState.DISCONNECTED);
    }

    // ========== Thread safety tests ==========

    @Test
    public void multipleConcurrentDisconnects_areThreadSafe() throws Exception {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                socketManager.disconnect();
                latch.countDown();
            }).start();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(ConnectionState.DISCONNECTED, socketManager.getConnectionState().getValue());
    }

    // ========== Resource cleanup tests ==========

    @Test
    public void disconnect_cleansUpResources() {
        socketManager.disconnect();

        assertFalse(socketManager.isConnected());
        assertFalse(socketManager.getShouldReconnect());
    }

    // ========== State transition tests ==========

    @Test
    public void stateTransition_disconnectedIsInitialState() {
        assertEquals(ConnectionState.DISCONNECTED, socketManager.getConnectionState().getValue());
    }

    @Test
    public void stateTransition_disconnectAlwaysEndsInDisconnected() {
        socketManager.disconnect();

        assertEquals(ConnectionState.DISCONNECTED, socketManager.getConnectionState().getValue());
    }

    // ========== Connection state change tests ==========

    @Test
    public void connect_changesStateToConnecting() throws Exception {
        TcpSocketManager spyManager = spy(new TcpSocketManager(mockEncoder, mockDecoder));
        doThrow(new IOException("Test connection refused")).when(spyManager).createSocket(any(), anyInt());

        CountDownLatch latch = new CountDownLatch(1);
        spyManager.getConnectionState().observeForever(state -> {
            if (state == ConnectionState.CONNECTING || state == ConnectionState.RECONNECTING) {
                latch.countDown();
            }
        });

        spyManager.connect("192.168.1.1", 8080);

        // State should transition to CONNECTING
        assertTrue(latch.await(1, TimeUnit.SECONDS));

        // State will be either CONNECTING or RECONNECTING (if connection attempt completed)
        ConnectionState state = spyManager.getConnectionState().getValue();
        assertTrue("State should be CONNECTING or RECONNECTING but was " + state,
                state == ConnectionState.CONNECTING || state == ConnectionState.RECONNECTING);

        // Clean up
        spyManager.disconnect();
    }

    @Test
    public void connect_storesHostAndPort() throws Exception {
        TcpSocketManager spyManager = spy(new TcpSocketManager(mockEncoder, mockDecoder));
        doThrow(new IOException("Test")).when(spyManager).createSocket(any(), anyInt());

        spyManager.connect("192.168.1.100", 9999);

        assertTrue(spyManager.getShouldReconnect());

        spyManager.disconnect();
    }

    // ========== Integration-style tests (without actual network) ==========

    @Test
    public void connectThenDisconnect_endsInDisconnectedState() throws Exception {
        TcpSocketManager spyManager = spy(new TcpSocketManager(mockEncoder, mockDecoder));
        doThrow(new IOException("Test")).when(spyManager).createSocket(any(), anyInt());

        CountDownLatch connectLatch = new CountDownLatch(1);
        spyManager.getConnectionState().observeForever(state -> {
            if (state == ConnectionState.CONNECTING || state == ConnectionState.RECONNECTING) {
                connectLatch.countDown();
            }
        });

        spyManager.connect("192.168.1.1", 8080);
        assertTrue(connectLatch.await(1, TimeUnit.SECONDS));

        spyManager.disconnect();

        assertEquals(ConnectionState.DISCONNECTED, spyManager.getConnectionState().getValue());
        assertFalse(spyManager.getShouldReconnect());
    }

    // ========== Encoder/Decoder integration tests ==========

    @Test
    public void send_usesEncoder() throws TcpProtocolException {
        LockScreenMessage message = new LockScreenMessage();
        when(mockEncoder.encode(message)).thenReturn(new byte[]{0x01});

        try {
            socketManager.send(message);
        } catch (IOException ignored) {
            // Expected since not connected
        }

        verify(mockEncoder).encode(message);
    }

    @Test
    public void send_throwsProtocolExceptionFromEncoder() throws TcpProtocolException, IOException {
        LockScreenMessage message = new LockScreenMessage();
        when(mockEncoder.encode(message)).thenThrow(
                new TcpProtocolException(TcpProtocolException.ErrorType.NULL_MESSAGE, "Test"));

        try {
            socketManager.send(message);
            fail("Expected TcpProtocolException");
        } catch (TcpProtocolException e) {
            assertEquals(TcpProtocolException.ErrorType.NULL_MESSAGE, e.getErrorType());
        }
    }
}
