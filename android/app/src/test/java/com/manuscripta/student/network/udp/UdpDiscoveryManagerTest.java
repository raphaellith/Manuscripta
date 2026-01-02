package com.manuscripta.student.network.udp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.manuscripta.student.utils.MulticastLockManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Unit tests for {@link UdpDiscoveryManager}.
 */
public class UdpDiscoveryManagerTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private UdpDiscoveryManager manager;
    private DatagramSocket mockSocket;
    private Context mockContext;
    private MulticastLockManager mockLockManager;

    @Before
    public void setUp() throws Exception {
        mockSocket = mock(DatagramSocket.class);
        mockContext = mock(Context.class);
        mockLockManager = mock(MulticastLockManager.class);
        
        // Configure multicast lock manager to succeed by default
        when(mockLockManager.acquire(any(Context.class))).thenReturn(true);
        
        AtomicBoolean closed = new AtomicBoolean(false);
        when(mockSocket.isClosed()).thenAnswer(invocation -> closed.get());
        doAnswer(invocation -> {
            closed.set(true);
            return null;
        }).when(mockSocket).close();
    }

    @Test
    public void testConstructor_createsInstance() {
        // When
        manager = new UdpDiscoveryManager(mockContext, mockLockManager);

        // Then
        assertNotNull(manager);
        assertFalse(manager.isRunning());
        assertNull(manager.getDiscoveredServer());
    }

    @Test
    public void testUdpPort_isCorrectValue() {
        // Then
        assertEquals(5913, UdpDiscoveryManager.UDP_PORT);
    }

    @Test
    public void testStartDiscovery_setsRunningTrue() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        configureMockSocketToTimeout();

        // When
        manager.startDiscovery();
        awaitCondition(manager::isRunning, 2000, "Manager should start running");

        // Then
        assertTrue(manager.isRunning());

        // Cleanup
        manager.stopDiscovery();
    }

    @Test
    public void testStartDiscovery_whenAlreadyRunning_noOp() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        configureMockSocketToTimeout();
        manager.startDiscovery();
        awaitCondition(manager::isRunning, 2000, "Manager should start running");
        assertTrue(manager.isRunning());

        // When - call start again
        manager.startDiscovery();

        // Then - still running, no exception
        assertTrue(manager.isRunning());

        // Cleanup
        manager.stopDiscovery();
    }

    @Test
    public void testStopDiscovery_setsRunningFalse() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        configureMockSocketToTimeout();
        manager.startDiscovery();
        awaitCondition(manager::isRunning, 2000, "Manager should start running");
        assertTrue(manager.isRunning());

        // When
        manager.stopDiscovery();
        awaitCondition(() -> !manager.isRunning(), 2000, "Manager should stop running");

        // Then
        assertFalse(manager.isRunning());
    }

    @Test
    public void testStopDiscovery_whenNotRunning_noOp() {
        // Given
        manager = new UdpDiscoveryManager(mockContext, mockLockManager);
        assertFalse(manager.isRunning());

        // When
        manager.stopDiscovery();

        // Then - no exception, still not running
        assertFalse(manager.isRunning());
    }

    @Test
    public void testStopDiscovery_closesSocket() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        configureMockSocketToTimeout();
        manager.startDiscovery();
        awaitCondition(manager::isRunning, 2000, "Manager should start running");

        // When
        manager.stopDiscovery();
        // Wait for the socket to be closed (happens in the finally block of the listening thread)
        awaitCondition(mockSocket::isClosed, 2000, "Socket should be closed");

        // Then - verify close was called at least once
        verify(mockSocket, atLeastOnce()).close();
    }

    @Test
    public void testGetDiscoveredServer_initiallyNull() {
        // Given
        manager = new UdpDiscoveryManager(mockContext, mockLockManager);

        // Then
        assertNull(manager.getDiscoveredServer());
    }

    @Test
    public void testClearDiscoveredServer_clearsStoredServer() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        byte[] validPacketData = createValidDiscoveryMessage(
                new byte[]{(byte) 192, (byte) 168, 1, 100}, 8080, 9090);
        configureMockSocketToReceiveOnce(validPacketData);
        
        manager.startDiscovery();
        awaitCondition(() -> manager.getDiscoveredServer() != null, 2000, "Server should be discovered");
        manager.stopDiscovery();
        awaitCondition(() -> !manager.isRunning(), 2000, "Manager should stop running");
        
        assertNotNull(manager.getDiscoveredServer());

        // When
        manager.clearDiscoveredServer();

        // Then
        assertNull(manager.getDiscoveredServer());
    }

    @Test
    public void testReceiveValidPacket_storesDiscoveryMessage() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        byte[] validPacketData = createValidDiscoveryMessage(
                new byte[]{(byte) 192, (byte) 168, 1, 100}, 8080, 9090);
        configureMockSocketToReceiveOnce(validPacketData);

        // When
        manager.startDiscovery();
        awaitCondition(() -> manager.getDiscoveredServer() != null, 2000, "Server should be discovered");
        manager.stopDiscovery();
        awaitCondition(() -> !manager.isRunning(), 2000, "Manager should stop running");

        // Then
        DiscoveryMessage message = manager.getDiscoveredServer();
        assertNotNull(message);
        assertEquals("192.168.1.100", message.getIpAddress());
        assertEquals(8080, message.getHttpPort());
        assertEquals(9090, message.getTcpPort());
    }

    @Test
    public void testReceiveMultiplePackets_storesFirst() throws Exception {
        // Given - discovery stops after first valid packet is found (per design)
        manager = createManagerWithMockSocket();
        byte[] firstPacket = createValidDiscoveryMessage(
                new byte[]{(byte) 192, (byte) 168, 1, 1}, 8080, 9090);
        byte[] secondPacket = createValidDiscoveryMessage(
                new byte[]{10, 0, 0, 1}, 443, 8443);
        configureMockSocketToReceiveMultiple(firstPacket, secondPacket);

        // When
        manager.startDiscovery();
        awaitCondition(() -> manager.getDiscoveredServer() != null, 
                2000, "Server should be discovered");

        // Then - should have the first packet (discovery stops after finding a server)
        DiscoveryMessage message = manager.getDiscoveredServer();
        assertNotNull(message);
        assertEquals("192.168.1.1", message.getIpAddress());
        assertEquals(8080, message.getHttpPort());
        assertEquals(9090, message.getTcpPort());
        
        // Verify state transitioned to FOUND
        assertEquals(DiscoveryState.FOUND, manager.getDiscoveryState().getValue());
    }

    @Test
    public void testReceiveInvalidPacket_doesNotStore() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        byte[] invalidPacket = new byte[]{0x01, 0x00, 0x00}; // Wrong opcode and too short
        configureMockSocketToReceiveOnce(invalidPacket);

        // When
        manager.startDiscovery();
        awaitCondition(manager::isRunning, 2000, "Manager should start running");
        manager.stopDiscovery();
        awaitCondition(() -> !manager.isRunning(), 2000, "Manager should stop running");

        // Then - should not have stored anything
        assertNull(manager.getDiscoveredServer());
    }

    @Test
    public void testReceiveInvalidPacket_logsWarningOnParseFailure() throws Exception {
        // Given - packet with correct length (9 bytes) but wrong opcode
        // This triggers IllegalArgumentException in DiscoveryMessageParser.parse()
        // which is caught and logged as a warning in processPacket()
        manager = createManagerWithMockSocket();
        AtomicInteger receiveCallCount = new AtomicInteger(0);
        byte[] invalidOpcodePacket = new byte[]{
                0x01, // Wrong opcode (should be 0x00)
                (byte) 192, (byte) 168, 1, 100, // IP address
                0x1F, (byte) 0x90, // HTTP port (8080 little-endian)
                (byte) 0x82, 0x23  // TCP port (9090 little-endian)
        };
        
        doAnswer(invocation -> {
            int count = receiveCallCount.incrementAndGet();
            if (count == 1) {
                // First call: return invalid packet
                DatagramPacket packet = invocation.getArgument(0);
                System.arraycopy(invalidOpcodePacket, 0, packet.getData(), 0, invalidOpcodePacket.length);
                packet.setLength(invalidOpcodePacket.length);
                return null;
            }
            // Subsequent calls: timeout
            throw new SocketTimeoutException();
        }).when(mockSocket).receive(any(DatagramPacket.class));

        // When
        manager.startDiscovery();
        // Wait for the invalid packet to be received and processed (receiveCallCount >= 2 means
        // first packet was processed and second call hit timeout, ensuring processPacket completed)
        awaitCondition(() -> receiveCallCount.get() >= 2, 2000, "Packet should be received and processed");
        manager.stopDiscovery();
        awaitCondition(() -> !manager.isRunning(), 2000, "Manager should stop running");

        // Then - should not have stored anything due to parse failure
        // The IllegalArgumentException catch block logs the warning
        assertNull(manager.getDiscoveredServer());
    }

    @Test
    public void testSocketException_handledGracefully() throws Exception {
        // Given
        manager = new UdpDiscoveryManager(mockContext, mockLockManager) {
            @Override
            DatagramSocket createSocket() throws SocketException {
                throw new SocketException("Cannot bind to port");
            }
        };

        // When/Then - should not throw, just log error
        manager.startDiscovery();
        awaitCondition(() -> !manager.isRunning(), 2000, "Manager should stop after socket error");
        
        // Should handle gracefully
        assertFalse(manager.isRunning());
    }

    @Test
    public void testIsRunning_reflectsState() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        configureMockSocketToTimeout();

        // Then - initially not running
        assertFalse(manager.isRunning());

        // When - start
        manager.startDiscovery();
        awaitCondition(manager::isRunning, 2000, "Manager should start running");

        // Then - running
        assertTrue(manager.isRunning());

        // When - stop
        manager.stopDiscovery();
        awaitCondition(() -> !manager.isRunning(), 2000, "Manager should stop running");

        // Then - not running
        assertFalse(manager.isRunning());
    }

    @Test
    public void testSocketReceiveIOException_continuesListening() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        AtomicInteger receiveCount = new AtomicInteger(0);
        
        doAnswer(invocation -> {
            int count = receiveCount.incrementAndGet();
            if (count == 1) {
                throw new IOException("Network error");
            } else if (count == 2) {
                // Return valid packet on second call
                DatagramPacket packet = invocation.getArgument(0);
                byte[] data = createValidDiscoveryMessage(
                        new byte[]{(byte) 192, (byte) 168, 1, 1}, 8080, 9090);
                System.arraycopy(data, 0, packet.getData(), 0, data.length);
                packet.setLength(data.length);
                return null;
            } else {
                throw new SocketTimeoutException();
            }
        }).when(mockSocket).receive(any(DatagramPacket.class));

        // When
        manager.startDiscovery();
        awaitCondition(() -> manager.getDiscoveredServer() != null, 2000, "Server should be discovered after recovery");
        manager.stopDiscovery();
        awaitCondition(() -> !manager.isRunning(), 2000, "Manager should stop running");

        // Then - should have recovered and stored message
        assertNotNull(manager.getDiscoveredServer());
    }

    /**
     * Creates a UdpDiscoveryManager that uses a mock socket.
     *
     * @return UdpDiscoveryManager with mock socket
     */
    private UdpDiscoveryManager createManagerWithMockSocket() {
        return new UdpDiscoveryManager(mockContext, mockLockManager) {
            @Override
            DatagramSocket createSocket() {
                return mockSocket;
            }
        };
    }

    /**
     * Configures the mock socket to throw SocketTimeoutException on receive.
     *
     * @throws IOException if configuration fails
     */
    private void configureMockSocketToTimeout() throws IOException {
        doThrow(new SocketTimeoutException()).when(mockSocket).receive(any(DatagramPacket.class));
    }

    /**
     * Configures the mock socket to receive one packet, then timeout.
     *
     * @param data The packet data to receive
     * @throws IOException if configuration fails
     */
    private void configureMockSocketToReceiveOnce(byte[] data) throws IOException {
        AtomicInteger callCount = new AtomicInteger(0);
        doAnswer(invocation -> {
            if (callCount.getAndIncrement() == 0) {
                DatagramPacket packet = invocation.getArgument(0);
                System.arraycopy(data, 0, packet.getData(), 0, data.length);
                packet.setLength(data.length);
                return null;
            }
            throw new SocketTimeoutException();
        }).when(mockSocket).receive(any(DatagramPacket.class));
    }

    /**
     * Configures the mock socket to receive multiple packets, then timeout.
     *
     * @param packets The packet data arrays to receive in order
     * @throws IOException if configuration fails
     */
    private void configureMockSocketToReceiveMultiple(byte[]... packets) throws IOException {
        AtomicInteger callCount = new AtomicInteger(0);
        doAnswer(invocation -> {
            int count = callCount.getAndIncrement();
            if (count < packets.length) {
                DatagramPacket packet = invocation.getArgument(0);
                byte[] data = packets[count];
                System.arraycopy(data, 0, packet.getData(), 0, data.length);
                packet.setLength(data.length);
                return null;
            }
            throw new SocketTimeoutException();
        }).when(mockSocket).receive(any(DatagramPacket.class));
    }

    /**
     * Creates a valid discovery message byte array.
     *
     * @param ipBytes The IP address as 4 bytes
     * @param httpPort The HTTP port
     * @param tcpPort The TCP port
     * @return The discovery message as byte array
     */
    private byte[] createValidDiscoveryMessage(byte[] ipBytes, int httpPort, int tcpPort) {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.put((byte) 0x00); // Opcode
        buffer.put(ipBytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) httpPort);
        buffer.putShort((short) tcpPort);
        return buffer.array();
    }

    @Test
    public void testCreateSocket_returnsDatagramSocket() throws Exception {
        // Given
        manager = new UdpDiscoveryManager(mockContext, mockLockManager);

        // When
        DatagramSocket socket = manager.createSocket();

        // Then
        assertNotNull(socket);
        assertEquals(UdpDiscoveryManager.UDP_PORT, socket.getLocalPort());

        // Cleanup
        socket.close();
    }

    /**
     * Polls a condition until it becomes true or timeout is reached.
     * This replaces flaky Thread.sleep() calls with deterministic waiting.
     *
     * @param condition The condition to wait for
     * @param timeoutMs Maximum time to wait in milliseconds
     * @param message Error message if timeout is reached
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    private void awaitCondition(Supplier<Boolean> condition, long timeoutMs, String message)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!condition.get()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Timeout: " + message);
            }
            Thread.sleep(10);  // Small polling interval
        }
    }

    // ========== State Transition Tests ==========

    @Test
    public void testGetDiscoveryState_initiallyIdle() {
        // Given
        manager = new UdpDiscoveryManager(mockContext, mockLockManager);

        // Then
        assertEquals(DiscoveryState.IDLE, manager.getDiscoveryState().getValue());
    }

    @Test
    public void testStartDiscovery_transitionsToSearchingState() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        configureMockSocketToTimeout();

        // When
        manager.startDiscovery();
        awaitCondition(() -> manager.getDiscoveryState().getValue() == DiscoveryState.SEARCHING,
                2000, "State should transition to SEARCHING");

        // Then
        assertEquals(DiscoveryState.SEARCHING, manager.getDiscoveryState().getValue());

        // Cleanup
        manager.stopDiscovery();
    }

    @Test
    public void testReceiveValidPacket_transitionsToFoundState() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        byte[] validPacketData = createValidDiscoveryMessage(
                new byte[]{(byte) 192, (byte) 168, 1, 100}, 8080, 9090);
        configureMockSocketToReceiveOnce(validPacketData);

        // When
        manager.startDiscovery();
        awaitCondition(() -> manager.getDiscoveryState().getValue() == DiscoveryState.FOUND,
                2000, "State should transition to FOUND");

        // Then
        assertEquals(DiscoveryState.FOUND, manager.getDiscoveryState().getValue());

        // Cleanup
        manager.stopDiscovery();
    }

    @Test
    public void testStopDiscovery_transitionsToIdleState() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        configureMockSocketToTimeout();
        manager.startDiscovery();
        awaitCondition(() -> manager.getDiscoveryState().getValue() == DiscoveryState.SEARCHING,
                2000, "State should transition to SEARCHING");

        // When
        manager.stopDiscovery();
        awaitCondition(() -> manager.getDiscoveryState().getValue() == DiscoveryState.IDLE,
                2000, "State should transition to IDLE");

        // Then
        assertEquals(DiscoveryState.IDLE, manager.getDiscoveryState().getValue());
    }

    @Test
    public void testSocketException_transitionsToErrorState() throws Exception {
        // Given
        manager = new UdpDiscoveryManager(mockContext, mockLockManager) {
            @Override
            DatagramSocket createSocket() throws SocketException {
                throw new SocketException("Cannot bind to port");
            }
        };

        // When
        manager.startDiscovery();
        awaitCondition(() -> manager.getDiscoveryState().getValue() == DiscoveryState.ERROR,
                2000, "State should transition to ERROR");

        // Then
        assertEquals(DiscoveryState.ERROR, manager.getDiscoveryState().getValue());
    }

    @Test
    public void testSocketException_setsLastError() throws Exception {
        // Given
        manager = new UdpDiscoveryManager(mockContext, mockLockManager) {
            @Override
            DatagramSocket createSocket() throws SocketException {
                throw new SocketException("Cannot bind to port");
            }
        };

        // When
        manager.startDiscovery();
        awaitCondition(() -> manager.getLastError() != null, 2000, "Error should be set");

        // Then
        assertEquals("Cannot bind to port", manager.getLastError());
    }

    @Test
    public void testGetLastError_initiallyNull() {
        // Given
        manager = new UdpDiscoveryManager(mockContext, mockLockManager);

        // Then
        assertNull(manager.getLastError());
    }

    // ========== Timeout Tests ==========

    @Test
    public void testTimeout_transitionsToTimeoutState() throws Exception {
        // Given - very short timeout for testing
        manager = createManagerWithMockSocket();
        manager.setTimeoutMs(100); // 100ms timeout
        configureMockSocketToTimeout();

        // When
        manager.startDiscovery();
        awaitCondition(() -> manager.getDiscoveryState().getValue() == DiscoveryState.TIMEOUT,
                2000, "State should transition to TIMEOUT");

        // Then
        assertEquals(DiscoveryState.TIMEOUT, manager.getDiscoveryState().getValue());
        assertFalse(manager.isRunning());
    }

    @Test
    public void testTimeout_cancelledOnDiscovery() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        manager.setTimeoutMs(5000); // Long timeout
        byte[] validPacketData = createValidDiscoveryMessage(
                new byte[]{(byte) 192, (byte) 168, 1, 100}, 8080, 9090);
        configureMockSocketToReceiveOnce(validPacketData);

        // When - server discovered before timeout
        manager.startDiscovery();
        awaitCondition(() -> manager.getDiscoveryState().getValue() == DiscoveryState.FOUND,
                2000, "State should transition to FOUND");

        // Then - should be FOUND, not TIMEOUT
        assertEquals(DiscoveryState.FOUND, manager.getDiscoveryState().getValue());

        // Cleanup
        manager.stopDiscovery();
    }

    @Test
    public void testTimeout_cancelledOnStop() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        manager.setTimeoutMs(10000); // Long timeout
        configureMockSocketToTimeout();
        manager.startDiscovery();
        awaitCondition(() -> manager.getDiscoveryState().getValue() == DiscoveryState.SEARCHING,
                2000, "State should transition to SEARCHING");

        // When - stop before timeout
        manager.stopDiscovery();
        awaitCondition(() -> manager.getDiscoveryState().getValue() == DiscoveryState.IDLE,
                2000, "State should transition to IDLE");

        // Then - should be IDLE, not TIMEOUT
        assertEquals(DiscoveryState.IDLE, manager.getDiscoveryState().getValue());
    }

    @Test
    public void testGetTimeoutMs_returnsDefaultValue() {
        // Given
        manager = new UdpDiscoveryManager(mockContext, mockLockManager);

        // Then
        assertEquals(UdpDiscoveryManager.DEFAULT_TIMEOUT_MS, manager.getTimeoutMs());
    }

    @Test
    public void testSetTimeoutMs_updatesTimeoutValue() {
        // Given
        manager = new UdpDiscoveryManager(mockContext, mockLockManager);

        // When
        manager.setTimeoutMs(5000);

        // Then
        assertEquals(5000, manager.getTimeoutMs());
    }

    // ========== Listener Tests ==========

    @Test
    public void testAddListener_receivesDiscoveryCallback() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        byte[] validPacketData = createValidDiscoveryMessage(
                new byte[]{(byte) 192, (byte) 168, 1, 100}, 8080, 9090);
        configureMockSocketToReceiveOnce(validPacketData);

        AtomicBoolean callbackReceived = new AtomicBoolean(false);
        OnServerDiscoveredListener listener = message -> {
            assertEquals("192.168.1.100", message.getIpAddress());
            callbackReceived.set(true);
        };
        manager.addListener(listener);

        // When
        manager.startDiscovery();
        awaitCondition(callbackReceived::get, 2000, "Listener should receive callback");

        // Then
        assertTrue(callbackReceived.get());

        // Cleanup
        manager.stopDiscovery();
    }

    @Test
    public void testRemoveListener_noLongerReceivesCallback() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        byte[] validPacketData = createValidDiscoveryMessage(
                new byte[]{(byte) 192, (byte) 168, 1, 100}, 8080, 9090);
        configureMockSocketToReceiveOnce(validPacketData);

        AtomicBoolean callbackReceived = new AtomicBoolean(false);
        OnServerDiscoveredListener listener = message -> callbackReceived.set(true);
        manager.addListener(listener);
        manager.removeListener(listener);

        // When
        manager.startDiscovery();
        awaitCondition(() -> manager.getDiscoveredServer() != null, 2000, "Server should be discovered");
        manager.stopDiscovery();
        awaitCondition(() -> !manager.isRunning(), 2000, "Manager should stop running");

        // Then - callback should NOT have been received
        assertFalse(callbackReceived.get());
    }

    @Test
    public void testAddListener_duplicate_notAddedTwice() {
        // Given
        manager = new UdpDiscoveryManager(mockContext, mockLockManager);
        OnServerDiscoveredListener listener = message -> { };

        // When
        manager.addListener(listener);
        manager.addListener(listener);

        // Then
        assertEquals(1, manager.getListenerCount());
    }

    @Test
    public void testGetListenerCount_initiallyZero() {
        // Given
        manager = new UdpDiscoveryManager(mockContext, mockLockManager);

        // Then
        assertEquals(0, manager.getListenerCount());
    }

    @Test
    public void testAddListener_incrementsCount() {
        // Given
        manager = new UdpDiscoveryManager(mockContext, mockLockManager);
        OnServerDiscoveredListener listener = message -> { };

        // When
        manager.addListener(listener);

        // Then
        assertEquals(1, manager.getListenerCount());
    }

    @Test
    public void testRemoveListener_decrementsCount() {
        // Given
        manager = new UdpDiscoveryManager(mockContext, mockLockManager);
        OnServerDiscoveredListener listener = message -> { };
        manager.addListener(listener);
        assertEquals(1, manager.getListenerCount());

        // When
        manager.removeListener(listener);

        // Then
        assertEquals(0, manager.getListenerCount());
    }

    @Test
    public void testListenerException_doesNotAffectOtherListeners() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        byte[] validPacketData = createValidDiscoveryMessage(
                new byte[]{(byte) 192, (byte) 168, 1, 100}, 8080, 9090);
        configureMockSocketToReceiveOnce(validPacketData);

        AtomicBoolean secondListenerCalled = new AtomicBoolean(false);

        // First listener throws exception
        OnServerDiscoveredListener throwingListener = message -> {
            throw new RuntimeException("Test exception");
        };
        // Second listener should still be called
        OnServerDiscoveredListener normalListener = message -> secondListenerCalled.set(true);

        manager.addListener(throwingListener);
        manager.addListener(normalListener);

        // When
        manager.startDiscovery();
        awaitCondition(secondListenerCalled::get, 2000, "Second listener should be called");

        // Then
        assertTrue(secondListenerCalled.get());

        // Cleanup
        manager.stopDiscovery();
    }

    // ========== Default Timeout Constant Test ==========

    @Test
    public void testDefaultTimeoutMs_isCorrectValue() {
        // Then
        assertEquals(15000, UdpDiscoveryManager.DEFAULT_TIMEOUT_MS);
    }

    // ========== Multicast Lock Tests ==========

    @Test
    public void testStartDiscovery_acquiresMulticastLock() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        configureMockSocketToTimeout();

        // When
        manager.startDiscovery();
        awaitCondition(manager::isRunning, 2000, "Manager should start running");

        // Then
        verify(mockLockManager).acquire(mockContext);

        // Cleanup
        manager.stopDiscovery();
    }

    @Test
    public void testStopDiscovery_releasesMulticastLock() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        configureMockSocketToTimeout();
        manager.startDiscovery();
        awaitCondition(manager::isRunning, 2000, "Manager should start running");

        // When
        manager.stopDiscovery();
        awaitCondition(() -> !manager.isRunning(), 2000, "Manager should stop running");

        // Then
        verify(mockLockManager).release();
    }

    @Test
    public void testTimeout_releasesMulticastLock() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        manager.setTimeoutMs(100); // Short timeout
        configureMockSocketToTimeout();

        // When
        manager.startDiscovery();
        awaitCondition(() -> manager.getDiscoveryState().getValue() == DiscoveryState.TIMEOUT,
                2000, "State should transition to TIMEOUT");

        // Then
        verify(mockLockManager).release();
    }

    @Test
    public void testSocketException_releasesMulticastLock() throws Exception {
        // Given
        manager = new UdpDiscoveryManager(mockContext, mockLockManager) {
            @Override
            DatagramSocket createSocket() throws SocketException {
                throw new SocketException("Cannot bind to port");
            }
        };

        // When
        manager.startDiscovery();
        awaitCondition(() -> manager.getDiscoveryState().getValue() == DiscoveryState.ERROR,
                2000, "State should transition to ERROR");

        // Then
        verify(mockLockManager).release();
    }

}
