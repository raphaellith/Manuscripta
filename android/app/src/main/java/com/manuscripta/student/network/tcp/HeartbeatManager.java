package com.manuscripta.student.network.tcp;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.Gson;
import com.manuscripta.student.domain.model.DeviceStatus;
import com.manuscripta.student.network.tcp.message.DistributeMaterialMessage;
import com.manuscripta.student.network.tcp.message.ReturnFeedbackMessage;
import com.manuscripta.student.network.tcp.message.StatusUpdateMessage;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages the TCP heartbeat mechanism for maintaining connection and triggering fetches.
 *
 * <p>The heartbeat pattern works as follows:
 * <ol>
 *   <li>Android sends periodic {@code STATUS_UPDATE} (0x10) messages via TCP</li>
 *   <li>Server checks if new materials or feedback are available for this device</li>
 *   <li>If materials are available, server responds with {@code DISTRIBUTE_MATERIAL} (0x05)</li>
 *   <li>If feedback is available, server responds with {@code RETURN_FEEDBACK} (0x07)</li>
 *   <li>HeartbeatManager notifies the appropriate callback to trigger HTTP fetch</li>
 * </ol>
 *
 * <p>Usage:
 * <pre>{@code
 * HeartbeatManager heartbeat = new HeartbeatManager(socketManager);
 * heartbeat.setDeviceStatusProvider(() -> getCurrentDeviceStatus());
 * heartbeat.setMaterialCallback(() -> fetchMaterialsViaHttp());
 * heartbeat.setFeedbackCallback(() -> fetchFeedbackViaHttp());
 * heartbeat.start();
 * }</pre>
 *
 * @see HeartbeatConfig
 * @see TcpSocketManager
 */
@Singleton
public class HeartbeatManager implements TcpMessageListener {

    /** Tag for logging. */
    private static final String TAG = "HeartbeatManager";

    /**
     * Provides the current device status for heartbeat messages.
     */
    public interface DeviceStatusProvider {
        /**
         * Returns the current device status.
         *
         * @return The current DeviceStatus, or null if unavailable.
         */
        @Nullable
        DeviceStatus getDeviceStatus();
    }

    /**
     * Callback for when the server signals that new materials are available.
     */
    public interface MaterialAvailableCallback {
        /**
         * Called when the server sends a DISTRIBUTE_MATERIAL message.
         * The implementation should trigger an HTTP fetch to download materials.
         */
        void onMaterialsAvailable();
    }

    /**
     * Callback for when the server signals that feedback is available.
     */
    public interface FeedbackAvailableCallback {
        /**
         * Called when the server sends a RETURN_FEEDBACK message.
         * The implementation should trigger an HTTP fetch to download feedback.
         */
        void onFeedbackAvailable();
    }

    /** The TCP socket manager for sending messages. */
    private final TcpSocketManager socketManager;
    /** Gson instance for JSON serialization. */
    private final Gson gson;
    /** Lock object for synchronising heartbeat operations. */
    private final Object lock = new Object();

    /** The executor service for scheduling heartbeats. */
    @Nullable
    private ScheduledExecutorService scheduler;
    /** The future representing the scheduled heartbeat task. */
    @Nullable
    private ScheduledFuture<?> heartbeatFuture;
    /** Provider for current device status. */
    @Nullable
    private DeviceStatusProvider statusProvider;
    /** Callback for when materials are available. */
    @Nullable
    private MaterialAvailableCallback materialCallback;
    /** Callback for when feedback is available. */
    @Nullable
    private FeedbackAvailableCallback feedbackCallback;

    /** The heartbeat configuration. */
    private HeartbeatConfig config;
    /** Whether the heartbeat is currently running. */
    private final AtomicBoolean running = new AtomicBoolean(false);
    /** Counter for heartbeats sent. */
    private final AtomicLong heartbeatCount = new AtomicLong(0);
    /** Timestamp of the last heartbeat sent. */
    private final AtomicLong lastHeartbeatTimestamp = new AtomicLong(0);

