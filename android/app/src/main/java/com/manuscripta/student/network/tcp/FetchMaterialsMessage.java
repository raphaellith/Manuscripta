package com.manuscripta.student.network.tcp;

import androidx.annotation.NonNull;

/**
 * TCP message instructing the student device to fetch new materials via HTTP.
 * Sent from server to client with opcode 0x04.
 *
 * <p>This message is part of the heartbeat-triggered material fetch pattern:
 * <ol>
 *   <li>Client sends STATUS_UPDATE heartbeat</li>
 *   <li>Server responds with FETCH_MATERIALS if new content available</li>
 *   <li>Client initiates HTTP GET /materials</li>
 * </ol>
 *
 * <p>This message has no operand payload.
 */
public final class FetchMaterialsMessage extends TcpMessage {

    /**
     * Empty operand array for messages with no payload.
     */
    private static final byte[] EMPTY_OPERAND = new byte[0];

    /**
     * Creates a new FetchMaterialsMessage.
     */
    public FetchMaterialsMessage() {
        super(TcpOpcode.FETCH_MATERIALS);
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
