package com.manuscripta.student.network.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.manuscripta.student.network.tcp.message.DistributeAckMessage;
import com.manuscripta.student.network.tcp.message.DistributeMaterialMessage;
import com.manuscripta.student.network.tcp.message.FeedbackAckMessage;
import com.manuscripta.student.network.tcp.message.HandAckMessage;
import com.manuscripta.student.network.tcp.message.HandRaisedMessage;
import com.manuscripta.student.network.tcp.message.LockScreenMessage;
import com.manuscripta.student.network.tcp.message.PairingAckMessage;
import com.manuscripta.student.network.tcp.message.PairingRequestMessage;
import com.manuscripta.student.network.tcp.message.RefreshConfigMessage;
import com.manuscripta.student.network.tcp.message.ReturnFeedbackMessage;
import com.manuscripta.student.network.tcp.message.StatusUpdateMessage;
import com.manuscripta.student.network.tcp.message.UnlockScreenMessage;
import com.manuscripta.student.network.tcp.message.UnpairMessage;

import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

/**
 * Unit tests for {@link TcpMessageDecoder}.
 */
public class TcpMessageDecoderTest {

    private TcpMessageDecoder decoder;

    @Before
    public void setUp() {
        decoder = new TcpMessageDecoder();
    }

    // ========== No-operand message tests ==========

    @Test
    public void decode_lockScreenOpcode_returnsLockScreenMessage() throws TcpProtocolException {
        byte[] data = {(byte) 0x01};

        TcpMessage result = decoder.decode(data);

        assertTrue(result instanceof LockScreenMessage);
        assertEquals(TcpOpcode.LOCK_SCREEN, result.getOpcode());
    }

    @Test
    public void decode_unlockScreenOpcode_returnsUnlockScreenMessage() throws TcpProtocolException {
        byte[] data = {(byte) 0x02};

        TcpMessage result = decoder.decode(data);

        assertTrue(result instanceof UnlockScreenMessage);
        assertEquals(TcpOpcode.UNLOCK_SCREEN, result.getOpcode());
    }

    @Test
    public void decode_refreshConfigOpcode_returnsRefreshConfigMessage() throws TcpProtocolException {
        byte[] data = {(byte) 0x03};

        TcpMessage result = decoder.decode(data);

        assertTrue(result instanceof RefreshConfigMessage);
        assertEquals(TcpOpcode.REFRESH_CONFIG, result.getOpcode());
    }

    @Test
    public void decode_distributeMaterialOpcode_returnsDistributeMaterialMessage()
            throws TcpProtocolException {
        byte[] data = {(byte) 0x05};

        TcpMessage result = decoder.decode(data);

        assertTrue(result instanceof DistributeMaterialMessage);
        assertEquals(TcpOpcode.DISTRIBUTE_MATERIAL, result.getOpcode());
    }

    @Test
    public void decode_unpairOpcode_returnsUnpairMessage() throws TcpProtocolException {
        byte[] data = {(byte) 0x04};

        TcpMessage result = decoder.decode(data);

        assertTrue(result instanceof UnpairMessage);
        assertEquals(TcpOpcode.UNPAIR, result.getOpcode());
    }

    @Test
    public void decode_pairingAckOpcode_returnsPairingAckMessage() throws TcpProtocolException {
        byte[] data = {(byte) 0x21};

        TcpMessage result = decoder.decode(data);

        assertTrue(result instanceof PairingAckMessage);
        assertEquals(TcpOpcode.PAIRING_ACK, result.getOpcode());
    }

    @Test
    public void decode_returnFeedbackOpcode_returnsReturnFeedbackMessage()
            throws TcpProtocolException {
        byte[] data = {(byte) 0x07};

        TcpMessage result = decoder.decode(data);

        assertTrue(result instanceof ReturnFeedbackMessage);
        assertEquals(TcpOpcode.RETURN_FEEDBACK, result.getOpcode());
    }

    // ========== Operand message tests ==========

    @Test
    public void decode_statusUpdateOpcode_returnsStatusUpdateMessage() throws TcpProtocolException {
        String json = "{\"status\":\"ACTIVE\"}";
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[1 + jsonBytes.length];
        data[0] = (byte) 0x10;
        System.arraycopy(jsonBytes, 0, data, 1, jsonBytes.length);

        TcpMessage result = decoder.decode(data);

        assertTrue(result instanceof StatusUpdateMessage);
        assertEquals(TcpOpcode.STATUS_UPDATE, result.getOpcode());
        assertEquals(json, ((StatusUpdateMessage) result).getJsonPayload());
    }

