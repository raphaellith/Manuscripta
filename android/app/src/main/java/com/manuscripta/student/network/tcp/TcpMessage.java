package com.manuscripta.student.network.tcp;

import androidx.annotation.NonNull;

/**
 * Abstract base class for all TCP messages in the protocol.
 * Each message has an opcode identifying its type and an optional operand payload.
 *
 * <p>Message structure as defined in API Contract Section 3:
 * <ul>
 *   <li>1 byte: opcode</li>
 *   <li>Variable length: operand (may be empty)</li>
 * </ul>
 */
public abstract class TcpMessage {

    /**
     * The opcode identifying this message type.
     */
    private final TcpOpcode opcode;

    /**
     * Creates a new TcpMessage with the specified opcode.
     *
     * @param opcode The opcode for this message type.
     */
    protected TcpMessage(@NonNull TcpOpcode opcode) {
        this.opcode = opcode;
    }

    /**
     * Returns the opcode for this message.
     *
     * @return The message opcode.
     */
    @NonNull
    public TcpOpcode getOpcode() {
        return opcode;
    }

    /**
     * Returns the operand payload for this message.
     * Messages with no operand should return an empty byte array.
     *
     * @return The operand bytes, never null.
     */
    @NonNull
    public abstract byte[] getOperand();

    /**
     * Checks if this message has an operand payload.
     *
     * @return true if the message has a non-empty operand, false otherwise.
     */
    public boolean hasOperand() {
        return getOperand().length > 0;
    }

    /**
     * Returns a string representation of this message.
     *
     * @return A string containing the message type and opcode.
     */
    @NonNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{opcode=" + opcode + "}";
    }
}
