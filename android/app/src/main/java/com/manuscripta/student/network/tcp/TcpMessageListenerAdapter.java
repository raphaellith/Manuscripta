package com.manuscripta.student.network.tcp;

import androidx.annotation.NonNull;

/**
 * Adapter class providing empty default implementations of {@link TcpMessageListener} methods.
 *
 * <p>This convenience class allows listeners to override only the methods they are
 * interested in, rather than implementing all three methods of the interface.
 *
 * <p>Example usage:
 * <pre>{@code
 * tcpSocketManager.addMessageListener(new TcpMessageListenerAdapter() {
 *     @Override
 *     public void onMessageReceived(@NonNull TcpMessage message) {
 *         // Handle incoming messages only
 *     }
 * });
 * }</pre>
 *
 * @see TcpMessageListener
 * @see TcpSocketManager
 */
public class TcpMessageListenerAdapter implements TcpMessageListener {

    /**
     * Creates a new TcpMessageListenerAdapter.
     */
    public TcpMessageListenerAdapter() {
        // Empty constructor
    }

    /**
     * {@inheritDoc}
     *
     * <p>Default implementation does nothing. Override to handle incoming messages.
     */
    @Override
    public void onMessageReceived(@NonNull TcpMessage message) {
        // Default empty implementation
    }

    /**
     * {@inheritDoc}
     *
     * <p>Default implementation does nothing. Override to handle connection state changes.
     */
    @Override
    public void onConnectionStateChanged(@NonNull ConnectionState state) {
        // Default empty implementation
    }

    /**
     * {@inheritDoc}
     *
     * <p>Default implementation does nothing. Override to handle errors.
     */
    @Override
    public void onError(@NonNull TcpProtocolException error) {
        // Default empty implementation
    }
}
