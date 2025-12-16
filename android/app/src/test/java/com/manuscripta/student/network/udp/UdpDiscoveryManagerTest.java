package com.manuscripta.student.network;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for {@link UdpDiscoveryManager}.
 */
public class UdpDiscoveryManagerTest {

    private UdpDiscoveryManager manager;
    private DatagramSocket mockSocket;

    @Before
    public void setUp() throws Exception {
        mockSocket = mock(DatagramSocket.class);
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
        manager = new UdpDiscoveryManager();

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
        Thread.sleep(50); // Allow thread to start

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
        Thread.sleep(50);
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
        Thread.sleep(50);
        assertTrue(manager.isRunning());

        // When
        manager.stopDiscovery();
        Thread.sleep(50);

        // Then
        assertFalse(manager.isRunning());
    }

    @Test
    public void testStopDiscovery_whenNotRunning_noOp() {
        // Given
        manager = new UdpDiscoveryManager();
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
        Thread.sleep(50);

        // When
        manager.stopDiscovery();
        Thread.sleep(100);

        // Then - verify close was called at least once
        verify(mockSocket, atLeastOnce()).close();
    }

    @Test
    public void testGetDiscoveredServer_initiallyNull() {
        // Given
        manager = new UdpDiscoveryManager();

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
        Thread.sleep(100);
        manager.stopDiscovery();
        Thread.sleep(50);
        
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
        Thread.sleep(100);
        manager.stopDiscovery();
        Thread.sleep(50);

        // Then
        DiscoveryMessage message = manager.getDiscoveredServer();
        assertNotNull(message);
        assertEquals("192.168.1.100", message.getIpAddress());
        assertEquals(8080, message.getHttpPort());
        assertEquals(9090, message.getTcpPort());
    }

    @Test
    public void testReceiveMultiplePackets_storesLatest() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        byte[] firstPacket = createValidDiscoveryMessage(
                new byte[]{(byte) 192, (byte) 168, 1, 1}, 8080, 9090);
        byte[] secondPacket = createValidDiscoveryMessage(
                new byte[]{10, 0, 0, 1}, 443, 8443);
        configureMockSocketToReceiveMultiple(firstPacket, secondPacket);

        // When
        manager.startDiscovery();
        Thread.sleep(150);
        manager.stopDiscovery();
        Thread.sleep(50);

        // Then - should have the second (latest) packet
        DiscoveryMessage message = manager.getDiscoveredServer();
        assertNotNull(message);
        assertEquals("10.0.0.1", message.getIpAddress());
        assertEquals(443, message.getHttpPort());
        assertEquals(8443, message.getTcpPort());
    }

    @Test
    public void testReceiveInvalidPacket_doesNotStore() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        byte[] invalidPacket = new byte[]{0x01, 0x00, 0x00}; // Wrong opcode and too short
        configureMockSocketToReceiveOnce(invalidPacket);

        // When
        manager.startDiscovery();
        Thread.sleep(100);
        manager.stopDiscovery();
        Thread.sleep(50);

        // Then - should not have stored anything
        assertNull(manager.getDiscoveredServer());
    }

    @Test
    public void testSocketException_handledGracefully() throws Exception {
        // Given
        manager = new UdpDiscoveryManager() {
            @Override
            DatagramSocket createSocket() throws SocketException {
                throw new SocketException("Cannot bind to port");
            }
        };

        // When/Then - should not throw, just log error
        manager.startDiscovery();
        Thread.sleep(100);
        
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
        Thread.sleep(50);

        // Then - running
        assertTrue(manager.isRunning());

        // When - stop
        manager.stopDiscovery();
        Thread.sleep(50);

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
        Thread.sleep(200);
        manager.stopDiscovery();
        Thread.sleep(50);

        // Then - should have recovered and stored message
        assertNotNull(manager.getDiscoveredServer());
    }

    @Test
    public void testSocketReceiveIOException_whenNotRunning_doesNotLog() throws Exception {
        manager = createManagerWithMockSocket();
        
        doAnswer(invocation -> {
            // Simulate shutdown happening elsewhere
            java.lang.reflect.Field runningField = UdpDiscoveryManager.class.getDeclaredField("running");
            runningField.setAccessible(true);
            AtomicBoolean running = (AtomicBoolean) runningField.get(manager);
            running.set(false);
            
            throw new IOException("Socket closed");
        }).when(mockSocket).receive(any(DatagramPacket.class));

        manager.startDiscovery();
        Thread.sleep(100);
        
        assertFalse(manager.isRunning());
    }

    /**
     * Creates a UdpDiscoveryManager that uses a mock socket.
     *
     * @return UdpDiscoveryManager with mock socket
     */
    private UdpDiscoveryManager createManagerWithMockSocket() {
        return new UdpDiscoveryManager() {
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
        manager = new UdpDiscoveryManager();

        // When
        DatagramSocket socket = manager.createSocket();

        // Then
        assertNotNull(socket);
        assertEquals(UdpDiscoveryManager.UDP_PORT, socket.getLocalPort());

        // Cleanup
        socket.close();
    }

    @Test
    public void testStopDiscovery_whenNeverStarted_socketAndExecutorNull() {
        // Given - fresh manager never started (socket and executorService are null)
        manager = new UdpDiscoveryManager();

        // Manually set running to true to force stopDiscovery to execute cleanup paths
        // This tests the null branches in closeSocket() and shutdownExecutor()
        try {
            java.lang.reflect.Field runningField = UdpDiscoveryManager.class.getDeclaredField("running");
            runningField.setAccessible(true);
            AtomicBoolean running = (AtomicBoolean) runningField.get(manager);
            running.set(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // When - stop discovery with null socket and null executorService
        manager.stopDiscovery();

        // Then - should handle gracefully without NullPointerException
        assertFalse(manager.isRunning());
    }

    @Test
    public void testStopDiscovery_whenSocketAlreadyClosed_handlesGracefully() throws Exception {
        // Given
        manager = createManagerWithMockSocket();
        when(mockSocket.isClosed()).thenReturn(true); // Socket already closed
        configureMockSocketToTimeout();
        
        // Manually set state as if discovery was running
        try {
            java.lang.reflect.Field runningField = UdpDiscoveryManager.class.getDeclaredField("running");
            runningField.setAccessible(true);
            AtomicBoolean running = (AtomicBoolean) runningField.get(manager);
            running.set(true);
            
            java.lang.reflect.Field socketField = UdpDiscoveryManager.class.getDeclaredField("socket");
            socketField.setAccessible(true);
            socketField.set(manager, mockSocket);
            
            java.lang.reflect.Field executorField =
                    UdpDiscoveryManager.class.getDeclaredField("executorService");
            executorField.setAccessible(true);
            ExecutorService mockExecutor = mock(ExecutorService.class);
            when(mockExecutor.isShutdown()).thenReturn(true); // Already shutdown
            executorField.set(manager, mockExecutor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // When
        manager.stopDiscovery();

        // Then - should not call close() on already-closed socket
        verify(mockSocket, org.mockito.Mockito.never()).close();
        assertFalse(manager.isRunning());
    }
}
