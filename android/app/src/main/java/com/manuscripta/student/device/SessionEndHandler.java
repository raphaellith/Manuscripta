package com.manuscripta.student.device;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.manuscripta.student.data.model.SessionStatus;
import com.manuscripta.student.data.repository.SessionRepository;
import com.manuscripta.student.domain.model.Session;
import com.manuscripta.student.network.tcp.ConnectionState;
import com.manuscripta.student.network.tcp.TcpMessage;
import com.manuscripta.student.network.tcp.TcpMessageListener;
import com.manuscripta.student.network.tcp.TcpProtocolException;
import com.manuscripta.student.network.tcp.TcpSocketManager;
import com.manuscripta.student.network.tcp.message.UnpairMessage;

import java.util.List;

/**
 * Handles session end and cleanup when the device is unpaired or
 * when the teacher ends a session.
 *
 * <p>Per {@code Session Interaction.md} §5(7), sessions shall be
 * automatically transitioned to {@code CANCELLED} if the device
 * is deemed unpaired. Per §6(4)(a), on receipt of {@code UNPAIR}
 * (0x04), the Android device must terminate the TCP connection.</p>
 *
 * <p>Related requirements: CON6 (ability to end sessions for
 * selected students).</p>
 */
public class SessionEndHandler implements TcpMessageListener {

    /** Tag for logging. */
    private static final String TAG = "SessionEndHandler";

    /** The repository for session management. */
    private final SessionRepository sessionRepository;

    /** The TCP socket manager for connection management. */
    private final TcpSocketManager tcpSocketManager;

    /** Lock for thread-safe operations. */
    private final Object lock = new Object();

    /** Whether this handler is actively monitoring. */
    private boolean active;

    /** Listener for session end events. */
    @Nullable
    private SessionEndListener listener;

    /**
     * Listener interface for session end events.
     */
    public interface SessionEndListener {
        /**
         * Called when all active sessions have been ended.
         *
         * @param cancelledCount The number of sessions cancelled
         */
        void onSessionsEnded(int cancelledCount);

        /**
         * Called when the device has been unpaired by the teacher.
         */
        void onDeviceUnpaired();
    }

    /**
     * Creates a new SessionEndHandler.
     *
     * @param sessionRepository The repository for session management
     * @param tcpSocketManager  The TCP socket manager
     * @throws IllegalArgumentException if any parameter is null
     */
    public SessionEndHandler(@NonNull SessionRepository sessionRepository,
                             @NonNull TcpSocketManager tcpSocketManager) {
        if (sessionRepository == null) {
            throw new IllegalArgumentException(
                    "SessionRepository cannot be null");
        }
        if (tcpSocketManager == null) {
            throw new IllegalArgumentException(
                    "TcpSocketManager cannot be null");
        }
        this.sessionRepository = sessionRepository;
        this.tcpSocketManager = tcpSocketManager;
        this.active = false;
    }

    /**
     * Starts listening for session end events.
     */
    public void start() {
        synchronized (lock) {
            this.active = true;
        }
        tcpSocketManager.addMessageListener(this);
    }

    /**
     * Stops listening for session end events.
     */
    public void stop() {
        synchronized (lock) {
            this.active = false;
        }
        tcpSocketManager.removeMessageListener(this);
    }

    /**
     * Returns whether this handler is actively monitoring.
     *
     * @return true if active
     */
    public boolean isActive() {
        synchronized (lock) {
            return active;
        }
    }

    /**
     * Handles device unpairing: cancels all non-terminal sessions
     * and disconnects the TCP socket.
     *
     * @return The number of sessions cancelled
     */
    public int handleUnpair() {
        int cancelled = cancelAllActiveSessions();

        tcpSocketManager.disconnect();

        SessionEndListener currentListener;
        synchronized (lock) {
            currentListener = this.listener;
        }
        if (currentListener != null) {
            currentListener.onDeviceUnpaired();
        }

        Log.i(TAG, "Device unpaired. Cancelled " + cancelled + " sessions");
        return cancelled;
    }

    /**
     * Cancels all non-terminal sessions (RECEIVED, ACTIVE, PAUSED).
     * Per {@code Session Interaction.md} §5(7), sessions are
     * transitioned to CANCELLED when the device is unpaired.
     *
     * @return The number of sessions cancelled
     */
    public int cancelAllActiveSessions() {
        int cancelledCount = 0;

        SessionStatus[] nonTerminalStatuses = {
            SessionStatus.RECEIVED,
            SessionStatus.ACTIVE,
            SessionStatus.PAUSED
        };

        for (SessionStatus status : nonTerminalStatuses) {
            List<Session> sessions =
                    sessionRepository.getSessionsByStatus(status);
            for (Session session : sessions) {
                sessionRepository.endSession(session.getId(),
                        SessionStatus.CANCELLED);
                cancelledCount++;
            }
        }

        SessionEndListener currentListener;
        synchronized (lock) {
            currentListener = this.listener;
        }
        if (currentListener != null && cancelledCount > 0) {
            currentListener.onSessionsEnded(cancelledCount);
        }

        Log.d(TAG, "Cancelled " + cancelledCount + " active sessions");
        return cancelledCount;
    }

    /**
     * Sets the listener for session end events.
     *
     * @param listener The listener, or null to remove
     */
    public void setSessionEndListener(
            @Nullable SessionEndListener listener) {
        synchronized (lock) {
            this.listener = listener;
        }
    }

    /**
     * Removes the current session end listener.
     */
    public void removeSessionEndListener() {
        synchronized (lock) {
            this.listener = null;
        }
    }

    @Override
    public void onMessageReceived(@NonNull TcpMessage message) {
        synchronized (lock) {
            if (!active) {
                return;
            }
        }

        if (message instanceof UnpairMessage) {
            Log.i(TAG, "Received UNPAIR message from server");
            handleUnpair();
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
}
