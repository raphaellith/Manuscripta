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
import okhttp3.Response;
import okhttp3.ResponseBody;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RetryInterceptor}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class RetryInterceptorTest {

    @Mock
    private Interceptor.Chain mockChain;

    private Request testRequest;

    /**
     * Test implementation of RetryInterceptor that doesn't actually sleep.
     */
    private static class TestableRetryInterceptor extends RetryInterceptor {
        private int sleepCallCount = 0;
        private long lastSleepDuration = 0;

        TestableRetryInterceptor() {
            super();
        }

        TestableRetryInterceptor(int maxRetries, long initialBackoffMs,
                                  long maxBackoffMs, double backoffMultiplier) {
            super(maxRetries, initialBackoffMs, maxBackoffMs, backoffMultiplier);
        }

        @Override
        protected void sleep(long millis) throws IOException {
            sleepCallCount++;
            lastSleepDuration = millis;
            // Don't actually sleep in tests
        }

        int getSleepCallCount() {
            return sleepCallCount;
        }

        long getLastSleepDuration() {
            return lastSleepDuration;
        }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        testRequest = new Request.Builder()
                .url("https://api.test.com/endpoint")
                .build();
        when(mockChain.request()).thenReturn(testRequest);
    }

    // ========== Constructor tests ==========

    @Test
    public void testConstructor_default_succeeds() {
        RetryInterceptor interceptor = new RetryInterceptor();

        assertNotNull(interceptor);
        assertEquals(3, interceptor.getMaxRetries());
        assertEquals(1000L, interceptor.getInitialBackoffMs());
        assertEquals(32000L, interceptor.getMaxBackoffMs());
        assertEquals(2.0, interceptor.getBackoffMultiplier(), 0.001);
    }

    @Test
    public void testConstructor_customValid_succeeds() {
        RetryInterceptor interceptor = new RetryInterceptor(5, 2000L, 64000L, 3.0);

        assertNotNull(interceptor);
        assertEquals(5, interceptor.getMaxRetries());
        assertEquals(2000L, interceptor.getInitialBackoffMs());
        assertEquals(64000L, interceptor.getMaxBackoffMs());
        assertEquals(3.0, interceptor.getBackoffMultiplier(), 0.001);
    }

    @Test
    public void testConstructor_negativeMaxRetries_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new RetryInterceptor(-1, 1000L, 32000L, 2.0));
    }

    @Test
    public void testConstructor_zeroInitialBackoff_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new RetryInterceptor(3, 0L, 32000L, 2.0));
    }

    @Test
    public void testConstructor_negativeInitialBackoff_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new RetryInterceptor(3, -1000L, 32000L, 2.0));
    }

    @Test
    public void testConstructor_maxBackoffLessThanInitial_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new RetryInterceptor(3, 2000L, 1000L, 2.0));
    }

    @Test
    public void testConstructor_backoffMultiplierOne_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new RetryInterceptor(3, 1000L, 32000L, 1.0));
    }

    @Test
    public void testConstructor_backoffMultiplierLessThanOne_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new RetryInterceptor(3, 1000L, 32000L, 0.5));
    }

    @Test
    public void testConstructor_zeroMaxRetries_succeeds() {
        RetryInterceptor interceptor = new RetryInterceptor(0, 1000L, 32000L, 2.0);

        assertNotNull(interceptor);
        assertEquals(0, interceptor.getMaxRetries());
    }

    // ========== Successful response tests ==========

    @Test
    public void testIntercept_successfulResponse_noRetry() throws IOException {
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor();
        Response successResponse = createResponse(200, "OK");
        when(mockChain.proceed(any(Request.class))).thenReturn(successResponse);

        Response response = interceptor.intercept(mockChain);

        assertNotNull(response);
        assertEquals(200, response.code());
        verify(mockChain, times(1)).proceed(testRequest);
        assertEquals(0, interceptor.getSleepCallCount());
    }

    @Test
    public void testIntercept_201Created_noRetry() throws IOException {
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor();
        Response successResponse = createResponse(201, "Created");
        when(mockChain.proceed(any(Request.class))).thenReturn(successResponse);

        Response response = interceptor.intercept(mockChain);

        assertNotNull(response);
        assertEquals(201, response.code());
        verify(mockChain, times(1)).proceed(testRequest);
        assertEquals(0, interceptor.getSleepCallCount());
    }

    // ========== 5xx error retry tests ==========

    @Test
    public void testIntercept_500InternalServerError_retries() throws IOException {
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor();
        when(mockChain.proceed(any(Request.class)))
                .thenAnswer(inv -> createResponse(500, "Internal Server Error"));

        Response response = interceptor.intercept(mockChain);

        assertNotNull(response);
        assertEquals(500, response.code());
        // Should try initial + 3 retries = 4 times
        verify(mockChain, times(4)).proceed(testRequest);
        // Should sleep 3 times (after first 3 attempts, not after last)
        assertEquals(3, interceptor.getSleepCallCount());
    }

    @Test
    public void testIntercept_502BadGateway_retries() throws IOException {
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor();
        when(mockChain.proceed(any(Request.class)))
                .thenAnswer(inv -> createResponse(502, "Bad Gateway"));

        Response response = interceptor.intercept(mockChain);

        assertNotNull(response);
        assertEquals(502, response.code());
        verify(mockChain, times(4)).proceed(testRequest);
        assertEquals(3, interceptor.getSleepCallCount());
    }

    @Test
    public void testIntercept_503ServiceUnavailable_retries() throws IOException {
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor();
        when(mockChain.proceed(any(Request.class)))
                .thenAnswer(inv -> createResponse(503, "Service Unavailable"));

        Response response = interceptor.intercept(mockChain);

        assertNotNull(response);
        assertEquals(503, response.code());
        verify(mockChain, times(4)).proceed(testRequest);
        assertEquals(3, interceptor.getSleepCallCount());
    }

    @Test
    public void testIntercept_504GatewayTimeout_retries() throws IOException {
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor();
        when(mockChain.proceed(any(Request.class)))
                .thenAnswer(inv -> createResponse(504, "Gateway Timeout"));

        Response response = interceptor.intercept(mockChain);

        assertNotNull(response);
        assertEquals(504, response.code());
        verify(mockChain, times(4)).proceed(testRequest);
        assertEquals(3, interceptor.getSleepCallCount());
    }

    // ========== 4xx error no retry tests ==========

    @Test
    public void testIntercept_400BadRequest_noRetry() throws IOException {
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor();
        Response errorResponse = createResponse(400, "Bad Request");
        when(mockChain.proceed(any(Request.class))).thenReturn(errorResponse);

        Response response = interceptor.intercept(mockChain);

        assertNotNull(response);
        assertEquals(400, response.code());
        verify(mockChain, times(1)).proceed(testRequest);
        assertEquals(0, interceptor.getSleepCallCount());
    }

    @Test
    public void testIntercept_401Unauthorized_noRetry() throws IOException {
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor();
        Response errorResponse = createResponse(401, "Unauthorized");
        when(mockChain.proceed(any(Request.class))).thenReturn(errorResponse);

        Response response = interceptor.intercept(mockChain);

        assertNotNull(response);
        assertEquals(401, response.code());
        verify(mockChain, times(1)).proceed(testRequest);
        assertEquals(0, interceptor.getSleepCallCount());
    }

    @Test
    public void testIntercept_404NotFound_noRetry() throws IOException {
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor();
        Response errorResponse = createResponse(404, "Not Found");
        when(mockChain.proceed(any(Request.class))).thenReturn(errorResponse);

        Response response = interceptor.intercept(mockChain);

        assertNotNull(response);
        assertEquals(404, response.code());
        verify(mockChain, times(1)).proceed(testRequest);
        assertEquals(0, interceptor.getSleepCallCount());
    }

    // ========== Special 4xx retry cases ==========

    @Test
    public void testIntercept_408RequestTimeout_retries() throws IOException {
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor();
        when(mockChain.proceed(any(Request.class)))
                .thenAnswer(inv -> createResponse(408, "Request Timeout"));

        Response response = interceptor.intercept(mockChain);

        assertNotNull(response);
        assertEquals(408, response.code());
        verify(mockChain, times(4)).proceed(testRequest);
        assertEquals(3, interceptor.getSleepCallCount());
    }

    @Test
    public void testIntercept_429TooManyRequests_retries() throws IOException {
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor();
        when(mockChain.proceed(any(Request.class)))
                .thenAnswer(inv -> createResponse(429, "Too Many Requests"));

        Response response = interceptor.intercept(mockChain);

        assertNotNull(response);
        assertEquals(429, response.code());
        verify(mockChain, times(4)).proceed(testRequest);
        assertEquals(3, interceptor.getSleepCallCount());
    }

    // ========== IOException retry tests ==========

    @Test
    public void testIntercept_ioException_retries() throws IOException {
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor();
        when(mockChain.proceed(any(Request.class)))
                .thenThrow(new IOException("Network error"));

        assertThrows(IOException.class, () -> interceptor.intercept(mockChain));

        verify(mockChain, times(4)).proceed(testRequest);
        assertEquals(3, interceptor.getSleepCallCount());
    }

    @Test
    public void testIntercept_ioExceptionThenSuccess_succeeds() throws IOException {
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor();
        Response successResponse = createResponse(200, "OK");

        when(mockChain.proceed(any(Request.class)))
                .thenThrow(new IOException("Network error"))
                .thenThrow(new IOException("Network error"))
                .thenReturn(successResponse);

        Response response = interceptor.intercept(mockChain);

        assertNotNull(response);
        assertEquals(200, response.code());
        verify(mockChain, times(3)).proceed(testRequest);
        assertEquals(2, interceptor.getSleepCallCount());
    }

    // ========== Retry then success tests ==========

    @Test
    public void testIntercept_500ThenSuccess_succeeds() throws IOException {
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor();
        Response errorResponse = createResponse(500, "Internal Server Error");
        Response successResponse = createResponse(200, "OK");

        when(mockChain.proceed(any(Request.class)))
                .thenReturn(errorResponse)
                .thenReturn(successResponse);

        Response response = interceptor.intercept(mockChain);

        assertNotNull(response);
        assertEquals(200, response.code());
        verify(mockChain, times(2)).proceed(testRequest);
        assertEquals(1, interceptor.getSleepCallCount());
    }

    // ========== Exponential backoff tests ==========

    @Test
    public void testCalculateNextBackoff_doublesBackoff() {
        RetryInterceptor interceptor = new RetryInterceptor();

        long next = interceptor.calculateNextBackoff(1000L);

        assertEquals(2000L, next);
    }

    @Test
    public void testCalculateNextBackoff_respectsMaxBackoff() {
        RetryInterceptor interceptor = new RetryInterceptor(3, 1000L, 5000L, 2.0);

        long next = interceptor.calculateNextBackoff(4000L);

        assertEquals(5000L, next);
    }

    @Test
    public void testCalculateNextBackoff_customMultiplier() {
        RetryInterceptor interceptor = new RetryInterceptor(3, 1000L, 100000L, 3.0);

        long next = interceptor.calculateNextBackoff(1000L);

        assertEquals(3000L, next);
    }

    @Test
    public void testIntercept_exponentialBackoffProgression() throws IOException {
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(3, 1000L, 32000L, 2.0);
        when(mockChain.proceed(any(Request.class)))
                .thenAnswer(inv -> createResponse(500, "Internal Server Error"));

        interceptor.intercept(mockChain);

        // Verify sleep was called 3 times with increasing durations
        assertEquals(3, interceptor.getSleepCallCount());
        // Last sleep should be 4000ms (1000 * 2^2)
        assertEquals(4000L, interceptor.getLastSleepDuration());
    }

    // ========== Custom retry configuration tests ==========

    @Test
    public void testIntercept_customMaxRetries_respectsLimit() throws IOException {
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(1, 1000L, 32000L, 2.0);
        when(mockChain.proceed(any(Request.class)))
                .thenAnswer(inv -> createResponse(500, "Internal Server Error"));

        Response response = interceptor.intercept(mockChain);

        assertNotNull(response);
        assertEquals(500, response.code());
        // Should try initial + 1 retry = 2 times
        verify(mockChain, times(2)).proceed(testRequest);
        assertEquals(1, interceptor.getSleepCallCount());
    }

    @Test
    public void testIntercept_zeroMaxRetries_noRetry() throws IOException {
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(0, 1000L, 32000L, 2.0);
        Response errorResponse = createResponse(500, "Internal Server Error");
        when(mockChain.proceed(any(Request.class))).thenReturn(errorResponse);

        Response response = interceptor.intercept(mockChain);

        assertNotNull(response);
        assertEquals(500, response.code());
        verify(mockChain, times(1)).proceed(testRequest);
        assertEquals(0, interceptor.getSleepCallCount());
    }

    // ========== shouldRetry tests ==========

    @Test
    public void testShouldRetry_500_returnsTrue() {
        RetryInterceptor interceptor = new RetryInterceptor();
        Response response = createResponse(500, "Internal Server Error");

        boolean shouldRetry = interceptor.shouldRetry(response);

        assertTrue(shouldRetry);
    }

    @Test
    public void testShouldRetry_503_returnsTrue() {
        RetryInterceptor interceptor = new RetryInterceptor();
        Response response = createResponse(503, "Service Unavailable");

        boolean shouldRetry = interceptor.shouldRetry(response);

        assertTrue(shouldRetry);
    }

    @Test
    public void testShouldRetry_408_returnsTrue() {
        RetryInterceptor interceptor = new RetryInterceptor();
        Response response = createResponse(408, "Request Timeout");

        boolean shouldRetry = interceptor.shouldRetry(response);

        assertTrue(shouldRetry);
    }

    @Test
    public void testShouldRetry_429_returnsTrue() {
        RetryInterceptor interceptor = new RetryInterceptor();
        Response response = createResponse(429, "Too Many Requests");

        boolean shouldRetry = interceptor.shouldRetry(response);

        assertTrue(shouldRetry);
    }

    @Test
    public void testShouldRetry_400_returnsFalse() {
        RetryInterceptor interceptor = new RetryInterceptor();
        Response response = createResponse(400, "Bad Request");

        boolean shouldRetry = interceptor.shouldRetry(response);

        assertFalse(shouldRetry);
    }

    @Test
    public void testShouldRetry_404_returnsFalse() {
        RetryInterceptor interceptor = new RetryInterceptor();
        Response response = createResponse(404, "Not Found");

        boolean shouldRetry = interceptor.shouldRetry(response);

        assertFalse(shouldRetry);
    }

    @Test
    public void testShouldRetry_200_returnsFalse() {
        RetryInterceptor interceptor = new RetryInterceptor();
        Response response = createResponse(200, "OK");

        boolean shouldRetry = interceptor.shouldRetry(response);

        assertFalse(shouldRetry);
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
}
