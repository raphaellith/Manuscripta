package com.manuscripta.student.network.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.manuscripta.student.network.tcp.message.LockScreenMessage;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link TcpMessageListenerAdapter}.
 */
public class TcpMessageListenerAdapterTest {

    private TcpMessageListenerAdapter adapter;

    @Before
    public void setUp() {
        adapter = new TcpMessageListenerAdapter();
    }

    @Test
    public void constructor_createsInstance() {
        assertNotNull(adapter);
    }

    @Test
    public void onMessageReceived_doesNotThrow() {
        // Default implementation should do nothing and not throw
        TcpMessage message = new LockScreenMessage();
        adapter.onMessageReceived(message);
        // Test passes if no exception is thrown
    }

    @Test
    public void onConnectionStateChanged_doesNotThrow() {
        // Default implementation should do nothing and not throw
        adapter.onConnectionStateChanged(ConnectionState.CONNECTED);
        adapter.onConnectionStateChanged(ConnectionState.DISCONNECTED);
        adapter.onConnectionStateChanged(ConnectionState.CONNECTING);
        adapter.onConnectionStateChanged(ConnectionState.RECONNECTING);
        // Test passes if no exception is thrown
    }

    @Test
    public void onError_doesNotThrow() {
        // Default implementation should do nothing and not throw
        TcpProtocolException error = new TcpProtocolException("Test error");
        adapter.onError(error);
        // Test passes if no exception is thrown
    }

    @Test
    public void subclass_canOverrideOnMessageReceived() {
        final boolean[] called = {false};

        TcpMessageListenerAdapter customAdapter = new TcpMessageListenerAdapter() {
            @Override
            public void onMessageReceived(TcpMessage message) {
                called[0] = true;
            }
        };

        customAdapter.onMessageReceived(new LockScreenMessage());
        assertTrue(called[0]);
    }

    @Test
    public void subclass_canOverrideOnConnectionStateChanged() {
        final boolean[] called = {false};

        TcpMessageListenerAdapter customAdapter = new TcpMessageListenerAdapter() {
            @Override
            public void onConnectionStateChanged(ConnectionState state) {
                called[0] = true;
            }
        };

        customAdapter.onConnectionStateChanged(ConnectionState.CONNECTED);
        assertTrue(called[0]);
    }

    @Test
    public void subclass_canOverrideOnError() {
        final boolean[] called = {false};

        TcpMessageListenerAdapter customAdapter = new TcpMessageListenerAdapter() {
            @Override
            public void onError(TcpProtocolException error) {
                called[0] = true;
            }
        };

        customAdapter.onError(new TcpProtocolException("Test"));
        assertTrue(called[0]);
    }

    @Test
    public void subclass_canOverrideSelectiveMethods() {
        // Verify that only overriding one method works without affecting others
        final int[] callCounts = {0, 0, 0};

        TcpMessageListenerAdapter customAdapter = new TcpMessageListenerAdapter() {
            @Override
            public void onMessageReceived(TcpMessage message) {
                callCounts[0]++;
            }
            // Not overriding onConnectionStateChanged or onError
        };

        customAdapter.onMessageReceived(new LockScreenMessage());
        customAdapter.onConnectionStateChanged(ConnectionState.CONNECTED);
        customAdapter.onError(new TcpProtocolException("Test"));

        assertEquals(1, callCounts[0]);
        // Other methods should have been called with default (no-op) implementation
    }
}