    /**
     * Creates a new HeartbeatManager with the specified socket manager.
     *
     * @param socketManager The TCP socket manager for sending messages.
     */
    @Inject
    public HeartbeatManager(@NonNull TcpSocketManager socketManager) {
        this(socketManager, new HeartbeatConfig(), new Gson());
    }

    /**
     * Creates a new HeartbeatManager with the specified socket manager and config.
     *
     * @param socketManager The TCP socket manager for sending messages.
     * @param config        The heartbeat configuration.
     */
    public HeartbeatManager(@NonNull TcpSocketManager socketManager,
                            @NonNull HeartbeatConfig config) {
        this(socketManager, config, new Gson());
    }

    /**
     * Creates a new HeartbeatManager with all dependencies.
     *
     * @param socketManager The TCP socket manager for sending messages.
     * @param config        The heartbeat configuration.
     * @param gson          The Gson instance for JSON serialization.
     */
    @VisibleForTesting
    HeartbeatManager(@NonNull TcpSocketManager socketManager,
                     @NonNull HeartbeatConfig config,
                     @NonNull Gson gson) {
        this.socketManager = socketManager;
        this.config = config;
        this.gson = gson;
        // Register as listener to receive DISTRIBUTE_MATERIAL messages
        this.socketManager.addMessageListener(this);
    }

    /**
     * Sets the provider for device status information.
     *
     * @param provider The status provider.
     */
    public void setDeviceStatusProvider(@Nullable DeviceStatusProvider provider) {
        this.statusProvider = provider;
    }

    /**
     * Sets the callback for when materials are available.
     *
     * @param callback The callback to invoke when DISTRIBUTE_MATERIAL is received.
     */
    public void setMaterialCallback(@Nullable MaterialAvailableCallback callback) {
        this.materialCallback = callback;
    }

    /**
     * Sets the callback for when feedback is available.
     *
     * @param callback The callback to invoke when RETURN_FEEDBACK is received.
     */
    public void setFeedbackCallback(@Nullable FeedbackAvailableCallback callback) {
        this.feedbackCallback = callback;
    }

    /**
     * Updates the heartbeat configuration.
     *
     * @param config The new configuration.
     */
    public void setConfig(@NonNull HeartbeatConfig config) {
        synchronized (lock) {
            this.config = config;
            // If running, restart with new config
            if (running.get()) {
                stopInternal();
                startInternal();
            }
        }
    }

    /**
     * Returns the current heartbeat configuration.
     *
     * @return The current config.
     */
    @NonNull
    public HeartbeatConfig getConfig() {
        return config;
    }

    /**
     * Starts the heartbeat mechanism.
     * Heartbeats will be sent at the configured interval while connected.
     */
    public void start() {
        synchronized (lock) {
            if (!config.isEnabled()) {
                Log.d(TAG, "Heartbeat disabled in config, not starting");
                return;
            }
            startInternal();
        }
    }

    /**
     * Stops the heartbeat mechanism.
     */
    public void stop() {
        synchronized (lock) {
            stopInternal();
        }
    }

    /**
     * Returns whether the heartbeat is currently running.
     *
     * @return true if heartbeat is actively sending.
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Returns the number of heartbeats sent since start.
     *
     * @return The heartbeat count.
     */
    public long getHeartbeatCount() {
        return heartbeatCount.get();
    }

    /**
     * Returns the timestamp of the last sent heartbeat.
     *
     * @return Unix timestamp in milliseconds, or 0 if never sent.
     */
    public long getLastHeartbeatTimestamp() {
        return lastHeartbeatTimestamp.get();
    }

    // ========== TcpMessageListener implementation ==========

