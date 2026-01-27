package com.manuscripta.student.network.udp;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.manuscripta.student.utils.MulticastLockManager;

import dagger.hilt.android.qualifiers.ApplicationContext;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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
 * <p>This class is thread-safe and exposes discovery state via LiveData for UI observation.</p>
 *
 * <p>Per API Contract §3.3, the discovery message format is:</p>
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
     * Default discovery timeout in milliseconds.
     */
    public static final long DEFAULT_TIMEOUT_MS = 15000;

    /**
     * The most recently discovered server information.
     */
    private final AtomicReference<DiscoveryMessage> discoveredServer = new AtomicReference<>(null);

    /**
     * Flag indicating whether discovery is currently running.
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * LiveData for discovery state observation.
     */
    private final MutableLiveData<DiscoveryState> discoveryState;

    /**
     * The last error message, if any.
     */
    private final AtomicReference<String> lastError = new AtomicReference<>(null);

    /**
     * Thread-safe list of discovery listeners.
     */
    private final List<OnServerDiscoveredListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Executor service for background UDP listening.
     * Marked volatile to ensure visibility across threads.
     */
    @Nullable
    private volatile ExecutorService executorService;

    /**
     * Scheduled executor for timeout handling.
     */
    @Nullable
    private volatile ScheduledExecutorService scheduledExecutor;

    /**
     * Future for the timeout task.
     */
    @Nullable
    private volatile ScheduledFuture<?> timeoutFuture;

    /**
     * Configurable timeout in milliseconds.
     */
    private volatile long timeoutMs = DEFAULT_TIMEOUT_MS;

    /**
     * Application context for acquiring multicast lock.
     */
    @NonNull
    private final Context applicationContext;

    /**
     * Manager for multicast lock acquisition and release.
     */
    @NonNull
    private final MulticastLockManager multicastLockManager;

    /**
     * Constructs a new UdpDiscoveryManager.
     *
     * @param applicationContext The application context for multicast lock.
     * @param multicastLockManager The multicast lock manager.
     */
    @Inject
    public UdpDiscoveryManager(
            @ApplicationContext @NonNull Context applicationContext,
            @NonNull MulticastLockManager multicastLockManager) {
        this.applicationContext = applicationContext;
        this.multicastLockManager = multicastLockManager;
        this.discoveryState = new MutableLiveData<>(DiscoveryState.IDLE);
    }

    /**
     * Returns the current discovery state as LiveData for UI observation.
     *
     * @return LiveData containing the current discovery state.
     */
    @NonNull
    public LiveData<DiscoveryState> getDiscoveryState() {
        return discoveryState;
    }

    /**
     * Returns the last error message, if any.
     *
     * @return The last error message, or null if no error occurred.
     */
    @Nullable
    public String getLastError() {
        return lastError.get();
    }

    /**
     * Adds a listener to receive server discovery events.
     *
     * <p>The same listener instance can only be added once.</p>
     *
     * @param listener The listener to add. Must not be null.
     * @see #removeListener(OnServerDiscoveredListener)
     */
    public void addListener(@NonNull OnServerDiscoveredListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a previously registered listener.
     *
     * @param listener The listener to remove. Must not be null.
     * @see #addListener(OnServerDiscoveredListener)
     */
    public void removeListener(@NonNull OnServerDiscoveredListener listener) {
        listeners.remove(listener);
    }

    /**
     * Sets the discovery timeout in milliseconds.
     *
     * <p>Must be called before startDiscovery() to take effect.</p>
     *
     * @param timeoutMs The timeout in milliseconds.
     */
    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    /**
     * Returns the configured timeout in milliseconds.
     *
     * @return The timeout in milliseconds.
     */
    @VisibleForTesting
    long getTimeoutMs() {
        return timeoutMs;
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
            lastError.set(null);
            updateState(DiscoveryState.SEARCHING);
            
            // Acquire multicast lock to receive broadcast packets
            if (!multicastLockManager.acquire(applicationContext)) {
                Log.w(TAG, "Failed to acquire multicast lock, discovery may not work");
            }
            
            // Schedule timeout before starting listener to avoid race condition
            // where listener fails fast and tries to cancel a not-yet-scheduled timeout
            scheduleTimeout();
            
            executorService = Executors.newSingleThreadExecutor();
            executorService.submit(this::listenForDiscovery);
            
            scheduleTimeout();
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
            cancelTimeout();
            cancelTimeout();
            shutdownExecutor();
            multicastLockManager.release();
            updateState(DiscoveryState.IDLE);
            multicastLockManager.release();
            updateState(DiscoveryState.IDLE);
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
     * Returns the number of registered listeners for testing purposes.
     *
     * @return The number of registered listeners.
     */
    @VisibleForTesting
    int getListenerCount() {
        return listeners.size();
    }

    /**
     * Schedules the timeout task.
     */
    private void scheduleTimeout() {
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        timeoutFuture = scheduledExecutor.schedule(
                this::handleTimeout,
                timeoutMs,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Cancels the timeout task and shuts down the scheduled executor.
     */
    private void cancelTimeout() {
        ScheduledFuture<?> future = timeoutFuture;
        if (future != null) {
            future.cancel(false);
            timeoutFuture = null;
        }
        
        ScheduledExecutorService executor = scheduledExecutor;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            scheduledExecutor = null;
        }
    }

    /**
     * Handles the timeout event.
     * 
     * <p>Only transitions to TIMEOUT state if currently SEARCHING. This prevents
     * a race condition where the timeout could override FOUND state if both events
     * occur near-simultaneously.</p>
     */
    private void handleTimeout() {
        if (running.compareAndSet(true, false)) {
            // Only set TIMEOUT if we're still SEARCHING - avoid overriding FOUND state
            DiscoveryState currentState = discoveryState.getValue();
            if (currentState == DiscoveryState.SEARCHING) {
                Log.d(TAG, "Discovery timeout after " + timeoutMs + "ms");
                updateState(DiscoveryState.TIMEOUT);
            }
            shutdownExecutor();
            multicastLockManager.release();
        }
    }

    /**
     * Returns the number of registered listeners for testing purposes.
     *
     * @return The number of registered listeners.
     */
    @VisibleForTesting
    int getListenerCount() {
        return listeners.size();
    }

    /**
     * Schedules the timeout task.
     */
    private void scheduleTimeout() {
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        timeoutFuture = scheduledExecutor.schedule(
                this::handleTimeout,
                timeoutMs,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Cancels the timeout task and shuts down the scheduled executor.
     */
    private void cancelTimeout() {
        ScheduledFuture<?> future = timeoutFuture;
        if (future != null) {
            future.cancel(false);
            timeoutFuture = null;
        }
        
        ScheduledExecutorService executor = scheduledExecutor;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            scheduledExecutor = null;
        }
    }

    /**
     * Handles the timeout event.
     * 
     * <p>Only transitions to TIMEOUT state if currently SEARCHING. This prevents
     * a race condition where the timeout could override FOUND state if both events
     * occur near-simultaneously.</p>
     */
    private void handleTimeout() {
        if (running.compareAndSet(true, false)) {
            // Only set TIMEOUT if we're still SEARCHING - avoid overriding FOUND state
            DiscoveryState currentState = discoveryState.getValue();
            if (currentState == DiscoveryState.SEARCHING) {
                Log.d(TAG, "Discovery timeout after " + timeoutMs + "ms");
                updateState(DiscoveryState.TIMEOUT);
            }
            shutdownExecutor();
            multicastLockManager.release();
        }
    }

    /**
     * Main listening loop for UDP discovery messages.
     * Runs on a background thread.
     */
    private void listenForDiscovery() {
        DatagramSocket socket = null;
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
            handleSocketError(e);
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                Log.d(TAG, "UDP socket closed");
            }
            Log.d(TAG, "UDP discovery listener terminated");
        }
    }

    /**
     * Handles socket errors by setting error state.
     *
     * @param e The socket exception that occurred.
     */
    private void handleSocketError(@NonNull SocketException e) {
        running.set(false);
        cancelTimeout();
        multicastLockManager.release();
        lastError.set(e.getMessage());
        updateState(DiscoveryState.ERROR);
    }

    /**
     * Handles socket errors by setting error state.
     *
     * @param e The socket exception that occurred.
     */
    private void handleSocketError(@NonNull SocketException e) {
        running.set(false);
        cancelTimeout();
        multicastLockManager.release();
        lastError.set(e.getMessage());
        updateState(DiscoveryState.ERROR);
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
            
            // Set running to false to prevent timeout from firing after discovery
            running.set(false);
            
            // Cancel timeout and update state
            cancelTimeout();
            updateState(DiscoveryState.FOUND);
            multicastLockManager.release();
            
            // Notify listeners
            notifyListeners(message);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Failed to parse discovery message: " + e.getMessage());
        }
    }

    /**
     * Notifies all registered listeners of a discovered server.
     *
     * @param message The discovery message.
     */
    private void notifyListeners(@NonNull DiscoveryMessage message) {
        for (OnServerDiscoveredListener listener : listeners) {
            try {
                listener.onServerDiscovered(message);
            } catch (Exception e) {
                Log.e(TAG, "Exception in listener onServerDiscovered", e);
            }
        }
    }

    /**
     * Notifies all registered listeners of a discovered server.
     *
     * @param message The discovery message.
     */
    private void notifyListeners(@NonNull DiscoveryMessage message) {
        for (OnServerDiscoveredListener listener : listeners) {
            try {
                listener.onServerDiscovered(message);
            } catch (Exception e) {
                Log.e(TAG, "Exception in listener onServerDiscovered", e);
            }
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
     * Updates the discovery state using thread-safe postValue.
     *
     * @param state The new discovery state.
     */
    private void updateState(@NonNull DiscoveryState state) {
        discoveryState.postValue(state);
    }

    /**
     * Updates the discovery state using thread-safe postValue.
     *
     * @param state The new discovery state.
     */
    private void updateState(@NonNull DiscoveryState state) {
        discoveryState.postValue(state);
    }

    /**
     * Shuts down the executor service and waits for the listening thread to terminate.
     * 
     * <p>Note: The listening thread is blocked on socket.receive() which does not
     * respond to thread interruption. The thread will terminate when either:
     * <ul>
     *   <li>The socket timeout (SOCKET_TIMEOUT_MS) is reached</li>
     *   <li>The socket is closed in the finally block after running flag is checked</li>
     * </ul>
     * This method waits for up to SOCKET_TIMEOUT_MS + 500ms to ensure the socket
     * is fully closed before returning, preventing port binding conflicts on restart.</p>
     */
    private void shutdownExecutor() {
        ExecutorService executor = executorService;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            executorService = null;
            try {
                // Wait for the listening thread to close its socket
                // Timeout is slightly longer than SOCKET_TIMEOUT_MS to allow for cleanup
                if (!executor.awaitTermination(SOCKET_TIMEOUT_MS + 500, TimeUnit.MILLISECONDS)) {
                    Log.w(TAG, "Executor did not terminate within timeout");
                }
            } catch (InterruptedException e) {
                Log.w(TAG, "Interrupted while waiting for executor termination", e);
                Thread.currentThread().interrupt();
            }
            Log.d(TAG, "Executor service shut down");
        }
    }
}
