package com.manuscripta.student.network.tcp;

import androidx.annotation.NonNull;

/**
 * Configuration for the TCP pairing handshake.
 *
 * <p>This class holds settings for pairing timeout and retry behaviour.
 * See {@code Pairing Process.md} Section 2 for the pairing specification.
 *
 * @see PairingManager
 */
public final class PairingConfig {

    /**
     * Default timeout for pairing handshake in milliseconds (10 seconds).
     */
    public static final long DEFAULT_TIMEOUT_MS = 10000L;

    /**
     * Minimum allowed timeout in milliseconds (1 second).
     */
    public static final long MIN_TIMEOUT_MS = 1000L;

    /**
     * Maximum allowed timeout in milliseconds (60 seconds).
     */
    public static final long MAX_TIMEOUT_MS = 60000L;

    /**
     * Default number of retry attempts.
     */
    public static final int DEFAULT_RETRY_COUNT = 3;

    /** The timeout for waiting for PAIRING_ACK in milliseconds. */
    private final long timeoutMs;

    /** The number of retry attempts before giving up. */
    private final int retryCount;

    /**
     * Creates a new PairingConfig with default settings.
     * Timeout is 10 seconds and retry count is 3.
     */
    public PairingConfig() {
        this(DEFAULT_TIMEOUT_MS, DEFAULT_RETRY_COUNT);
    }

    /**
     * Creates a new PairingConfig with the specified settings.
     *
     * @param timeoutMs  The timeout for pairing in milliseconds.
     *                   Will be clamped to [{@link #MIN_TIMEOUT_MS}, {@link #MAX_TIMEOUT_MS}].
     * @param retryCount The number of retry attempts. Must be non-negative.
     */
    public PairingConfig(long timeoutMs, int retryCount) {
        this.timeoutMs = clampTimeout(timeoutMs);
        this.retryCount = Math.max(0, retryCount);
    }

    /**
     * Returns the timeout for pairing in milliseconds.
     *
     * @return The pairing timeout.
     */
    public long getTimeoutMs() {
        return timeoutMs;
    }

    /**
     * Returns the number of retry attempts.
     *
     * @return The retry count.
     */
    public int getRetryCount() {
        return retryCount;
    }

    /**
     * Creates a new config with the specified timeout.
     *
     * @param timeoutMs The new timeout in milliseconds.
     * @return A new PairingConfig with the specified timeout.
     */
    @NonNull
    public PairingConfig withTimeout(long timeoutMs) {
        return new PairingConfig(timeoutMs, this.retryCount);
    }

    /**
     * Creates a new config with the specified retry count.
     *
     * @param retryCount The new retry count.
     * @return A new PairingConfig with the specified retry count.
     */
    @NonNull
    public PairingConfig withRetryCount(int retryCount) {
        return new PairingConfig(this.timeoutMs, retryCount);
    }

    /**
     * Clamps the timeout to valid bounds.
     *
     * @param timeoutMs The timeout to clamp.
     * @return The clamped timeout.
     */
    private static long clampTimeout(long timeoutMs) {
        return Math.max(MIN_TIMEOUT_MS, Math.min(MAX_TIMEOUT_MS, timeoutMs));
    }

    @NonNull
    @Override
    public String toString() {
        return "PairingConfig{"
                + "timeoutMs=" + timeoutMs
                + ", retryCount=" + retryCount
                + '}';
    }
}
