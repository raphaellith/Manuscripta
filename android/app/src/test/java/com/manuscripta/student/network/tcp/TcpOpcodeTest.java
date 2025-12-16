package com.manuscripta.student.network.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the TcpOpcode enum.
 */
public class TcpOpcodeTest {

    // ==================== getValue Tests ====================

    @Test
    public void testLockScreen_getValue() {
        assertEquals((byte) 0x01, TcpOpcode.LOCK_SCREEN.getValue());
    }

    @Test
    public void testUnlockScreen_getValue() {
        assertEquals((byte) 0x02, TcpOpcode.UNLOCK_SCREEN.getValue());
    }

    @Test
    public void testRefreshConfig_getValue() {
        assertEquals((byte) 0x03, TcpOpcode.REFRESH_CONFIG.getValue());
    }

    @Test
    public void testUnpair_getValue() {
        assertEquals((byte) 0x04, TcpOpcode.UNPAIR.getValue());
    }

    @Test
    public void testDistributeMaterial_getValue() {
        assertEquals((byte) 0x05, TcpOpcode.DISTRIBUTE_MATERIAL.getValue());
    }

    @Test
    public void testHandAck_getValue() {
        assertEquals((byte) 0x06, TcpOpcode.HAND_ACK.getValue());
    }

    @Test
    public void testStatusUpdate_getValue() {
        assertEquals((byte) 0x10, TcpOpcode.STATUS_UPDATE.getValue());
    }

    @Test
    public void testHandRaised_getValue() {
        assertEquals((byte) 0x11, TcpOpcode.HAND_RAISED.getValue());
    }

    @Test
    public void testDistributeAck_getValue() {
        assertEquals((byte) 0x12, TcpOpcode.DISTRIBUTE_ACK.getValue());
    }

    @Test
    public void testPairingRequest_getValue() {
        assertEquals((byte) 0x20, TcpOpcode.PAIRING_REQUEST.getValue());
    }

    @Test
    public void testPairingAck_getValue() {
        assertEquals((byte) 0x21, TcpOpcode.PAIRING_ACK.getValue());
    }

    // ==================== fromValue Tests ====================

    @Test
    public void testFromValue_lockScreen() {
        assertEquals(TcpOpcode.LOCK_SCREEN, TcpOpcode.fromValue((byte) 0x01));
    }

    @Test
    public void testFromValue_unlockScreen() {
        assertEquals(TcpOpcode.UNLOCK_SCREEN, TcpOpcode.fromValue((byte) 0x02));
    }

    @Test
    public void testFromValue_refreshConfig() {
        assertEquals(TcpOpcode.REFRESH_CONFIG, TcpOpcode.fromValue((byte) 0x03));
    }

    @Test
    public void testFromValue_unpair() {
        assertEquals(TcpOpcode.UNPAIR, TcpOpcode.fromValue((byte) 0x04));
    }

    @Test
    public void testFromValue_distributeMaterial() {
        assertEquals(TcpOpcode.DISTRIBUTE_MATERIAL, TcpOpcode.fromValue((byte) 0x05));
    }

    @Test
    public void testFromValue_handAck() {
        assertEquals(TcpOpcode.HAND_ACK, TcpOpcode.fromValue((byte) 0x06));
    }

    @Test
    public void testFromValue_statusUpdate() {
        assertEquals(TcpOpcode.STATUS_UPDATE, TcpOpcode.fromValue((byte) 0x10));
    }

    @Test
    public void testFromValue_handRaised() {
        assertEquals(TcpOpcode.HAND_RAISED, TcpOpcode.fromValue((byte) 0x11));
    }

    @Test
    public void testFromValue_distributeAck() {
        assertEquals(TcpOpcode.DISTRIBUTE_ACK, TcpOpcode.fromValue((byte) 0x12));
    }

    @Test
    public void testFromValue_pairingRequest() {
        assertEquals(TcpOpcode.PAIRING_REQUEST, TcpOpcode.fromValue((byte) 0x20));
    }

    @Test
    public void testFromValue_pairingAck() {
        assertEquals(TcpOpcode.PAIRING_ACK, TcpOpcode.fromValue((byte) 0x21));
    }

    @Test
    public void testFromValue_unknownOpcode_returnsNull() {
        assertNull(TcpOpcode.fromValue((byte) 0x00));
        assertNull(TcpOpcode.fromValue((byte) 0xFF));
        assertNull(TcpOpcode.fromValue((byte) 0x07));
    }

