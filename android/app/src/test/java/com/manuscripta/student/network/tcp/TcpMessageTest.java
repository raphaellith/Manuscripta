package com.manuscripta.student.network.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for TcpMessage subclasses.
 */
public class TcpMessageTest {

    // ==================== LockScreenMessage Tests ====================

    @Test
    public void testLockScreenMessage_opcode() {
        LockScreenMessage message = new LockScreenMessage();
        assertEquals(TcpOpcode.LOCK_SCREEN, message.getOpcode());
    }

    @Test
    public void testLockScreenMessage_operand_isEmpty() {
        LockScreenMessage message = new LockScreenMessage();
        assertEquals(0, message.getOperand().length);
    }

    @Test
    public void testLockScreenMessage_hasOperand_returnsFalse() {
        LockScreenMessage message = new LockScreenMessage();
        assertFalse(message.hasOperand());
    }

    @Test
    public void testLockScreenMessage_toString() {
        LockScreenMessage message = new LockScreenMessage();
        String toString = message.toString();
        assertTrue(toString.contains("LockScreenMessage"));
    }

    // ==================== UnlockScreenMessage Tests ====================

    @Test
    public void testUnlockScreenMessage_opcode() {
        UnlockScreenMessage message = new UnlockScreenMessage();
        assertEquals(TcpOpcode.UNLOCK_SCREEN, message.getOpcode());
    }

    @Test
    public void testUnlockScreenMessage_operand_isEmpty() {
        UnlockScreenMessage message = new UnlockScreenMessage();
        assertEquals(0, message.getOperand().length);
    }


    // ==================== RefreshConfigMessage Tests ====================

    @Test
    public void testRefreshConfigMessage_opcode() {
        RefreshConfigMessage message = new RefreshConfigMessage();
        assertEquals(TcpOpcode.REFRESH_CONFIG, message.getOpcode());
    }

    @Test
    public void testRefreshConfigMessage_operand_isEmpty() {
        RefreshConfigMessage message = new RefreshConfigMessage();
        assertEquals(0, message.getOperand().length);
    }


    // ==================== FetchMaterialsMessage Tests ====================

    @Test
    public void testFetchMaterialsMessage_opcode() {
        FetchMaterialsMessage message = new FetchMaterialsMessage();
        assertEquals(TcpOpcode.FETCH_MATERIALS, message.getOpcode());
    }

    @Test
    public void testFetchMaterialsMessage_operand_isEmpty() {
        FetchMaterialsMessage message = new FetchMaterialsMessage();
        assertEquals(0, message.getOperand().length);
    }


    // ==================== PairingAckMessage Tests ====================

    @Test
    public void testPairingAckMessage_opcode() {
        PairingAckMessage message = new PairingAckMessage();
        assertEquals(TcpOpcode.PAIRING_ACK, message.getOpcode());
    }

    @Test
    public void testPairingAckMessage_operand_isEmpty() {
        PairingAckMessage message = new PairingAckMessage();
        assertEquals(0, message.getOperand().length);
    }


    // ==================== StatusUpdateMessage Tests ====================

    @Test
    public void testStatusUpdateMessage_opcode() {
        StatusUpdateMessage message = new StatusUpdateMessage("{\"battery\": 85}");
        assertEquals(TcpOpcode.STATUS_UPDATE, message.getOpcode());
    }

    @Test
    public void testStatusUpdateMessage_jsonPayload() {
        String json = "{\"battery\": 85, \"connected\": true}";
        StatusUpdateMessage message = new StatusUpdateMessage(json);
        assertEquals(json, message.getJsonPayload());
    }

