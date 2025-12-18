package com.manuscripta.student.network.udp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link DiscoveryState} enum.
 */
public class DiscoveryStateTest {

    @Test
    public void values_containsAllStates() {
        // When
        DiscoveryState[] values = DiscoveryState.values();

        // Then
        assertEquals(5, values.length);
        assertEquals(DiscoveryState.IDLE, values[0]);
        assertEquals(DiscoveryState.SEARCHING, values[1]);
        assertEquals(DiscoveryState.FOUND, values[2]);
        assertEquals(DiscoveryState.TIMEOUT, values[3]);
        assertEquals(DiscoveryState.ERROR, values[4]);
    }

    @Test
    public void valueOf_returnsIdleState() {
        // When
        DiscoveryState state = DiscoveryState.valueOf("IDLE");

        // Then
        assertEquals(DiscoveryState.IDLE, state);
    }

    @Test
    public void valueOf_returnsSearchingState() {
        // When
        DiscoveryState state = DiscoveryState.valueOf("SEARCHING");

        // Then
        assertEquals(DiscoveryState.SEARCHING, state);
    }

    @Test
    public void valueOf_returnsFoundState() {
        // When
        DiscoveryState state = DiscoveryState.valueOf("FOUND");

        // Then
        assertEquals(DiscoveryState.FOUND, state);
    }

    @Test
    public void valueOf_returnsTimeoutState() {
        // When
        DiscoveryState state = DiscoveryState.valueOf("TIMEOUT");

        // Then
        assertEquals(DiscoveryState.TIMEOUT, state);
    }

    @Test
    public void valueOf_returnsErrorState() {
        // When
        DiscoveryState state = DiscoveryState.valueOf("ERROR");

        // Then
        assertEquals(DiscoveryState.ERROR, state);
    }

    @Test(expected = IllegalArgumentException.class)
    public void valueOf_throwsForInvalidState() {
        // When
        DiscoveryState.valueOf("INVALID_STATE");
    }
}
