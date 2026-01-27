package com.manuscripta.student.network.tcp;

import androidx.annotation.NonNull;

/**
 * Configuration for the TCP heartbeat mechanism.
 *
 * <p>The heartbeat is a periodic {@code STATUS_UPDATE} message sent from the Android client
 * to the teacher server. It serves two purposes:
 * <ol>
 *   <li>Keep-alive: Indicates the device is still connected</li>
 *   <li>Material trigger: Allows the server to respond with {@code DISTRIBUTE_MATERIAL}
 *       when new content is available</li>
 * </ol>
 *
 * @see TcpSocketManager
 */
public final class HeartbeatConfig {

    /**
     * Default heartbeat interval in milliseconds (5 seconds).
     */
    public static final long DEFAULT_INTERVAL_MS = 5000L;

    /**
     * Minimum allowed heartbeat interval in milliseconds (1 second).
     */
    public static final long MIN_INTERVAL_MS = 1000L;

    /**
     * Maximum allowed heartbeat interval in milliseconds (60 seconds).
     */
    public static final long MAX_INTERVAL_MS = 60000L;

    /** The heartbeat interval in milliseconds. */
    private final long intervalMs;
    /** Whether heartbeat is enabled. */
    private final boolean enabled;

    /**
     * Creates a new HeartbeatConfig with default settings.
     * Heartbeat is enabled with a 5-second interval.
     */
    public HeartbeatConfig() {
        this(DEFAULT_INTERVAL_MS, true);
    }

    /**
     * Creates a new HeartbeatConfig with the specified settings.
     *
     * @param intervalMs The interval between heartbeats in milliseconds.
     *                   Will be clamped to [{@link #MIN_INTERVAL_MS}, {@link #MAX_INTERVAL_MS}].
     * @param enabled    Whether heartbeat is enabled.
     */
    public HeartbeatConfig(long intervalMs, boolean enabled) {
        this.intervalMs = clampInterval(intervalMs);
        this.enabled = enabled;
    }

    /**
     * Returns the heartbeat interval in milliseconds.
     *
     * @return The interval between heartbeats.
     */
    public long getIntervalMs() {
        return intervalMs;
    }

    /**
     * Returns whether heartbeat is enabled.
     *
     * @return {@code true} if heartbeat should be sent periodically.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Creates a new config with heartbeat enabled.
     *
     * @return A new HeartbeatConfig with enabled set to true.
     */
    @NonNull
    public HeartbeatConfig withEnabled() {
        return new HeartbeatConfig(this.intervalMs, true);
    }

    /**
     * Creates a new config with heartbeat disabled.
     *
     * @return A new HeartbeatConfig with enabled set to false.
     */
    @NonNull
    public HeartbeatConfig withDisabled() {
        return new HeartbeatConfig(this.intervalMs, false);
    }

    /**
     * Creates a new config with the specified interval.
     *
     * @param intervalMs The new interval in milliseconds.
     * @return A new HeartbeatConfig with the specified interval.
     */
    @NonNull
    public HeartbeatConfig withInterval(long intervalMs) {
        return new HeartbeatConfig(intervalMs, this.enabled);
    }

    /**
     * Clamps the interval to valid bounds.
     *
     * @param intervalMs The interval to clamp.
     * @return The clamped interval.
     */
    private static long clampInterval(long intervalMs) {
        return Math.max(MIN_INTERVAL_MS, Math.min(MAX_INTERVAL_MS, intervalMs));
    }

    @NonNull
    @Override
    public String toString() {
        return "HeartbeatConfig{"
                + "intervalMs=" + intervalMs
                + ", enabled=" + enabled
                + '}';
    }
}
