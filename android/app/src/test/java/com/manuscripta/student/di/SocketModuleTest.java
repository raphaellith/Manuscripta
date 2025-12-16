package com.manuscripta.student.di;

import static org.junit.Assert.assertNotNull;

import com.manuscripta.student.network.UdpDiscoveryManager;

import org.junit.Test;

/**
 * Unit tests for {@link SocketModule}.
 */
public class SocketModuleTest {

    @Test
    public void testProvideUdpDiscoveryManager_returnsNonNull() {
        // Given
        SocketModule module = new SocketModule();

        // When
        UdpDiscoveryManager manager = module.provideUdpDiscoveryManager();

        // Then
        assertNotNull(manager);
    }
}
