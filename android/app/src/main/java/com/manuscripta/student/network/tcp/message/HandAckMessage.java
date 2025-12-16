package com.manuscripta.student.network.tcp.message;

import androidx.annotation.NonNull;

import com.manuscripta.student.network.tcp.TcpMessage;
import com.manuscripta.student.network.tcp.TcpOpcode;

import java.nio.charset.StandardCharsets;

/**
 * TCP message acknowledging the server received a hand raised notification.
 * Sent from server to client with opcode 0x06.
 *
 * <p>The operand is the device ID as a UTF-8 encoded string,
 * confirming which device's hand raise was acknowledged.
 */
public final class HandAckMessage extends TcpMessage {

    /**
     * The device ID of the student whose hand raise was acknowledged.
     */
    private final String deviceId;

    /**
     * The encoded operand bytes.
     */
    private final byte[] operand;

    /**
     * Creates a new HandAckMessage with the specified device ID.
     *
     * @param deviceId The device ID of the student.
     */
    public HandAckMessage(@NonNull String deviceId) {
        super(TcpOpcode.HAND_ACK);
        this.deviceId = deviceId;
        this.operand = deviceId.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Returns the device ID of the student.
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
        return "HandAckMessage{deviceId=" + deviceId + "}";
    }
}
