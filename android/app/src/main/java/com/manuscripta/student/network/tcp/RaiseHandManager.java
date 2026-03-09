package com.manuscripta.student.network.tcp;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.manuscripta.student.network.tcp.message.HandAckMessage;
import com.manuscripta.student.network.tcp.message.HandRaisedMessage;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages the hand-raise lifecycle for requesting teacher attention.
 *
 * <p>Per Session Interaction §4A:
 * <ol>
 *   <li>Sends TCP HAND_RAISED (0x11) with device ID</li>
 *   <li>Enters a 3-second cooldown to prevent spamming</li>
 *   <li>Exposes observable state: IDLE or COOLDOWN</li>
 * </ol>
 */
@Singleton
public class RaiseHandManager implements TcpMessageListener {

    /** Tag for logging. */
    private static final String TAG = "RaiseHandManager";

    /** Cooldown duration in seconds to prevent spamming. */
    static final long COOLDOWN_SECONDS = 3L;

    /** The TCP socket manager for sending messages. */
    private final TcpSocketManager socketManager;

    /** The pairing manager for obtaining the device ID. */
    private final PairingManager pairingManager;

    /** Scheduler for cooldown logic. */
    private final ScheduledExecutorService scheduler;

    /** Whether the manager has been destroyed. */
    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    /** Observable hand-raise state. */
    private final MutableLiveData<HandRaiseState> state =
            new MutableLiveData<>(HandRaiseState.IDLE);

    /**
     * Possible states for the hand-raise lifecycle.
     */
    public enum HandRaiseState {
        /** No hand raised; button enabled. */
        IDLE,
        /** Hand raised; button disabled for cooldown. */
        COOLDOWN
    }

    /**
     * Constructor for RaiseHandManager with Hilt injection.
     *
     * @param socketManager  The TCP socket manager for sending messages
     * @param pairingManager The pairing manager providing the device ID
     */
    @Inject
    public RaiseHandManager(@NonNull TcpSocketManager socketManager,
                            @NonNull PairingManager pairingManager) {
        this(socketManager, pairingManager,
                Executors.newSingleThreadScheduledExecutor());
    }

    /**
     * Constructor allowing injection of a custom scheduler for testing.
     *
     * @param socketManager  The TCP socket manager for sending messages
     * @param pairingManager The pairing manager providing the device ID
     * @param scheduler      The scheduler for cooldown logic
     */
    @VisibleForTesting
    RaiseHandManager(@NonNull TcpSocketManager socketManager,
                     @NonNull PairingManager pairingManager,
                     @NonNull ScheduledExecutorService scheduler) {
        this.socketManager = socketManager;
        this.pairingManager = pairingManager;
        this.scheduler = scheduler;
        this.socketManager.addMessageListener(this);
    }

    /**
     * Gets the observable hand-raise state.
     *
     * @return LiveData containing the current hand-raise state
     */
    @NonNull
    public LiveData<HandRaiseState> getState() {
        return state;
    }

    /**
     * Raises the student's hand, sending HAND_RAISED via TCP.
     * Enters a 3-second cooldown to prevent spamming.
     */
    public void raiseHand() {
        if (destroyed.get()) {
            return;
        }
        HandRaiseState current = state.getValue();
        if (current == HandRaiseState.COOLDOWN) {
            return;
        }
        state.postValue(HandRaiseState.COOLDOWN);
        scheduler.execute(this::sendHandRaised);
        scheduler.schedule(
                () -> state.postValue(HandRaiseState.IDLE),
                COOLDOWN_SECONDS,
                TimeUnit.SECONDS
        );
    }

    /**
     * Cleans up resources. After this call, the manager will not
     * send any further messages.
     */
    public void destroy() {
        destroyed.set(true);
        scheduler.shutdownNow();
    }

    @Override
    public void onMessageReceived(@NonNull TcpMessage message) {
        if (destroyed.get()) {
            return;
        }
        if (message instanceof HandAckMessage) {
            Log.d(TAG, "Received HAND_ACK");
        }
    }

    @Override
    public void onConnectionStateChanged(@NonNull ConnectionState connectionState) {
        // No action needed on connection state changes
    }

    @Override
    public void onError(@NonNull TcpProtocolException error) {
        Log.w(TAG, "TCP error: " + error.getMessage());
    }

    /**
     * Sends a HAND_RAISED message via TCP.
     */
    private void sendHandRaised() {
        String deviceId = pairingManager.getDeviceId();
        if (deviceId == null || deviceId.isEmpty()) {
            Log.w(TAG, "Cannot raise hand: no device ID");
            return;
        }
        try {
            socketManager.send(new HandRaisedMessage(deviceId));
            Log.d(TAG, "Sent HAND_RAISED");
        } catch (IOException | TcpProtocolException e) {
            Log.w(TAG, "Failed to send HAND_RAISED: " + e.getMessage());
        }
    }
}