    @Override
    public void onMessageReceived(@NonNull TcpMessage message) {
        if (message instanceof DistributeMaterialMessage) {
            Log.d(TAG, "Received DISTRIBUTE_MATERIAL signal");
            MaterialAvailableCallback callback = this.materialCallback;
            if (callback != null) {
                callback.onMaterialsAvailable();
            }
        } else if (message instanceof ReturnFeedbackMessage) {
            Log.d(TAG, "Received RETURN_FEEDBACK signal");
            FeedbackAvailableCallback callback = this.feedbackCallback;
            if (callback != null) {
                callback.onFeedbackAvailable();
            }
        }
    }

    @Override
    public void onConnectionStateChanged(@NonNull ConnectionState state) {
        synchronized (lock) {
            if (state == ConnectionState.CONNECTED) {
                Log.d(TAG, "Connected - starting heartbeat");
                if (config.isEnabled() && !running.get()) {
                    startInternal();
                }
            } else if (state == ConnectionState.DISCONNECTED) {
                Log.d(TAG, "Disconnected - stopping heartbeat");
                stopInternal();
            }
        }
    }

    @Override
    public void onError(@NonNull TcpProtocolException error) {
        Log.w(TAG, "TCP error: " + error.getMessage());
    }

    // ========== Internal methods ==========

    private void startInternal() {
        if (running.get()) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        heartbeatFuture = scheduler.scheduleAtFixedRate(
                this::sendHeartbeat,
                0, // Send first heartbeat immediately
                config.getIntervalMs(),
                TimeUnit.MILLISECONDS
        );
        running.set(true);
        Log.i(TAG, "Heartbeat started with interval " + config.getIntervalMs() + "ms");
    }

    private void stopInternal() {
        running.set(false);

        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(false);
            heartbeatFuture = null;
        }

        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }

        Log.i(TAG, "Heartbeat stopped. Total heartbeats sent: " + heartbeatCount.get());
    }

    /**
     * Sends a single heartbeat message.
     */
    @VisibleForTesting
    void sendHeartbeat() {
        if (!socketManager.isConnected()) {
            Log.d(TAG, "Not connected, skipping heartbeat");
            return;
        }

        try {
            String jsonPayload = buildStatusJson();
            StatusUpdateMessage message = new StatusUpdateMessage(jsonPayload);
            socketManager.send(message);

            heartbeatCount.incrementAndGet();
            lastHeartbeatTimestamp.set(System.currentTimeMillis());
            Log.d(TAG, "Heartbeat #" + heartbeatCount.get() + " sent");

        } catch (IOException e) {
            Log.e(TAG, "Failed to send heartbeat: " + e.getMessage());
        } catch (TcpProtocolException e) {
            Log.e(TAG, "Protocol error sending heartbeat: " + e.getMessage());
        }
    }

    /**
     * Builds the JSON payload for the STATUS_UPDATE message.
     *
     * @return JSON string containing device status.
     */
    @NonNull
    private String buildStatusJson() {
        DeviceStatusProvider provider = this.statusProvider;
        DeviceStatus status = provider != null ? provider.getDeviceStatus() : null;

        Map<String, Object> json = new LinkedHashMap<>();
        if (status != null) {
            json.put("DeviceId", status.getDeviceId());
            json.put("Status", status.getStatus().name());
            json.put("BatteryLevel", status.getBatteryLevel());
            if (status.getCurrentMaterialId() != null) {
                json.put("CurrentMaterialId", status.getCurrentMaterialId());
            }
            if (status.getStudentView() != null) {
                json.put("StudentView", status.getStudentView());
            }
            json.put("Timestamp", status.getLastUpdated() / 1000); // Convert to seconds
        } else {
            // Minimal payload when no status available
            json.put("DeviceId", "unknown");
            json.put("Status", "IDLE");
            json.put("BatteryLevel", 0);
            json.put("Timestamp", System.currentTimeMillis() / 1000);
        }
        return gson.toJson(json);
    }

    /**
     * Cleans up resources. Should be called when the manager is no longer needed.
     */
    public void destroy() {
        stop();
        socketManager.removeMessageListener(this);
    }
}
