package com.manuscripta.student.device;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.manuscripta.student.network.tcp.ConnectionState;
import com.manuscripta.student.network.tcp.TcpMessage;
import com.manuscripta.student.network.tcp.TcpMessageListener;
import com.manuscripta.student.network.tcp.TcpProtocolException;
import com.manuscripta.student.network.tcp.TcpSocketManager;
import com.manuscripta.student.network.tcp.message.HandAckMessage;
import com.manuscripta.student.network.tcp.message.HandRaisedMessage;

import java.io.IOException;

/**
 * Manages the "help needed" / hand raise functionality.
 *
 * <p>Per {@code Session Interaction.md} §4A, the Android client signals
 * that its user needs help by sending a TCP {@code HAND_RAISED} (0x11)
 * message. If no {@code HAND_ACK} (0x06) is received within
 * {@link #RETRY_INTERVAL_MS}, the message should be resent.</p>
 *
 * <p>Related requirements: MAT7 (Student) — "Raise Hand" button.</p>
 */
public class HelpRequestManager implements TcpMessageListener {

    /** Tag for logging. */
    private static final String TAG = "HelpRequestManager";

    /**
     * Retry interval in milliseconds for resending HAND_RAISED if not
     * acknowledged, per {@code Session Interaction.md} §4A(3).
     */
    public static final long RETRY_INTERVAL_MS = 3000L;

    /** The TCP socket manager for sending messages. */
    private final TcpSocketManager tcpSocketManager;

    /** Lock for thread-safe operations. */
    private final Object lock = new Object();

    /** Whether the hand is currently raised. */
    private boolean handRaised;

    /** Whether the server has acknowledged the hand raise. */
    private boolean acknowledged;

    /** Listener for help request events. */
    @Nullable
    private HelpRequestListener listener;

    /**
     * Listener interface for help request events.
     */
    public interface HelpRequestListener {
        /**
         * Called when the server acknowledges the help request.
         */
        void onHelpAcknowledged();
    }

    /**
     * Creates a new HelpRequestManager.
     *
     * @param tcpSocketManager The TCP socket manager for message sending
     * @throws IllegalArgumentException if tcpSocketManager is null
     */
    public HelpRequestManager(@NonNull TcpSocketManager tcpSocketManager) {
        if (tcpSocketManager == null) {
            throw new IllegalArgumentException(
                    "TcpSocketManager cannot be null");
        }
        this.tcpSocketManager = tcpSocketManager;
        this.handRaised = false;
        this.acknowledged = false;
        tcpSocketManager.addMessageListener(this);
    }

    /**
     * Raises the student's hand to request help from the teacher.
     * Sends a {@code HAND_RAISED} TCP message with the device ID.
     *
     * @param deviceId The device ID of the student
     * @return true if the message was sent successfully, false otherwise
     * @throws IllegalArgumentException if deviceId is null or empty
     */
    public boolean raiseHand(@NonNull String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Device ID cannot be null or empty");
        }

        synchronized (lock) {
            if (handRaised && acknowledged) {
                return true;
            }

            try {
                HandRaisedMessage message =
                        new HandRaisedMessage(deviceId);
                tcpSocketManager.send(message);
                handRaised = true;
                acknowledged = false;
                Log.d(TAG, "Hand raised for device " + deviceId);
                return true;
            } catch (IOException e) {
                Log.e(TAG,
                        "Failed to send hand raised: " + e.getMessage());
                return false;
            } catch (TcpProtocolException e) {
                Log.e(TAG,
                        "Protocol error raising hand: " + e.getMessage());
                return false;
            }
        }
    }

    /**
     * Lowers the student's hand, cancelling the help request.
     */
    public void lowerHand() {
        synchronized (lock) {
            handRaised = false;
            acknowledged = false;
        }
    }

    /**
     * Returns whether the hand is currently raised.
     *
     * @return true if the hand is raised
     */
    public boolean isHandRaised() {
        synchronized (lock) {
            return handRaised;
        }
    }

    /**
     * Returns whether the server has acknowledged the hand raise.
     *
     * @return true if acknowledged
     */
    public boolean isAcknowledged() {
        synchronized (lock) {
            return acknowledged;
        }
    }

    /**
     * Returns the retry interval for resending unacknowledged requests.
     *
     * @return The retry interval in milliseconds
     */
    public long getRetryIntervalMs() {
        return RETRY_INTERVAL_MS;
    }

    /**
     * Sets the listener for help request events.
     *
     * @param listener The listener, or null to remove
     */
    public void setHelpRequestListener(
            @Nullable HelpRequestListener listener) {
        synchronized (lock) {
            this.listener = listener;
        }
    }

    /**
     * Removes the current help request listener.
     */
    public void removeHelpRequestListener() {
        synchronized (lock) {
            this.listener = null;
        }
    }

    /**
     * Cleans up resources. Should be called when no longer needed.
     */
    public void destroy() {
        tcpSocketManager.removeMessageListener(this);
        lowerHand();
    }

    @Override
    public void onMessageReceived(@NonNull TcpMessage message) {
        if (message instanceof HandAckMessage) {
            synchronized (lock) {
                if (handRaised) {
                    acknowledged = true;
                    Log.d(TAG, "Hand raise acknowledged by server");
                    HelpRequestListener currentListener = this.listener;
                    if (currentListener != null) {
                        currentListener.onHelpAcknowledged();
                    }
                }
            }
        }
    }

    @Override
    public void onConnectionStateChanged(
            @NonNull ConnectionState state) {
        // Connection state changes handled by ConnectionTracker
    }

    @Override
    public void onError(@NonNull TcpProtocolException error) {
        // Errors are logged by TcpSocketManager
    }
}
