package com.manuscripta.student.network.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Unit tests for {@link ConnectionState}.
 */
public class ConnectionStateTest {

    @Test
    public void enum_hasAllExpectedValues() {
        ConnectionState[] values = ConnectionState.values();

        assertEquals(4, values.length);
    }

    @Test
    public void disconnected_exists() {
        ConnectionState state = ConnectionState.DISCONNECTED;

        assertNotNull(state);
        assertEquals("DISCONNECTED", state.name());
    }

    @Test
    public void connecting_exists() {
        ConnectionState state = ConnectionState.CONNECTING;

        assertNotNull(state);
        assertEquals("CONNECTING", state.name());
    }

    @Test
    public void connected_exists() {
        ConnectionState state = ConnectionState.CONNECTED;

        assertNotNull(state);
        assertEquals("CONNECTED", state.name());
    }

    @Test
    public void reconnecting_exists() {
        ConnectionState state = ConnectionState.RECONNECTING;

        assertNotNull(state);
        assertEquals("RECONNECTING", state.name());
    }

    @Test
    public void valueOf_returnsCorrectValue() {
        assertEquals(ConnectionState.DISCONNECTED, ConnectionState.valueOf("DISCONNECTED"));
        assertEquals(ConnectionState.CONNECTING, ConnectionState.valueOf("CONNECTING"));
        assertEquals(ConnectionState.CONNECTED, ConnectionState.valueOf("CONNECTED"));
        assertEquals(ConnectionState.RECONNECTING, ConnectionState.valueOf("RECONNECTING"));
    }

    @Test
    public void ordinal_valuesAreSequential() {
        assertEquals(0, ConnectionState.DISCONNECTED.ordinal());
        assertEquals(1, ConnectionState.CONNECTING.ordinal());
        assertEquals(2, ConnectionState.CONNECTED.ordinal());
        assertEquals(3, ConnectionState.RECONNECTING.ordinal());
    }
}
