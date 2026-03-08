package com.manuscripta.student.network.tcp;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.manuscripta.student.network.tcp.RaiseHandManager.HandRaiseState;
import com.manuscripta.student.network.tcp.message.HandAckMessage;
import com.manuscripta.student.network.tcp.message.HandRaisedMessage;
import com.manuscripta.student.network.tcp.message.LockScreenMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Unit tests for {@link RaiseHandManager}.
 */
public class RaiseHandManagerTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private TcpSocketManager mockSocketManager;

    @Mock
    private PairingManager mockPairingManager;

    private ScheduledExecutorService scheduler;

    private RaiseHandManager manager;

    /**
     * Sets up mocks and creates the manager with a real scheduler.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockPairingManager.getDeviceId()).thenReturn("test-device-id");
        scheduler = Executors.newSingleThreadScheduledExecutor();
        manager = new RaiseHandManager(mockSocketManager, mockPairingManager, scheduler);
    }

    /**
     * Cleans up the manager after each test.
     */
    @After
    public void tearDown() {
        manager.destroy();
    }

    @Test
    public void testInitialStateIsIdle() {
        assertEquals(HandRaiseState.IDLE, manager.getState().getValue());
    }

    @Test
    public void testRaiseHandSetsStateToPending() {
        manager.raiseHand();

        assertEquals(HandRaiseState.PENDING, manager.getState().getValue());
    }

    @Test
    public void testRaiseHandSendsMessage() throws Exception {
        manager.raiseHand();

        ArgumentCaptor<TcpMessage> captor = ArgumentCaptor.forClass(TcpMessage.class);
        verify(mockSocketManager, atLeast(1)).send(captor.capture());
        TcpMessage sent = captor.getValue();
        assertEquals(TcpOpcode.HAND_RAISED, sent.getOpcode());
    }

    @Test
    public void testLowerHandSetsStateToIdle() {
        manager.raiseHand();

        manager.lowerHand();

        assertEquals(HandRaiseState.IDLE, manager.getState().getValue());
    }

    @Test
    public void testHandAckSetsStateToAcknowledged() {
        manager.raiseHand();

        manager.onMessageReceived(new HandAckMessage("test-device-id"));

        assertEquals(HandRaiseState.ACKNOWLEDGED, manager.getState().getValue());
    }

    @Test
    public void testToggleFromIdleRaisesHand() {
        manager.toggle();

        assertEquals(HandRaiseState.PENDING, manager.getState().getValue());
    }

    @Test
    public void testToggleFromPendingLowersHand() {
        manager.raiseHand();

        manager.toggle();

        assertEquals(HandRaiseState.IDLE, manager.getState().getValue());
    }

    @Test
    public void testToggleFromAcknowledgedLowersHand() {
        manager.raiseHand();
        manager.onMessageReceived(new HandAckMessage("test-device-id"));

        manager.toggle();

        assertEquals(HandRaiseState.IDLE, manager.getState().getValue());
    }

    @Test
    public void testIgnoresNonHandAckMessages() {
        manager.raiseHand();

        manager.onMessageReceived(new LockScreenMessage());

        assertEquals(HandRaiseState.PENDING, manager.getState().getValue());
    }

    @Test
    public void testRaiseHandWithNullDeviceIdDoesNotThrow() {
        when(mockPairingManager.getDeviceId()).thenReturn(null);

        manager.raiseHand();

        assertEquals(HandRaiseState.PENDING, manager.getState().getValue());
    }

    @Test
    public void testDestroyPreventsRaiseHand() {
        manager.destroy();

        manager.raiseHand();

        assertEquals(HandRaiseState.IDLE, manager.getState().getValue());
    }

    @Test
    public void testDestroyPreventsOnMessageReceived() {
        manager.raiseHand();
        manager.destroy();

        manager.onMessageReceived(new HandAckMessage("test-device-id"));

        assertEquals(HandRaiseState.PENDING, manager.getState().getValue());
    }

    @Test
    public void testOnConnectionStateChangedDoesNotThrow() {
        manager.onConnectionStateChanged(ConnectionState.CONNECTED);
        manager.onConnectionStateChanged(ConnectionState.DISCONNECTED);
        // No exception expected
    }

    @Test
    public void testOnErrorDoesNotThrow() {
        manager.onError(new TcpProtocolException("test error"));
        // No exception expected
    }

    @Test
    public void testHandAckCancelsRetry() throws Exception {
        manager.raiseHand();
        verify(mockSocketManager, atLeast(1)).send(any(TcpMessage.class));

        manager.onMessageReceived(new HandAckMessage("test-device-id"));

        // Wait past one retry interval to confirm no more sends
        Thread.sleep(RaiseHandManager.RETRY_INTERVAL_SECONDS * 1000 + 500);
        int sendCountAfterAck = org.mockito.Mockito.mockingDetails(mockSocketManager)
                .getInvocations().size();

        Thread.sleep(RaiseHandManager.RETRY_INTERVAL_SECONDS * 1000 + 500);
        int sendCountLater = org.mockito.Mockito.mockingDetails(mockSocketManager)
                .getInvocations().size();

        assertEquals(sendCountAfterAck, sendCountLater);
    }

    @Test
    public void onConnectionStateChanged_doesNotThrow() {
        manager.onConnectionStateChanged(ConnectionState.CONNECTED);
        manager.onConnectionStateChanged(ConnectionState.DISCONNECTED);
        // No exception expected — method is a no-op
    }

    @Test
    public void onError_doesNotThrow() {
        manager.onError(new TcpProtocolException("test error"));
        // No exception expected — method only logs
    }

    @Test
    public void raiseHand_sendFailure_doesNotCrash() throws Exception {
        when(mockPairingManager.getDeviceId()).thenReturn("device-1");
        org.mockito.Mockito.doThrow(new java.io.IOException("connection lost"))
                .when(mockSocketManager).send(any(TcpMessage.class));

        manager.raiseHand();

        // Should still transition to PENDING despite send failure
        assertEquals(HandRaiseState.PENDING, manager.getState().getValue());
    }
}
