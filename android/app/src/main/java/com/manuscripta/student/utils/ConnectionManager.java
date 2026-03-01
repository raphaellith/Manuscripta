package com.manuscripta.student.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Utility class for monitoring network connectivity and server reachability.
 * Provides real-time updates on network state changes via LiveData.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Monitor WiFi/cellular connectivity changes</li>
 *   <li>Check server reachability via HTTP ping</li>
 *   <li>Provide reactive connection state via LiveData</li>
 * </ul>
 */
@Singleton
public class ConnectionManager {

    /** Tag for logging. */
    private static final String TAG = "ConnectionManager";

    /** Timeout for server reachability check in milliseconds. */
    private static final int SERVER_CHECK_TIMEOUT_MS = 5000;

    /** The Android connectivity manager. */
    private final ConnectivityManager connectivityManager;

    /** LiveData for network connectivity state. */
    private final MutableLiveData<Boolean> isConnected;

    /** Network callback for monitoring connectivity changes. */
    private final ConnectivityManager.NetworkCallback networkCallback;

    /**
     * Creates a ConnectionManager with the given context.
     *
     * @param context The application context
     * @throws IllegalArgumentException if context is null
     */
    @Inject
    public ConnectionManager(@NonNull Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        this.connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (this.connectivityManager == null) {
            throw new IllegalStateException("ConnectivityManager not available");
        }

        this.isConnected = new MutableLiveData<>(checkCurrentConnection());

        // Create network callback for monitoring changes
        this.networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Log.d(TAG, "Network available: " + network);
                isConnected.postValue(checkCurrentConnection());
            }

            @Override
            public void onLost(@NonNull Network network) {
                Log.d(TAG, "Network lost: " + network);
                // Only update to false if no other networks are available
                isConnected.postValue(checkCurrentConnection());
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network,
                                                @NonNull NetworkCapabilities capabilities) {
                boolean hasInternet = capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET);
                boolean validated = capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED);

                Log.d(TAG, "Network capabilities changed - Internet: " + hasInternet
                        + ", Validated: " + validated);

                // Only update state based on current active network to avoid false positives
                // from non-active network callbacks
                isConnected.postValue(checkCurrentConnection());
            }
        };

        // Register callback
        registerNetworkCallback();
    }

    /**
     * Gets LiveData for network connectivity state.
     * Observers will be notified of connectivity changes.
     *
     * @return LiveData with current connection state (true = connected, false = disconnected)
     */
    @NonNull
    public LiveData<Boolean> getConnectionState() {
        return isConnected;
    }

    /**
     * Checks if the device currently has network connectivity.
     *
     * @return true if connected to a network, false otherwise
     */
    public boolean isNetworkAvailable() {
        return checkCurrentConnection();
    }

    /**
     * Checks if a specific server is reachable via HTTP.
     * Performs a HEAD request to the server URL to verify connectivity.
     *
     * <p><b>IMPORTANT:</b> This method performs a blocking network operation and must be
     * called from a background thread. Calling from the main thread will cause
     * NetworkOnMainThreadException and ANR (Application Not Responding) errors.</p>
     *
     * @param serverUrl The server URL to check (e.g., "https://api.example.com")
     * @return true if server is reachable, false otherwise
     */
    public boolean isServerReachable(@NonNull String serverUrl) {
        serverUrl = serverUrl.trim();

        if (serverUrl.isEmpty()) {
            Log.w(TAG, "Server URL is empty");
            return false;
        }

        if (!isNetworkAvailable()) {
            Log.d(TAG, "No network available, server unreachable");
            return false;
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(serverUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(SERVER_CHECK_TIMEOUT_MS);
            connection.setReadTimeout(SERVER_CHECK_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);

            int responseCode = connection.getResponseCode();

            boolean reachable = (responseCode >= 200 && responseCode < 500);
            Log.d(TAG, "Server reachability check: " + serverUrl
                    + " - HTTP " + responseCode + " - Reachable: " + reachable);

            return reachable;
        } catch (IOException e) {
            Log.e(TAG, "Server unreachable: " + serverUrl + " - " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking server reachability: " + e.getMessage(), e);
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Unregisters the network callback.
     * Should be called when the connection manager is no longer needed.
     */
    public void shutdown() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            Log.d(TAG, "Network callback unregistered");
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering network callback: " + e.getMessage());
        }
    }

    /**
     * Checks the current network connection state.
     *
     * @return true if connected, false otherwise
     */
    private boolean checkCurrentConnection() {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }

        NetworkCapabilities capabilities =
                connectivityManager.getNetworkCapabilities(activeNetwork);

        if (capabilities == null) {
            return false;
        }

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    /**
     * Registers the network callback to monitor connectivity changes.
     */
    private void registerNetworkCallback() {
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback);
            Log.d(TAG, "Network callback registered");
        } catch (Exception e) {
            Log.e(TAG, "Failed to register network callback: " + e.getMessage(), e);
        }
    }
}
