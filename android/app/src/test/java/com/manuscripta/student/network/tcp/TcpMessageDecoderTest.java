package com.manuscripta.student.network.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
    public void decode_distributeMaterialOpcode_returnsDistributeMaterialMessage() throws TcpProtocolException {
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
    public void decode_pairingRequestOpcode_returnsPairingRequestMessage() throws TcpProtocolException {
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
    public void decode_distributeAckOpcode_returnsDistributeAckMessage() throws TcpProtocolException {
        String deviceId = "device-dist-789";
        byte[] idBytes = deviceId.getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[1 + idBytes.length];
        data[0] = (byte) 0x12;
        System.arraycopy(idBytes, 0, data, 1, idBytes.length);

        TcpMessage result = decoder.decode(data);

        assertTrue(result instanceof DistributeAckMessage);
        assertEquals(TcpOpcode.DISTRIBUTE_ACK, result.getOpcode());
        assertEquals(deviceId, ((DistributeAckMessage) result).getDeviceId());
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
        assertTrue(decoder.decode(new byte[]{0x21}) instanceof PairingAckMessage);

        // Messages with operand
        byte[] statusBytes = new byte[]{0x10, '{', '}'};
        assertTrue(decoder.decode(statusBytes) instanceof StatusUpdateMessage);

        byte[] handBytes = new byte[]{0x11, 'i', 'd'};
        assertTrue(decoder.decode(handBytes) instanceof HandRaisedMessage);

        byte[] handAckBytes = new byte[]{0x06, 'i', 'd'};
        assertTrue(decoder.decode(handAckBytes) instanceof HandAckMessage);

        byte[] distAckBytes = new byte[]{0x12, 'i', 'd'};
        assertTrue(decoder.decode(distAckBytes) instanceof DistributeAckMessage);

        byte[] pairBytes = new byte[]{0x20, 'i', 'd'};
        assertTrue(decoder.decode(pairBytes) instanceof PairingRequestMessage);
    }
}
