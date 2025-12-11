package com.manuscripta.student.network.tcp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages TCP socket connections for communication with the teacher's desktop application.
 * Provides connection lifecycle management, message sending/receiving, and automatic
 * reconnection with exponential backoff.
 *
 * <p>This class is thread-safe and exposes connection state via LiveData for UI observation.
 */
@Singleton
public class TcpSocketManager {

    /**
     * Initial delay for reconnection attempts in milliseconds.
     */
    private static final long INITIAL_RECONNECT_DELAY_MS = 1000L;

    /**
     * Maximum delay for reconnection attempts in milliseconds.
     */
    private static final long MAX_RECONNECT_DELAY_MS = 8000L;

    /**
     * Multiplier for exponential backoff.
     */
    private static final int BACKOFF_MULTIPLIER = 2;

    /**
     * Buffer size for reading from socket.
     */
    private static final int READ_BUFFER_SIZE = 4096;

    /** The message encoder for serialising outgoing messages. */
    private final TcpMessageEncoder encoder;
    /** The message decoder for deserialising incoming messages. */
    private final TcpMessageDecoder decoder;
    /** LiveData for connection state observation. */
    private final MutableLiveData<ConnectionState> connectionState;
    /** Lock object for synchronising socket operations. */
    private final Object socketLock = new Object();
    /** Flag indicating whether automatic reconnection should be attempted. */
    private final AtomicBoolean shouldReconnect = new AtomicBoolean(false);

    /** The TCP socket connection. */
    @Nullable
    private Socket socket;
    /** The output stream for sending data. */
    @Nullable
    private BufferedOutputStream outputStream;
    /** The input stream for receiving data. */
    @Nullable
    private BufferedInputStream inputStream;
    /** The executor service for the reader thread. */
    @Nullable
    private ExecutorService readerExecutor;
    /** The host address for the current connection. */
    @Nullable
    private String currentHost;
    /** The port number for the current connection. */
    private int currentPort;
    /** The current delay for reconnection attempts. */
    private long currentReconnectDelay = INITIAL_RECONNECT_DELAY_MS;

    /** The listener for receiving messages and errors. */
    @Nullable
    private MessageListener messageListener;

    /**
     * Listener interface for receiving TCP messages.
     */
    public interface MessageListener {
        /**
         * Called when a message is received from the server.
         *
         * @param message The received message.
         */
        void onMessageReceived(@NonNull TcpMessage message);

        /**
         * Called when an error occurs during message processing.
         *
         * @param error The error that occurred.
         */
        void onError(@NonNull Exception error);
    }

