package com.manuscripta.student.network.tcp;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.manuscripta.student.network.tcp.message.PairingAckMessage;
import com.manuscripta.student.network.tcp.message.PairingRequestMessage;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages the TCP pairing handshake with the teacher server.
 *
 * <p>The pairing flow as specified in {@code Pairing Process.md} Section 2:
 * <ol>
 *   <li>Android establishes TCP connection (via TcpSocketManager)</li>
 *   <li>Android sends PAIRING_REQUEST (0x20) with device ID</li>
 *   <li>Server responds with PAIRING_ACK (0x21)</li>
 *   <li>PairingManager notifies callback of success</li>
 * </ol>
 *
 * <p>Note: Full pairing also requires HTTP registration (POST /pair).
 * This class only handles the TCP portion.
 *
 * <p>Usage:
 * <pre>{@code
 * PairingManager pairing = new PairingManager(socketManager);
 * pairing.setPairingCallback(new PairingCallback() {
 *     public void onTcpPairingSuccess() { }
 *     public void onPairingFailed(String reason) { }
 *     public void onPairingTimeout() { }
 * });
 * pairing.startPairing("device-uuid", "192.168.1.100", 5912);
 * }</pre>
 *
 * @see PairingCallback
 * @see PairingConfig
 * @see TcpSocketManager
 */
@Singleton
public class PairingManager implements TcpMessageListener {

    /** Tag for logging. */
    private static final String TAG = "PairingManager";

    /** The TCP socket manager for connection and messaging. */
    private final TcpSocketManager socketManager;
    /** Lock object for synchronising pairing operations. */
    private final Object lock = new Object();

    /** The pairing configuration. */
    private PairingConfig config;
    /** The current pairing state. */
    private final MutableLiveData<PairingState> pairingState;
    /** Whether a pairing attempt is in progress. */
    private final AtomicBoolean pairingInProgress = new AtomicBoolean(false);
    /** The current retry attempt number. */
    private final AtomicInteger currentRetryAttempt = new AtomicInteger(0);

    /** The scheduled executor for timeout handling. */
    @Nullable
    private ScheduledExecutorService scheduler;
    /** The timeout future. */
    @Nullable
    private ScheduledFuture<?> timeoutFuture;
    /** The callback for pairing events. */
    @Nullable
    private PairingCallback callback;
    /** The device ID being used for pairing. */
    @Nullable
    private String deviceId;
    /** The host for the current pairing attempt. */
    @Nullable
    private String currentHost;
    /** The port for the current pairing attempt. */
    private int currentPort;

    /**
     * Creates a new PairingManager with the specified socket manager.
     *
     * @param socketManager The TCP socket manager for connection and messaging.
     */
    @Inject
    public PairingManager(@NonNull TcpSocketManager socketManager) {
        this(socketManager, new PairingConfig());
    }

    /**
     * Creates a new PairingManager with the specified socket manager and config.
     *
     * @param socketManager The TCP socket manager for connection and messaging.
     * @param config        The pairing configuration.
     */
    public PairingManager(@NonNull TcpSocketManager socketManager,
                          @NonNull PairingConfig config) {
        this.socketManager = socketManager;
        this.config = config;
        this.pairingState = new MutableLiveData<>(PairingState.NOT_PAIRED);
        // Register as listener to receive PAIRING_ACK messages
        this.socketManager.addMessageListener(this);
    }

    /**
     * Sets the callback for pairing events.
     *
     * @param callback The callback to notify on pairing events.
     */
    public void setPairingCallback(@Nullable PairingCallback callback) {
        this.callback = callback;
    }

    /**
     * Updates the pairing configuration.
     *
     * @param config The new configuration.
     */
    public void setConfig(@NonNull PairingConfig config) {
        synchronized (lock) {
            this.config = config;
        }
    }

    /**
     * Returns the current pairing configuration.
     *
     * @return The current config.
     */
    @NonNull
    public PairingConfig getConfig() {
        return config;
    }

