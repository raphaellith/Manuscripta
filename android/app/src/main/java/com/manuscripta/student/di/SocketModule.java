package com.manuscripta.student.di;

import com.manuscripta.student.network.udp.UdpDiscoveryManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Hilt module providing socket-related dependencies.
 * 
 * <p>Provides singleton instances for TCP and UDP socket managers
 * used in network communication.</p>
 */
@Module
@InstallIn(SingletonComponent.class)
public class SocketModule {

    /**
     * Provides the UdpDiscoveryManager singleton instance.
     *
     * @return UdpDiscoveryManager instance for UDP discovery
     */
    @Provides
    @Singleton
    public UdpDiscoveryManager provideUdpDiscoveryManager() {
        return new UdpDiscoveryManager();
    }
}
