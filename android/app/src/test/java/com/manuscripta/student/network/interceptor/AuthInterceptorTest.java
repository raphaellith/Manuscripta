package com.manuscripta.student.network.interceptor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthInterceptor}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class AuthInterceptorTest {

    @Mock
    private AuthInterceptor.DeviceIdProvider mockDeviceIdProvider;

    @Mock
    private Interceptor.Chain mockChain;

    private AuthInterceptor interceptor;
    private Request testRequest;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        testRequest = new Request.Builder()
                .url("https://api.test.com/endpoint")
                .build();
    }

    // ========== Constructor tests ==========

    @Test
    public void testConstructor_nullProvider_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuthInterceptor(null));
    }

    @Test
    public void testConstructor_validProvider_succeeds() {
        interceptor = new AuthInterceptor(mockDeviceIdProvider);
        assertNotNull(interceptor);
    }

    // ========== Intercept tests ==========

    @Test
    public void testIntercept_withDeviceId_addsHeader() throws IOException {
        // Arrange
        String deviceId = "device-123";
        when(mockDeviceIdProvider.getDeviceId()).thenReturn(deviceId);
        when(mockChain.request()).thenReturn(testRequest);

        Response mockResponse = new Response.Builder()
                .request(testRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build();
        when(mockChain.proceed(any(Request.class))).thenReturn(mockResponse);

        interceptor = new AuthInterceptor(mockDeviceIdProvider);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(response);
        verify(mockChain).proceed(any(Request.class));

        // Verify the request passed to chain.proceed has the header
        org.mockito.ArgumentCaptor<Request> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(Request.class);
        verify(mockChain).proceed(requestCaptor.capture());
        Request capturedRequest = requestCaptor.getValue();

        assertEquals(deviceId, capturedRequest.header("X-Device-ID"));
    }

    @Test
    public void testIntercept_withNullDeviceId_noHeader() throws IOException {
        // Arrange
        when(mockDeviceIdProvider.getDeviceId()).thenReturn(null);
        when(mockChain.request()).thenReturn(testRequest);

        Response mockResponse = new Response.Builder()
                .request(testRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build();
        when(mockChain.proceed(any(Request.class))).thenReturn(mockResponse);

        interceptor = new AuthInterceptor(mockDeviceIdProvider);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(response);
        verify(mockChain).proceed(any(Request.class));

        // Verify the request passed to chain.proceed has NO header
        org.mockito.ArgumentCaptor<Request> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(Request.class);
        verify(mockChain).proceed(requestCaptor.capture());
        Request capturedRequest = requestCaptor.getValue();

        assertNull(capturedRequest.header("X-Device-ID"));
    }

    @Test
    public void testIntercept_withEmptyDeviceId_noHeader() throws IOException {
        // Arrange
        when(mockDeviceIdProvider.getDeviceId()).thenReturn("");
        when(mockChain.request()).thenReturn(testRequest);

        Response mockResponse = new Response.Builder()
                .request(testRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build();
        when(mockChain.proceed(any(Request.class))).thenReturn(mockResponse);

        interceptor = new AuthInterceptor(mockDeviceIdProvider);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(response);

        // Verify the request passed to chain.proceed has NO header
        org.mockito.ArgumentCaptor<Request> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(Request.class);
        verify(mockChain).proceed(requestCaptor.capture());
        Request capturedRequest = requestCaptor.getValue();

        assertNull(capturedRequest.header("X-Device-ID"));
    }

    @Test
    public void testIntercept_propagatesIOException() throws IOException {
        // Arrange
        when(mockDeviceIdProvider.getDeviceId()).thenReturn("device-123");
        when(mockChain.request()).thenReturn(testRequest);
        when(mockChain.proceed(any(Request.class)))
                .thenThrow(new IOException("Network error"));

        interceptor = new AuthInterceptor(mockDeviceIdProvider);

        // Act & Assert
        assertThrows(IOException.class, () -> interceptor.intercept(mockChain));
    }
}
