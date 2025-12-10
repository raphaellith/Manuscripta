package com.manuscripta.student.network.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Round-trip tests for {@link TcpMessageEncoder} and {@link TcpMessageDecoder}.
 * These tests verify that encoding then decoding a message produces an equivalent message.
 */
public class TcpEncoderDecoderRoundTripTest {

    private TcpMessageEncoder encoder;
    private TcpMessageDecoder decoder;

    @Before
    public void setUp() {
        encoder = new TcpMessageEncoder();
        decoder = new TcpMessageDecoder();
    }

    // ========== No-operand message round-trips ==========

    @Test
    public void roundTrip_lockScreenMessage_preservesMessage() throws TcpProtocolException {
        LockScreenMessage original = new LockScreenMessage();

        byte[] encoded = encoder.encode(original);
        TcpMessage decoded = decoder.decode(encoded);

        assertTrue(decoded instanceof LockScreenMessage);
        assertEquals(original.getOpcode(), decoded.getOpcode());
    }

    @Test
    public void roundTrip_unlockScreenMessage_preservesMessage() throws TcpProtocolException {
        UnlockScreenMessage original = new UnlockScreenMessage();

        byte[] encoded = encoder.encode(original);
        TcpMessage decoded = decoder.decode(encoded);

        assertTrue(decoded instanceof UnlockScreenMessage);
        assertEquals(original.getOpcode(), decoded.getOpcode());
    }

    @Test
    public void roundTrip_refreshConfigMessage_preservesMessage() throws TcpProtocolException {
        RefreshConfigMessage original = new RefreshConfigMessage();

        byte[] encoded = encoder.encode(original);
        TcpMessage decoded = decoder.decode(encoded);

        assertTrue(decoded instanceof RefreshConfigMessage);
        assertEquals(original.getOpcode(), decoded.getOpcode());
    }

    @Test
    public void roundTrip_fetchMaterialsMessage_preservesMessage() throws TcpProtocolException {
        FetchMaterialsMessage original = new FetchMaterialsMessage();

        byte[] encoded = encoder.encode(original);
        TcpMessage decoded = decoder.decode(encoded);

        assertTrue(decoded instanceof FetchMaterialsMessage);
        assertEquals(original.getOpcode(), decoded.getOpcode());
    }

    @Test
    public void roundTrip_pairingAckMessage_preservesMessage() throws TcpProtocolException {
        PairingAckMessage original = new PairingAckMessage();

        byte[] encoded = encoder.encode(original);
        TcpMessage decoded = decoder.decode(encoded);

        assertTrue(decoded instanceof PairingAckMessage);
        assertEquals(original.getOpcode(), decoded.getOpcode());
    }

    // ========== Operand message round-trips ==========

    @Test
    public void roundTrip_statusUpdateMessage_preservesPayload() throws TcpProtocolException {
        String json = "{\"status\":\"ACTIVE\",\"battery\":75}";
        StatusUpdateMessage original = new StatusUpdateMessage(json);

        byte[] encoded = encoder.encode(original);
        TcpMessage decoded = decoder.decode(encoded);

        assertTrue(decoded instanceof StatusUpdateMessage);
        assertEquals(original.getOpcode(), decoded.getOpcode());
        assertEquals(json, ((StatusUpdateMessage) decoded).getJsonPayload());
    }

    @Test
    public void roundTrip_handRaisedMessage_preservesDeviceId() throws TcpProtocolException {
        String deviceId = "device-abc-123-xyz";
        HandRaisedMessage original = new HandRaisedMessage(deviceId);

        byte[] encoded = encoder.encode(original);
        TcpMessage decoded = decoder.decode(encoded);

        assertTrue(decoded instanceof HandRaisedMessage);
        assertEquals(original.getOpcode(), decoded.getOpcode());
        assertEquals(deviceId, ((HandRaisedMessage) decoded).getDeviceId());
    }

