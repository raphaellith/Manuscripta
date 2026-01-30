package com.manuscripta.student.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.lifecycle.LiveData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link ConnectionManager}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class ConnectionManagerTest {

    @Mock
    private Context mockContext;

    @Mock
    private ConnectivityManager mockConnectivityManager;

    @Mock
    private Network mockNetwork;

    @Mock
    private NetworkCapabilities mockCapabilities;

    private ConnectionManager connectionManager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mockConnectivityManager);
    }

    // ========== Constructor tests ==========

    @Test
    public void testConstructor_nullContext_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConnectionManager(null));
    }

    @Test
    public void testConstructor_nullConnectivityManager_throwsException() {
        when(mockContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(null);

        assertThrows(IllegalStateException.class,
                () -> new ConnectionManager(mockContext));
    }

    @Test
    public void testConstructor_validContext_createsInstance() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(null);

        connectionManager = new ConnectionManager(mockContext);

        assertNotNull(connectionManager);
        verify(mockContext).getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    // ========== getConnectionState tests ==========

    @Test
    public void testGetConnectionState_returnsLiveData() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(null);
        connectionManager = new ConnectionManager(mockContext);

        LiveData<Boolean> state = connectionManager.getConnectionState();

        assertNotNull(state);
    }

    @Test
    public void testGetConnectionState_initialValueFalseWhenNoNetwork() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(null);
        connectionManager = new ConnectionManager(mockContext);

        LiveData<Boolean> state = connectionManager.getConnectionState();

        assertNotNull(state.getValue());
        assertFalse(state.getValue());
    }

    @Test
    public void testGetConnectionState_initialValueTrueWhenConnected() {
        // Mock active network with internet and validated capabilities
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
                .thenReturn(mockCapabilities);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .thenReturn(true);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                .thenReturn(true);

        connectionManager = new ConnectionManager(mockContext);

        LiveData<Boolean> state = connectionManager.getConnectionState();

        assertNotNull(state.getValue());
        assertTrue(state.getValue());
    }

    // ========== Network capability edge cases ==========

    @Test
    public void testIsNetworkAvailable_networkHasInternetButNotValidated_returnsFalse() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
                .thenReturn(mockCapabilities);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .thenReturn(true);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                .thenReturn(false);

        connectionManager = new ConnectionManager(mockContext);

        boolean available = connectionManager.isNetworkAvailable();

        assertFalse(available);
    }

    @Test
    public void testIsNetworkAvailable_networkHasValidatedButNotInternet_returnsFalse() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
                .thenReturn(mockCapabilities);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .thenReturn(false);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                .thenReturn(true);

        connectionManager = new ConnectionManager(mockContext);

        boolean available = connectionManager.isNetworkAvailable();

        assertFalse(available);
    }

    // ========== isNetworkAvailable tests ==========

    @Test
    public void testIsNetworkAvailable_noNetwork_returnsFalse() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(null);
        connectionManager = new ConnectionManager(mockContext);

        boolean available = connectionManager.isNetworkAvailable();

        assertFalse(available);
    }

    @Test
    public void testIsNetworkAvailable_networkWithoutCapabilities_returnsFalse() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork)).thenReturn(null);

        connectionManager = new ConnectionManager(mockContext);

        boolean available = connectionManager.isNetworkAvailable();

        assertFalse(available);
    }

    @Test
    public void testIsNetworkAvailable_networkWithInternet_returnsTrue() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
                .thenReturn(mockCapabilities);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .thenReturn(true);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                .thenReturn(true);

        connectionManager = new ConnectionManager(mockContext);

        boolean available = connectionManager.isNetworkAvailable();

        assertTrue(available);
    }

    @Test
    public void testIsNetworkAvailable_networkWithoutValidation_returnsFalse() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
                .thenReturn(mockCapabilities);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .thenReturn(true);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                .thenReturn(false);

        connectionManager = new ConnectionManager(mockContext);

        boolean available = connectionManager.isNetworkAvailable();

        assertFalse(available);
    }

    // ========== isServerReachable tests ==========

    @Test
    public void testIsServerReachable_nullUrl_returnsFalse() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(null);
        connectionManager = new ConnectionManager(mockContext);

        boolean reachable = connectionManager.isServerReachable(null);

        assertFalse(reachable);
    }

    @Test
    public void testIsServerReachable_emptyUrl_returnsFalse() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(null);
        connectionManager = new ConnectionManager(mockContext);

        boolean reachable = connectionManager.isServerReachable("");

        assertFalse(reachable);
    }

    @Test
    public void testIsServerReachable_noNetwork_returnsFalse() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(null);
        connectionManager = new ConnectionManager(mockContext);

        boolean reachable = connectionManager.isServerReachable("https://api.test.com");

        assertFalse(reachable);
    }

    @Test
    public void testIsServerReachable_invalidUrl_returnsFalse() {
        // Mock network as available
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
                .thenReturn(mockCapabilities);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .thenReturn(true);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                .thenReturn(true);

        connectionManager = new ConnectionManager(mockContext);

        boolean reachable = connectionManager.isServerReachable("not-a-valid-url");

        assertFalse(reachable);
    }

    // ========== shutdown tests ==========

    @Test
    public void testShutdown_unregistersCallback() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(null);
        connectionManager = new ConnectionManager(mockContext);

        connectionManager.shutdown();

        verify(mockConnectivityManager).unregisterNetworkCallback(any(
                ConnectivityManager.NetworkCallback.class));
    }

    @Test
    public void testShutdown_multipleCallsDoNotThrow() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(null);
        connectionManager = new ConnectionManager(mockContext);

        // First shutdown should work
        connectionManager.shutdown();

        // Second shutdown should not throw (even though it might log an error)
        connectionManager.shutdown(); // Should not throw
    }

    // ========== Exception handling tests ==========

    @Test
    public void testConstructor_registerCallbackThrowsException_handlesGracefully() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(null);
        when(mockConnectivityManager.registerNetworkCallback(
                any(android.net.NetworkRequest.class),
                any(ConnectivityManager.NetworkCallback.class)))
                .thenThrow(new RuntimeException("Failed to register"));

        // Should not throw - exception is caught and logged
        connectionManager = new ConnectionManager(mockContext);

        assertNotNull(connectionManager);
    }

    @Test
    public void testShutdown_unregisterCallbackThrowsException_handlesGracefully() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(null);
        connectionManager = new ConnectionManager(mockContext);

        // Mock unregister to throw exception
        doThrow(new IllegalArgumentException("Callback not registered"))
                .when(mockConnectivityManager)
                .unregisterNetworkCallback(any(ConnectivityManager.NetworkCallback.class));

        // Should not throw - exception is caught and logged
        connectionManager.shutdown();
    }
}
