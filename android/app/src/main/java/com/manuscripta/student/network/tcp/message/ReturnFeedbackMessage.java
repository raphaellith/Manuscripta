package com.manuscripta.student.network.tcp.message;

import androidx.annotation.NonNull;

import com.manuscripta.student.network.tcp.TcpMessage;
import com.manuscripta.student.network.tcp.TcpOpcode;

/**
 * TCP message instructing the student device to fetch feedback via HTTP.
 * Sent from server to client with opcode 0x07.
 *
 * <p>This message is part of the feedback retrieval pattern:
 * <ol>
 *   <li>Server sends RETURN_FEEDBACK to notify client</li>
 *   <li>Client initiates HTTP GET /feedback/{deviceId} to fetch feedback</li>
 *   <li>Client sends FEEDBACK_ACK to confirm receipt</li>
 * </ol>
 *
 * <p>This message has no operand payload.
 */
public final class ReturnFeedbackMessage extends TcpMessage {

    /**
     * Empty operand array for messages with no payload.
     */
    private static final byte[] EMPTY_OPERAND = new byte[0];

    /**
     * Creates a new ReturnFeedbackMessage.
     */
    public ReturnFeedbackMessage() {
        super(TcpOpcode.RETURN_FEEDBACK);
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
