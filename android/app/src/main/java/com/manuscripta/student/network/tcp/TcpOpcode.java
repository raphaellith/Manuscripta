package com.manuscripta.student.network.tcp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Enumeration of TCP message opcodes as defined in the API Contract Section 3.
 * Each opcode represents a specific message type in the TCP protocol.
 */
public enum TcpOpcode {

    /**
     * Lock screen command from server to client.
     * Instructs the student device to lock its screen.
     */
    LOCK_SCREEN((byte) 0x01),

    /**
     * Unlock screen command from server to client.
     * Instructs the student device to unlock its screen.
     */
    UNLOCK_SCREEN((byte) 0x02),

    /**
     * Refresh configuration command from server to client.
     * Instructs the student device to refresh its configuration.
     */
    REFRESH_CONFIG((byte) 0x03),

    /**
     * Unpair command from server to client.
     * Instructs the student device to disconnect and clear pairing state.
     */
    UNPAIR((byte) 0x04),

    /**
     * Distribute material command from server to client.
     * Instructs the student device to fetch new materials via HTTP.
     */
    DISTRIBUTE_MATERIAL((byte) 0x05),

    /**
     * Hand acknowledgement from server to client.
     * Confirms the server received the hand raised notification.
     * Operand: Device ID (UTF-8).
     */
    HAND_ACK((byte) 0x06),

    /**
     * Return feedback command from server to client.
     * Instructs the student device to fetch feedback via HTTP.
     */
    RETURN_FEEDBACK((byte) 0x07),

    /**
     * Status update message from client to server.
     * Contains device status information as JSON.
     */
    STATUS_UPDATE((byte) 0x10),

    /**
     * Hand raised notification from client to server.
     * Indicates the student has raised their hand.
     * Operand: Device ID (UTF-8).
     */
    HAND_RAISED((byte) 0x11),

    /**
     * Distribute acknowledgement from client to server.
     * Confirms the client received the distributed materials.
     * Operand: Device ID (UTF-8).
     */
    DISTRIBUTE_ACK((byte) 0x12),

    /**
     * Feedback acknowledgement from client to server.
     * Confirms the client received the feedback.
     * Operand: Device ID (UTF-8).
     */
    FEEDBACK_ACK((byte) 0x13),

    /**
     * Pairing request from client to server.
     * Initiates the pairing handshake with device ID.
     */
    PAIRING_REQUEST((byte) 0x20),

    /**
     * Pairing acknowledgment from server to client.
     * Confirms successful pairing.
     */
    PAIRING_ACK((byte) 0x21);

    /**
     * The byte value of this opcode.
     */
    private final byte value;

    /**
     * Creates a new TcpOpcode with the specified byte value.
     *
     * @param value The byte value for this opcode.
     */
    TcpOpcode(byte value) {
        this.value = value;
    }

    /**
     * Returns the byte value of this opcode.
     *
     * @return The byte value.
     */
    public byte getValue() {
        return value;
    }

    /**
     * Looks up a TcpOpcode by its byte value.
     *
     * @param value The byte value to look up.
     * @return The corresponding TcpOpcode, or null if not found.
     */
    @Nullable
    public static TcpOpcode fromValue(byte value) {
        for (TcpOpcode opcode : values()) {
            if (opcode.value == value) {
                return opcode;
            }
        }
        return null;
    }

    /**
     * Checks if this opcode represents a server-to-client message.
     *
     * @return true if this is a server-to-client opcode, false otherwise.
     */
    public boolean isServerToClient() {
        return this == LOCK_SCREEN
                || this == UNLOCK_SCREEN
                || this == REFRESH_CONFIG
                || this == UNPAIR
                || this == DISTRIBUTE_MATERIAL
                || this == HAND_ACK
                || this == RETURN_FEEDBACK
                || this == PAIRING_ACK;
    }

    /**
     * Checks if this opcode represents a client-to-server message.
     *
     * @return true if this is a client-to-server opcode, false otherwise.
     */
    public boolean isClientToServer() {
        return this == STATUS_UPDATE
                || this == HAND_RAISED
                || this == DISTRIBUTE_ACK
                || this == FEEDBACK_ACK
                || this == PAIRING_REQUEST;
    }

    /**
     * Returns a string representation of this opcode.
     *
     * @return A string containing the opcode name and hex value.
     */
    @NonNull
    @Override
    public String toString() {
        return name() + "(0x" + String.format("%02X", value) + ")";
    }
}
