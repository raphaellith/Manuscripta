package com.manuscripta.student.network.tcp.message;

import androidx.annotation.NonNull;

import com.manuscripta.student.network.tcp.TcpMessage;
import com.manuscripta.student.network.tcp.TcpOpcode;

import java.nio.charset.StandardCharsets;

/**
 * TCP message initiating the pairing handshake with the server.
 * Sent from client to server with opcode 0x20.
 *
 * <p>The operand is the device ID as a UTF-8 encoded string,
 * which the server uses to identify and register the device.
 *
 * <p>This is part of the TCP pairing handshake as specified in
 * Pairing Process.md Section 2:
 * <ol>
 *   <li>Client sends PAIRING_REQUEST with device ID</li>
 *   <li>Server responds with PAIRING_ACK on success</li>
 * </ol>
 */
public final class PairingRequestMessage extends TcpMessage {

    /**
     * The device ID for pairing.
     */
    private final String deviceId;

    /**
     * The encoded operand bytes.
     */
    private final byte[] operand;

    /**
     * Creates a new PairingRequestMessage with the specified device ID.
     *
     * @param deviceId The device ID to register with the server.
     */
    public PairingRequestMessage(@NonNull String deviceId) {
        super(TcpOpcode.PAIRING_REQUEST);
        this.deviceId = deviceId;
        this.operand = deviceId.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Returns the device ID for pairing.
     *
     * @return The device ID.
     */
    @NonNull
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Returns the UTF-8 encoded device ID as the operand.
     *
     * @return The encoded operand bytes.
     */
    @NonNull
    @Override
    public byte[] getOperand() {
        return operand.clone();
    }

    /**
     * Returns a string representation of this message.
     *
     * @return A string containing the message type and device ID.
     */
    @NonNull
    @Override
    public String toString() {
        return "PairingRequestMessage{deviceId=" + deviceId + "}";
    }
}
