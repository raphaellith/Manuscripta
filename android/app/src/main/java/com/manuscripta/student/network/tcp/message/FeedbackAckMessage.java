package com.manuscripta.student.network.tcp.message;

import androidx.annotation.NonNull;

import com.manuscripta.student.network.tcp.TcpMessage;
import com.manuscripta.student.network.tcp.TcpOpcode;

import java.nio.charset.StandardCharsets;

/**
 * TCP message confirming the client received a single feedback entity.
 * Sent from client to server with opcode 0x13.
 *
 * <p>Per API Contract §3.6.2, one message is sent per feedback entity. The
 * operand encodes both the device ID and the feedback ID, separated by a null
 * byte:
 * <pre>
 * [Device ID (UTF-8)] + [0x00] + [Feedback ID (UTF-8)]
 * </pre>
 *
 * <p>This message completes the feedback retrieval handshake:
 * <ol>
 *   <li>Server sends RETURN_FEEDBACK</li>
 *   <li>Client fetches feedback via HTTP</li>
 *   <li>Client sends one FEEDBACK_ACK per feedback entity to confirm</li>
 * </ol>
 */
public final class FeedbackAckMessage extends TcpMessage {

    /** The device ID of the client acknowledging feedback receipt. */
    private final String deviceId;

    /** The ID of the feedback entity being acknowledged. */
    private final String feedbackId;

    /** The encoded operand bytes. */
    private final byte[] operand;

    /**
     * Creates a new FeedbackAckMessage for a specific feedback entity.
     *
     * @param deviceId   The device ID of the client.
     * @param feedbackId The ID of the feedback entity being acknowledged.
     */
    public FeedbackAckMessage(@NonNull String deviceId, @NonNull String feedbackId) {
        super(TcpOpcode.FEEDBACK_ACK);
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID cannot be null or empty");
        }
        if (feedbackId == null || feedbackId.trim().isEmpty()) {
            throw new IllegalArgumentException("Feedback ID cannot be null or empty");
        }
        this.deviceId = deviceId;
        this.feedbackId = feedbackId;

        byte[] deviceBytes = deviceId.getBytes(StandardCharsets.UTF_8);
        byte[] feedbackBytes = feedbackId.getBytes(StandardCharsets.UTF_8);
        this.operand = new byte[deviceBytes.length + 1 + feedbackBytes.length];
        System.arraycopy(deviceBytes, 0, this.operand, 0, deviceBytes.length);
        this.operand[deviceBytes.length] = 0x00;
        System.arraycopy(feedbackBytes, 0, this.operand, deviceBytes.length + 1,
                feedbackBytes.length);
    }

    /**
     * Returns the device ID of the client.
     *
     * @return The device ID.
     */
    @NonNull
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Returns the ID of the feedback entity being acknowledged.
     *
     * @return The feedback ID.
     */
    @NonNull
    public String getFeedbackId() {
        return feedbackId;
    }

    /**
     * Returns the encoded operand containing device ID and feedback ID
     * separated by a null byte.
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
     * @return A string containing the message type, device ID, and feedback ID.
     */
    @NonNull
    @Override
    public String toString() {
        return "FeedbackAckMessage{deviceId=" + deviceId
                + ", feedbackId=" + feedbackId + "}";
    }
}
