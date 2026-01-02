package com.manuscripta.student.network.udp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Unit tests for {@link DiscoveryState} enum.
 * 
 * <p>Note: Tests for built-in Java enum methods (values(), valueOf()) have been
 * intentionally omitted as they test guaranteed Java language behavior rather
 * than application-specific logic.</p>
 */
public class DiscoveryStateTest {

    @Test
    public void discoveryState_hasExpectedNumberOfStates() {
        // Verify the enum has the expected number of states
        // This serves as a regression test if states are accidentally added/removed
        assertEquals("DiscoveryState should have exactly 5 states", 
                5, DiscoveryState.values().length);
    }

    @Test
    public void discoveryState_allStatesExist() {
        // Verify all expected states are defined (compile-time check essentially)
        assertNotNull(DiscoveryState.IDLE);
        assertNotNull(DiscoveryState.SEARCHING);
        assertNotNull(DiscoveryState.FOUND);
        assertNotNull(DiscoveryState.TIMEOUT);
        assertNotNull(DiscoveryState.ERROR);
    }
}
