package com.manuscripta.student.network.udp;

/**
 * Represents the discovery state of the UDP discovery manager.
 * Used to track and expose discovery lifecycle to the UI layer.
 */
public enum DiscoveryState {
    /**
     * Not searching for servers.
     */
    IDLE,

    /**
     * Actively listening for UDP broadcast messages.
     */
    SEARCHING,

    /**
     * Server discovered successfully.
     */
    FOUND,

    /**
     * No server found within the configured timeout period.
     */
    TIMEOUT,

    /**
     * An error occurred (socket or permission error).
     */
    ERROR
}
