package com.manuscripta.student.network.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.manuscripta.student.network.tcp.message.PairingAckMessage;
import com.manuscripta.student.network.tcp.message.PairingRequestMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

/**
 * Unit tests for {@link PairingManager}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class PairingManagerTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static final String TEST_DEVICE_ID = "test-device-123";
    private static final String TEST_HOST = "192.168.1.100";
    private static final int TEST_PORT = 5912;

    @Mock
    private TcpSocketManager mockSocketManager;

    @Mock
    private PairingCallback mockCallback;

    private PairingManager pairingManager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Use short timeout for faster tests
        PairingConfig testConfig = new PairingConfig(PairingConfig.MIN_TIMEOUT_MS, 0);
        pairingManager = new PairingManager(mockSocketManager, testConfig);
        pairingManager.setPairingCallback(mockCallback);
    }

    @After
    public void tearDown() {
        if (pairingManager != null) {
            pairingManager.destroy();
        }
    }

    // ========== Constructor tests ==========

    @Test
    public void constructor_registersAsListener() {
        verify(mockSocketManager).addMessageListener(pairingManager);
    }

    @Test
    public void constructor_setsConfig() {
        PairingConfig config = new PairingConfig(5000L, 2);
        PairingManager manager = new PairingManager(mockSocketManager, config);

        assertEquals(5000L, manager.getConfig().getTimeoutMs());
        assertEquals(2, manager.getConfig().getRetryCount());

        manager.destroy();
    }

    @Test
    public void constructor_setsInitialStateToNotPaired() {
        assertEquals(PairingState.NOT_PAIRED, pairingManager.getCurrentState());
    }

    // ========== startPairing tests ==========

    @Test
    public void startPairing_setsStateToInProgress() {
        pairingManager.startPairing(TEST_DEVICE_ID, TEST_HOST, TEST_PORT);

        assertEquals(PairingState.PAIRING_IN_PROGRESS, pairingManager.getCurrentState());
    }

    @Test
    public void startPairing_connectsToServer() {
        pairingManager.startPairing(TEST_DEVICE_ID, TEST_HOST, TEST_PORT);

        verify(mockSocketManager).connect(TEST_HOST, TEST_PORT);
    }

    @Test
    public void startPairing_storesDeviceId() {
        pairingManager.startPairing(TEST_DEVICE_ID, TEST_HOST, TEST_PORT);

        assertEquals(TEST_DEVICE_ID, pairingManager.getDeviceId());
    }

    @Test
    public void startPairing_setsInProgressFlag() {
        pairingManager.startPairing(TEST_DEVICE_ID, TEST_HOST, TEST_PORT);

        assertTrue(pairingManager.isPairingInProgress());
    }

    @Test
    public void startPairing_ignoresIfAlreadyInProgress() {
        pairingManager.startPairing(TEST_DEVICE_ID, TEST_HOST, TEST_PORT);
        pairingManager.startPairing("other-device", "other-host", 9999);

        // Should still have first device ID
        assertEquals(TEST_DEVICE_ID, pairingManager.getDeviceId());
        verify(mockSocketManager).connect(TEST_HOST, TEST_PORT);
    }

    // ========== Connection state handling tests ==========

    @Test
    public void onConnectionStateChanged_sendsPairingRequestWhenConnected() throws Exception {
        pairingManager.startPairing(TEST_DEVICE_ID, TEST_HOST, TEST_PORT);

        pairingManager.onConnectionStateChanged(ConnectionState.CONNECTED);

        ArgumentCaptor<TcpMessage> captor = ArgumentCaptor.forClass(TcpMessage.class);
        verify(mockSocketManager).send(captor.capture());
        assertTrue(captor.getValue() instanceof PairingRequestMessage);
        assertEquals(TEST_DEVICE_ID, ((PairingRequestMessage) captor.getValue()).getDeviceId());
    }

    @Test
    public void onConnectionStateChanged_ignoresIfNotPairing() throws Exception {
        // Not started pairing
        pairingManager.onConnectionStateChanged(ConnectionState.CONNECTED);

        verify(mockSocketManager, never()).send(any());
    }

    // ========== PAIRING_ACK handling tests ==========

    @Test
    public void onMessageReceived_handlesPairingAck() {
        pairingManager.startPairing(TEST_DEVICE_ID, TEST_HOST, TEST_PORT);
        pairingManager.onConnectionStateChanged(ConnectionState.CONNECTED);

        pairingManager.onMessageReceived(new PairingAckMessage());

        assertEquals(PairingState.PAIRED, pairingManager.getCurrentState());
        assertFalse(pairingManager.isPairingInProgress());
    }

    @Test
    public void onMessageReceived_callsCallbackOnSuccess() throws Exception {
        when(mockSocketManager.isConnected()).thenReturn(true);
        pairingManager.startPairing(TEST_DEVICE_ID, TEST_HOST, TEST_PORT);
        pairingManager.onConnectionStateChanged(ConnectionState.CONNECTED);

        pairingManager.onMessageReceived(new PairingAckMessage());

        verify(mockCallback).onTcpPairingSuccess();
    }

    @Test
    public void onMessageReceived_ignoresPairingAckIfNotPairing() {
        pairingManager.onMessageReceived(new PairingAckMessage());

        assertEquals(PairingState.NOT_PAIRED, pairingManager.getCurrentState());
        verify(mockCallback, never()).onTcpPairingSuccess();
    }

    // ========== cancelPairing tests ==========

    @Test
    public void cancelPairing_setsStateToNotPaired() {
        pairingManager.startPairing(TEST_DEVICE_ID, TEST_HOST, TEST_PORT);

        pairingManager.cancelPairing();

        assertEquals(PairingState.NOT_PAIRED, pairingManager.getCurrentState());
        assertFalse(pairingManager.isPairingInProgress());
    }

    @Test
    public void cancelPairing_doesNothingIfNotPairing() {
        pairingManager.cancelPairing();

        assertEquals(PairingState.NOT_PAIRED, pairingManager.getCurrentState());
    }

    // ========== setConfig tests ==========

    @Test
    public void setConfig_updatesConfig() {
        PairingConfig newConfig = new PairingConfig(8000L, 5);

        pairingManager.setConfig(newConfig);

        assertEquals(8000L, pairingManager.getConfig().getTimeoutMs());
        assertEquals(5, pairingManager.getConfig().getRetryCount());
    }

    // ========== destroy tests ==========

    @Test
    public void destroy_removesListener() {
        pairingManager.destroy();

        verify(mockSocketManager).removeMessageListener(pairingManager);
    }

    @Test
    public void destroy_cancelsPairing() {
        pairingManager.startPairing(TEST_DEVICE_ID, TEST_HOST, TEST_PORT);

        pairingManager.destroy();

        assertFalse(pairingManager.isPairingInProgress());
    }

    // ========== LiveData tests ==========

    @Test
    public void getPairingState_returnsNonNullLiveData() {
        assertNotNull(pairingManager.getPairingState());
    }

    @Test
    public void getCurrentState_returnsNotPairedInitially() {
        assertEquals(PairingState.NOT_PAIRED, pairingManager.getCurrentState());
    }

    // ========== Error handling tests ==========

    @Test
    public void onConnectionStateChanged_handlesDisconnectionDuringPairing() throws Exception {
        // Use config with no retries
        PairingConfig noRetryConfig = new PairingConfig(PairingConfig.MIN_TIMEOUT_MS, 0);
        pairingManager.setConfig(noRetryConfig);

        pairingManager.startPairing(TEST_DEVICE_ID, TEST_HOST, TEST_PORT);
        pairingManager.onConnectionStateChanged(ConnectionState.CONNECTED);
        pairingManager.onConnectionStateChanged(ConnectionState.DISCONNECTED);

        assertEquals(PairingState.PAIRING_FAILED, pairingManager.getCurrentState());
        verify(mockCallback).onPairingFailed(any());
    }

    @Test
    public void sendPairingRequest_handlesIOException() throws Exception {
        doThrow(new IOException("Test")).when(mockSocketManager).send(any());
        PairingConfig noRetryConfig = new PairingConfig(PairingConfig.MIN_TIMEOUT_MS, 0);
        pairingManager.setConfig(noRetryConfig);

        pairingManager.startPairing(TEST_DEVICE_ID, TEST_HOST, TEST_PORT);
        pairingManager.onConnectionStateChanged(ConnectionState.CONNECTED);

        assertEquals(PairingState.PAIRING_FAILED, pairingManager.getCurrentState());
    }

    // ========== Callback tests ==========

    @Test
    public void setPairingCallback_handlesNullCallback() {
        pairingManager.setPairingCallback(null);

        pairingManager.startPairing(TEST_DEVICE_ID, TEST_HOST, TEST_PORT);
        pairingManager.onConnectionStateChanged(ConnectionState.CONNECTED);
        pairingManager.onMessageReceived(new PairingAckMessage());

        // Should not throw
        assertEquals(PairingState.PAIRED, pairingManager.getCurrentState());
    }

    // ========== Retry tests ==========

    @Test
    public void getCurrentRetryAttempt_returnsZeroInitially() {
        assertEquals(0, pairingManager.getCurrentRetryAttempt());
    }

    @Test
    public void getDeviceId_returnsNullInitially() {
        assertNull(pairingManager.getDeviceId());
    }
}