    /**
     * Returns the current pairing state as LiveData for UI observation.
     *
     * @return LiveData containing the current pairing state.
     */
    @NonNull
    public LiveData<PairingState> getPairingState() {
        return pairingState;
    }

    /**
     * Returns the current pairing state value.
     *
     * @return The current pairing state.
     */
    @NonNull
    public PairingState getCurrentState() {
        PairingState state = pairingState.getValue();
        return state != null ? state : PairingState.NOT_PAIRED;
    }

    /**
     * Starts the TCP pairing handshake.
     *
     * <p>This will:
     * <ol>
     *   <li>Connect to the server via TCP</li>
     *   <li>Send PAIRING_REQUEST with the device ID</li>
     *   <li>Wait for PAIRING_ACK with timeout</li>
     *   <li>Notify callback of result</li>
     * </ol>
     *
     * @param deviceId The unique device identifier to register.
     * @param host     The server IP address.
     * @param port     The server TCP port.
     */
    public void startPairing(@NonNull String deviceId, @NonNull String host, int port) {
        synchronized (lock) {
            if (pairingInProgress.get()) {
                Log.w(TAG, "Pairing already in progress");
                return;
            }

            this.deviceId = deviceId;
            this.currentHost = host;
            this.currentPort = port;
            this.currentRetryAttempt.set(0);

            pairingInProgress.set(true);
            updatePairingState(PairingState.PAIRING_IN_PROGRESS);

            attemptPairing();
        }
    }

    /**
     * Attempts a single pairing handshake.
     */
    private void attemptPairing() {
        Log.i(TAG, "Starting pairing attempt " + (currentRetryAttempt.get() + 1)
                + " to " + currentHost + ":" + currentPort);

        // Connect to server
        socketManager.connect(currentHost, currentPort);
    }

    /**
     * Cancels any ongoing pairing attempt.
     */
    public void cancelPairing() {
        synchronized (lock) {
            if (!pairingInProgress.get()) {
                return;
            }

            Log.i(TAG, "Pairing cancelled");
            cleanup();
            updatePairingState(PairingState.NOT_PAIRED);
        }
    }

    /**
     * Returns whether a pairing attempt is in progress.
     *
     * @return true if pairing is in progress.
     */
    public boolean isPairingInProgress() {
        return pairingInProgress.get();
    }

    // ========== TcpMessageListener implementation ==========

    @Override
    public void onMessageReceived(@NonNull TcpMessage message) {
        if (message instanceof PairingAckMessage) {
            handlePairingAck();
        }
    }

    @Override
    public void onConnectionStateChanged(@NonNull ConnectionState state) {
        synchronized (lock) {
            if (!pairingInProgress.get()) {
                return;
            }

            if (state == ConnectionState.CONNECTED) {
                Log.d(TAG, "TCP connected - sending PAIRING_REQUEST");
                sendPairingRequest();
            } else if (state == ConnectionState.DISCONNECTED) {
                // Connection failed or lost during pairing
                if (pairingInProgress.get()) {
                    handlePairingFailure("Connection lost during pairing");
                }
            }
        }
    }

    @Override
    public void onError(@NonNull TcpProtocolException error) {
        if (pairingInProgress.get()) {
            Log.w(TAG, "TCP error during pairing: " + error.getMessage());
            handlePairingFailure("TCP protocol error: " + error.getMessage());
        }
    }

    // ========== Internal methods ==========

    /**
     * Sends the PAIRING_REQUEST message and starts the timeout timer.
     */
    private void sendPairingRequest() {
        // Capture deviceId under lock for thread safety
        final String capturedDeviceId;
        synchronized (lock) {
            capturedDeviceId = deviceId;
        }

        try {
            PairingRequestMessage request = new PairingRequestMessage(capturedDeviceId);
            socketManager.send(request);
            Log.d(TAG, "Sent PAIRING_REQUEST with deviceId: " + capturedDeviceId);

            // Start timeout timer
            startTimeoutTimer();

        } catch (IOException | TcpProtocolException e) {
            Log.e(TAG, "Failed to send PAIRING_REQUEST: " + e.getMessage());
            handlePairingFailure("Failed to send pairing request: " + e.getMessage());
        }
    }

