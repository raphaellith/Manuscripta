package com.manuscripta.student.network;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages UDP discovery for locating teacher servers on the local network.
 * 
 * <p>Listens for UDP broadcast messages on port 5913 and parses incoming
 * discovery announcements from the teacher server. The manager maintains
 * the most recently discovered server information for retrieval.</p>
 *
 * <p>Per API Contract §1.1, the discovery message format is:</p>
 * <pre>
 * | Field      | Offset | Size    | Description                              |
 * |------------|--------|---------|------------------------------------------|
 * | Opcode     | 0      | 1 byte  | 0x00 = DISCOVERY                         |
 * | IP Address | 1      | 4 bytes | IPv4 address (network byte order)        |
 * | HTTP Port  | 5      | 2 bytes | Unsigned, little-endian                  |
 * | TCP Port   | 7      | 2 bytes | Unsigned, little-endian                  |
 * </pre>
 *
 * @author William Stephen
 */
@Singleton
public class UdpDiscoveryManager {

    /**
     * Tag for logging.
     */
    private static final String TAG = "UdpDiscoveryManager";

    /**
     * UDP broadcast port for discovery (per API Contract §1.1).
     */
    public static final int UDP_PORT = 5913;

    /**
     * Buffer size for receiving UDP packets.
     * 16 bytes provides margin over the 9-byte message size.
     */
    private static final int BUFFER_SIZE = 16;

    /**
     * Socket timeout in milliseconds.
     * Allows periodic checking of the running flag.
     */
    private static final int SOCKET_TIMEOUT_MS = 1000;

    /**
     * The most recently discovered server information.
     */
    private final AtomicReference<DiscoveryMessage> discoveredServer = new AtomicReference<>(null);

    /**
     * Flag indicating whether discovery is currently running.
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Executor service for background UDP listening.
     */
    @Nullable
    private ExecutorService executorService;

    /**
     * The UDP socket used for receiving discovery broadcasts.
     */
    @Nullable
    private DatagramSocket socket;

    /**
     * Constructs a new UdpDiscoveryManager.
     */
    @Inject
    public UdpDiscoveryManager() {
        // Default constructor for Hilt injection
    }

    /**
     * Starts listening for UDP discovery broadcasts.
     * 
     * <p>If discovery is already running, this method does nothing (no-op).
     * The listener runs on a background thread using an ExecutorService.</p>
     */
    public void startDiscovery() {
        if (running.compareAndSet(false, true)) {
            Log.d(TAG, "Starting UDP discovery on port " + UDP_PORT);
            executorService = Executors.newSingleThreadExecutor();
            executorService.submit(this::listenForDiscovery);
        } else {
            Log.d(TAG, "Discovery already running, ignoring start request");
        }
    }

    /**
     * Stops listening for UDP discovery broadcasts.
     * 
     * <p>Closes the socket and shuts down the executor service.
     * If discovery is not running, this method does nothing.</p>
     */
    public void stopDiscovery() {
        if (running.compareAndSet(true, false)) {
            Log.d(TAG, "Stopping UDP discovery");
            closeSocket();
            shutdownExecutor();
        } else {
            Log.d(TAG, "Discovery not running, ignoring stop request");
        }
    }

    /**
     * Returns the most recently discovered server information.
     *
     * @return The discovered {@link DiscoveryMessage}, or null if no server
     *         has been discovered yet
     */
    @Nullable
    public DiscoveryMessage getDiscoveredServer() {
        return discoveredServer.get();
    }

    /**
     * Checks whether discovery is currently running.
     *
     * @return true if discovery is active, false otherwise
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Clears the currently stored discovered server information.
     */
    public void clearDiscoveredServer() {
        discoveredServer.set(null);
        Log.d(TAG, "Cleared discovered server");
    }

    /**
     * Main listening loop for UDP discovery messages.
     * Runs on a background thread.
     */
    private void listenForDiscovery() {
        try {
            socket = createSocket();
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            Log.d(TAG, "UDP socket bound to port " + UDP_PORT);

            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (running.get()) {
                try {
                    socket.receive(packet);
                    processPacket(packet);
                } catch (SocketTimeoutException e) {
                    // Expected - allows checking running flag periodically
                    continue;
                } catch (IOException e) {
                    if (running.get()) {
                        Log.e(TAG, "Error receiving UDP packet", e);
                    }
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "Failed to create UDP socket on port " + UDP_PORT, e);
            running.set(false);
        } finally {
            closeSocket();
            Log.d(TAG, "UDP discovery listener terminated");
        }
    }

    /**
     * Processes a received UDP packet.
     *
     * @param packet The received datagram packet
     */
    private void processPacket(@NonNull DatagramPacket packet) {
        try {
            byte[] data = extractPacketData(packet);
            DiscoveryMessage message = DiscoveryMessageParser.parse(data);
            discoveredServer.set(message);
            Log.d(TAG, "Discovered server: " + message.getIpAddress()
                    + " HTTP:" + message.getHttpPort()
                    + " TCP:" + message.getTcpPort());
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Failed to parse discovery message: " + e.getMessage());
        }
    }

    /**
     * Extracts the actual data from a datagram packet.
     *
     * @param packet The datagram packet to extract data from
     * @return A byte array containing only the received data
     */
    @NonNull
    private byte[] extractPacketData(@NonNull DatagramPacket packet) {
        byte[] data = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
        return data;
    }

    /**
     * Creates a new DatagramSocket bound to the UDP port.
     * 
     * <p>This method is package-private to allow overriding in tests.</p>
     *
     * @return A new DatagramSocket
     * @throws SocketException if the socket cannot be created
     */
    DatagramSocket createSocket() throws SocketException {
        return new DatagramSocket(UDP_PORT);
    }

    /**
     * Closes the UDP socket if it is open.
     */
    private void closeSocket() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
            socket = null;
            Log.d(TAG, "UDP socket closed");
        }
    }

    /**
     * Shuts down the executor service.
     */
    private void shutdownExecutor() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            executorService = null;
            Log.d(TAG, "Executor service shut down");
        }
    }
}
