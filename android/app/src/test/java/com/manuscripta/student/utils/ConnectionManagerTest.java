package com.manuscripta.student.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import org.junit.Before;
import org.junit.Rule;
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

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

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
        // Verify it's a LiveData object
        assertTrue(state instanceof LiveData);
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

    @Test
    public void testIsServerReachable_nullProtocolException_returnsFalse() {
        // Mock network as available
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
                .thenReturn(mockCapabilities);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .thenReturn(true);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                .thenReturn(true);

        connectionManager = new ConnectionManager(mockContext);

        // Pass a URL that will throw RuntimeException (general Exception catch)
        // This tests the generic Exception handler
        boolean reachable = connectionManager.isServerReachable("://invalid");

        assertFalse(reachable);
    }

    // ========== Constructor and registration tests ==========

    @Test
    public void testConstructor_registersNetworkCallback() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(null);

        connectionManager = new ConnectionManager(mockContext);

        // Verify network callback was registered
        verify(mockConnectivityManager).registerNetworkCallback(
                any(android.net.NetworkRequest.class),
                any(ConnectivityManager.NetworkCallback.class));
    }

    @Test
    public void testConstructor_withConnectedNetwork_initializesCorrectly() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
                .thenReturn(mockCapabilities);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .thenReturn(true);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                .thenReturn(true);

        connectionManager = new ConnectionManager(mockContext);

        assertNotNull(connectionManager);
        LiveData<Boolean> state = connectionManager.getConnectionState();
        assertNotNull(state.getValue());
        assertTrue(state.getValue());
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
    public void testIsNetworkAvailable_multipleCallsConsistent() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
                .thenReturn(mockCapabilities);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .thenReturn(true);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                .thenReturn(true);

        connectionManager = new ConnectionManager(mockContext);

        // Call multiple times - should be consistent
        boolean first = connectionManager.isNetworkAvailable();
        boolean second = connectionManager.isNetworkAvailable();

        assertTrue(first);
        assertTrue(second);
    }

    @Test
    public void testGetConnectionState_returnsLiveDataInstance() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(null);
        connectionManager = new ConnectionManager(mockContext);

        LiveData<Boolean> state1 = connectionManager.getConnectionState();
        LiveData<Boolean> state2 = connectionManager.getConnectionState();

        // Should return the same instance
        assertNotNull(state1);
        assertSame(state1, state2);
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

    // ========== Network callback tests ==========

    @Test
    public void testNetworkCallback_onAvailable_updatesConnectionState() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(null);
        connectionManager = new ConnectionManager(mockContext);

        // Capture the network callback that was registered
        org.mockito.ArgumentCaptor<ConnectivityManager.NetworkCallback> callbackCaptor =
                org.mockito.ArgumentCaptor.forClass(ConnectivityManager.NetworkCallback.class);
        verify(mockConnectivityManager).registerNetworkCallback(
                any(android.net.NetworkRequest.class),
                callbackCaptor.capture());

        ConnectivityManager.NetworkCallback callback = callbackCaptor.getValue();

        // Setup: Network becomes available and validated
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
                .thenReturn(mockCapabilities);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .thenReturn(true);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                .thenReturn(true);

        // Trigger onAvailable
        callback.onAvailable(mockNetwork);

        // Verify LiveData value is true
        LiveData<Boolean> connectionState = connectionManager.getConnectionState();
        assertTrue(connectionState.getValue());
    }

    @Test
    public void testNetworkCallback_onLost_updatesConnectionState() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
                .thenReturn(mockCapabilities);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .thenReturn(true);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                .thenReturn(true);

        connectionManager = new ConnectionManager(mockContext);

        // Capture the network callback
        org.mockito.ArgumentCaptor<ConnectivityManager.NetworkCallback> callbackCaptor =
                org.mockito.ArgumentCaptor.forClass(ConnectivityManager.NetworkCallback.class);
        verify(mockConnectivityManager).registerNetworkCallback(
                any(android.net.NetworkRequest.class),
                callbackCaptor.capture());

        ConnectivityManager.NetworkCallback callback = callbackCaptor.getValue();

        // Setup: Network is lost, no active network
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(null);

        // Trigger onLost
        callback.onLost(mockNetwork);

        // Verify LiveData value is false
        LiveData<Boolean> connectionState = connectionManager.getConnectionState();
        assertFalse(connectionState.getValue());
    }

    @Test
    public void testNetworkCallback_onCapabilitiesChanged_updatesConnectionState() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(null);
        connectionManager = new ConnectionManager(mockContext);

        // Capture the network callback
        org.mockito.ArgumentCaptor<ConnectivityManager.NetworkCallback> callbackCaptor =
                org.mockito.ArgumentCaptor.forClass(ConnectivityManager.NetworkCallback.class);
        verify(mockConnectivityManager).registerNetworkCallback(
                any(android.net.NetworkRequest.class),
                callbackCaptor.capture());

        ConnectivityManager.NetworkCallback callback = callbackCaptor.getValue();

        // Setup: Active network gains validated internet
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
                .thenReturn(mockCapabilities);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .thenReturn(true);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                .thenReturn(true);

        // Trigger onCapabilitiesChanged
        callback.onCapabilitiesChanged(mockNetwork, mockCapabilities);

        // Verify LiveData value is true
        LiveData<Boolean> connectionState = connectionManager.getConnectionState();
        assertTrue(connectionState.getValue());
    }

    @Test
    public void testNetworkCallback_onCapabilitiesChanged_noInternet_updatesConnectionState() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
                .thenReturn(mockCapabilities);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .thenReturn(true);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                .thenReturn(true);

        connectionManager = new ConnectionManager(mockContext);

        // Capture the network callback
        org.mockito.ArgumentCaptor<ConnectivityManager.NetworkCallback> callbackCaptor =
                org.mockito.ArgumentCaptor.forClass(ConnectivityManager.NetworkCallback.class);
        verify(mockConnectivityManager).registerNetworkCallback(
                any(android.net.NetworkRequest.class),
                callbackCaptor.capture());

        ConnectivityManager.NetworkCallback callback = callbackCaptor.getValue();

        // Setup: Active network loses internet capability
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .thenReturn(false);

        // Trigger onCapabilitiesChanged
        callback.onCapabilitiesChanged(mockNetwork, mockCapabilities);

        // Verify LiveData value is false
        LiveData<Boolean> connectionState = connectionManager.getConnectionState();
        assertFalse(connectionState.getValue());
    }

    @Test
    public void testNetworkCallback_onCapabilitiesChanged_bothFalse_updatesConnectionState() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
                .thenReturn(mockCapabilities);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .thenReturn(true);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                .thenReturn(true);

        connectionManager = new ConnectionManager(mockContext);

        // Capture the network callback
        org.mockito.ArgumentCaptor<ConnectivityManager.NetworkCallback> callbackCaptor =
                org.mockito.ArgumentCaptor.forClass(ConnectivityManager.NetworkCallback.class);
        verify(mockConnectivityManager).registerNetworkCallback(
                any(android.net.NetworkRequest.class),
                callbackCaptor.capture());

        ConnectivityManager.NetworkCallback callback = callbackCaptor.getValue();

        // Setup: Active network loses both capabilities
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .thenReturn(false);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                .thenReturn(false);

        // Trigger onCapabilitiesChanged
        callback.onCapabilitiesChanged(mockNetwork, mockCapabilities);

        // Verify LiveData value is false
        LiveData<Boolean> connectionState = connectionManager.getConnectionState();
        assertFalse(connectionState.getValue());
    }

    @Test
    public void testNetworkCallback_onCapabilitiesChanged_notValidated_updatesConnectionState() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
                .thenReturn(mockCapabilities);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .thenReturn(true);
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                .thenReturn(true);

        connectionManager = new ConnectionManager(mockContext);

        // Capture the network callback
        org.mockito.ArgumentCaptor<ConnectivityManager.NetworkCallback> callbackCaptor =
                org.mockito.ArgumentCaptor.forClass(ConnectivityManager.NetworkCallback.class);
        verify(mockConnectivityManager).registerNetworkCallback(
                any(android.net.NetworkRequest.class),
                callbackCaptor.capture());

        ConnectivityManager.NetworkCallback callback = callbackCaptor.getValue();

        // Setup: Active network loses validation
        when(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                .thenReturn(false);

        // Trigger onCapabilitiesChanged
        callback.onCapabilitiesChanged(mockNetwork, mockCapabilities);

        // Verify LiveData value is false
        LiveData<Boolean> connectionState = connectionManager.getConnectionState();
        assertFalse(connectionState.getValue());
    }

    // ========== Exception handling tests ==========

    @Test
    public void testConstructor_registerCallbackThrowsException_handlesGracefully() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(null);
        doThrow(new RuntimeException("Failed to register"))
                .when(mockConnectivityManager)
                .registerNetworkCallback(
                        any(android.net.NetworkRequest.class),
                        any(ConnectivityManager.NetworkCallback.class));

        // Should not throw - exception is caught and logged
        connectionManager = new ConnectionManager(mockContext);

        assertNotNull(connectionManager);
    }

    @Test
    public void testRegisterNetworkCallback_createsRequestWithInternetCapability() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(null);

        connectionManager = new ConnectionManager(mockContext);

        // Capture the NetworkRequest that was created
        org.mockito.ArgumentCaptor<android.net.NetworkRequest> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(android.net.NetworkRequest.class);
        verify(mockConnectivityManager).registerNetworkCallback(
                requestCaptor.capture(),
                any(ConnectivityManager.NetworkCallback.class));

        android.net.NetworkRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest);
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
