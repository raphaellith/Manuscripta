package com.manuscripta.student.network.tcp;

import androidx.annotation.NonNull;

/**
 * Callback interface for pairing handshake events.
 *
 * <p>Implementations receive notifications about pairing progress and outcomes.
 * Callbacks are invoked on the main (UI) thread for safe UI updates.
 *
 * @see PairingManager
 */
public interface PairingCallback {

    /**
     * Called when the TCP portion of the pairing handshake succeeds.
     *
     * <p>This indicates that:
     * <ol>
     *   <li>TCP connection was established</li>
     *   <li>PAIRING_REQUEST was sent</li>
     *   <li>PAIRING_ACK was received from server</li>
     * </ol>
     *
     * <p>Note: Full pairing also requires HTTP registration to succeed.
     * This callback only indicates TCP pairing success.
     */
    void onTcpPairingSuccess();

    /**
     * Called when pairing fails due to an error.
     *
     * @param reason A human-readable description of the failure reason.
     */
    void onPairingFailed(@NonNull String reason);

    /**
     * Called when pairing times out waiting for server response.
     *
     * <p>This typically means the PAIRING_ACK was not received
     * within the configured timeout period.
     */
    void onPairingTimeout();
}