    @Test
    public void roundTrip_pairingRequestMessage_preservesDeviceId() throws TcpProtocolException {
        String deviceId = "tablet-789-xyz-456";
        PairingRequestMessage original = new PairingRequestMessage(deviceId);

        byte[] encoded = encoder.encode(original);
        TcpMessage decoded = decoder.decode(encoded);

        assertTrue(decoded instanceof PairingRequestMessage);
        assertEquals(original.getOpcode(), decoded.getOpcode());
        assertEquals(deviceId, ((PairingRequestMessage) decoded).getDeviceId());
    }

    // ========== Unicode round-trips ==========

    @Test
    public void roundTrip_statusUpdate_preservesUnicodePayload() throws TcpProtocolException {
        String json = "{\"message\":\"こんにちは世界\",\"emoji\":\"🎉🚀\"}";
        StatusUpdateMessage original = new StatusUpdateMessage(json);

        byte[] encoded = encoder.encode(original);
        TcpMessage decoded = decoder.decode(encoded);

        assertTrue(decoded instanceof StatusUpdateMessage);
        assertEquals(json, ((StatusUpdateMessage) decoded).getJsonPayload());
    }

    @Test
    public void roundTrip_handRaised_preservesUnicodeDeviceId() throws TcpProtocolException {
        String deviceId = "device-日本語-🔥";
        HandRaisedMessage original = new HandRaisedMessage(deviceId);

        byte[] encoded = encoder.encode(original);
        TcpMessage decoded = decoder.decode(encoded);

        assertTrue(decoded instanceof HandRaisedMessage);
        assertEquals(deviceId, ((HandRaisedMessage) decoded).getDeviceId());
    }

    @Test
    public void roundTrip_pairingRequest_preservesUnicodeDeviceId() throws TcpProtocolException {
        String deviceId = "tablet-émoji-🎮";
        PairingRequestMessage original = new PairingRequestMessage(deviceId);

        byte[] encoded = encoder.encode(original);
        TcpMessage decoded = decoder.decode(encoded);

        assertTrue(decoded instanceof PairingRequestMessage);
        assertEquals(deviceId, ((PairingRequestMessage) decoded).getDeviceId());
    }

    // ========== Large payload round-trips ==========

    @Test
    public void roundTrip_statusUpdate_preservesLargePayload() throws TcpProtocolException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"data\":\"");
        for (int i = 0; i < 5000; i++) {
            sb.append("x");
        }
        sb.append("\"}");
        String json = sb.toString();
        StatusUpdateMessage original = new StatusUpdateMessage(json);

        byte[] encoded = encoder.encode(original);
        TcpMessage decoded = decoder.decode(encoded);

        assertTrue(decoded instanceof StatusUpdateMessage);
        assertEquals(json, ((StatusUpdateMessage) decoded).getJsonPayload());
    }

    // ========== Complex JSON round-trips ==========

    @Test
    public void roundTrip_statusUpdate_preservesComplexJson() throws TcpProtocolException {
        String json = "{\"deviceId\":\"abc-123\",\"status\":\"ON_TASK\","
                + "\"battery\":85,\"connected\":true,\"timestamp\":1702200000,"
                + "\"metadata\":{\"version\":\"1.0\",\"tags\":[\"test\",\"dev\"]}}";
        StatusUpdateMessage original = new StatusUpdateMessage(json);

        byte[] encoded = encoder.encode(original);
        TcpMessage decoded = decoder.decode(encoded);

        assertTrue(decoded instanceof StatusUpdateMessage);
        assertEquals(json, ((StatusUpdateMessage) decoded).getJsonPayload());
    }

    // ========== All message types ==========

    @Test
    public void roundTrip_allMessageTypes_succeed() throws TcpProtocolException {
        TcpMessage[] messages = {
                new LockScreenMessage(),
                new UnlockScreenMessage(),
                new RefreshConfigMessage(),
                new FetchMaterialsMessage(),
                new PairingAckMessage(),
                new StatusUpdateMessage("{\"test\":true}"),
                new HandRaisedMessage("device-id"),
                new PairingRequestMessage("device-id")
        };

        for (TcpMessage original : messages) {
            byte[] encoded = encoder.encode(original);
            TcpMessage decoded = decoder.decode(encoded);

            assertEquals(original.getOpcode(), decoded.getOpcode());
            assertEquals(original.getClass(), decoded.getClass());
        }
    }
}
