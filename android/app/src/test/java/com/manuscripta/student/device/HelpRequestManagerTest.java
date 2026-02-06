package com.manuscripta.student.device;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.manuscripta.student.network.tcp.ConnectionState;
import com.manuscripta.student.network.tcp.TcpProtocolException;
import com.manuscripta.student.network.tcp.TcpSocketManager;
import com.manuscripta.student.network.tcp.message.HandAckMessage;
import com.manuscripta.student.network.tcp.message.HandRaisedMessage;
import com.manuscripta.student.network.tcp.message.LockScreenMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

/**
 * Unit tests for {@link HelpRequestManager}.
 */
public class HelpRequestManagerTest {

    private static final String TEST_DEVICE_ID = "test-device-123";

    @Mock
    private TcpSocketManager mockSocketManager;

    @Mock
    private HelpRequestManager.HelpRequestListener mockListener;

    private HelpRequestManager manager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        manager = new HelpRequestManager(mockSocketManager);
    }

    // ========== Constructor tests ==========

    @Test
    public void testConstructor_createsInstance() {
        assertNotNull(manager);
    }

    @Test
    public void testConstructor_nullSocketManager_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new HelpRequestManager(null));
    }

    @Test
    public void testConstructor_registersAsListener() {
        verify(mockSocketManager).addMessageListener(manager);
    }

    @Test
    public void testConstructor_initiallyNotRaised() {
        assertFalse(manager.isHandRaised());
    }

    @Test
    public void testConstructor_initiallyNotAcknowledged() {
        assertFalse(manager.isAcknowledged());
    }

    // ========== raiseHand tests ==========

    @Test
    public void testRaiseHand_success_returnsTrue() throws Exception {
        assertTrue(manager.raiseHand(TEST_DEVICE_ID));
    }

    @Test
    public void testRaiseHand_success_setsHandRaised() throws Exception {
        manager.raiseHand(TEST_DEVICE_ID);
        assertTrue(manager.isHandRaised());
    }

    @Test
    public void testRaiseHand_success_sendsMessage() throws Exception {
        manager.raiseHand(TEST_DEVICE_ID);
        verify(mockSocketManager).send(any(HandRaisedMessage.class));
    }

    @Test
    public void testRaiseHand_success_notAcknowledged() throws Exception {
        manager.raiseHand(TEST_DEVICE_ID);
        assertFalse(manager.isAcknowledged());
    }

    @Test
    public void testRaiseHand_ioException_returnsFalse() throws Exception {
        doThrow(new IOException("Connection lost"))
                .when(mockSocketManager).send(any());
        assertFalse(manager.raiseHand(TEST_DEVICE_ID));
    }

    @Test
    public void testRaiseHand_protocolException_returnsFalse() throws Exception {
        doThrow(new TcpProtocolException(
                TcpProtocolException.ErrorType.EMPTY_DATA, "error"))
                .when(mockSocketManager).send(any());
        assertFalse(manager.raiseHand(TEST_DEVICE_ID));
    }

    @Test
    public void testRaiseHand_alreadyRaisedAndAcked_returnsTrue() throws Exception {
        manager.raiseHand(TEST_DEVICE_ID);
        manager.onMessageReceived(new HandAckMessage(TEST_DEVICE_ID));
        assertTrue(manager.raiseHand(TEST_DEVICE_ID));
    }

    @Test
    public void testRaiseHand_nullDeviceId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.raiseHand(null));
    }

    @Test
    public void testRaiseHand_emptyDeviceId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.raiseHand(""));
    }

    @Test
    public void testRaiseHand_blankDeviceId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.raiseHand("   "));
    }

    // ========== lowerHand tests ==========

    @Test
    public void testLowerHand_resetsHandRaised() throws Exception {
        manager.raiseHand(TEST_DEVICE_ID);
        manager.lowerHand();
        assertFalse(manager.isHandRaised());
    }

    @Test
    public void testLowerHand_resetsAcknowledged() throws Exception {
        manager.raiseHand(TEST_DEVICE_ID);
        manager.onMessageReceived(new HandAckMessage(TEST_DEVICE_ID));
        manager.lowerHand();
        assertFalse(manager.isAcknowledged());
    }

    @Test
    public void testLowerHand_whenNotRaised_noEffect() {
        manager.lowerHand();
        assertFalse(manager.isHandRaised());
    }

    // ========== onMessageReceived tests ==========

    @Test
    public void testOnMessageReceived_handAck_setsAcknowledged() throws Exception {
        manager.raiseHand(TEST_DEVICE_ID);
        manager.onMessageReceived(new HandAckMessage(TEST_DEVICE_ID));
        assertTrue(manager.isAcknowledged());
    }

    @Test
    public void testOnMessageReceived_handAck_notifiesListener() throws Exception {
        manager.raiseHand(TEST_DEVICE_ID);
        manager.setHelpRequestListener(mockListener);
        manager.onMessageReceived(new HandAckMessage(TEST_DEVICE_ID));
        verify(mockListener).onHelpAcknowledged();
    }

    @Test
    public void testOnMessageReceived_handAck_notRaised_noAck() {
        manager.onMessageReceived(new HandAckMessage(TEST_DEVICE_ID));
        assertFalse(manager.isAcknowledged());
    }

    @Test
    public void testOnMessageReceived_otherMessage_noEffect() throws Exception {
        manager.raiseHand(TEST_DEVICE_ID);
        manager.onMessageReceived(new LockScreenMessage());
        assertFalse(manager.isAcknowledged());
    }

    // ========== getRetryIntervalMs tests ==========

    @Test
    public void testGetRetryIntervalMs_returnsConstant() {
        assertEquals(3000L, manager.getRetryIntervalMs());
    }

    @Test
    public void testRetryIntervalMs_constant() {
        assertEquals(3000L, HelpRequestManager.RETRY_INTERVAL_MS);
    }

    // ========== Listener tests ==========

    @Test
    public void testSetHelpRequestListener_null_doesNotThrow() throws Exception {
        manager.setHelpRequestListener(null);
        manager.raiseHand(TEST_DEVICE_ID);
        manager.onMessageReceived(new HandAckMessage(TEST_DEVICE_ID));
        // Should not throw
    }

    @Test
    public void testRemoveHelpRequestListener_removesListener() throws Exception {
        manager.setHelpRequestListener(mockListener);
        manager.removeHelpRequestListener();
        manager.raiseHand(TEST_DEVICE_ID);
        manager.onMessageReceived(new HandAckMessage(TEST_DEVICE_ID));
        verify(mockListener, never()).onHelpAcknowledged();
    }

    // ========== destroy tests ==========

    @Test
    public void testDestroy_removesListener() {
        manager.destroy();
        verify(mockSocketManager).removeMessageListener(manager);
    }

    @Test
    public void testDestroy_lowersHand() throws Exception {
        manager.raiseHand(TEST_DEVICE_ID);
        manager.destroy();
        assertFalse(manager.isHandRaised());
    }

    // ========== onConnectionStateChanged tests ==========

    @Test
    public void testOnConnectionStateChanged_doesNothing() {
        manager.onConnectionStateChanged(ConnectionState.DISCONNECTED);
        // Should not throw
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