    // ==================== isServerToClient Tests ====================

    @Test
    public void testIsServerToClient_lockScreen() {
        assertTrue(TcpOpcode.LOCK_SCREEN.isServerToClient());
    }

    @Test
    public void testIsServerToClient_unlockScreen() {
        assertTrue(TcpOpcode.UNLOCK_SCREEN.isServerToClient());
    }

    @Test
    public void testIsServerToClient_refreshConfig() {
        assertTrue(TcpOpcode.REFRESH_CONFIG.isServerToClient());
    }

    @Test
    public void testIsServerToClient_unpair() {
        assertTrue(TcpOpcode.UNPAIR.isServerToClient());
    }

    @Test
    public void testIsServerToClient_distributeMaterial() {
        assertTrue(TcpOpcode.DISTRIBUTE_MATERIAL.isServerToClient());
    }

    @Test
    public void testIsServerToClient_handAck() {
        assertTrue(TcpOpcode.HAND_ACK.isServerToClient());
    }

    @Test
    public void testIsServerToClient_pairingAck() {
        assertTrue(TcpOpcode.PAIRING_ACK.isServerToClient());
    }

    @Test
    public void testIsServerToClient_statusUpdate_returnsFalse() {
        assertFalse(TcpOpcode.STATUS_UPDATE.isServerToClient());
    }

    @Test
    public void testIsServerToClient_handRaised_returnsFalse() {
        assertFalse(TcpOpcode.HAND_RAISED.isServerToClient());
    }

    @Test
    public void testIsServerToClient_distributeAck_returnsFalse() {
        assertFalse(TcpOpcode.DISTRIBUTE_ACK.isServerToClient());
    }

    @Test
    public void testIsServerToClient_pairingRequest_returnsFalse() {
        assertFalse(TcpOpcode.PAIRING_REQUEST.isServerToClient());
    }

    // ==================== isClientToServer Tests ====================

    @Test
    public void testIsClientToServer_statusUpdate() {
        assertTrue(TcpOpcode.STATUS_UPDATE.isClientToServer());
    }

    @Test
    public void testIsClientToServer_handRaised() {
        assertTrue(TcpOpcode.HAND_RAISED.isClientToServer());
    }

    @Test
    public void testIsClientToServer_distributeAck() {
        assertTrue(TcpOpcode.DISTRIBUTE_ACK.isClientToServer());
    }

    @Test
    public void testIsClientToServer_pairingRequest() {
        assertTrue(TcpOpcode.PAIRING_REQUEST.isClientToServer());
    }

    @Test
    public void testIsClientToServer_lockScreen_returnsFalse() {
        assertFalse(TcpOpcode.LOCK_SCREEN.isClientToServer());
    }

    @Test
    public void testIsClientToServer_unlockScreen_returnsFalse() {
        assertFalse(TcpOpcode.UNLOCK_SCREEN.isClientToServer());
    }

    @Test
    public void testIsClientToServer_refreshConfig_returnsFalse() {
        assertFalse(TcpOpcode.REFRESH_CONFIG.isClientToServer());
    }

    @Test
    public void testIsClientToServer_unpair_returnsFalse() {
        assertFalse(TcpOpcode.UNPAIR.isClientToServer());
    }

    @Test
    public void testIsClientToServer_distributeMaterial_returnsFalse() {
        assertFalse(TcpOpcode.DISTRIBUTE_MATERIAL.isClientToServer());
    }

    @Test
    public void testIsClientToServer_handAck_returnsFalse() {
        assertFalse(TcpOpcode.HAND_ACK.isClientToServer());
    }

    @Test
    public void testIsClientToServer_pairingAck_returnsFalse() {
        assertFalse(TcpOpcode.PAIRING_ACK.isClientToServer());
    }

    // ==================== toString Tests ====================

    @Test
    public void testToString_lockScreen() {
        assertEquals("LOCK_SCREEN(0x01)", TcpOpcode.LOCK_SCREEN.toString());
    }

    @Test
    public void testToString_statusUpdate() {
        assertEquals("STATUS_UPDATE(0x10)", TcpOpcode.STATUS_UPDATE.toString());
    }

    @Test
    public void testToString_pairingRequest() {
        assertEquals("PAIRING_REQUEST(0x20)", TcpOpcode.PAIRING_REQUEST.toString());
    }

    // ==================== Enum completeness ====================

    @Test
    public void testAllOpcodesPresent() {
        assertEquals(11, TcpOpcode.values().length);
    }
}
