package com.manuscripta.student.network.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.manuscripta.student.data.model.DeviceStatus;
import com.manuscripta.student.network.tcp.message.DistributeMaterialMessage;
import com.manuscripta.student.network.tcp.message.LockScreenMessage;
import com.manuscripta.student.network.tcp.message.StatusUpdateMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unit tests for {@link HeartbeatManager}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class HeartbeatManagerTest {

    @Mock
    private TcpSocketManager mockSocketManager;

    @Mock
    private TcpMessageEncoder mockEncoder;

    @Mock
    private TcpMessageDecoder mockDecoder;

    private HeartbeatManager heartbeatManager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Use minimum interval (1000ms) for tests - values below MIN_INTERVAL_MS are clamped
        HeartbeatConfig testConfig = new HeartbeatConfig(HeartbeatConfig.MIN_INTERVAL_MS, true);
        heartbeatManager = new HeartbeatManager(mockSocketManager, testConfig);
    }

    @After
    public void tearDown() {
        if (heartbeatManager != null) {
            heartbeatManager.destroy();
        }
    }

    // ========== Constructor tests ==========

    @Test
    public void constructor_registersAsListener() {
        verify(mockSocketManager).addMessageListener(heartbeatManager);
    }

    @Test
    public void constructor_setsConfig() {
        HeartbeatConfig config = new HeartbeatConfig(3000L, false);
        HeartbeatManager manager = new HeartbeatManager(mockSocketManager, config);

        assertEquals(3000L, manager.getConfig().getIntervalMs());
        assertFalse(manager.getConfig().isEnabled());

        manager.destroy();
    }

    @Test
    public void constructor_withSocketManagerOnly_usesDefaultConfig() {
        HeartbeatManager manager = new HeartbeatManager(mockSocketManager);

        assertEquals(HeartbeatConfig.DEFAULT_INTERVAL_MS,
                     manager.getConfig().getIntervalMs());
        assertTrue(manager.getConfig().isEnabled());

        manager.destroy();
    }

    // ========== start/stop tests ==========

    @Test
    public void start_setsRunningToTrue() {
        when(mockSocketManager.isConnected()).thenReturn(true);

        heartbeatManager.start();

        assertTrue(heartbeatManager.isRunning());
    }

    @Test
    public void start_doesNotStartWhenDisabled() {
        HeartbeatConfig disabledConfig = new HeartbeatConfig(1000L, false);
        heartbeatManager.setConfig(disabledConfig);

        heartbeatManager.start();

        assertFalse(heartbeatManager.isRunning());
    }

    @Test
    public void stop_setsRunningToFalse() {
        when(mockSocketManager.isConnected()).thenReturn(true);
        heartbeatManager.start();

        heartbeatManager.stop();

        assertFalse(heartbeatManager.isRunning());
    }

    @Test
    public void stop_canBeCalledMultipleTimes() {
        heartbeatManager.stop();
        heartbeatManager.stop();
        heartbeatManager.stop();

        assertFalse(heartbeatManager.isRunning());
    }

    // ========== Heartbeat sending tests ==========

    @Test
    public void sendHeartbeat_sendsStatusUpdateMessage() throws Exception {
        when(mockSocketManager.isConnected()).thenReturn(true);

        heartbeatManager.sendHeartbeat();

        ArgumentCaptor<TcpMessage> captor = ArgumentCaptor.forClass(TcpMessage.class);
        verify(mockSocketManager).send(captor.capture());
        assertTrue(captor.getValue() instanceof StatusUpdateMessage);
    }

    @Test
    public void sendHeartbeat_incrementsCount() throws Exception {
        when(mockSocketManager.isConnected()).thenReturn(true);

        heartbeatManager.sendHeartbeat();
        heartbeatManager.sendHeartbeat();

        assertEquals(2, heartbeatManager.getHeartbeatCount());
    }

    @Test
    public void sendHeartbeat_updatesTimestamp() throws Exception {
        when(mockSocketManager.isConnected()).thenReturn(true);
        long before = System.currentTimeMillis();

        heartbeatManager.sendHeartbeat();

        long timestamp = heartbeatManager.getLastHeartbeatTimestamp();
        assertTrue(timestamp >= before);
        assertTrue(timestamp <= System.currentTimeMillis());
    }

    @Test
    public void sendHeartbeat_skipsWhenNotConnected() throws Exception {
        when(mockSocketManager.isConnected()).thenReturn(false);

        heartbeatManager.sendHeartbeat();

        verify(mockSocketManager, never()).send(any());
    }

    @Test
    public void sendHeartbeat_handlesIOException() throws Exception {
        when(mockSocketManager.isConnected()).thenReturn(true);
        doThrow(new IOException("Test")).when(mockSocketManager).send(any());

        // Should not throw
        heartbeatManager.sendHeartbeat();

        assertEquals(0, heartbeatManager.getHeartbeatCount());
    }

    @Test
    public void sendHeartbeat_handlesTcpProtocolException() throws Exception {
        when(mockSocketManager.isConnected()).thenReturn(true);
        doThrow(new TcpProtocolException("Test protocol error"))
            .when(mockSocketManager).send(any());

        // Should not throw
        heartbeatManager.sendHeartbeat();

        assertEquals(0, heartbeatManager.getHeartbeatCount());
    }

    // ========== DeviceStatusProvider tests ==========

    @Test
    public void sendHeartbeat_usesStatusProvider() throws Exception {
        when(mockSocketManager.isConnected()).thenReturn(true);

        com.manuscripta.student.domain.model.DeviceStatus status =
                new com.manuscripta.student.domain.model.DeviceStatus(
                        "test-device-123",
                        DeviceStatus.ON_TASK,
                        75,
                        "material-abc",
                        "page-5",
                        System.currentTimeMillis()
                );

        heartbeatManager.setDeviceStatusProvider(() -> status);
        heartbeatManager.sendHeartbeat();

        ArgumentCaptor<TcpMessage> captor = ArgumentCaptor.forClass(TcpMessage.class);
        verify(mockSocketManager).send(captor.capture());

        StatusUpdateMessage message = (StatusUpdateMessage) captor.getValue();
        String payload = message.getJsonPayload();
        assertTrue(payload.contains("test-device-123"));
        assertTrue(payload.contains("ON_TASK"));
        assertTrue(payload.contains("75"));
    }

    @Test
    public void sendHeartbeat_handlesNullStatusProvider() throws Exception {
        when(mockSocketManager.isConnected()).thenReturn(true);
        heartbeatManager.setDeviceStatusProvider(null);

        // Should not throw
        heartbeatManager.sendHeartbeat();

        verify(mockSocketManager).send(any());
    }

    @Test
    public void sendHeartbeat_handlesNullStatus() throws Exception {
        when(mockSocketManager.isConnected()).thenReturn(true);
        heartbeatManager.setDeviceStatusProvider(() -> null);

        // Should not throw
        heartbeatManager.sendHeartbeat();

        verify(mockSocketManager).send(any());
    }

    // ========== DISTRIBUTE_MATERIAL handling tests ==========

    @Test
    public void onMessageReceived_callsMaterialCallback() {
        AtomicBoolean callbackCalled = new AtomicBoolean(false);
        heartbeatManager.setMaterialCallback(() -> callbackCalled.set(true));

        heartbeatManager.onMessageReceived(new DistributeMaterialMessage());

        assertTrue(callbackCalled.get());
    }

    @Test
    public void onMessageReceived_ignoresOtherMessages() {
        AtomicBoolean callbackCalled = new AtomicBoolean(false);
        heartbeatManager.setMaterialCallback(() -> callbackCalled.set(true));

        heartbeatManager.onMessageReceived(new LockScreenMessage());

        assertFalse(callbackCalled.get());
    }

    @Test
    public void onMessageReceived_handlesNullCallback() {
        heartbeatManager.setMaterialCallback(null);

        // Should not throw
        heartbeatManager.onMessageReceived(new DistributeMaterialMessage());
    }

    // ========== Connection state handling tests ==========

    @Test
    public void onConnectionStateChanged_startsOnConnected() {
        heartbeatManager.onConnectionStateChanged(ConnectionState.CONNECTED);

        assertTrue(heartbeatManager.isRunning());
    }

    @Test
    public void onConnectionStateChanged_stopsOnDisconnected() {
        when(mockSocketManager.isConnected()).thenReturn(true);
        heartbeatManager.start();

        heartbeatManager.onConnectionStateChanged(ConnectionState.DISCONNECTED);

        assertFalse(heartbeatManager.isRunning());
    }

    @Test
    public void onConnectionStateChanged_doesNotStartWhenDisabled() {
        HeartbeatConfig disabledConfig = new HeartbeatConfig(1000L, false);
        heartbeatManager.setConfig(disabledConfig);

        heartbeatManager.onConnectionStateChanged(ConnectionState.CONNECTED);

        assertFalse(heartbeatManager.isRunning());
    }

    @Test
    public void onError_handlesGracefully() {
        when(mockSocketManager.isConnected()).thenReturn(true);
        heartbeatManager.start();
        assertTrue(heartbeatManager.isRunning());

        // Should not throw and heartbeat should continue
        heartbeatManager.onError(new TcpProtocolException("Test error"));

        // Verify heartbeat continues running
        assertTrue(heartbeatManager.isRunning());
    }

    // ========== Config update tests ==========

    @Test
    public void setConfig_updatesConfig() {
        HeartbeatConfig newConfig = new HeartbeatConfig(10000L, false);

        heartbeatManager.setConfig(newConfig);

        assertEquals(10000L, heartbeatManager.getConfig().getIntervalMs());
        assertFalse(heartbeatManager.getConfig().isEnabled());
    }

    @Test
    public void setConfig_doesNotStartWhenNotRunning() {
        assertFalse(heartbeatManager.isRunning());
        HeartbeatConfig newConfig = new HeartbeatConfig(5000L, true);

        heartbeatManager.setConfig(newConfig);

        assertEquals(5000L, heartbeatManager.getConfig().getIntervalMs());
        assertFalse(heartbeatManager.isRunning()); // Should NOT auto-start
    }

    @Test
    public void setConfig_restartsIfRunning() throws Exception {
        when(mockSocketManager.isConnected()).thenReturn(true);
        heartbeatManager.start();
        assertTrue(heartbeatManager.isRunning());

        // Update to still-enabled config
        HeartbeatConfig newConfig = new HeartbeatConfig(2000L, true);
        heartbeatManager.setConfig(newConfig);

        assertTrue(heartbeatManager.isRunning());
        assertEquals(2000L, heartbeatManager.getConfig().getIntervalMs());
    }

    // ========== destroy tests ==========

    @Test
    public void destroy_stopsHeartbeat() {
        when(mockSocketManager.isConnected()).thenReturn(true);
        heartbeatManager.start();

        heartbeatManager.destroy();

        assertFalse(heartbeatManager.isRunning());
    }

    @Test
    public void destroy_removesListener() {
        heartbeatManager.destroy();

        verify(mockSocketManager).removeMessageListener(heartbeatManager);
    }

    // ========== Periodic sending tests ==========

    @Test
    public void heartbeat_sendsPeriodically() throws Exception {
        when(mockSocketManager.isConnected()).thenReturn(true);

        heartbeatManager.start();

        // HeartbeatConfig clamps interval to MIN_INTERVAL_MS (1000ms), so wait for 2+ heartbeats
        // First heartbeat at t=0, second at t=1000ms
        Thread.sleep(1500);

        // Verify at least 2 heartbeats were sent
        verify(mockSocketManager, atLeastOnce()).send(any());
        assertTrue("Expected at least 2 heartbeats", heartbeatManager.getHeartbeatCount() >= 2);
    }

    // ========== Initial state tests ==========

    @Test
    public void initialState_notRunning() {
        assertFalse(heartbeatManager.isRunning());
    }

    @Test
    public void initialState_zeroHeartbeatCount() {
        assertEquals(0, heartbeatManager.getHeartbeatCount());
    }

    @Test
    public void initialState_zeroLastTimestamp() {
        assertEquals(0, heartbeatManager.getLastHeartbeatTimestamp());
    }

    @Test
    public void getConfig_returnsNonNull() {
        assertNotNull(heartbeatManager.getConfig());
    }
}
