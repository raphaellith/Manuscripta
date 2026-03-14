package com.manuscripta.student.network.tcp;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.manuscripta.student.network.tcp.RaiseHandManager.HandRaiseState;
import com.manuscripta.student.network.tcp.message.HandAckMessage;
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
    public InstantTaskExecutorRule instantTaskExecutorRule =
            new InstantTaskExecutorRule();

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
        manager = new RaiseHandManager(
                mockSocketManager, mockPairingManager, scheduler);
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
    public void testRaiseHandSetsStateToCooldown() {
        manager.raiseHand();

        assertEquals(HandRaiseState.COOLDOWN,
                manager.getState().getValue());
    }

    @Test
    public void testRaiseHandSendsMessage() throws Exception {
        manager.raiseHand();

        ArgumentCaptor<TcpMessage> captor =
                ArgumentCaptor.forClass(TcpMessage.class);
        verify(mockSocketManager, timeout(2000).atLeast(1))
                .send(captor.capture());
        TcpMessage sent = captor.getValue();
        assertEquals(TcpOpcode.HAND_RAISED, sent.getOpcode());
    }

    @Test
    public void testRaiseHandDuringCooldownIsIgnored() throws Exception {
        manager.raiseHand();
        assertEquals(HandRaiseState.COOLDOWN,
                manager.getState().getValue());

        // Second raise during cooldown should be ignored
        manager.raiseHand();
        assertEquals(HandRaiseState.COOLDOWN,
                manager.getState().getValue());
    }

    @Test
    public void testCooldownExpiresBackToIdle() throws Exception {
        manager.raiseHand();
        assertEquals(HandRaiseState.COOLDOWN,
                manager.getState().getValue());

        // Wait for cooldown to expire (3s + buffer)
        Thread.sleep(RaiseHandManager.COOLDOWN_SECONDS * 1000 + 500);

        assertEquals(HandRaiseState.IDLE,
                manager.getState().getValue());
    }

    @Test
    public void testHandAckDoesNotChangeState() {
        manager.raiseHand();

        manager.onMessageReceived(
                new HandAckMessage("test-device-id"));

        // State remains COOLDOWN — ACK is just logged
        assertEquals(HandRaiseState.COOLDOWN,
                manager.getState().getValue());
    }

    @Test
    public void testIgnoresNonHandAckMessages() {
        manager.raiseHand();

        manager.onMessageReceived(new LockScreenMessage());

        assertEquals(HandRaiseState.COOLDOWN,
                manager.getState().getValue());
    }

    @Test
    public void testRaiseHandWithNullDeviceIdDoesNotThrow() {
        when(mockPairingManager.getDeviceId()).thenReturn(null);

        manager.raiseHand();

        assertEquals(HandRaiseState.COOLDOWN,
                manager.getState().getValue());
    }

    @Test
    public void testDestroyPreventsRaiseHand() {
        manager.destroy();

        manager.raiseHand();

        assertEquals(HandRaiseState.IDLE,
                manager.getState().getValue());
    }

    @Test
    public void testDestroyPreventsOnMessageReceived() {
        manager.raiseHand();
        manager.destroy();

        manager.onMessageReceived(
                new HandAckMessage("test-device-id"));

        // State unchanged after destroy
        assertEquals(HandRaiseState.COOLDOWN,
                manager.getState().getValue());
    }

    @Test
    public void testOnConnectionStateChangedDoesNotThrow() {
        manager.onConnectionStateChanged(ConnectionState.CONNECTED);
        manager.onConnectionStateChanged(ConnectionState.DISCONNECTED);
    }

    @Test
    public void testOnErrorDoesNotThrow() {
        manager.onError(new TcpProtocolException("test error"));
    }

    @Test
    public void raiseHand_sendFailure_doesNotCrash() throws Exception {
        when(mockPairingManager.getDeviceId()).thenReturn("device-1");
        org.mockito.Mockito.doThrow(
                new java.io.IOException("connection lost"))
                .when(mockSocketManager).send(any(TcpMessage.class));

        manager.raiseHand();

        assertEquals(HandRaiseState.COOLDOWN,
                manager.getState().getValue());
    }

    @Test
    public void testNoSendAfterCooldown() throws Exception {
        manager.raiseHand();
        verify(mockSocketManager, timeout(2000).atLeast(1))
                .send(any(TcpMessage.class));

        int sendCount = org.mockito.Mockito
                .mockingDetails(mockSocketManager)
                .getInvocations().size();

        // Wait past cooldown — no further sends
        Thread.sleep(RaiseHandManager.COOLDOWN_SECONDS * 1000 + 500);

        int sendCountLater = org.mockito.Mockito
                .mockingDetails(mockSocketManager)
                .getInvocations().size();

        assertEquals(sendCount, sendCountLater);
    }
}
