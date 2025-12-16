package com.manuscripta.student.network.tcp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Exception thrown when there is an error in the TCP protocol.
 * This includes errors such as unknown opcodes, malformed data,
 * or invalid message formats.
 */
public final class TcpProtocolException extends Exception {

    /**
     * The type of protocol error that occurred.
     */
    private final ErrorType errorType;

    /**
     * Optional invalid opcode value for unknown opcode errors.
     */
    private final Byte invalidOpcode;

    /**
     * Enumeration of possible protocol error types.
     */
    public enum ErrorType {
        /**
         * The message data is empty or null.
         */
        EMPTY_DATA,

        /**
         * The opcode byte is not a recognized opcode.
         */
        UNKNOWN_OPCODE,

        /**
         * The message data is malformed or corrupted.
         */
        MALFORMED_DATA,

        /**
         * The message is null.
         */
        NULL_MESSAGE
    }

    /**
     * Creates a new TcpProtocolException with the specified error type and message.
     *
     * @param errorType The type of protocol error.
     * @param message   A descriptive error message.
     */
    public TcpProtocolException(@NonNull ErrorType errorType, @NonNull String message) {
        super(message);
        this.errorType = errorType;
        this.invalidOpcode = null;
    }

    /**
     * Creates a new TcpProtocolException for an unknown opcode error.
     *
     * @param invalidOpcode The unrecognized opcode value.
     */
    public TcpProtocolException(byte invalidOpcode) {
        super(String.format("Unknown opcode: 0x%02X", invalidOpcode));
        this.errorType = ErrorType.UNKNOWN_OPCODE;
        this.invalidOpcode = invalidOpcode;
    }

    /**
     * Returns the type of protocol error.
     *
     * @return The error type.
     */
    @NonNull
    public ErrorType getErrorType() {
        return errorType;
    }

    /**
     * Returns the invalid opcode value if this is an unknown opcode error.
     *
     * @return The invalid opcode, or null if not applicable.
     */
    @Nullable
    public Byte getInvalidOpcode() {
        return invalidOpcode;
    }

    /**
     * Returns a string representation of this exception.
     *
     * @return A string containing the error type and message.
     */
    @NonNull
    @Override
    public String toString() {
        if (invalidOpcode != null) {
            return String.format("TcpProtocolException{type=%s, opcode=0x%02X}",
                    errorType, invalidOpcode);
        }
        return String.format("TcpProtocolException{type=%s, message=%s}",
                errorType, getMessage());
    }
}
