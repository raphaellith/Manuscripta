package com.manuscripta.student.network.tcp.message;

import androidx.annotation.NonNull;

import com.manuscripta.student.network.tcp.TcpMessage;
import com.manuscripta.student.network.tcp.TcpOpcode;

import java.nio.charset.StandardCharsets;

/**
 * TCP message containing device status information.
 * Sent from client to server with opcode 0x10.
 *
 * <p>The operand is a JSON string containing status information such as
 * battery level, connection state, and other device metrics.
 *
 * <p>This message is used as the heartbeat mechanism - the server may respond
 * with control messages like FETCH_MATERIALS if new content is available.
 */
public final class StatusUpdateMessage extends TcpMessage {

    /**
     * The JSON status payload.
     */
    private final String jsonPayload;

    /**
     * The encoded operand bytes.
     */
    private final byte[] operand;

    /**
     * Creates a new StatusUpdateMessage with the specified JSON payload.
     *
     * @param jsonPayload The JSON string containing status information.
     */
    public StatusUpdateMessage(@NonNull String jsonPayload) {
        super(TcpOpcode.STATUS_UPDATE);
        this.jsonPayload = jsonPayload;
        this.operand = jsonPayload.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Returns the JSON status payload.
     *
     * @return The JSON string.
     */
    @NonNull
    public String getJsonPayload() {
        return jsonPayload;
    }

    /**
     * Returns the UTF-8 encoded JSON payload as the operand.
     *
     * @return The encoded operand bytes.
     */
    @NonNull
    @Override
    public byte[] getOperand() {
        return operand.clone();
    }

    /**
     * Returns a string representation of this message.
     *
     * @return A string containing the message type and payload preview.
     */
    @NonNull
    @Override
    public String toString() {
        String preview = jsonPayload.length() > 50
                ? jsonPayload.substring(0, 50) + "..."
                : jsonPayload;
        return "StatusUpdateMessage{payload=" + preview + "}";
    }
}
