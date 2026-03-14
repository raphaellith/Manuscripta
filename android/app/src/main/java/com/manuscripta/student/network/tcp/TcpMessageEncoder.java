package com.manuscripta.student.network.tcp;

import androidx.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Encoder for TCP messages.
 * Serialises {@link TcpMessage} objects to byte arrays according to the binary protocol format.
 *
 * <p>Protocol format:
 * <pre>
 * [1 byte: opcode][N bytes: operand]
 * </pre>
 *
 * <p>The opcode is always 1 byte. The operand length varies by message type and may be empty.
 */
@Singleton
public final class TcpMessageEncoder {

    /**
     * Creates a new TcpMessageEncoder.
     */
    @Inject
    public TcpMessageEncoder() {
        // Empty constructor for Hilt injection
    }

    /**
     * Encodes a TCP message to a byte array.
     *
     * <p>The resulting byte array contains the opcode as the first byte,
     * followed by the operand bytes (if any).
     *
     * @param message The message to encode.
     * @return The encoded byte array.
     * @throws TcpProtocolException If the message is null.
     */
    @NonNull
    public byte[] encode(@NonNull TcpMessage message) throws TcpProtocolException {
        if (message == null) {
            throw new TcpProtocolException(
                    TcpProtocolException.ErrorType.NULL_MESSAGE,
                    "Cannot encode null message");
        }

        byte[] operand = message.getOperand();

        // For messages with no operand, just return the opcode
        if (operand.length == 0) {
            return new byte[]{message.getOpcode().getValue()};
        }

        // For messages with operand, combine opcode + operand
        byte[] result = new byte[1 + operand.length];
        result[0] = message.getOpcode().getValue();
        System.arraycopy(operand, 0, result, 1, operand.length);

        return result;
    }
}
