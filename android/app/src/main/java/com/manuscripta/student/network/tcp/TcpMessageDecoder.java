package com.manuscripta.student.network.tcp;

import androidx.annotation.NonNull;

import com.manuscripta.student.network.tcp.message.DistributeAckMessage;
import com.manuscripta.student.network.tcp.message.DistributeMaterialMessage;
import com.manuscripta.student.network.tcp.message.HandAckMessage;
import com.manuscripta.student.network.tcp.message.HandRaisedMessage;
import com.manuscripta.student.network.tcp.message.LockScreenMessage;
import com.manuscripta.student.network.tcp.message.PairingAckMessage;
import com.manuscripta.student.network.tcp.message.PairingRequestMessage;
import com.manuscripta.student.network.tcp.message.RefreshConfigMessage;
import com.manuscripta.student.network.tcp.message.StatusUpdateMessage;
import com.manuscripta.student.network.tcp.message.UnlockScreenMessage;
import com.manuscripta.student.network.tcp.message.UnpairMessage;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Decoder for TCP messages.
 * Deserialises byte arrays to {@link TcpMessage} objects according to the binary protocol format.
 *
 * <p>Protocol format:
 * <pre>
 * [1 byte: opcode][N bytes: operand]
 * </pre>
 *
 * <p>The opcode is always 1 byte. The operand length varies by message type and may be empty.
 */
@Singleton
public final class TcpMessageDecoder {

    /**
     * Creates a new TcpMessageDecoder.
     */
    @Inject
    public TcpMessageDecoder() {
        // Empty constructor for Hilt injection
    }

    /**
     * Decodes a byte array to a TCP message.
     *
     * <p>The first byte is interpreted as the opcode, and any remaining bytes
     * are treated as the operand payload.
     *
     * @param data The byte array to decode.
     * @return The decoded message.
     * @throws TcpProtocolException If the data is null, empty, or contains an unknown opcode.
     */
    @NonNull
    public TcpMessage decode(@NonNull byte[] data) throws TcpProtocolException {
        if (data == null || data.length == 0) {
            throw new TcpProtocolException(
                    TcpProtocolException.ErrorType.EMPTY_DATA,
                    "Cannot decode null or empty data");
        }

        byte opcodeValue = data[0];
        TcpOpcode opcode = TcpOpcode.fromValue(opcodeValue);

        if (opcode == null) {
            throw new TcpProtocolException(opcodeValue);
        }

        // Extract operand (all bytes after the opcode)
        byte[] operand = data.length > 1
                ? Arrays.copyOfRange(data, 1, data.length)
                : new byte[0];

        return createMessage(opcode, operand);
    }

    /**
     * Creates the appropriate message instance based on the opcode.
     *
     * @param opcode  The decoded opcode.
     * @param operand The operand bytes (may be empty).
     * @return The appropriate TcpMessage subclass instance.
     * @throws TcpProtocolException If the operand is invalid for the message type.
     */
    @NonNull
    private TcpMessage createMessage(@NonNull TcpOpcode opcode, @NonNull byte[] operand)
            throws TcpProtocolException {
        switch (opcode) {
            case LOCK_SCREEN:
                return new LockScreenMessage();
            case UNLOCK_SCREEN:
                return new UnlockScreenMessage();
            case REFRESH_CONFIG:
                return new RefreshConfigMessage();
            case UNPAIR:
                return new UnpairMessage();
            case DISTRIBUTE_MATERIAL:
                return new DistributeMaterialMessage();
            case HAND_ACK:
                return createHandAckMessage(operand);
            case PAIRING_ACK:
                return new PairingAckMessage();
            case STATUS_UPDATE:
                return createStatusUpdateMessage(operand);
            case HAND_RAISED:
                return createHandRaisedMessage(operand);
            case DISTRIBUTE_ACK:
                return createDistributeAckMessage(operand);
            case PAIRING_REQUEST:
            default:
                // PAIRING_REQUEST is the only remaining case or fallback
                return createPairingRequestMessage(operand);
        }
    }

    /**
     * Creates a StatusUpdateMessage from the operand.
     *
     * @param operand The UTF-8 encoded JSON payload.
     * @return The StatusUpdateMessage.
     * @throws TcpProtocolException If the operand is empty.
     */
    @NonNull
    private StatusUpdateMessage createStatusUpdateMessage(@NonNull byte[] operand)
            throws TcpProtocolException {
        if (operand.length == 0) {
            throw new TcpProtocolException(
                    TcpProtocolException.ErrorType.MALFORMED_DATA,
                    "STATUS_UPDATE message requires JSON payload");
        }
        String jsonPayload = new String(operand, StandardCharsets.UTF_8);
        return new StatusUpdateMessage(jsonPayload);
    }

    /**
     * Creates a HandRaisedMessage from the operand.
     *
     * @param operand The UTF-8 encoded device ID.
     * @return The HandRaisedMessage.
     * @throws TcpProtocolException If the operand is empty.
     */
    @NonNull
    private HandRaisedMessage createHandRaisedMessage(@NonNull byte[] operand)
            throws TcpProtocolException {
        if (operand.length == 0) {
            throw new TcpProtocolException(
                    TcpProtocolException.ErrorType.MALFORMED_DATA,
                    "HAND_RAISED message requires device ID");
        }
        String deviceId = new String(operand, StandardCharsets.UTF_8);
        return new HandRaisedMessage(deviceId);
    }

    /**
     * Creates a HandAckMessage from the operand.
     *
     * @param operand The UTF-8 encoded device ID.
     * @return The HandAckMessage.
     * @throws TcpProtocolException If the operand is empty.
     */
    @NonNull
    private HandAckMessage createHandAckMessage(@NonNull byte[] operand)
            throws TcpProtocolException {
        if (operand.length == 0) {
            throw new TcpProtocolException(
                    TcpProtocolException.ErrorType.MALFORMED_DATA,
                    "HAND_ACK message requires device ID");
        }
        String deviceId = new String(operand, StandardCharsets.UTF_8);
        return new HandAckMessage(deviceId);
    }

    /**
     * Creates a DistributeAckMessage from the operand.
     *
     * @param operand The UTF-8 encoded device ID.
     * @return The DistributeAckMessage.
     * @throws TcpProtocolException If the operand is empty.
     */
    @NonNull
    private DistributeAckMessage createDistributeAckMessage(@NonNull byte[] operand)
            throws TcpProtocolException {
        if (operand.length == 0) {
            throw new TcpProtocolException(
                    TcpProtocolException.ErrorType.MALFORMED_DATA,
                    "DISTRIBUTE_ACK message requires device ID");
        }
        String deviceId = new String(operand, StandardCharsets.UTF_8);
        return new DistributeAckMessage(deviceId);
    }

    /**
     * Creates a PairingRequestMessage from the operand.
     *
     * @param operand The UTF-8 encoded device ID.
     * @return The PairingRequestMessage.
     * @throws TcpProtocolException If the operand is empty.
     */
    @NonNull
    private PairingRequestMessage createPairingRequestMessage(@NonNull byte[] operand)
            throws TcpProtocolException {
        if (operand.length == 0) {
            throw new TcpProtocolException(
                    TcpProtocolException.ErrorType.MALFORMED_DATA,
                    "PAIRING_REQUEST message requires device ID");
        }
        String deviceId = new String(operand, StandardCharsets.UTF_8);
        return new PairingRequestMessage(deviceId);
    }
}
