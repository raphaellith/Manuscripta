package com.manuscripta.student.device;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.manuscripta.student.data.repository.DeviceStatusRepository;
import com.manuscripta.student.data.repository.SessionRepository;
import com.manuscripta.student.network.tcp.TcpSocketManager;

/**
 * Coordinates all device management features including battery monitoring,
 * connection tracking, kiosk mode, screen lock, help requests, and
 * session end handling.
 *
 * <p>This service acts as a facade for the individual device management
 * components, providing a unified API for the UI layer.</p>
 *
 * <p>Related requirements: SYS1, CON2, CON5.</p>
 */
public class DeviceManagementService {

    /** Tag for logging. */
    private static final String TAG = "DeviceManagementService";

    /** Battery level monitor. */
    private final BatteryMonitor batteryMonitor;

    /** Connection status tracker. */
    private final ConnectionTracker connectionTracker;

    /** Kiosk mode manager. */
    private final KioskModeManager kioskModeManager;

    /** Screen lock manager. */
    private final ScreenLockManager screenLockManager;

    /** Help request manager. */
    private final HelpRequestManager helpRequestManager;

    /** Session end handler. */
    private final SessionEndHandler sessionEndHandler;

    /** Lock for thread-safe operations. */
    private final Object lock = new Object();

    /** Whether the service has been initialised. */
    private boolean initialised;

    /** The current device ID. */
    @Nullable
    private String deviceId;

    /**
     * Creates a new DeviceManagementService with the required dependencies.
     *
     * @param deviceStatusRepository The repository for device status
     * @param sessionRepository      The repository for session management
     * @param tcpSocketManager       The TCP socket manager
     * @throws IllegalArgumentException if any parameter is null
     */
    public DeviceManagementService(
            @NonNull DeviceStatusRepository deviceStatusRepository,
            @NonNull SessionRepository sessionRepository,
            @NonNull TcpSocketManager tcpSocketManager) {
        if (deviceStatusRepository == null) {
            throw new IllegalArgumentException(
                    "DeviceStatusRepository cannot be null");
        }
        if (sessionRepository == null) {
            throw new IllegalArgumentException(
                    "SessionRepository cannot be null");
        }
        if (tcpSocketManager == null) {
            throw new IllegalArgumentException(
                    "TcpSocketManager cannot be null");
        }

        this.batteryMonitor = new BatteryMonitor(deviceStatusRepository);
        this.connectionTracker = new ConnectionTracker(
                deviceStatusRepository, tcpSocketManager);
        this.kioskModeManager = new KioskModeManager();
        this.screenLockManager = new ScreenLockManager(
                deviceStatusRepository, tcpSocketManager);
        this.helpRequestManager = new HelpRequestManager(tcpSocketManager);
        this.sessionEndHandler = new SessionEndHandler(
                sessionRepository, tcpSocketManager);
        this.initialised = false;
    }

    /**
     * Initialises the device management service for the specified device.
     * Starts all sub-managers.
     *
     * @param deviceId The device ID to manage
     * @throws IllegalArgumentException if deviceId is null or empty
     */
    public void initialise(@NonNull String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Device ID cannot be null or empty");
        }

        synchronized (lock) {
            this.deviceId = deviceId;
            connectionTracker.start(deviceId);
            screenLockManager.start(deviceId);
            sessionEndHandler.start();
            initialised = true;
        }

        Log.i(TAG, "Device management initialised for " + deviceId);
    }

    /**
     * Shuts down the device management service and cleans up resources.
     */
    public void shutdown() {
        synchronized (lock) {
            connectionTracker.stop();
            screenLockManager.stop();
            sessionEndHandler.stop();
            helpRequestManager.destroy();
            kioskModeManager.disableKioskMode();
            initialised = false;
            deviceId = null;
        }

        Log.i(TAG, "Device management shut down");
    }

    /**
     * Returns whether the service has been initialised.
     *
     * @return true if initialised
     */
    public boolean isInitialised() {
        synchronized (lock) {
            return initialised;
        }
    }

    // ========== Convenience methods ==========

    /**
     * Raises the student's hand to request help.
     *
     * @param deviceId The device ID
     * @return true if the message was sent successfully
     */
    public boolean raiseHand(@NonNull String deviceId) {
        return helpRequestManager.raiseHand(deviceId);
    }

    /**
     * Lowers the student's hand.
     */
    public void lowerHand() {
        helpRequestManager.lowerHand();
    }

    /**
     * Returns whether the screen is currently locked.
     *
     * @return true if the screen is locked
     */
    public boolean isScreenLocked() {
        return screenLockManager.isScreenLocked();
    }

    /**
     * Returns whether the device is connected to the server.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return connectionTracker.isConnected();
    }

    /**
     * Returns whether kiosk mode is enabled.
     *
     * @return true if kiosk mode is enabled
     */
    public boolean isKioskModeEnabled() {
        return kioskModeManager.isKioskModeEnabled();
    }

    /**
     * Enables kiosk mode.
     *
     * @return true if kiosk mode was enabled successfully
     */
    public boolean enableKioskMode() {
        return kioskModeManager.enableKioskMode();
    }

    /**
     * Disables kiosk mode.
     *
     * @return true if kiosk mode was disabled successfully
     */
    public boolean disableKioskMode() {
        return kioskModeManager.disableKioskMode();
    }

    /**
     * Reports a battery level change.
     *
     * @param level The new battery level percentage (0-100)
     */
    public void onBatteryLevelChanged(int level) {
        batteryMonitor.onBatteryLevelChanged(level);
    }

    // ========== Sub-manager getters ==========

    /**
     * Returns the battery monitor.
     *
     * @return The battery monitor instance
     */
    @NonNull
    public BatteryMonitor getBatteryMonitor() {
        return batteryMonitor;
    }

    /**
     * Returns the connection tracker.
     *
     * @return The connection tracker instance
     */
    @NonNull
    public ConnectionTracker getConnectionTracker() {
        return connectionTracker;
    }

    /**
     * Returns the kiosk mode manager.
     *
     * @return The kiosk mode manager instance
     */
    @NonNull
    public KioskModeManager getKioskModeManager() {
        return kioskModeManager;
    }

    /**
     * Returns the screen lock manager.
     *
     * @return The screen lock manager instance
     */
    @NonNull
    public ScreenLockManager getScreenLockManager() {
        return screenLockManager;
    }

    /**
     * Returns the help request manager.
     *
     * @return The help request manager instance
     */
    @NonNull
    public HelpRequestManager getHelpRequestManager() {
        return helpRequestManager;
    }

    /**
     * Returns the session end handler.
     *
     * @return The session end handler instance
     */
    @NonNull
    public SessionEndHandler getSessionEndHandler() {
        return sessionEndHandler;
    }
}
