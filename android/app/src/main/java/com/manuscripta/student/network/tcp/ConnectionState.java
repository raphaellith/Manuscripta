package com.manuscripta.student.network.tcp;

/**
 * Represents the connection state of a TCP socket.
 * Used to track and expose connection lifecycle to the UI layer.
 */
public enum ConnectionState {
    /**
     * Socket is disconnected and not attempting to connect.
     */
    DISCONNECTED,

    /**
     * Socket is actively attempting to establish a connection.
     */
    CONNECTING,

    /**
     * Socket is connected and ready for communication.
     */
    CONNECTED,

    /**
     * Socket lost connection and is attempting to reconnect.
     */
    RECONNECTING
}