    /**
     * Creates a new TcpSocketManager with the specified encoder and decoder.
     *
     * @param encoder The message encoder for serialising outgoing messages.
     * @param decoder The message decoder for deserialising incoming messages.
     */
    @Inject
    public TcpSocketManager(@NonNull TcpMessageEncoder encoder,
                            @NonNull TcpMessageDecoder decoder) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.connectionState = new MutableLiveData<>(ConnectionState.DISCONNECTED);
    }

    /**
     * Returns the current connection state as LiveData for UI observation.
     *
     * @return LiveData containing the current connection state.
     */
    @NonNull
    public LiveData<ConnectionState> getConnectionState() {
        return connectionState;
    }

    /**
     * Sets the message listener for receiving incoming messages.
     *
     * @param listener The listener to receive messages, or null to remove.
     */
    public void setMessageListener(@Nullable MessageListener listener) {
        this.messageListener = listener;
    }

    /**
     * Establishes a TCP connection to the specified host and port.
     * This method runs asynchronously and updates connection state via LiveData.
     *
     * @param host The IP address or hostname to connect to.
     * @param port The port number to connect to.
     */
    public void connect(@NonNull String host, int port) {
        synchronized (socketLock) {
            if (isConnected()) {
                return;
            }

            this.currentHost = host;
            this.currentPort = port;
            this.shouldReconnect.set(true);
            this.currentReconnectDelay = INITIAL_RECONNECT_DELAY_MS;
        }

        connectionState.postValue(ConnectionState.CONNECTING);
        doConnect(host, port);
    }

    /**
     * Performs the actual connection attempt.
     *
     * @param host The host to connect to.
     * @param port The port to connect to.
     */
    private void doConnect(@NonNull String host, int port) {
        ExecutorService connectExecutor = Executors.newSingleThreadExecutor();
        connectExecutor.execute(() -> {
            try {
                synchronized (socketLock) {
                    socket = createSocket(host, port);
                    outputStream = new BufferedOutputStream(socket.getOutputStream());
                    inputStream = new BufferedInputStream(socket.getInputStream());
                }

                currentReconnectDelay = INITIAL_RECONNECT_DELAY_MS;
                connectionState.postValue(ConnectionState.CONNECTED);
                startReaderThread();

            } catch (IOException e) {
                handleConnectionFailure(e);
            }
        });
        connectExecutor.shutdown();
    }

    /**
     * Creates a socket connection. This method is protected to allow mocking in tests.
     *
     * @param host The host to connect to.
     * @param port The port to connect to.
     * @return The created socket.
     * @throws IOException If the connection fails.
     */
    @VisibleForTesting
    @NonNull
    protected Socket createSocket(@NonNull String host, int port) throws IOException {
        return new Socket(host, port);
    }

    /**
     * Handles connection failure by triggering reconnection with exponential backoff.
     *
     * @param error The error that caused the failure.
     */
    private void handleConnectionFailure(@NonNull Exception error) {
        if (messageListener != null) {
            messageListener.onError(error);
        }

        if (shouldReconnect.get() && currentHost != null) {
            scheduleReconnect();
        } else {
            connectionState.postValue(ConnectionState.DISCONNECTED);
        }
    }

    /**
     * Schedules a reconnection attempt with exponential backoff.
     */
    private void scheduleReconnect() {
        connectionState.postValue(ConnectionState.RECONNECTING);

        ExecutorService reconnectExecutor = Executors.newSingleThreadExecutor();
        reconnectExecutor.execute(() -> {
            try {
                Thread.sleep(currentReconnectDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            if (shouldReconnect.get() && currentHost != null) {
                currentReconnectDelay = Math.min(
                        currentReconnectDelay * BACKOFF_MULTIPLIER,
                        MAX_RECONNECT_DELAY_MS
                );
                connectionState.postValue(ConnectionState.CONNECTING);
                doConnect(currentHost, currentPort);
            }
        });
        reconnectExecutor.shutdown();
    }

    /**
     * Starts the background reader thread for receiving messages.
     */
    private void startReaderThread() {
        readerExecutor = Executors.newSingleThreadExecutor();
        readerExecutor.execute(this::readLoop);
    }

    /**
     * The main read loop that continuously reads messages from the socket.
     */
    private void readLoop() {
        byte[] buffer = new byte[READ_BUFFER_SIZE];

        while (isConnected() && !Thread.currentThread().isInterrupted()) {
            try {
                InputStream stream;
                synchronized (socketLock) {
                    stream = inputStream;
                }

                if (stream == null) {
                    break;
                }

                int bytesRead = stream.read(buffer);
                if (bytesRead == -1) {
                    handleDisconnection();
                    break;
                }

                if (bytesRead > 0) {
                    processReceivedData(buffer, bytesRead);
                }

            } catch (IOException e) {
                if (isConnected()) {
                    handleDisconnection();
                }
                break;
            }
        }
    }

    /**
     * Processes received data from the socket.
     *
     * @param buffer    The buffer containing received data.
     * @param bytesRead The number of bytes read.
     */
    private void processReceivedData(@NonNull byte[] buffer, int bytesRead) {
        byte[] data = new byte[bytesRead];
        System.arraycopy(buffer, 0, data, 0, bytesRead);

        try {
            TcpMessage message = decoder.decode(data);
            if (messageListener != null) {
                messageListener.onMessageReceived(message);
            }
        } catch (TcpProtocolException e) {
            if (messageListener != null) {
                messageListener.onError(e);
            }
        }
    }

    /**
     * Handles unexpected disconnection by cleaning up and triggering reconnection.
     */
    private void handleDisconnection() {
        cleanupSocket();

        if (shouldReconnect.get() && currentHost != null) {
            scheduleReconnect();
        } else {
            connectionState.postValue(ConnectionState.DISCONNECTED);
        }
    }

    /**
     * Sends a TCP message to the connected server.
     * This method is thread-safe.
     *
     * @param message The message to send.
     * @throws IOException          If sending fails due to connection issues.
     * @throws TcpProtocolException If the message cannot be encoded.
     */
    public void send(@NonNull TcpMessage message) throws IOException, TcpProtocolException {
        byte[] data = encoder.encode(message);

        synchronized (socketLock) {
            if (outputStream == null || !isConnected()) {
                throw new IOException("Not connected");
            }

            outputStream.write(data);
            outputStream.flush();
        }
    }

    /**
     * Disconnects from the server and stops reconnection attempts.
     */
    public void disconnect() {
        shouldReconnect.set(false);
        cleanupSocket();
        connectionState.postValue(ConnectionState.DISCONNECTED);
    }

    /**
     * Cleans up socket resources.
     */
    private void cleanupSocket() {
        synchronized (socketLock) {
            if (readerExecutor != null) {
                readerExecutor.shutdownNow();
                readerExecutor = null;
            }

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                    // Ignore close errors
                }
                inputStream = null;
            }

            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {
                    // Ignore close errors
                }
                outputStream = null;
            }

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                    // Ignore close errors
                }
                socket = null;
            }
        }
    }

    /**
     * Checks if the socket is currently connected.
     *
     * @return true if connected, false otherwise.
     */
    public boolean isConnected() {
        synchronized (socketLock) {
            return socket != null && socket.isConnected() && !socket.isClosed();
        }
    }

    /**
     * Returns the current reconnect delay for testing purposes.
     *
     * @return The current reconnect delay in milliseconds.
     */
    @VisibleForTesting
    long getCurrentReconnectDelay() {
        return currentReconnectDelay;
    }

    /**
     * Sets the reconnect delay for testing purposes.
     *
     * @param delay The delay in milliseconds.
     */
    @VisibleForTesting
    void setCurrentReconnectDelay(long delay) {
        this.currentReconnectDelay = delay;
    }

    /**
     * Returns whether reconnection is enabled for testing purposes.
     *
     * @return true if reconnection is enabled.
     */
    @VisibleForTesting
    boolean getShouldReconnect() {
        return shouldReconnect.get();
    }
}