    @Test
    public void decode_handRaisedOpcode_returnsHandRaisedMessage() throws TcpProtocolException {
        String deviceId = "device-abc-123";
        byte[] idBytes = deviceId.getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[1 + idBytes.length];
        data[0] = (byte) 0x11;
        System.arraycopy(idBytes, 0, data, 1, idBytes.length);

        TcpMessage result = decoder.decode(data);

        assertTrue(result instanceof HandRaisedMessage);
        assertEquals(TcpOpcode.HAND_RAISED, result.getOpcode());
        assertEquals(deviceId, ((HandRaisedMessage) result).getDeviceId());
    }

    @Test
    public void decode_pairingRequestOpcode_returnsPairingRequestMessage()
            throws TcpProtocolException {
        String deviceId = "tablet-xyz-789";
        byte[] idBytes = deviceId.getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[1 + idBytes.length];
        data[0] = (byte) 0x20;
        System.arraycopy(idBytes, 0, data, 1, idBytes.length);

        TcpMessage result = decoder.decode(data);

        assertTrue(result instanceof PairingRequestMessage);
        assertEquals(TcpOpcode.PAIRING_REQUEST, result.getOpcode());
        assertEquals(deviceId, ((PairingRequestMessage) result).getDeviceId());
    }

    @Test
    public void decode_handAckOpcode_returnsHandAckMessage() throws TcpProtocolException {
        String deviceId = "device-ack-456";
        byte[] idBytes = deviceId.getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[1 + idBytes.length];
        data[0] = (byte) 0x06;
        System.arraycopy(idBytes, 0, data, 1, idBytes.length);

        TcpMessage result = decoder.decode(data);

        assertTrue(result instanceof HandAckMessage);
        assertEquals(TcpOpcode.HAND_ACK, result.getOpcode());
        assertEquals(deviceId, ((HandAckMessage) result).getDeviceId());
    }

    @Test
    public void decode_distributeAckOpcode_returnsDistributeAckMessage()
            throws TcpProtocolException {
        byte[] data = buildNullSeparatedAck((byte) 0x12, "device-dist-789", "mat-uuid-1");

        TcpMessage result = decoder.decode(data);

        assertTrue(result instanceof DistributeAckMessage);
        assertEquals(TcpOpcode.DISTRIBUTE_ACK, result.getOpcode());
        assertEquals("device-dist-789", ((DistributeAckMessage) result).getDeviceId());
        assertEquals("mat-uuid-1", ((DistributeAckMessage) result).getMaterialId());
    }

    @Test
    public void decode_feedbackAckOpcode_returnsFeedbackAckMessage() throws TcpProtocolException {
        byte[] data = buildNullSeparatedAck((byte) 0x13, "device-fb-123", "fb-uuid-5");

        TcpMessage result = decoder.decode(data);

        assertTrue(result instanceof FeedbackAckMessage);
        assertEquals(TcpOpcode.FEEDBACK_ACK, result.getOpcode());
        assertEquals("device-fb-123", ((FeedbackAckMessage) result).getDeviceId());
        assertEquals("fb-uuid-5", ((FeedbackAckMessage) result).getFeedbackId());
    }

    // ========== Unicode and special character tests ==========

    @Test
    public void decode_statusUpdate_handlesUnicodeCharacters() throws TcpProtocolException {
        String json = "{\"name\":\"日本語テスト\"}";
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[1 + jsonBytes.length];
        data[0] = (byte) 0x10;
        System.arraycopy(jsonBytes, 0, data, 1, jsonBytes.length);

        TcpMessage result = decoder.decode(data);

        assertTrue(result instanceof StatusUpdateMessage);
        assertEquals(json, ((StatusUpdateMessage) result).getJsonPayload());
    }

    @Test
    public void decode_handRaised_handlesSpecialCharacters() throws TcpProtocolException {
        String deviceId = "device-émoji-🎉";
        byte[] idBytes = deviceId.getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[1 + idBytes.length];
        data[0] = (byte) 0x11;
        System.arraycopy(idBytes, 0, data, 1, idBytes.length);

        TcpMessage result = decoder.decode(data);

        assertTrue(result instanceof HandRaisedMessage);
        assertEquals(deviceId, ((HandRaisedMessage) result).getDeviceId());
    }

    // ========== No-operand messages with extra bytes ==========

    @Test
    public void decode_lockScreenWithExtraBytes_ignoresExtraBytes() throws TcpProtocolException {
        byte[] data = {(byte) 0x01, 0x00, 0x00, 0x00};

        TcpMessage result = decoder.decode(data);

        assertTrue(result instanceof LockScreenMessage);
    }

    @Test
    public void decode_returnFeedbackWithExtraBytes_ignoresExtraBytes()
            throws TcpProtocolException {
        byte[] data = {(byte) 0x07, 0x00, 0x00, 0x00};

        TcpMessage result = decoder.decode(data);

        assertTrue(result instanceof ReturnFeedbackMessage);
    }

