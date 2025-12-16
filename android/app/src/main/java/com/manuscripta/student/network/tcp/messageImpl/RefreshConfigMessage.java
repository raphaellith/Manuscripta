package com.manuscripta.student.network.tcp;

import androidx.annotation.NonNull;

/**
 * TCP message instructing the student device to refresh its configuration.
 * Sent from server to client with opcode 0x03.
 *
 * <p>This message has no operand payload.
 */
public final class RefreshConfigMessage extends TcpMessage {

    /**
     * Empty operand array for messages with no payload.
     */
    private static final byte[] EMPTY_OPERAND = new byte[0];

    /**
     * Creates a new RefreshConfigMessage.
     */
    public RefreshConfigMessage() {
        super(TcpOpcode.REFRESH_CONFIG);
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
