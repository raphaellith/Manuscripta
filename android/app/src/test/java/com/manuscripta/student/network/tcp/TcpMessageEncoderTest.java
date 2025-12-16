package com.manuscripta.student.network.tcp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.manuscripta.student.network.tcp.message.DistributeMaterialMessage;
import com.manuscripta.student.network.tcp.message.HandRaisedMessage;
import com.manuscripta.student.network.tcp.message.LockScreenMessage;
import com.manuscripta.student.network.tcp.message.PairingAckMessage;
import com.manuscripta.student.network.tcp.message.PairingRequestMessage;
import com.manuscripta.student.network.tcp.message.RefreshConfigMessage;
import com.manuscripta.student.network.tcp.message.StatusUpdateMessage;
import com.manuscripta.student.network.tcp.message.UnlockScreenMessage;

import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

/**
 * Unit tests for {@link TcpMessageEncoder}.
 */
public class TcpMessageEncoderTest {

    private TcpMessageEncoder encoder;

    @Before
    public void setUp() {
        encoder = new TcpMessageEncoder();
    }

    // ========== No-operand message tests ==========

    @Test
    public void encode_lockScreenMessage_returnsOpcodeOnly() throws TcpProtocolException {
        LockScreenMessage message = new LockScreenMessage();

        byte[] result = encoder.encode(message);

        assertEquals(1, result.length);
        assertEquals((byte) 0x01, result[0]);
    }

    @Test
    public void encode_unlockScreenMessage_returnsOpcodeOnly() throws TcpProtocolException {
        UnlockScreenMessage message = new UnlockScreenMessage();

        byte[] result = encoder.encode(message);

        assertEquals(1, result.length);
        assertEquals((byte) 0x02, result[0]);
    }

    @Test
    public void encode_refreshConfigMessage_returnsOpcodeOnly() throws TcpProtocolException {
        RefreshConfigMessage message = new RefreshConfigMessage();

        byte[] result = encoder.encode(message);

        assertEquals(1, result.length);
        assertEquals((byte) 0x03, result[0]);
    }

    @Test
    public void encode_distributeMaterialMessage_returnsOpcodeOnly() throws TcpProtocolException {
        DistributeMaterialMessage message = new DistributeMaterialMessage();

        byte[] result = encoder.encode(message);

        assertEquals(1, result.length);
        assertEquals((byte) 0x05, result[0]);
    }

    @Test
    public void encode_pairingAckMessage_returnsOpcodeOnly() throws TcpProtocolException {
        PairingAckMessage message = new PairingAckMessage();

        byte[] result = encoder.encode(message);

        assertEquals(1, result.length);
        assertEquals((byte) 0x21, result[0]);
    }

    // ========== Operand message tests ==========

    @Test
    public void encode_statusUpdateMessage_returnsOpcodeAndJsonPayload() throws TcpProtocolException {
        String json = "{\"status\":\"ACTIVE\"}";
        StatusUpdateMessage message = new StatusUpdateMessage(json);

        byte[] result = encoder.encode(message);

        assertEquals(1 + json.getBytes(StandardCharsets.UTF_8).length, result.length);
        assertEquals((byte) 0x10, result[0]);

        byte[] payload = new byte[result.length - 1];
        System.arraycopy(result, 1, payload, 0, payload.length);
        assertEquals(json, new String(payload, StandardCharsets.UTF_8));
    }

    @Test
    public void encode_handRaisedMessage_returnsOpcodeAndDeviceId() throws TcpProtocolException {
        String deviceId = "device-abc-123";
        HandRaisedMessage message = new HandRaisedMessage(deviceId);

        byte[] result = encoder.encode(message);

        assertEquals(1 + deviceId.getBytes(StandardCharsets.UTF_8).length, result.length);
        assertEquals((byte) 0x11, result[0]);

        byte[] payload = new byte[result.length - 1];
        System.arraycopy(result, 1, payload, 0, payload.length);
        assertEquals(deviceId, new String(payload, StandardCharsets.UTF_8));
    }