    @Test
    public void testStatusUpdateMessage_operand_containsUtf8Json() {
        String json = "{\"status\": \"ok\"}";
        StatusUpdateMessage message = new StatusUpdateMessage(json);
        byte[] operand = message.getOperand();
        assertEquals(json, new String(operand, java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    public void testStatusUpdateMessage_hasOperand_returnsTrue() {
        StatusUpdateMessage message = new StatusUpdateMessage("{\"data\": 1}");
        assertTrue(message.hasOperand());
    }

    @Test
    public void testStatusUpdateMessage_operand_isDefensiveCopy() {
        StatusUpdateMessage message = new StatusUpdateMessage("{\"test\": true}");
        byte[] operand1 = message.getOperand();
        byte[] operand2 = message.getOperand();
        operand1[0] = 0;
        assertFalse(operand1[0] == operand2[0]);
    }

    @Test
    public void testStatusUpdateMessage_toString_shortPayload() {
        String json = "{\"short\": true}";
        StatusUpdateMessage message = new StatusUpdateMessage(json);
        String toString = message.toString();
        assertTrue(toString.contains("StatusUpdateMessage"));
        assertTrue(toString.contains(json));
    }

    @Test
    public void testStatusUpdateMessage_toString_longPayload_isTruncated() {
        String json = "{\"data\": \"" + "x".repeat(100) + "\"}";
        StatusUpdateMessage message = new StatusUpdateMessage(json);
        String toString = message.toString();
        assertTrue(toString.contains("..."));
        assertTrue(toString.length() < json.length() + 50);
    }

    @Test
    public void testStatusUpdateMessage_unicodePayload() {
        String json = "{\"message\": \"こんにちは\"}";
        StatusUpdateMessage message = new StatusUpdateMessage(json);
        assertEquals(json, message.getJsonPayload());
        byte[] operand = message.getOperand();
        assertEquals(json, new String(operand, java.nio.charset.StandardCharsets.UTF_8));
    }

    // ==================== HandRaisedMessage Tests ====================

    @Test
    public void testHandRaisedMessage_opcode() {
        HandRaisedMessage message = new HandRaisedMessage("device-123");
        assertEquals(TcpOpcode.HAND_RAISED, message.getOpcode());
    }

    @Test
    public void testHandRaisedMessage_deviceId() {
        String deviceId = "tablet-abc-456";
        HandRaisedMessage message = new HandRaisedMessage(deviceId);
        assertEquals(deviceId, message.getDeviceId());
    }

    @Test
    public void testHandRaisedMessage_operand_containsUtf8DeviceId() {
        String deviceId = "device-xyz";
        HandRaisedMessage message = new HandRaisedMessage(deviceId);
        byte[] operand = message.getOperand();
        assertEquals(deviceId, new String(operand, java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    public void testHandRaisedMessage_toString() {
        HandRaisedMessage message = new HandRaisedMessage("my-device");
        String toString = message.toString();
        assertTrue(toString.contains("HandRaisedMessage"));
        assertTrue(toString.contains("my-device"));
    }

    // ==================== PairingRequestMessage Tests ====================

    @Test
    public void testPairingRequestMessage_opcode() {
        PairingRequestMessage message = new PairingRequestMessage("device-123");
        assertEquals(TcpOpcode.PAIRING_REQUEST, message.getOpcode());
    }

    @Test
    public void testPairingRequestMessage_deviceId() {
        String deviceId = "student-tablet-001";
        PairingRequestMessage message = new PairingRequestMessage(deviceId);
        assertEquals(deviceId, message.getDeviceId());
    }

    @Test
    public void testPairingRequestMessage_operand_containsUtf8DeviceId() {
        String deviceId = "pairing-device";
        PairingRequestMessage message = new PairingRequestMessage(deviceId);
        byte[] operand = message.getOperand();
        assertEquals(deviceId, new String(operand, java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    public void testPairingRequestMessage_toString() {
        PairingRequestMessage message = new PairingRequestMessage("pair-device");
        String toString = message.toString();
        assertTrue(toString.contains("PairingRequestMessage"));
        assertTrue(toString.contains("pair-device"));
    }

    // ==================== Empty String Edge Cases ====================

    @Test
    public void testStatusUpdateMessage_emptyPayload() {
        StatusUpdateMessage message = new StatusUpdateMessage("");
        assertEquals("", message.getJsonPayload());
        assertEquals(0, message.getOperand().length);
        assertFalse(message.hasOperand());
    }

    @Test
    public void testHandRaisedMessage_emptyDeviceId() {
        HandRaisedMessage message = new HandRaisedMessage("");
        assertEquals("", message.getDeviceId());
        assertEquals(0, message.getOperand().length);
        assertFalse(message.hasOperand());
    }

    @Test
    public void testPairingRequestMessage_emptyDeviceId() {
        PairingRequestMessage message = new PairingRequestMessage("");
        assertEquals("", message.getDeviceId());
        assertEquals(0, message.getOperand().length);
        assertFalse(message.hasOperand());
    }
}
