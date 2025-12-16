package com.manuscripta.student.network.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void testRefreshConfigMessage_hasOperand_returnsFalse() {
        RefreshConfigMessage message = new RefreshConfigMessage();
        assertFalse(message.hasOperand());
    }

    @Test
    public void testRefreshConfigMessage_toString() {
        RefreshConfigMessage message = new RefreshConfigMessage();
        String toString = message.toString();
        assertTrue(toString.contains("RefreshConfigMessage"));
    }

    // ==================== UnpairMessage Tests ====================

    @Test
    public void testUnpairMessage_opcode() {
        UnpairMessage message = new UnpairMessage();
        assertEquals(TcpOpcode.UNPAIR, message.getOpcode());
    }

    @Test
    public void testUnpairMessage_operand_isEmpty() {
        UnpairMessage message = new UnpairMessage();
        assertEquals(0, message.getOperand().length);
    }

    @Test
    public void testUnpairMessage_hasOperand_returnsFalse() {
        UnpairMessage message = new UnpairMessage();
        assertFalse(message.hasOperand());
    }

    @Test
    public void testUnpairMessage_toString() {
        UnpairMessage message = new UnpairMessage();
        String toString = message.toString();
        assertTrue(toString.contains("UnpairMessage"));
    }

    // ==================== DistributeMaterialMessage Tests ====================

    @Test
    public void testDistributeMaterialMessage_opcode() {
        DistributeMaterialMessage message = new DistributeMaterialMessage();
        assertEquals(TcpOpcode.DISTRIBUTE_MATERIAL, message.getOpcode());
    }

    @Test
    public void testDistributeMaterialMessage_operand_isEmpty() {
        DistributeMaterialMessage message = new DistributeMaterialMessage();
        assertEquals(0, message.getOperand().length);
    }

    @Test
    public void testDistributeMaterialMessage_hasOperand_returnsFalse() {
        DistributeMaterialMessage message = new DistributeMaterialMessage();
        assertFalse(message.hasOperand());
    }

    @Test
    public void testDistributeMaterialMessage_toString() {
        DistributeMaterialMessage message = new DistributeMaterialMessage();
        String toString = message.toString();
        assertTrue(toString.contains("DistributeMaterialMessage"));
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
    public void testHandRaisedMessage_hasOperand_returnsTrue() {
        HandRaisedMessage message = new HandRaisedMessage("device");
        assertTrue(message.hasOperand());
    }

    @Test
    public void testHandRaisedMessage_operand_isDefensiveCopy() {
        HandRaisedMessage message = new HandRaisedMessage("device-123");
        byte[] operand1 = message.getOperand();
        byte[] operand2 = message.getOperand();
        operand1[0] = 0;
        assertFalse(operand1[0] == operand2[0]);
    }

    @Test
    public void testDistributeAckMessage_toString() {
        DistributeAckMessage message = new DistributeAckMessage("my-device");
        String toString = message.toString();
        assertTrue(toString.contains("DistributeAckMessage"));
        assertTrue(toString.contains("my-device"));
    }

    // ==================== HandAckMessage Tests ====================

    @Test
    public void testHandAckMessage_opcode() {
        HandAckMessage message = new HandAckMessage("device-123");
        assertEquals(TcpOpcode.HAND_ACK, message.getOpcode());
    }

    @Test
    public void testHandAckMessage_deviceId() {
        String deviceId = "tablet-abc-456";
        HandAckMessage message = new HandAckMessage(deviceId);
        assertEquals(deviceId, message.getDeviceId());
    }

    @Test
    public void testHandAckMessage_operand_containsUtf8DeviceId() {
        String deviceId = "device-xyz";
        HandAckMessage message = new HandAckMessage(deviceId);
        byte[] operand = message.getOperand();
        assertEquals(deviceId, new String(operand, java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    public void testHandAckMessage_hasOperand_returnsTrue() {
        HandAckMessage message = new HandAckMessage("device");
        assertTrue(message.hasOperand());
    }

    @Test
    public void testHandAckMessage_operand_isDefensiveCopy() {
        HandAckMessage message = new HandAckMessage("device-123");
        byte[] operand1 = message.getOperand();
        byte[] operand2 = message.getOperand();
        operand1[0] = 0;
        assertFalse(operand1[0] == operand2[0]);
    }

    @Test
    public void testHandAckMessage_toString() {
        HandAckMessage message = new HandAckMessage("my-device");
        String toString = message.toString();
        assertTrue(toString.contains("HandAckMessage"));
        assertTrue(toString.contains("my-device"));
    }

    // ==================== DistributeAckMessage Tests ====================

    @Test
    public void testDistributeAckMessage_opcode() {
        DistributeAckMessage message = new DistributeAckMessage("device-123");
        assertEquals(TcpOpcode.DISTRIBUTE_ACK, message.getOpcode());
    }

    @Test
    public void testDistributeAckMessage_deviceId() {
        String deviceId = "tablet-abc-456";
        DistributeAckMessage message = new DistributeAckMessage(deviceId);
        assertEquals(deviceId, message.getDeviceId());
    }

    @Test
    public void testDistributeAckMessage_operand_containsUtf8DeviceId() {
        String deviceId = "device-xyz";
        DistributeAckMessage message = new DistributeAckMessage(deviceId);
        byte[] operand = message.getOperand();
        assertEquals(deviceId, new String(operand, java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    public void testDistributeAckMessage_hasOperand_returnsTrue() {
        DistributeAckMessage message = new DistributeAckMessage("device");
        assertTrue(message.hasOperand());
    }

    @Test
    public void testDistributeAckMessage_operand_isDefensiveCopy() {
        DistributeAckMessage message = new DistributeAckMessage("device-123");
        byte[] operand1 = message.getOperand();
        byte[] operand2 = message.getOperand();
        operand1[0] = 0;
        assertFalse(operand1[0] == operand2[0]);
    }

    @Test
    public void testDistributeAckMessage_toString() {
        DistributeAckMessage message = new DistributeAckMessage("my-device");
        String toString = message.toString();
        assertTrue(toString.contains("DistributeAckMessage"));
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
    public void testHandAckMessage_emptyDeviceId() {
        HandAckMessage message = new HandAckMessage("");
        assertEquals("", message.getDeviceId());
        assertEquals(0, message.getOperand().length);
        assertFalse(message.hasOperand());
    }

    @Test
    public void testDistributeAckMessage_emptyDeviceId() {
        DistributeAckMessage message = new DistributeAckMessage("");
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