    @Test
    public void encode_pairingRequestMessage_returnsOpcodeAndDeviceId() throws TcpProtocolException {
        String deviceId = "tablet-xyz-789";
        PairingRequestMessage message = new PairingRequestMessage(deviceId);

        byte[] result = encoder.encode(message);

        assertEquals(1 + deviceId.getBytes(StandardCharsets.UTF_8).length, result.length);
        assertEquals((byte) 0x20, result[0]);

        byte[] payload = new byte[result.length - 1];
        System.arraycopy(result, 1, payload, 0, payload.length);
        assertEquals(deviceId, new String(payload, StandardCharsets.UTF_8));
    }

    // ========== Unicode and special character tests ==========

    @Test
    public void encode_statusUpdateMessage_handlesUnicodeCharacters() throws TcpProtocolException {
        String json = "{\"name\":\"日本語テスト\"}";
        StatusUpdateMessage message = new StatusUpdateMessage(json);

        byte[] result = encoder.encode(message);
        byte[] expectedPayload = json.getBytes(StandardCharsets.UTF_8);

        assertEquals(1 + expectedPayload.length, result.length);
        assertEquals((byte) 0x10, result[0]);

        byte[] actualPayload = new byte[result.length - 1];
        System.arraycopy(result, 1, actualPayload, 0, actualPayload.length);
        assertArrayEquals(expectedPayload, actualPayload);
    }

    @Test
    public void encode_handRaisedMessage_handlesSpecialCharacters() throws TcpProtocolException {
        String deviceId = "device-émoji-🎉";
        HandRaisedMessage message = new HandRaisedMessage(deviceId);

        byte[] result = encoder.encode(message);
        byte[] expectedPayload = deviceId.getBytes(StandardCharsets.UTF_8);

        assertEquals(1 + expectedPayload.length, result.length);

        byte[] actualPayload = new byte[result.length - 1];
        System.arraycopy(result, 1, actualPayload, 0, actualPayload.length);
        assertEquals(deviceId, new String(actualPayload, StandardCharsets.UTF_8));
    }

    // ========== Edge case tests ==========

    @Test
    public void encode_statusUpdateMessage_handlesEmptyJson() throws TcpProtocolException {
        String json = "";
        StatusUpdateMessage message = new StatusUpdateMessage(json);

        byte[] result = encoder.encode(message);

        // Empty string results in opcode only
        assertEquals(1, result.length);
        assertEquals((byte) 0x10, result[0]);
    }

    @Test
    public void encode_statusUpdateMessage_handlesLargePayload() throws TcpProtocolException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"data\":\"");
        for (int i = 0; i < 10000; i++) {
            sb.append("x");
        }
        sb.append("\"}");
        String json = sb.toString();
        StatusUpdateMessage message = new StatusUpdateMessage(json);

        byte[] result = encoder.encode(message);

        assertEquals(1 + json.getBytes(StandardCharsets.UTF_8).length, result.length);
        assertEquals((byte) 0x10, result[0]);
    }

    // ========== Error handling tests ==========

    @Test
    public void encode_nullMessage_throwsTcpProtocolException() {
        try {
            encoder.encode(null);
            fail("Expected TcpProtocolException");
        } catch (TcpProtocolException e) {
            assertEquals(TcpProtocolException.ErrorType.NULL_MESSAGE, e.getErrorType());
        }
    }

    // ========== Constructor test ==========

    @Test
    public void constructor_createsInstance() {
        TcpMessageEncoder encoder = new TcpMessageEncoder();
        assertNotNull(encoder);
    }

    // ========== Round-trip preparation tests ==========

    @Test
    public void encode_allMessageTypes_producesNonEmptyResult() throws TcpProtocolException {
        TcpMessage[] messages = {
                new LockScreenMessage(),
                new UnlockScreenMessage(),
                new RefreshConfigMessage(),
                new DistributeMaterialMessage(),
                new PairingAckMessage(),
                new StatusUpdateMessage("{\"test\":true}"),
                new HandRaisedMessage("device-id"),
                new PairingRequestMessage("device-id")
        };

        for (TcpMessage message : messages) {
            byte[] result = encoder.encode(message);
            assertNotNull(result);
            assertEquals(message.getOpcode().getValue(), result[0]);
        }
    }
}
