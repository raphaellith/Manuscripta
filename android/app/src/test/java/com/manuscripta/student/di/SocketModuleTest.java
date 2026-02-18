package com.manuscripta.student.di;

import static org.junit.Assert.assertNotNull;

import com.manuscripta.student.utils.MulticastLockManager;

import org.junit.Test;

/**
 * Unit tests for {@link SocketModule}.
 *
 * <p>Note: UdpDiscoveryManager uses constructor injection with @Inject and @Singleton,
 * so it doesn't require a @Provides method in SocketModule.</p>
 */
public class SocketModuleTest {

    @Test
    public void testModuleInstantiation_createsNonNull() {
        // Given/When
        SocketModule module = new SocketModule();

        // Then
        assertNotNull(module);
    }

    @Test
    public void testProvideMulticastLockManager_returnsInstance() {
        SocketModule module = new SocketModule();

        MulticastLockManager result = module.provideMulticastLockManager();

        assertNotNull(result);
    }
}
