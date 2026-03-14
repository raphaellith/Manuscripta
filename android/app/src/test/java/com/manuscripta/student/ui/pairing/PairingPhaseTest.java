package com.manuscripta.student.ui.pairing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Unit tests for {@link PairingPhase}.
 */
public class PairingPhaseTest {

    @Test
    public void allPhasesExist() {
        PairingPhase[] values = PairingPhase.values();
        assertEquals(6, values.length);
    }

    @Test
    public void valueOfIdle() {
        assertEquals(PairingPhase.IDLE, PairingPhase.valueOf("IDLE"));
    }

    @Test
    public void valueOfDiscovering() {
        assertEquals(PairingPhase.DISCOVERING, PairingPhase.valueOf("DISCOVERING"));
    }

    @Test
    public void valueOfTcpPairing() {
        assertEquals(PairingPhase.TCP_PAIRING, PairingPhase.valueOf("TCP_PAIRING"));
    }

    @Test
    public void valueOfHttpRegistering() {
        assertNotNull(PairingPhase.HTTP_REGISTERING);
        assertEquals(PairingPhase.HTTP_REGISTERING,
                PairingPhase.valueOf("HTTP_REGISTERING"));
    }

    @Test
    public void valueOfPaired() {
        assertEquals(PairingPhase.PAIRED, PairingPhase.valueOf("PAIRED"));
    }

    @Test
    public void valueOfError() {
        assertEquals(PairingPhase.ERROR, PairingPhase.valueOf("ERROR"));
    }

    @Test
    public void ordinalValues_areSequential() {
        assertEquals(0, PairingPhase.IDLE.ordinal());
        assertEquals(1, PairingPhase.DISCOVERING.ordinal());
        assertEquals(2, PairingPhase.TCP_PAIRING.ordinal());
        assertEquals(3, PairingPhase.HTTP_REGISTERING.ordinal());
        assertEquals(4, PairingPhase.PAIRED.ordinal());
        assertEquals(5, PairingPhase.ERROR.ordinal());
    }
}
