package com.manuscripta.student.network.tcp.message;

import androidx.annotation.NonNull;

import com.manuscripta.student.network.tcp.TcpMessage;
import com.manuscripta.student.network.tcp.TcpOpcode;

import java.nio.charset.StandardCharsets;

/**
 * TCP message confirming the client received distributed materials.
 * Sent from client to server with opcode 0x12.
 *
 * <p>The operand is the device ID as a UTF-8 encoded string,
 * allowing the server to track which devices have received materials.
 *
 * <p>This message completes the material distribution handshake:
 * <ol>
 *   <li>Server sends DISTRIBUTE_MATERIAL</li>
 *   <li>Client fetches materials via HTTP</li>
 *   <li>Client sends DISTRIBUTE_ACK to confirm</li>
 * </ol>
 */
public final class DistributeAckMessage extends TcpMessage {

    /**
     * The device ID of the client acknowledging material receipt.
     */
    private final String deviceId;

    /**
     * The encoded operand bytes.
     */
    private final byte[] operand;

    /**
     * Creates a new DistributeAckMessage with the specified device ID.
     *
     * @param deviceId The device ID of the client.
     */
    public DistributeAckMessage(@NonNull String deviceId) {
        super(TcpOpcode.DISTRIBUTE_ACK);
        this.deviceId = deviceId;
        this.operand = deviceId.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Returns the device ID of the client.
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
        return "DistributeAckMessage{deviceId=" + deviceId + "}";
    }
}
