package com.manuscripta.student.network.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Unit tests for {@link PairingState}.
 */
public class PairingStateTest {

    @Test
    public void values_containsAllExpectedStates() {
        PairingState[] values = PairingState.values();

        assertEquals(5, values.length);
    }

    @Test
    public void valueOf_returnsCorrectEnumValues() {
        assertEquals(PairingState.NOT_PAIRED, PairingState.valueOf("NOT_PAIRED"));
        assertEquals(PairingState.PAIRING_IN_PROGRESS, PairingState.valueOf("PAIRING_IN_PROGRESS"));
        assertEquals(PairingState.PAIRED, PairingState.valueOf("PAIRED"));
        assertEquals(PairingState.PAIRING_FAILED, PairingState.valueOf("PAIRING_FAILED"));
        assertEquals(PairingState.PAIRING_TIMEOUT, PairingState.valueOf("PAIRING_TIMEOUT"));
    }

    @Test
    public void allStates_areNotNull() {
        for (PairingState state : PairingState.values()) {
            assertNotNull(state);
            assertNotNull(state.name());
        }
    }
}
