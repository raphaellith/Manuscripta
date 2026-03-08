package com.manuscripta.student.ui.pairing;

/**
 * Represents the high-level phases of the full pairing flow.
 *
 * <p>Per Pairing Process §2, pairing proceeds through discovery,
 * TCP handshake, and HTTP registration. This enum tracks which
 * phase is currently active for the UI.</p>
 */
public enum PairingPhase {

    /** Not yet started or cancelled. */
    IDLE,

    /** Listening for UDP discovery broadcast from the teacher server. */
    DISCOVERING,

    /** TCP connection and PAIRING_REQUEST/ACK handshake in progress. */
    TCP_PAIRING,

    /** HTTP POST /pair registration in progress. */
    HTTP_REGISTERING,

    /** All three phases completed successfully. */
    PAIRED,

    /** An error occurred during any phase. */
    ERROR
}
