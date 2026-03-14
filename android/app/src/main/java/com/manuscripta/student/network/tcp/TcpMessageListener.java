package com.manuscripta.student.network.tcp;

import androidx.annotation.NonNull;

/**
 * Listener interface for receiving TCP message events from {@link TcpSocketManager}.
 *
 * <p>Implementations of this interface can be registered with a {@link TcpSocketManager}
 * to receive notifications about incoming messages, connection state changes, and errors.
 *
 * <p>All callback methods are dispatched on the main (UI) thread for safe UI updates.
 *
 * @see TcpSocketManager#addMessageListener(TcpMessageListener)
 * @see TcpSocketManager#removeMessageListener(TcpMessageListener)
 * @see TcpMessageListenerAdapter
 */
public interface TcpMessageListener {

    /**
     * Called when a message is received from the server.
     *
     * <p>This method is invoked on the main thread after a TCP message has been
     * successfully decoded by the {@link TcpMessageDecoder}.
     *
     * @param message The received message. Never null.
     */
    void onMessageReceived(@NonNull TcpMessage message);

    /**
     * Called when the connection state changes.
     *
     * <p>This method is invoked on the main thread whenever the socket connection
     * transitions between states (e.g., DISCONNECTED to CONNECTING to CONNECTED).
     *
     * @param state The new connection state. Never null.
     */
    void onConnectionStateChanged(@NonNull ConnectionState state);

    /**
     * Called when an error occurs during TCP communication.
     *
     * <p>This method is invoked on the main thread when errors occur during
     * message decoding, socket operations, or other TCP-related operations.
     *
     * @param error The error that occurred. Never null.
     */
    void onError(@NonNull TcpProtocolException error);
}
