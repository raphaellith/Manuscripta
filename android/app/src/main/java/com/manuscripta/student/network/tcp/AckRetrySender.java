package com.manuscripta.student.network.tcp;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.io.IOException;

/**
 * Sends a {@link TcpMessage} with retry logic for transient failures.
 *
 * <p>Both {@code FEEDBACK_ACK} and {@code DISTRIBUTE_ACK} share the same retry
 * policy: up to {@link #MAX_ATTEMPTS} attempts with a fixed
 * {@link #RETRY_DELAY_MS} delay between them. The server allows a 30-second
 * window for ACK receipt, so the total maximum sleep time for a single
 * message ({@code (MAX_ATTEMPTS - 1) * RETRY_DELAY_MS} = 1 000 ms) is well
 * within bounds. When many ACKs are sent sequentially, their cumulative retry
 * delays may exceed 30 seconds; callers should schedule ACK sending
 * appropriately (for example, asynchronously or with a shared deadline).</p>
 *
 * <p>If the current thread is interrupted during a retry delay, the retry loop
 * aborts immediately and the interrupt flag is preserved, consistent with the
 * behaviour of {@link com.manuscripta.student.network.interceptor.RetryInterceptor}.</p>
 */
public class AckRetrySender {

    /** Maximum number of send attempts. */
    @VisibleForTesting
    static final int MAX_ATTEMPTS = 3;

    /** Delay in milliseconds between retry attempts. */
    @VisibleForTesting
    static final long RETRY_DELAY_MS = 500L;

    /** The TCP socket manager used to send messages. */
    private final TcpSocketManager tcpSocketManager;

    /**
     * Creates a new AckRetrySender.
     *
     * @param tcpSocketManager The TCP socket manager used to send messages
     */
    public AckRetrySender(@NonNull TcpSocketManager tcpSocketManager) {
        this.tcpSocketManager = tcpSocketManager;
    }

    /**
     * Sends the given message, retrying up to {@link #MAX_ATTEMPTS} times on
     * {@link TcpProtocolException} or {@link IOException}.
     *
     * @param message The ACK message to send
     * @param tag     The logging tag of the caller (used for log messages)
     */
    public void send(@NonNull TcpMessage message, @NonNull String tag) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                tcpSocketManager.send(message);
                Log.d(tag, "Sent " + message.getClass().getSimpleName()
                        + " (attempt " + attempt + ")");
                return;
            } catch (TcpProtocolException | IOException e) {
                if (attempt < MAX_ATTEMPTS) {
                    Log.w(tag, message.getClass().getSimpleName()
                            + " attempt " + attempt + " failed, retrying: "
                            + e.getMessage());
                    if (!sleep(RETRY_DELAY_MS)) {
                        // Thread was interrupted — abort retries
                        Log.w(tag, "Retry interrupted, aborting "
                                + message.getClass().getSimpleName());
                        return;
                    }
                } else {
                    Log.e(tag, "Failed to send " + message.getClass().getSimpleName()
                            + " after " + MAX_ATTEMPTS + " attempts: "
                            + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Sleeps for the specified duration.
     *
     * <p>This method is protected so that tests can override it to a no-op,
     * avoiding real delays in unit tests.</p>
     *
     * @param millis The duration to sleep in milliseconds
     * @return {@code true} if the sleep completed normally,
     *         {@code false} if the thread was interrupted
     */
    @VisibleForTesting
    protected boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
