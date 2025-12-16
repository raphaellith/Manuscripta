package com.manuscripta.student.network.tcp;

/**
 * Represents the pairing state of the device with the teacher server.
 *
 * <p>Pairing requires successful handshakes on both TCP and HTTP channels.
 * See {@code Pairing Process.md} Section 2 for the full pairing sequence.
 */
public enum PairingState {
    /**
     * Device is not paired with any server.
     * This is the initial state and the state after unpairing.
     */
    NOT_PAIRED,

    /**
     * Pairing is in progress.
     * TCP and/or HTTP handshakes are being performed.
     */
    PAIRING_IN_PROGRESS,

    /**
     * Device is successfully paired with the teacher server.
     * Both TCP and HTTP handshakes completed successfully.
     */
    PAIRED,

    /**
     * Pairing failed due to an error or timeout.
     * The device should retry pairing or show an error to the user.
     */
    PAIRING_FAILED,

    /**
     * Pairing timed out waiting for server response.
     * This is a specific failure case that may warrant a retry.
     */
    PAIRING_TIMEOUT
}