    /**
     * Starts the timeout timer for waiting for PAIRING_ACK.
     */
    private void startTimeoutTimer() {
        cancelTimeoutTimer();

        scheduler = Executors.newSingleThreadScheduledExecutor();
        timeoutFuture = scheduler.schedule(
                this::handleTimeout,
                config.getTimeoutMs(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Cancels the timeout timer.
     */
    private void cancelTimeoutTimer() {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
            timeoutFuture = null;
        }

        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }

    /**
     * Handles successful receipt of PAIRING_ACK.
     */
    private void handlePairingAck() {
        synchronized (lock) {
            if (!pairingInProgress.get()) {
                Log.w(TAG, "Received PAIRING_ACK but not in pairing state");
                return;
            }

            Log.i(TAG, "Received PAIRING_ACK - TCP pairing successful");
            cancelTimeoutTimer();
            pairingInProgress.set(false);
            updatePairingState(PairingState.PAIRED);

            PairingCallback cb = this.callback;
            if (cb != null) {
                cb.onTcpPairingSuccess();
            }
        }
    }

    /**
     * Handles pairing timeout.
     */
    private void handleTimeout() {
        synchronized (lock) {
            if (!pairingInProgress.get()) {
                return;
            }

            int attempt = currentRetryAttempt.incrementAndGet();
            Log.w(TAG, "Pairing timeout (attempt " + attempt + " of "
                    + (config.getRetryCount() + 1) + ")");

            if (attempt <= config.getRetryCount()) {
                // Retry - clean up resources before next attempt
                Log.i(TAG, "Retrying pairing...");
                cancelTimeoutTimer();
                socketManager.disconnect();
                attemptPairing();
            } else {
                // All retries exhausted
                Log.e(TAG, "Pairing failed after " + attempt + " attempts");
                cleanup();
                updatePairingState(PairingState.PAIRING_TIMEOUT);

                PairingCallback cb = this.callback;
                if (cb != null) {
                    cb.onPairingTimeout();
                }
            }
        }
    }

    /**
     * Handles pairing failure.
     *
     * @param reason The failure reason.
     */
    private void handlePairingFailure(@NonNull String reason) {
        synchronized (lock) {
            if (!pairingInProgress.get()) {
                return;
            }

            int attempt = currentRetryAttempt.incrementAndGet();
            Log.w(TAG, "Pairing failed: " + reason + " (attempt " + attempt + " of "
                    + (config.getRetryCount() + 1) + ")");

            if (attempt <= config.getRetryCount()) {
                // Retry
                Log.i(TAG, "Retrying pairing...");
                cancelTimeoutTimer();
                attemptPairing();
            } else {
                // All retries exhausted
                Log.e(TAG, "Pairing failed after " + attempt + " attempts: " + reason);
                cleanup();
                updatePairingState(PairingState.PAIRING_FAILED);

                PairingCallback cb = this.callback;
                if (cb != null) {
                    cb.onPairingFailed(reason);
                }
            }
        }
    }

    /**
     * Cleans up resources after pairing completes or fails.
     */
    private void cleanup() {
        cancelTimeoutTimer();
        pairingInProgress.set(false);
    }

    /**
     * Updates the pairing state and posts to LiveData.
     *
     * @param state The new pairing state.
     */
    private void updatePairingState(@NonNull PairingState state) {
        pairingState.postValue(state);
    }

    /**
     * Cleans up resources. Should be called when the manager is no longer needed.
     */
    public void destroy() {
        cancelPairing();
        socketManager.removeMessageListener(this);
    }

    /**
     * Returns the current retry attempt number for testing purposes.
     *
     * @return The current retry attempt (0-based).
     */
    @VisibleForTesting
    int getCurrentRetryAttempt() {
        return currentRetryAttempt.get();
    }

    /**
     * Returns the device ID being used for pairing.
     *
     * @return The device ID, or null if not set.
     */
    @Nullable
    @VisibleForTesting
    String getDeviceId() {
        return deviceId;
    }
}
