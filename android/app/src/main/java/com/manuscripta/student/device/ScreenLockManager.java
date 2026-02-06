package com.manuscripta.student.device;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.manuscripta.student.data.repository.DeviceStatusRepository;
import com.manuscripta.student.network.tcp.ConnectionState;
import com.manuscripta.student.network.tcp.TcpMessage;
import com.manuscripta.student.network.tcp.TcpMessageListener;
import com.manuscripta.student.network.tcp.TcpProtocolException;
import com.manuscripta.student.network.tcp.TcpSocketManager;
import com.manuscripta.student.network.tcp.message.LockScreenMessage;
import com.manuscripta.student.network.tcp.message.UnlockScreenMessage;

/**
 * Handles teacher-controlled screen lock and unlock commands.
 *
 * <p>Per {@code Session Interaction.md} §6(2), on receipt of
 * {@code LOCK_SCREEN} (0x01) the Android device must lock its UI and
 * report {@code LOCKED} status. On receipt of {@code UNLOCK_SCREEN}
 * (0x02) it must restore user interaction and report
 * {@code ON_TASK} or {@code IDLE}.</p>
 *
 * <p>Related requirements: CON6 (device-specific controls including
 * the ability to lock selected screens).</p>
 */
public class ScreenLockManager implements TcpMessageListener {

    /** The repository for persisting device status. */
    private final DeviceStatusRepository deviceStatusRepository;

    /** The TCP socket manager for message listening. */
    private final TcpSocketManager tcpSocketManager;

    /** Lock for thread-safe operations. */
    private final Object lock = new Object();

    /** Whether the screen is currently locked. */
    private boolean screenLocked;

    /** The device ID currently being managed. */
    @Nullable
    private String deviceId;

    /** Whether this manager is actively monitoring. */
    private boolean active;

    /** Listener for screen lock state changes. */
    @Nullable
    private ScreenLockListener listener;

    /**
     * Listener interface for screen lock state changes.
     */
    public interface ScreenLockListener {
        /**
         * Called when the screen lock state changes.
         *
         * @param locked true if the screen is now locked, false if unlocked
         */
        void onScreenLockChanged(boolean locked);
    }

    /**
     * Creates a new ScreenLockManager.
     *
     * @param deviceStatusRepository The repository for device status updates
     * @param tcpSocketManager       The TCP socket manager for message listening
     * @throws IllegalArgumentException if any parameter is null
     */
    public ScreenLockManager(
            @NonNull DeviceStatusRepository deviceStatusRepository,
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
        this.screenLocked = false;
        this.active = false;
    }

    /**
     * Starts listening for screen lock/unlock commands.
     *
     * @param deviceId The device ID to manage
     * @throws IllegalArgumentException if deviceId is null or empty
     */
    public void start(@NonNull String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Device ID cannot be null or empty");
        }
        synchronized (lock) {
            this.deviceId = deviceId;
            this.active = true;
            this.screenLocked = false;
        }
        tcpSocketManager.addMessageListener(this);
    }

    /**
     * Stops listening for screen lock/unlock commands.
     */
    public void stop() {
        synchronized (lock) {
            this.active = false;
            this.deviceId = null;
            this.screenLocked = false;
        }
        tcpSocketManager.removeMessageListener(this);
    }

    /**
     * Returns whether the screen is currently locked.
     *
     * @return true if the screen is locked
     */
    public boolean isScreenLocked() {
        synchronized (lock) {
            return screenLocked;
        }
    }

    /**
     * Returns whether this manager is actively monitoring.
     *
     * @return true if active
     */
    public boolean isActive() {
        synchronized (lock) {
            return active;
        }
    }

    /**
     * Sets the listener for screen lock state changes.
     *
     * @param listener The listener, or null to remove
     */
    public void setScreenLockListener(
            @Nullable ScreenLockListener listener) {
        synchronized (lock) {
            this.listener = listener;
        }
    }

    /**
     * Removes the current screen lock listener.
     */
    public void removeScreenLockListener() {
        synchronized (lock) {
            this.listener = null;
        }
    }

    @Override
    public void onMessageReceived(@NonNull TcpMessage message) {
        synchronized (lock) {
            if (!active || deviceId == null) {
                return;
            }

            if (message instanceof LockScreenMessage) {
                screenLocked = true;
                deviceStatusRepository.setLocked(deviceId);
                notifyListener(true);
            } else if (message instanceof UnlockScreenMessage) {
                screenLocked = false;
                // null materialId — no specific material on unlock
                deviceStatusRepository.setOnTask(deviceId, null);
                notifyListener(false);
            }
        }
    }

    @Override
    public void onConnectionStateChanged(@NonNull ConnectionState state) {
        // Connection state changes handled by ConnectionTracker
    }

    @Override
    public void onError(@NonNull TcpProtocolException error) {
        // Errors are logged by TcpSocketManager
    }

    /**
     * Notifies the registered listener of a screen lock state change.
     *
     * @param locked The new lock state
     */
    private void notifyListener(boolean locked) {
        ScreenLockListener currentListener = this.listener;
        if (currentListener != null) {
            currentListener.onScreenLockChanged(locked);
        }
    }
}
