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
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LoggingInterceptor}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class LoggingInterceptorTest {

    @Mock
    private Interceptor.Chain mockChain;

    private LoggingInterceptor interceptor;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        interceptor = new LoggingInterceptor();
    }

    // ========== Basic GET request tests ==========

    @Test
    public void testIntercept_simpleGETRequest_logsAndReturns() throws IOException {
        // Arrange
        Request request = new Request.Builder()
                .url("https://api.test.com/endpoint")
                .get()
                .build();
        when(mockChain.request()).thenReturn(request);

        Response response = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create("test body", MediaType.parse("text/plain")))
                .build();
        when(mockChain.proceed(any(Request.class))).thenReturn(response);

        // Act
        Response result = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.code());
        verify(mockChain).proceed(request);

        // Verify body is still readable after logging
        assertNotNull(result.body());
        assertEquals("test body", result.body().string());
    }

    // ========== POST request with body tests ==========

    @Test
    public void testIntercept_POSTWithBody_logsRequestBody() throws IOException {
        // Arrange
        String requestBodyContent = "{\"key\":\"value\"}";
        RequestBody requestBody = RequestBody.create(
                requestBodyContent,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url("https://api.test.com/endpoint")
                .post(requestBody)
                .build();
        when(mockChain.request()).thenReturn(request);

        Response response = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(201)
                .message("Created")
                .body(ResponseBody.create("", MediaType.parse("text/plain")))
                .build();
        when(mockChain.proceed(any(Request.class))).thenReturn(response);

        // Act
        Response result = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(result);
        assertEquals(201, result.code());
    }

    // ========== Request with headers tests ==========

    @Test
    public void testIntercept_requestWithHeaders_logsHeaders() throws IOException {
        // Arrange
        Request request = new Request.Builder()
                .url("https://api.test.com/endpoint")
                .header("X-Custom-Header", "custom-value")
                .header("Authorization", "Bearer token123")
                .get()
                .build();
        when(mockChain.request()).thenReturn(request);

        Response response = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create("", MediaType.parse("text/plain")))
                .build();
        when(mockChain.proceed(any(Request.class))).thenReturn(response);

        // Act
        Response result = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.code());
    }

    // ========== Response with headers tests ==========

    @Test
    public void testIntercept_responseWithHeaders_logsHeaders() throws IOException {
        // Arrange
        Request request = new Request.Builder()
                .url("https://api.test.com/endpoint")
                .get()
                .build();
        when(mockChain.request()).thenReturn(request);

        Response response = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .header("Content-Type", "application/json")
                .header("X-Request-ID", "req-123")
                .body(ResponseBody.create("{\"status\":\"ok\"}", MediaType.parse("application/json")))
                .build();
        when(mockChain.proceed(any(Request.class))).thenReturn(response);

        // Act
        Response result = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.code());
        assertNotNull(result.body());
        assertEquals("{\"status\":\"ok\"}", result.body().string());
    }

    // ========== Error response tests ==========

    @Test
    public void testIntercept_errorResponse_logsError() throws IOException {
        // Arrange
        Request request = new Request.Builder()
                .url("https://api.test.com/endpoint")
                .get()
                .build();
        when(mockChain.request()).thenReturn(request);

        Response response = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(404)
                .message("Not Found")
                .body(ResponseBody.create("Not found", MediaType.parse("text/plain")))
                .build();
        when(mockChain.proceed(any(Request.class))).thenReturn(response);

        // Act
        Response result = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(result);
        assertEquals(404, result.code());
        assertNotNull(result.body());
        assertEquals("Not found", result.body().string());
    }

    // ========== Empty response body tests ==========

    @Test
    public void testIntercept_emptyResponseBody_handles() throws IOException {
        // Arrange
        Request request = new Request.Builder()
                .url("https://api.test.com/endpoint")
                .get()
                .build();
        when(mockChain.request()).thenReturn(request);

        Response response = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(204)
                .message("No Content")
                .body(ResponseBody.create("", MediaType.parse("text/plain")))
                .build();
        when(mockChain.proceed(any(Request.class))).thenReturn(response);

        // Act
        Response result = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(result);
        assertEquals(204, result.code());
    }

    // ========== Network error tests ==========

    @Test
    public void testIntercept_networkError_logsAndThrows() throws IOException {
        // Arrange
        Request request = new Request.Builder()
                .url("https://api.test.com/endpoint")
                .get()
                .build();
        when(mockChain.request()).thenReturn(request);
        when(mockChain.proceed(any(Request.class)))
                .thenThrow(new IOException("Connection failed"));

        // Act & Assert
        assertThrows(IOException.class, () -> interceptor.intercept(mockChain));
    }

    // ========== Timing tests ==========

    @Test
    public void testIntercept_measuresTiming() throws IOException {
        // Arrange
        Request request = new Request.Builder()
                .url("https://api.test.com/endpoint")
                .get()
                .build();
        when(mockChain.request()).thenReturn(request);

        Response response = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create("", MediaType.parse("text/plain")))
                .build();
        when(mockChain.proceed(any(Request.class))).thenReturn(response);

        // Act
        long startTime = System.currentTimeMillis();
        Response result = interceptor.intercept(mockChain);
        long endTime = System.currentTimeMillis();

        // Assert
        assertNotNull(result);
        assertEquals(200, result.code());
        // Verify the call completed (timing is logged but we can't directly verify it)
    }

    // ========== JSON response body tests ==========

    @Test
    public void testIntercept_jsonResponse_preservesBody() throws IOException {
        // Arrange
        String jsonBody = "{\"id\":123,\"name\":\"test\",\"active\":true}";
        Request request = new Request.Builder()
                .url("https://api.test.com/endpoint")
                .get()
                .build();
        when(mockChain.request()).thenReturn(request);

        Response response = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(jsonBody, MediaType.parse("application/json")))
                .build();
        when(mockChain.proceed(any(Request.class))).thenReturn(response);

        // Act
        Response result = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(result);
        assertNotNull(result.body());
        assertEquals(jsonBody, result.body().string());
    }
}
