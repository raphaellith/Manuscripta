package com.manuscripta.student.network.tcp;

import androidx.annotation.NonNull;

/**
 * TCP message acknowledging a successful pairing.
 * Sent from server to client with opcode 0x21.
 *
 * <p>This message is the server's response to a PAIRING_REQUEST,
 * confirming that the device has been successfully registered.
 *
 * <p>This message has no operand payload.
 */
public final class PairingAckMessage extends TcpMessage {

    /**
     * Empty operand array for messages with no payload.
     */
    private static final byte[] EMPTY_OPERAND = new byte[0];

    /**
     * Creates a new PairingAckMessage.
     */
    public PairingAckMessage() {
        super(TcpOpcode.PAIRING_ACK);
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