    // ========== Error handling tests ==========

    @Test
    public void decode_nullData_throwsTcpProtocolException() {
        try {
            decoder.decode(null);
            fail("Expected TcpProtocolException");
        } catch (TcpProtocolException e) {
            assertEquals(TcpProtocolException.ErrorType.EMPTY_DATA, e.getErrorType());
        }
    }

    @Test
    public void decode_emptyData_throwsTcpProtocolException() {
        try {
            decoder.decode(new byte[0]);
            fail("Expected TcpProtocolException");
        } catch (TcpProtocolException e) {
            assertEquals(TcpProtocolException.ErrorType.EMPTY_DATA, e.getErrorType());
        }
    }

    @Test
    public void decode_unknownOpcode_throwsTcpProtocolException() {
        try {
            decoder.decode(new byte[]{(byte) 0xFF});
            fail("Expected TcpProtocolException");
        } catch (TcpProtocolException e) {
            assertEquals(TcpProtocolException.ErrorType.UNKNOWN_OPCODE, e.getErrorType());
            assertNotNull(e.getInvalidOpcode());
            assertEquals((byte) 0xFF, e.getInvalidOpcode().byteValue());
        }
    }

    @Test
    public void decode_unknownOpcodeZero_throwsTcpProtocolException() {
        try {
            decoder.decode(new byte[]{(byte) 0x00});
            fail("Expected TcpProtocolException");
        } catch (TcpProtocolException e) {
            assertEquals(TcpProtocolException.ErrorType.UNKNOWN_OPCODE, e.getErrorType());
        }
    }

    @Test
    public void decode_statusUpdateWithoutPayload_throwsTcpProtocolException() {
        try {
            decoder.decode(new byte[]{(byte) 0x10});
            fail("Expected TcpProtocolException");
        } catch (TcpProtocolException e) {
            assertEquals(TcpProtocolException.ErrorType.MALFORMED_DATA, e.getErrorType());
        }
    }

    @Test
    public void decode_handRaisedWithoutPayload_throwsTcpProtocolException() {
        try {
            decoder.decode(new byte[]{(byte) 0x11});
            fail("Expected TcpProtocolException");
        } catch (TcpProtocolException e) {
            assertEquals(TcpProtocolException.ErrorType.MALFORMED_DATA, e.getErrorType());
        }
    }

    @Test
    public void decode_pairingRequestWithoutPayload_throwsTcpProtocolException() {
        try {
            decoder.decode(new byte[]{(byte) 0x20});
            fail("Expected TcpProtocolException");
        } catch (TcpProtocolException e) {
            assertEquals(TcpProtocolException.ErrorType.MALFORMED_DATA, e.getErrorType());
        }
    }

    @Test
    public void decode_handAckWithoutPayload_throwsTcpProtocolException() {
        try {
            decoder.decode(new byte[]{(byte) 0x06});
            fail("Expected TcpProtocolException");
        } catch (TcpProtocolException e) {
            assertEquals(TcpProtocolException.ErrorType.MALFORMED_DATA, e.getErrorType());
        }
    }

    @Test
    public void decode_distributeAckWithoutPayload_throwsTcpProtocolException() {
        try {
            decoder.decode(new byte[]{(byte) 0x12});
            fail("Expected TcpProtocolException");
        } catch (TcpProtocolException e) {
            assertEquals(TcpProtocolException.ErrorType.MALFORMED_DATA, e.getErrorType());
        }
    }

    @Test
    public void decode_distributeAckMissingSeparator_throwsTcpProtocolException() {
        try {
            // deviceId only, no null byte
            byte[] idBytes = "device-123".getBytes(StandardCharsets.UTF_8);
            byte[] data = new byte[1 + idBytes.length];
            data[0] = (byte) 0x12;
            System.arraycopy(idBytes, 0, data, 1, idBytes.length);
            decoder.decode(data);
            fail("Expected TcpProtocolException");
        } catch (TcpProtocolException e) {
            assertEquals(TcpProtocolException.ErrorType.MALFORMED_DATA, e.getErrorType());
        }
    }

    @Test
    public void decode_distributeAckEmptyEntityId_throwsTcpProtocolException() {
        try {
            // deviceId + 0x00 but no entity ID
            byte[] idBytes = "device-123".getBytes(StandardCharsets.UTF_8);
            byte[] data = new byte[1 + idBytes.length + 1];
            data[0] = (byte) 0x12;
            System.arraycopy(idBytes, 0, data, 1, idBytes.length);
            data[1 + idBytes.length] = 0x00;
            decoder.decode(data);
            fail("Expected TcpProtocolException");
        } catch (TcpProtocolException e) {
            assertEquals(TcpProtocolException.ErrorType.MALFORMED_DATA, e.getErrorType());
        }
    }

