package com.manuscripta.student.network.tcp.message;

import androidx.annotation.NonNull;

import com.manuscripta.student.network.tcp.TcpMessage;
import com.manuscripta.student.network.tcp.TcpOpcode;

import java.nio.charset.StandardCharsets;

/**
 * TCP message confirming the client received a single distributed material.
 * Sent from client to server with opcode 0x12.
 *
 * <p>Per API Contract §3.6.2, one message is sent per material. The operand
 * encodes both the device ID and the material ID, separated by a null byte:
 * <pre>
 * [Device ID (UTF-8)] + [0x00] + [Material ID (UTF-8)]
 * </pre>
 *
 * <p>This message completes the material distribution handshake:
 * <ol>
 *   <li>Server sends DISTRIBUTE_MATERIAL</li>
 *   <li>Client fetches materials via HTTP</li>
 *   <li>Client sends one DISTRIBUTE_ACK per material to confirm</li>
 * </ol>
 */
public final class DistributeAckMessage extends TcpMessage {

    /** The device ID of the client acknowledging material receipt. */
    private final String deviceId;

    /** The ID of the material being acknowledged. */
    private final String materialId;

    /** The encoded operand bytes. */
    private final byte[] operand;

    /**
     * Creates a new DistributeAckMessage for a specific material.
     *
     * @param deviceId   The device ID of the client.
     * @param materialId The ID of the material being acknowledged.
     */
    public DistributeAckMessage(@NonNull String deviceId, @NonNull String materialId) {
        super(TcpOpcode.DISTRIBUTE_ACK);
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID cannot be null or empty");
        }
        if (materialId == null || materialId.trim().isEmpty()) {
            throw new IllegalArgumentException("Material ID cannot be null or empty");
        }
        this.deviceId = deviceId;
        this.materialId = materialId;

        byte[] deviceBytes = deviceId.getBytes(StandardCharsets.UTF_8);
        byte[] materialBytes = materialId.getBytes(StandardCharsets.UTF_8);
        this.operand = new byte[deviceBytes.length + 1 + materialBytes.length];
        System.arraycopy(deviceBytes, 0, this.operand, 0, deviceBytes.length);
        this.operand[deviceBytes.length] = 0x00;
        System.arraycopy(materialBytes, 0, this.operand, deviceBytes.length + 1,
                materialBytes.length);
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
     * Returns the ID of the material being acknowledged.
     *
     * @return The material ID.
     */
    @NonNull
    public String getMaterialId() {
        return materialId;
    }

    /**
     * Returns the encoded operand containing device ID and material ID
     * separated by a null byte.
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
     * @return A string containing the message type, device ID, and material ID.
     */
    @NonNull
    @Override
    public String toString() {
        return "DistributeAckMessage{deviceId=" + deviceId
                + ", materialId=" + materialId + "}";
    }
}
