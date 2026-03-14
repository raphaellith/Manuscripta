package com.manuscripta.student.network.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Unit tests for {@link BaseUrlInterceptor}.
 */
public class BaseUrlInterceptorTest {

    @Mock
    private BaseUrlInterceptor.ServerInfoProvider mockServerInfoProvider;

    @Mock
    private Interceptor.Chain mockChain;

    private BaseUrlInterceptor interceptor;
    private Request testRequest;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        testRequest = new Request.Builder()
                .url("http://localhost/api/v1/distribution/device-123")
                .build();
    }

    // ========== Constructor tests ==========

    @Test
    public void testConstructor_nullProvider_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new BaseUrlInterceptor(null));
    }

    @Test
    public void testConstructor_validProvider_succeeds() {
        interceptor = new BaseUrlInterceptor(mockServerInfoProvider);
        assertNotNull(interceptor);
    }

    // ========== Intercept tests — server info available ==========

    @Test
    public void testIntercept_withServerInfo_rewritesHostAndPort() throws IOException {
        // Given
        when(mockServerInfoProvider.getServerHost()).thenReturn("192.168.1.100");
        when(mockServerInfoProvider.getServerHttpPort()).thenReturn(5911);
        when(mockChain.request()).thenReturn(testRequest);
        when(mockChain.proceed(any(Request.class))).thenReturn(buildOkResponse(testRequest));

        interceptor = new BaseUrlInterceptor(mockServerInfoProvider);

        // When
        Response response = interceptor.intercept(mockChain);

        // Then
        assertNotNull(response);
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(mockChain).proceed(captor.capture());
        Request rewritten = captor.getValue();

        assertEquals("http", rewritten.url().scheme());
        assertEquals("192.168.1.100", rewritten.url().host());
        assertEquals(5911, rewritten.url().port());
        assertEquals("/api/v1/distribution/device-123", rewritten.url().encodedPath());
    }

    @Test
    public void testIntercept_withServerInfo_preservesPath() throws IOException {
        // Given
        Request request = new Request.Builder()
                .url("http://localhost/api/v1/feedback/device-123")
                .build();
        when(mockServerInfoProvider.getServerHost()).thenReturn("10.0.0.5");
        when(mockServerInfoProvider.getServerHttpPort()).thenReturn(8080);
        when(mockChain.request()).thenReturn(request);
        when(mockChain.proceed(any(Request.class))).thenReturn(buildOkResponse(request));

        interceptor = new BaseUrlInterceptor(mockServerInfoProvider);

        // When
        interceptor.intercept(mockChain);

        // Then
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(mockChain).proceed(captor.capture());
        assertEquals("/api/v1/feedback/device-123", captor.getValue().url().encodedPath());
    }

    @Test
    public void testIntercept_withServerInfo_preservesQueryParameters() throws IOException {
        // Given
        Request request = new Request.Builder()
                .url("http://localhost/api/v1/config?key=value")
                .build();
        when(mockServerInfoProvider.getServerHost()).thenReturn("192.168.1.100");
        when(mockServerInfoProvider.getServerHttpPort()).thenReturn(5911);
        when(mockChain.request()).thenReturn(request);
        when(mockChain.proceed(any(Request.class))).thenReturn(buildOkResponse(request));

        interceptor = new BaseUrlInterceptor(mockServerInfoProvider);

        // When
        interceptor.intercept(mockChain);

        // Then
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(mockChain).proceed(captor.capture());
        assertEquals("value", captor.getValue().url().queryParameter("key"));
    }

    @Test
    public void testIntercept_withServerInfo_preservesHeaders() throws IOException {
        // Given
        Request request = new Request.Builder()
                .url("http://localhost/api/v1/distribution/device-123")
                .header("X-Device-ID", "device-123")
                .build();
        when(mockServerInfoProvider.getServerHost()).thenReturn("192.168.1.100");
        when(mockServerInfoProvider.getServerHttpPort()).thenReturn(5911);
        when(mockChain.request()).thenReturn(request);
        when(mockChain.proceed(any(Request.class))).thenReturn(buildOkResponse(request));

        interceptor = new BaseUrlInterceptor(mockServerInfoProvider);

        // When
        interceptor.intercept(mockChain);

        // Then
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(mockChain).proceed(captor.capture());
        assertEquals("device-123", captor.getValue().header("X-Device-ID"));
    }

    @Test
    public void testIntercept_setsSchemeToHttp() throws IOException {
        // Given — original request uses https, interceptor should force http
        Request httpsRequest = new Request.Builder()
                .url("https://localhost/api/v1/distribution/device-123")
                .build();
        when(mockServerInfoProvider.getServerHost()).thenReturn("192.168.1.100");
        when(mockServerInfoProvider.getServerHttpPort()).thenReturn(5911);
        when(mockChain.request()).thenReturn(httpsRequest);
        when(mockChain.proceed(any(Request.class))).thenReturn(buildOkResponse(httpsRequest));

        interceptor = new BaseUrlInterceptor(mockServerInfoProvider);

        // When
        interceptor.intercept(mockChain);

        // Then
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(mockChain).proceed(captor.capture());
        assertEquals("http", captor.getValue().url().scheme());
    }

    // ========== Intercept tests — server info NOT available ==========

    @Test
    public void testIntercept_nullHost_proceedsWithOriginalUrl() throws IOException {
        // Given
        when(mockServerInfoProvider.getServerHost()).thenReturn(null);
        when(mockServerInfoProvider.getServerHttpPort()).thenReturn(5911);
        when(mockChain.request()).thenReturn(testRequest);
        when(mockChain.proceed(any(Request.class))).thenReturn(buildOkResponse(testRequest));

        interceptor = new BaseUrlInterceptor(mockServerInfoProvider);

        // When
        interceptor.intercept(mockChain);

        // Then — original request passed through unmodified
        verify(mockChain).proceed(testRequest);
    }

    @Test
    public void testIntercept_emptyHost_proceedsWithOriginalUrl() throws IOException {
        // Given
        when(mockServerInfoProvider.getServerHost()).thenReturn("");
        when(mockServerInfoProvider.getServerHttpPort()).thenReturn(5911);
        when(mockChain.request()).thenReturn(testRequest);
        when(mockChain.proceed(any(Request.class))).thenReturn(buildOkResponse(testRequest));

        interceptor = new BaseUrlInterceptor(mockServerInfoProvider);

        // When
        interceptor.intercept(mockChain);

        // Then
        verify(mockChain).proceed(testRequest);
    }

    @Test
    public void testIntercept_zeroPort_proceedsWithOriginalUrl() throws IOException {
        // Given
        when(mockServerInfoProvider.getServerHost()).thenReturn("192.168.1.100");
        when(mockServerInfoProvider.getServerHttpPort()).thenReturn(0);
        when(mockChain.request()).thenReturn(testRequest);
        when(mockChain.proceed(any(Request.class))).thenReturn(buildOkResponse(testRequest));

        interceptor = new BaseUrlInterceptor(mockServerInfoProvider);

        // When
        interceptor.intercept(mockChain);

        // Then
        verify(mockChain).proceed(testRequest);
    }

    @Test
    public void testIntercept_negativePort_proceedsWithOriginalUrl() throws IOException {
        // Given
        when(mockServerInfoProvider.getServerHost()).thenReturn("192.168.1.100");
        when(mockServerInfoProvider.getServerHttpPort()).thenReturn(-1);
        when(mockChain.request()).thenReturn(testRequest);
        when(mockChain.proceed(any(Request.class))).thenReturn(buildOkResponse(testRequest));

        interceptor = new BaseUrlInterceptor(mockServerInfoProvider);

        // When
        interceptor.intercept(mockChain);

        // Then
        verify(mockChain).proceed(testRequest);
    }

    /**
     * Builds a minimal 200 OK response for the given request.
     */
    private Response buildOkResponse(Request request) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build();
    }
}
