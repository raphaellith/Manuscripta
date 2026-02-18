package com.manuscripta.student.network.interceptor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ErrorInterceptor}.
 */
public class ErrorInterceptorTest {

    @Mock
    private Interceptor.Chain mockChain;

    private ErrorInterceptor interceptor;
    private Request testRequest;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        testRequest = new Request.Builder()
                .url("https://api.test.com/endpoint")
                .build();
        when(mockChain.request()).thenReturn(testRequest);
        interceptor = new ErrorInterceptor();
    }

    // ========== Success response tests ==========

    @Test
    public void testIntercept_successResponse_passesThrough() throws IOException {
        // Arrange
        Response successResponse = createResponse(200, "OK");
        when(mockChain.proceed(any(Request.class))).thenReturn(successResponse);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.code());
        verify(mockChain).proceed(testRequest);
    }

    @Test
    public void testIntercept_201Created_passesThrough() throws IOException {
        // Arrange
        Response successResponse = createResponse(201, "Created");
        when(mockChain.proceed(any(Request.class))).thenReturn(successResponse);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(response);
        assertEquals(201, response.code());
    }

    // ========== 4xx Client error tests ==========

    @Test
    public void testIntercept_400BadRequest_logsError() throws IOException {
        // Arrange
        Response errorResponse = createResponse(400, "Bad Request");
        when(mockChain.proceed(any(Request.class))).thenReturn(errorResponse);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert - interceptor logs but still returns the response
        assertNotNull(response);
        assertEquals(400, response.code());
    }

    @Test
    public void testIntercept_401Unauthorized_logsError() throws IOException {
        // Arrange
        Response errorResponse = createResponse(401, "Unauthorized");
        when(mockChain.proceed(any(Request.class))).thenReturn(errorResponse);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(response);
        assertEquals(401, response.code());
    }

    @Test
    public void testIntercept_404NotFound_logsError() throws IOException {
        // Arrange
        Response errorResponse = createResponse(404, "Not Found");
        when(mockChain.proceed(any(Request.class))).thenReturn(errorResponse);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(response);
        assertEquals(404, response.code());
    }

    @Test
    public void testIntercept_422UnprocessableEntity_logsError() throws IOException {
        // Arrange
        Response errorResponse = createResponse(422, "Unprocessable Entity");
        when(mockChain.proceed(any(Request.class))).thenReturn(errorResponse);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(response);
        assertEquals(422, response.code());
    }

    // ========== 5xx Server error tests ==========

    @Test
    public void testIntercept_500InternalServerError_logsError() throws IOException {
        // Arrange
        Response errorResponse = createResponse(500, "Internal Server Error");
        when(mockChain.proceed(any(Request.class))).thenReturn(errorResponse);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(response);
        assertEquals(500, response.code());
    }

    @Test
    public void testIntercept_502BadGateway_logsError() throws IOException {
        // Arrange
        Response errorResponse = createResponse(502, "Bad Gateway");
        when(mockChain.proceed(any(Request.class))).thenReturn(errorResponse);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(response);
        assertEquals(502, response.code());
    }

    @Test
    public void testIntercept_503ServiceUnavailable_logsError() throws IOException {
        // Arrange
        Response errorResponse = createResponse(503, "Service Unavailable");
        when(mockChain.proceed(any(Request.class))).thenReturn(errorResponse);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(response);
        assertEquals(503, response.code());
    }

    // ========== Additional status code tests ==========

    @Test
    public void testIntercept_403Forbidden_logsError() throws IOException {
        // Arrange
        Response errorResponse = createResponse(403, "Forbidden");
        when(mockChain.proceed(any(Request.class))).thenReturn(errorResponse);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(response);
        assertEquals(403, response.code());
    }

    @Test
    public void testIntercept_405MethodNotAllowed_logsError() throws IOException {
        // Arrange
        Response errorResponse = createResponse(405, "Method Not Allowed");
        when(mockChain.proceed(any(Request.class))).thenReturn(errorResponse);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(response);
        assertEquals(405, response.code());
    }

    @Test
    public void testIntercept_408RequestTimeout_logsError() throws IOException {
        // Arrange
        Response errorResponse = createResponse(408, "Request Timeout");
        when(mockChain.proceed(any(Request.class))).thenReturn(errorResponse);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(response);
        assertEquals(408, response.code());
    }

    @Test
    public void testIntercept_409Conflict_logsError() throws IOException {
        // Arrange
        Response errorResponse = createResponse(409, "Conflict");
        when(mockChain.proceed(any(Request.class))).thenReturn(errorResponse);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(response);
        assertEquals(409, response.code());
    }

    @Test
    public void testIntercept_429TooManyRequests_logsError() throws IOException {
        // Arrange
        Response errorResponse = createResponse(429, "Too Many Requests");
        when(mockChain.proceed(any(Request.class))).thenReturn(errorResponse);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(response);
        assertEquals(429, response.code());
    }

    @Test
    public void testIntercept_504GatewayTimeout_logsError() throws IOException {
        // Arrange
        Response errorResponse = createResponse(504, "Gateway Timeout");
        when(mockChain.proceed(any(Request.class))).thenReturn(errorResponse);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(response);
        assertEquals(504, response.code());
    }

    @Test
    public void testIntercept_unknownErrorCode_logsError() throws IOException {
        // Arrange
        Response errorResponse = createResponse(418, "I'm a teapot");
        when(mockChain.proceed(any(Request.class))).thenReturn(errorResponse);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(response);
        assertEquals(418, response.code());
    }

    // ========== Error with body tests ==========

    @Test
    public void testIntercept_errorWithBody_logsBody() throws IOException {
        // Arrange
        String errorBody = "{\"error\":\"Invalid request\"}";
        Response errorResponse = createResponseWithBody(400, "Bad Request", errorBody);
        when(mockChain.proceed(any(Request.class))).thenReturn(errorResponse);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert - body should still be readable after interceptor
        assertNotNull(response);
        assertEquals(400, response.code());
        assertNotNull(response.body());
    }

    @Test
    public void testIntercept_errorWithNullBody_handles() throws IOException {
        // Arrange
        Response errorResponse = new Response.Builder()
                .request(testRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Internal Server Error")
                .build();
        when(mockChain.proceed(any(Request.class))).thenReturn(errorResponse);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert - should handle null body gracefully
        assertNotNull(response);
        assertEquals(500, response.code());
    }

    @Test
    public void testIntercept_errorWithEmptyBody_logsWithoutBody() throws IOException {
        // Arrange
        Response errorResponse = createResponseWithBody(500, "Internal Server Error", "");
        when(mockChain.proceed(any(Request.class))).thenReturn(errorResponse);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert
        assertNotNull(response);
        assertEquals(500, response.code());
    }

    // ========== Response body reading tests ==========

    @Test
    public void testIntercept_500Error_withMockedResponse_returnsResponse() throws IOException {
        // Arrange
        // Use a mock response to verify the interceptor handles non-Builder responses
        Response mockErrorResponse = mock(Response.class);

        when(mockErrorResponse.isSuccessful()).thenReturn(false);
        when(mockErrorResponse.code()).thenReturn(500);
        when(mockErrorResponse.request()).thenReturn(testRequest);
        when(mockChain.request()).thenReturn(testRequest);
        when(mockChain.proceed(any(Request.class))).thenReturn(mockErrorResponse);

        // Act
        Response response = interceptor.intercept(mockChain);

        // Assert - interceptor logs the error and still returns the response
        assertNotNull(response);
        assertEquals(500, response.code());
    }

    // ========== Exception handling tests ==========

    @Test
    public void testIntercept_propagatesIOException() throws IOException {
        // Arrange
        when(mockChain.proceed(any(Request.class)))
                .thenThrow(new IOException("Network error"));

        // Act & Assert
        assertThrows(IOException.class, () -> interceptor.intercept(mockChain));
    }

    // ========== Helper methods ==========

    /**
     * Creates a mock HTTP response with the given status code.
     *
     * @param code    HTTP status code
     * @param message HTTP status message
     * @return Mock response
     */
    private Response createResponse(int code, String message) {
        return new Response.Builder()
                .request(testRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(message)
                .body(ResponseBody.create("", MediaType.parse("text/plain")))
                .build();
    }

    /**
     * Creates a mock HTTP response with the given status code and body.
     *
     * @param code    HTTP status code
     * @param message HTTP status message
     * @param body    Response body content
     * @return Mock response
     */
    private Response createResponseWithBody(int code, String message, String body) {
        return new Response.Builder()
                .request(testRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(message)
                .body(ResponseBody.create(body, MediaType.parse("application/json")))
                .build();
    }
}
