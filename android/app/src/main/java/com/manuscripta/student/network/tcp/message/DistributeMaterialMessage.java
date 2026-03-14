package com.manuscripta.student.network.tcp.message;

import androidx.annotation.NonNull;

import com.manuscripta.student.network.tcp.TcpMessage;
import com.manuscripta.student.network.tcp.TcpOpcode;

/**
 * TCP message instructing the student device to fetch new materials via HTTP.
 * Sent from server to client with opcode 0x05.
 *
 * <p>This message is part of the material distribution pattern:
 * <ol>
 *   <li>Server sends DISTRIBUTE_MATERIAL to notify client</li>
 *   <li>Client initiates HTTP GET /materials to fetch content</li>
 *   <li>Client sends DISTRIBUTE_ACK to confirm receipt</li>
 * </ol>
 *
 * <p>This message has no operand payload.
 */
public final class DistributeMaterialMessage extends TcpMessage {

    /**
     * Empty operand array for messages with no payload.
     */
    private static final byte[] EMPTY_OPERAND = new byte[0];

    /**
     * Creates a new DistributeMaterialMessage.
     */
    public DistributeMaterialMessage() {
        super(TcpOpcode.DISTRIBUTE_MATERIAL);
    }

    /**
     * Returns an empty operand as this message has no payload.
     *
     * @return An empty byte array.
     */
    @NonNull
    @Override
    public byte[] getOperand() {
        return EMPTY_OPERAND;
    }
}
