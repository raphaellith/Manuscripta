package com.manuscripta.student.di;

import com.manuscripta.student.network.udp.UdpDiscoveryManager;
import com.manuscripta.student.utils.MulticastLockManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Hilt module providing socket-related dependencies.
 * 
 * <p>Provides singleton instances for TCP socket managers
 * used in network communication. UDP discovery is handled by
 * {@link UdpDiscoveryManager} which uses constructor injection.</p>
 * 
 * <p>Note: UdpDiscoveryManager uses @Inject constructor with @Singleton,
 * so Hilt handles its injection directly without a @Provides method.</p>
 */
@Module
@InstallIn(SingletonComponent.class)
public class SocketModule {

    /**
     * Provides a singleton MulticastLockManager for UDP broadcast reception.
     *
     * @return MulticastLockManager instance
     */
    @Provides
    @Singleton
    public MulticastLockManager provideMulticastLockManager() {
        return new MulticastLockManager();
    }
}
