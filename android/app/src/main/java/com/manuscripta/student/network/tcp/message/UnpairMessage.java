package com.manuscripta.student.network.tcp.message;

import androidx.annotation.NonNull;

import com.manuscripta.student.network.tcp.TcpMessage;
import com.manuscripta.student.network.tcp.TcpOpcode;

/**
 * TCP message instructing the student device to disconnect and clear pairing state.
 * Sent from server to client with opcode 0x04.
 *
 * <p>When received, the client should:
 * <ol>
 *   <li>Close the TCP connection</li>
 *   <li>Clear stored pairing information</li>
 *   <li>Return to the discovery/pairing screen</li>
 * </ol>
 *
 * <p>This message has no operand payload.
 */
public final class UnpairMessage extends TcpMessage {

    /**
     * Empty operand array for messages with no payload.
     */
    private static final byte[] EMPTY_OPERAND = new byte[0];

    /**
     * Creates a new UnpairMessage.
     */
    public UnpairMessage() {
        super(TcpOpcode.UNPAIR);
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
