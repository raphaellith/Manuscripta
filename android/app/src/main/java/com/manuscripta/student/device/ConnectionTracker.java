package com.manuscripta.student.device;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.manuscripta.student.data.repository.DeviceStatusRepository;
import com.manuscripta.student.network.tcp.ConnectionState;
import com.manuscripta.student.network.tcp.TcpMessage;
import com.manuscripta.student.network.tcp.TcpMessageListener;
import com.manuscripta.student.network.tcp.TcpProtocolException;
import com.manuscripta.student.network.tcp.TcpSocketManager;

/**
 * Tracks the TCP connection status and updates device status accordingly.
 *
 * <p>Per {@code Session Interaction.md} §2, the Android device must maintain
 * a heartbeat with the Windows server. This tracker monitors the connection
 * state and sets the device status to {@code DISCONNECTED} when the
 * connection is lost.</p>
 *
 * <p>Related requirements: CON2A (connection status tracking).</p>
 */
public class ConnectionTracker implements TcpMessageListener {

    /**
     * Maximum number of consecutive unacknowledged heartbeats before
     * considering the connection lost, per {@code Session Interaction.md} §2(4).
     */
    public static final int MAX_UNACKNOWLEDGED_HEARTBEATS = 3;

    /**
     * Duration in milliseconds after which a broken connection triggers
     * unpairing, per {@code Session Interaction.md} §2(4).
     */
    public static final long UNPAIR_TIMEOUT_MS = 60_000L;

    /** The repository for persisting device status. */
    private final DeviceStatusRepository deviceStatusRepository;

    /** The TCP socket manager for connection state observation. */
    private final TcpSocketManager tcpSocketManager;

    /** The device ID currently being tracked. */
    @Nullable
    private volatile String deviceId;

    /** Whether this tracker is actively monitoring. */
    private volatile boolean tracking;

    /**
     * Creates a new ConnectionTracker.
     *
     * @param deviceStatusRepository The repository for device status updates
     * @param tcpSocketManager       The TCP socket manager to observe
     * @throws IllegalArgumentException if any parameter is null
     */
    public ConnectionTracker(@NonNull DeviceStatusRepository deviceStatusRepository,
                             @NonNull TcpSocketManager tcpSocketManager) {
        if (deviceStatusRepository == null) {
            throw new IllegalArgumentException(
                    "DeviceStatusRepository cannot be null");
        }
        if (tcpSocketManager == null) {
            throw new IllegalArgumentException(
                    "TcpSocketManager cannot be null");
        }
        this.deviceStatusRepository = deviceStatusRepository;
        this.tcpSocketManager = tcpSocketManager;
        this.tracking = false;
    }

    /**
     * Starts connection tracking for the specified device.
     * Registers this tracker as a TCP message listener.
     *
     * @param deviceId The device ID to track
     * @throws IllegalArgumentException if deviceId is null or empty
     */
    public void start(@NonNull String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Device ID cannot be null or empty");
        }
        this.deviceId = deviceId;
        this.tracking = true;
        tcpSocketManager.addMessageListener(this);
    }

    /**
     * Stops connection tracking and unregisters the TCP listener.
     */
    public void stop() {
        this.tracking = false;
        this.deviceId = null;
        tcpSocketManager.removeMessageListener(this);
    }

    /**
     * Returns whether the TCP socket is currently connected.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return tcpSocketManager.isConnected();
    }

    /**
     * Returns the connection state as observable LiveData.
     *
     * @return LiveData containing the current connection state
     */
    @NonNull
    public LiveData<ConnectionState> getConnectionState() {
        return tcpSocketManager.getConnectionState();
    }

    /**
     * Returns whether this tracker is actively monitoring.
     *
     * @return true if tracking is active
     */
    public boolean isTracking() {
        return tracking;
    }

    @Override
    public void onMessageReceived(@NonNull TcpMessage message) {
        // Messages are handled by other components; no action needed here
    }

    @Override
    public void onConnectionStateChanged(@NonNull ConnectionState state) {
        if (!tracking) {
            return;
        }

        String currentDeviceId = this.deviceId;
        if (currentDeviceId == null) {
            return;
        }

        if (state == ConnectionState.DISCONNECTED
                || state == ConnectionState.RECONNECTING) {
            deviceStatusRepository.setDisconnected(currentDeviceId);
        }
    }

    @Override
    public void onError(@NonNull TcpProtocolException error) {
        // Errors are logged by TcpSocketManager; no additional action needed
    }
}