    @Test
    public void decode_distributeAckEmptyDeviceId_throwsTcpProtocolException() {
        try {
            // 0x00 + entityId (empty device ID)
            byte[] entityBytes = "mat-1".getBytes(StandardCharsets.UTF_8);
            byte[] data = new byte[1 + 1 + entityBytes.length];
            data[0] = (byte) 0x12;
            data[1] = 0x00;
            System.arraycopy(entityBytes, 0, data, 2, entityBytes.length);
            decoder.decode(data);
            fail("Expected TcpProtocolException");
        } catch (TcpProtocolException e) {
            assertEquals(TcpProtocolException.ErrorType.MALFORMED_DATA, e.getErrorType());
        }
    }

    @Test
    public void decode_feedbackAckWithoutPayload_throwsTcpProtocolException() {
        try {
            decoder.decode(new byte[]{(byte) 0x13});
            fail("Expected TcpProtocolException");
        } catch (TcpProtocolException e) {
            assertEquals(TcpProtocolException.ErrorType.MALFORMED_DATA, e.getErrorType());
        }
    }

    @Test
    public void decode_feedbackAckMissingSeparator_throwsTcpProtocolException() {
        try {
            byte[] idBytes = "device-123".getBytes(StandardCharsets.UTF_8);
            byte[] data = new byte[1 + idBytes.length];
            data[0] = (byte) 0x13;
            System.arraycopy(idBytes, 0, data, 1, idBytes.length);
            decoder.decode(data);
            fail("Expected TcpProtocolException");
        } catch (TcpProtocolException e) {
            assertEquals(TcpProtocolException.ErrorType.MALFORMED_DATA, e.getErrorType());
        }
    }

    // ========== Constructor test ==========

    @Test
    public void constructor_createsInstance() {
        TcpMessageDecoder decoder = new TcpMessageDecoder();
        assertNotNull(decoder);
    }

    // ========== All opcodes test ==========

    @Test
    public void decode_allValidOpcodes_returnsCorrectMessageTypes() throws TcpProtocolException {
        // No-operand messages
        assertTrue(decoder.decode(new byte[]{0x01}) instanceof LockScreenMessage);
        assertTrue(decoder.decode(new byte[]{0x02}) instanceof UnlockScreenMessage);
        assertTrue(decoder.decode(new byte[]{0x03}) instanceof RefreshConfigMessage);
        assertTrue(decoder.decode(new byte[]{0x04}) instanceof UnpairMessage);
        assertTrue(decoder.decode(new byte[]{0x05}) instanceof DistributeMaterialMessage);
        assertTrue(decoder.decode(new byte[]{0x07}) instanceof ReturnFeedbackMessage);
        assertTrue(decoder.decode(new byte[]{0x21}) instanceof PairingAckMessage);

        // Messages with operand
        byte[] statusBytes = new byte[]{0x10, '{', '}'};
        assertTrue(decoder.decode(statusBytes) instanceof StatusUpdateMessage);

        byte[] handBytes = new byte[]{0x11, 'i', 'd'};
        assertTrue(decoder.decode(handBytes) instanceof HandRaisedMessage);

        byte[] handAckBytes = new byte[]{0x06, 'i', 'd'};
        assertTrue(decoder.decode(handAckBytes) instanceof HandAckMessage);

        byte[] distAckBytes = buildNullSeparatedAck((byte) 0x12, "id", "mat-1");
        assertTrue(decoder.decode(distAckBytes) instanceof DistributeAckMessage);

        byte[] feedbackAckBytes = buildNullSeparatedAck((byte) 0x13, "id", "fb-1");
        assertTrue(decoder.decode(feedbackAckBytes) instanceof FeedbackAckMessage);

        byte[] pairBytes = new byte[]{0x20, 'i', 'd'};
        assertTrue(decoder.decode(pairBytes) instanceof PairingRequestMessage);
    }

    // ========== Helper methods ==========

    /**
     * Builds a byte array for a per-entity ACK message:
     * [opcode][deviceId bytes][0x00][entityId bytes].
     */
    private byte[] buildNullSeparatedAck(byte opcode, String deviceId, String entityId) {
        byte[] deviceBytes = deviceId.getBytes(StandardCharsets.UTF_8);
        byte[] entityBytes = entityId.getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[1 + deviceBytes.length + 1 + entityBytes.length];
        data[0] = opcode;
        System.arraycopy(deviceBytes, 0, data, 1, deviceBytes.length);
        data[1 + deviceBytes.length] = 0x00;
        System.arraycopy(entityBytes, 0, data, 1 + deviceBytes.length + 1, entityBytes.length);
        return data;
    }
}
