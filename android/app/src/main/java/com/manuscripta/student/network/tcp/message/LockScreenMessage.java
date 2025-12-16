package com.manuscripta.student.network.tcp.message;

import androidx.annotation.NonNull;

import com.manuscripta.student.network.tcp.TcpMessage;
import com.manuscripta.student.network.tcp.TcpOpcode;

/**
 * TCP message instructing the student device to lock its screen.
 * Sent from server to client with opcode 0x01.
 *
 * <p>This message has no operand payload.
 */
public final class LockScreenMessage extends TcpMessage {

    /**
     * Empty operand array for messages with no payload.
     */
    private static final byte[] EMPTY_OPERAND = new byte[0];

    /**
     * Creates a new LockScreenMessage.
     */
    public LockScreenMessage() {
        super(TcpOpcode.LOCK_SCREEN);
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
