package com.manuscripta.student.device;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.manuscripta.student.data.model.SessionStatus;
import com.manuscripta.student.data.repository.SessionRepository;
import com.manuscripta.student.domain.model.Session;
import com.manuscripta.student.network.tcp.ConnectionState;
import com.manuscripta.student.network.tcp.TcpProtocolException;
import com.manuscripta.student.network.tcp.TcpSocketManager;
import com.manuscripta.student.network.tcp.message.LockScreenMessage;
import com.manuscripta.student.network.tcp.message.UnpairMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link SessionEndHandler}.
 */
public class SessionEndHandlerTest {

    @Mock
    private SessionRepository mockSessionRepository;

    @Mock
    private TcpSocketManager mockSocketManager;

    @Mock
    private SessionEndHandler.SessionEndListener mockListener;

    @Mock
    private Session mockActiveSession;

    @Mock
    private Session mockPausedSession;

    @Mock
    private Session mockReceivedSession;

    private SessionEndHandler handler;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockActiveSession.getId()).thenReturn("session-active");
        when(mockPausedSession.getId()).thenReturn("session-paused");
        when(mockReceivedSession.getId()).thenReturn("session-received");
        handler = new SessionEndHandler(
                mockSessionRepository, mockSocketManager);
    }

    // ========== Constructor tests ==========

    @Test
    public void testConstructor_createsInstance() {
        assertNotNull(handler);
    }

    @Test
    public void testConstructor_nullSessionRepository_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new SessionEndHandler(null, mockSocketManager));
    }

    @Test
    public void testConstructor_nullSocketManager_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new SessionEndHandler(mockSessionRepository, null));
    }

    @Test
    public void testConstructor_initiallyInactive() {
        assertFalse(handler.isActive());
    }

    // ========== start/stop tests ==========

    @Test
    public void testStart_setsActive() {
        handler.start();
        assertTrue(handler.isActive());
    }

    @Test
    public void testStart_registersListener() {
        handler.start();
        verify(mockSocketManager).addMessageListener(handler);
    }

    @Test
    public void testStop_setsInactive() {
        handler.start();
        handler.stop();
        assertFalse(handler.isActive());
    }

    @Test
    public void testStop_unregistersListener() {
        handler.start();
        handler.stop();
        verify(mockSocketManager).removeMessageListener(handler);
    }

    // ========== handleUnpair tests ==========

    @Test
    public void testHandleUnpair_cancelsActiveSessions() {
        List<Session> activeSessions = Arrays.asList(mockActiveSession);
        when(mockSessionRepository.getSessionsByStatus(SessionStatus.ACTIVE))
                .thenReturn(activeSessions);
        when(mockSessionRepository.getSessionsByStatus(SessionStatus.PAUSED))
                .thenReturn(Collections.emptyList());
        when(mockSessionRepository.getSessionsByStatus(SessionStatus.RECEIVED))
                .thenReturn(Collections.emptyList());

        handler.handleUnpair();

        verify(mockSessionRepository).endSession(
                "session-active", SessionStatus.CANCELLED);
    }

    @Test
    public void testHandleUnpair_cancelsPausedSessions() {
        when(mockSessionRepository.getSessionsByStatus(SessionStatus.ACTIVE))
                .thenReturn(Collections.emptyList());
        when(mockSessionRepository.getSessionsByStatus(SessionStatus.PAUSED))
                .thenReturn(Arrays.asList(mockPausedSession));
        when(mockSessionRepository.getSessionsByStatus(SessionStatus.RECEIVED))
                .thenReturn(Collections.emptyList());

        handler.handleUnpair();

        verify(mockSessionRepository).endSession(
                "session-paused", SessionStatus.CANCELLED);
    }

    @Test
    public void testHandleUnpair_cancelsReceivedSessions() {
        when(mockSessionRepository.getSessionsByStatus(SessionStatus.ACTIVE))
                .thenReturn(Collections.emptyList());
        when(mockSessionRepository.getSessionsByStatus(SessionStatus.PAUSED))
                .thenReturn(Collections.emptyList());
        when(mockSessionRepository.getSessionsByStatus(SessionStatus.RECEIVED))
                .thenReturn(Arrays.asList(mockReceivedSession));

        handler.handleUnpair();

        verify(mockSessionRepository).endSession(
                "session-received", SessionStatus.CANCELLED);
    }

    @Test
    public void testHandleUnpair_disconnectsTcp() {
        when(mockSessionRepository.getSessionsByStatus(any()))
                .thenReturn(Collections.emptyList());

        handler.handleUnpair();

        verify(mockSocketManager).disconnect();
    }

    @Test
    public void testHandleUnpair_notifiesUnpaired() {
        when(mockSessionRepository.getSessionsByStatus(any()))
                .thenReturn(Collections.emptyList());
        handler.setSessionEndListener(mockListener);

        handler.handleUnpair();

        verify(mockListener).onDeviceUnpaired();
    }

    @Test
    public void testHandleUnpair_returnsCount() {
        when(mockSessionRepository.getSessionsByStatus(SessionStatus.ACTIVE))
                .thenReturn(Arrays.asList(mockActiveSession));
        when(mockSessionRepository.getSessionsByStatus(SessionStatus.PAUSED))
                .thenReturn(Arrays.asList(mockPausedSession));
        when(mockSessionRepository.getSessionsByStatus(SessionStatus.RECEIVED))
                .thenReturn(Arrays.asList(mockReceivedSession));

        int count = handler.handleUnpair();

        assertEquals(3, count);
    }

    @Test
    public void testHandleUnpair_noSessions_returnsZero() {
        when(mockSessionRepository.getSessionsByStatus(any()))
                .thenReturn(Collections.emptyList());

        int count = handler.handleUnpair();

        assertEquals(0, count);
    }

    // ========== cancelAllActiveSessions tests ==========

    @Test
    public void testCancelAllActiveSessions_cancelsAll() {
        when(mockSessionRepository.getSessionsByStatus(SessionStatus.ACTIVE))
                .thenReturn(Arrays.asList(mockActiveSession));
        when(mockSessionRepository.getSessionsByStatus(SessionStatus.PAUSED))
                .thenReturn(Arrays.asList(mockPausedSession));
        when(mockSessionRepository.getSessionsByStatus(SessionStatus.RECEIVED))
                .thenReturn(Arrays.asList(mockReceivedSession));

        int count = handler.cancelAllActiveSessions();

        assertEquals(3, count);
        verify(mockSessionRepository, times(3)).endSession(
                any(), eq(SessionStatus.CANCELLED));
    }

    @Test
    public void testCancelAllActiveSessions_noSessions_returnsZero() {
        when(mockSessionRepository.getSessionsByStatus(any()))
                .thenReturn(Collections.emptyList());

        assertEquals(0, handler.cancelAllActiveSessions());
    }

    @Test
    public void testCancelAllActiveSessions_notifiesListener() {
        when(mockSessionRepository.getSessionsByStatus(SessionStatus.ACTIVE))
                .thenReturn(Arrays.asList(mockActiveSession));
        when(mockSessionRepository.getSessionsByStatus(SessionStatus.PAUSED))
                .thenReturn(Collections.emptyList());
        when(mockSessionRepository.getSessionsByStatus(SessionStatus.RECEIVED))
                .thenReturn(Collections.emptyList());
        handler.setSessionEndListener(mockListener);

        handler.cancelAllActiveSessions();

        verify(mockListener).onSessionsEnded(1);
    }

    @Test
    public void testCancelAllActiveSessions_noSessions_noNotification() {
        when(mockSessionRepository.getSessionsByStatus(any()))
                .thenReturn(Collections.emptyList());
        handler.setSessionEndListener(mockListener);

        handler.cancelAllActiveSessions();

        verify(mockListener, never()).onSessionsEnded(0);
    }

    // ========== onMessageReceived tests ==========

    @Test
    public void testOnMessageReceived_unpairMessage_handlesUnpair() {
        when(mockSessionRepository.getSessionsByStatus(any()))
                .thenReturn(Collections.emptyList());
        handler.start();

        handler.onMessageReceived(new UnpairMessage());

        verify(mockSocketManager).disconnect();
    }

    @Test
    public void testOnMessageReceived_otherMessage_noEffect() {
        handler.start();
        handler.onMessageReceived(new LockScreenMessage());
        verify(mockSocketManager, never()).disconnect();
    }

    @Test
    public void testOnMessageReceived_notActive_ignoresMessage() {
        handler.onMessageReceived(new UnpairMessage());
        verify(mockSocketManager, never()).disconnect();
    }

    @Test
    public void testOnMessageReceived_afterStop_ignoresMessage() {
        handler.start();
        handler.stop();
        handler.onMessageReceived(new UnpairMessage());
        verify(mockSocketManager, never()).disconnect();
    }

    // ========== Listener tests ==========

    @Test
    public void testSetSessionEndListener_null_doesNotThrow() {
        handler.setSessionEndListener(null);
        when(mockSessionRepository.getSessionsByStatus(any()))
                .thenReturn(Collections.emptyList());
        handler.handleUnpair();
        // Should not throw
    }

    @Test
    public void testRemoveSessionEndListener_removesListener() {
        handler.setSessionEndListener(mockListener);
        handler.removeSessionEndListener();
        when(mockSessionRepository.getSessionsByStatus(any()))
                .thenReturn(Collections.emptyList());
        handler.handleUnpair();
        verify(mockListener, never()).onDeviceUnpaired();
    }

    // ========== onConnectionStateChanged tests ==========

    @Test
    public void testOnConnectionStateChanged_doesNothing() {
        handler.onConnectionStateChanged(ConnectionState.DISCONNECTED);
        // Should not throw
    }

    // ========== onError tests ==========

    @Test
    public void testOnError_doesNotThrow() {
        TcpProtocolException error = new TcpProtocolException(
                TcpProtocolException.ErrorType.EMPTY_DATA, "test");
        handler.onError(error);
        // Should not throw
    }
}
