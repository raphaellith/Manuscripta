package com.manuscripta.student.network.interceptor;

import com.manuscripta.student.utils.ConnectionManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
public class RetryInterceptorTest {

    @Mock
    private Interceptor.Chain mockChain;

    @Mock
    private ConnectionManager mockConnectionManager;

    private Request testRequest;

    /**
     * Test implementation of RetryInterceptor that doesn't actually sleep.
     */
    private static class TestableRetryInterceptor extends RetryInterceptor {
        private final List<Long> sleepDurations = new ArrayList<>();

        TestableRetryInterceptor(ConnectionManager connectionManager) {
            super(connectionManager);
        }

        TestableRetryInterceptor(ConnectionManager connectionManager, int maxRetries,
                                  long initialBackoffMs, long maxBackoffMs, double backoffMultiplier) {
            super(connectionManager, maxRetries, initialBackoffMs, maxBackoffMs, backoffMultiplier);
        }

        @Override
        protected void sleep(long millis) throws IOException {
            sleepDurations.add(millis);
            // Don't actually sleep in tests
        }

        int getSleepCallCount() {
            return sleepDurations.size();
        }

        long getLastSleepDuration() {
            return sleepDurations.isEmpty() ? 0 : sleepDurations.get(sleepDurations.size() - 1);
        }

        List<Long> getSleepDurations() {
            return sleepDurations;
        }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        testRequest = new Request.Builder()
                .url("https://api.test.com/endpoint")
                .build();
        when(mockChain.request()).thenReturn(testRequest);
        // Default: network is available
        when(mockConnectionManager.isNetworkAvailable()).thenReturn(true);
    }

    // ========== Constructor tests ==========

    @Test
    public void testConstructor_default_succeeds() {
        RetryInterceptor interceptor = new RetryInterceptor(mockConnectionManager);

        assertNotNull(interceptor);
        assertEquals(3, interceptor.getMaxRetries());
        assertEquals(1000L, interceptor.getInitialBackoffMs());
        assertEquals(32000L, interceptor.getMaxBackoffMs());
        assertEquals(2.0, interceptor.getBackoffMultiplier(), 0.001);
    }

    @Test
    public void testConstructor_customValid_succeeds() {
        RetryInterceptor interceptor = new RetryInterceptor(mockConnectionManager, 5, 2000L, 64000L, 3.0);

        assertNotNull(interceptor);
        assertEquals(5, interceptor.getMaxRetries());
        assertEquals(2000L, interceptor.getInitialBackoffMs());
        assertEquals(64000L, interceptor.getMaxBackoffMs());
        assertEquals(3.0, interceptor.getBackoffMultiplier(), 0.001);
    }

    @Test
    public void testConstructor_negativeMaxRetries_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new RetryInterceptor(mockConnectionManager, -1, 1000L, 32000L, 2.0));
    }

    @Test
    public void testConstructor_zeroInitialBackoff_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new RetryInterceptor(mockConnectionManager, 3, 0L, 32000L, 2.0));
    }

    @Test
    public void testConstructor_negativeInitialBackoff_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new RetryInterceptor(mockConnectionManager, 3, -1000L, 32000L, 2.0));
    }

    @Test
    public void testConstructor_maxBackoffLessThanInitial_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new RetryInterceptor(mockConnectionManager, 3, 2000L, 1000L, 2.0));
    }

    @Test
    public void testConstructor_backoffMultiplierOne_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new RetryInterceptor(mockConnectionManager, 3, 1000L, 32000L, 1.0));
    }

    @Test
    public void testConstructor_backoffMultiplierLessThanOne_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new RetryInterceptor(mockConnectionManager, 3, 1000L, 32000L, 0.5));
    }

    @Test
    public void testConstructor_zeroMaxRetries_succeeds() {
        RetryInterceptor interceptor = new RetryInterceptor(mockConnectionManager, 0, 1000L, 32000L, 2.0);

        assertNotNull(interceptor);
        assertEquals(0, interceptor.getMaxRetries());
    }

    // ========== Successful response tests ==========

    @Test
    public void testIntercept_successfulResponse_noRetry() throws IOException {
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(mockConnectionManager);
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
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(mockConnectionManager);
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
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(mockConnectionManager);
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
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(mockConnectionManager);
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
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(mockConnectionManager);
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
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(mockConnectionManager);
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
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(mockConnectionManager);
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
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(mockConnectionManager);
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
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(mockConnectionManager);
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
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(mockConnectionManager);
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
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(mockConnectionManager);
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
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(mockConnectionManager);
        when(mockChain.proceed(any(Request.class)))
                .thenThrow(new IOException("Network error"));

        assertThrows(IOException.class, () -> interceptor.intercept(mockChain));

        verify(mockChain, times(4)).proceed(testRequest);
        assertEquals(3, interceptor.getSleepCallCount());
    }

    @Test
    public void testIntercept_ioExceptionThenSuccess_succeeds() throws IOException {
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(mockConnectionManager);
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
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(mockConnectionManager);
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
        RetryInterceptor interceptor = new RetryInterceptor(mockConnectionManager);

        long next = interceptor.calculateNextBackoff(1000L);

        assertEquals(2000L, next);
    }

    @Test
    public void testCalculateNextBackoff_respectsMaxBackoff() {
        RetryInterceptor interceptor = new RetryInterceptor(mockConnectionManager,3, 1000L, 5000L, 2.0);

        long next = interceptor.calculateNextBackoff(4000L);

        assertEquals(5000L, next);
    }

    @Test
    public void testCalculateNextBackoff_customMultiplier() {
        RetryInterceptor interceptor = new RetryInterceptor(mockConnectionManager,3, 1000L, 100000L, 3.0);

        long next = interceptor.calculateNextBackoff(1000L);

        assertEquals(3000L, next);
    }

    @Test
    public void testIntercept_exponentialBackoffProgression() throws IOException {
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(mockConnectionManager,3, 1000L, 32000L, 2.0);
        when(mockChain.proceed(any(Request.class)))
                .thenAnswer(inv -> createResponse(500, "Internal Server Error"));

        interceptor.intercept(mockChain);

        // Verify sleep was called 3 times with doubling durations: 1000 -> 2000 -> 4000
        List<Long> durations = interceptor.getSleepDurations();
        assertEquals(3, durations.size());
        assertEquals(1000L, (long) durations.get(0));
        assertEquals(2000L, (long) durations.get(1));
        assertEquals(4000L, (long) durations.get(2));
    }

    // ========== Custom retry configuration tests ==========

    @Test
    public void testIntercept_customMaxRetries_respectsLimit() throws IOException {
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(mockConnectionManager,1, 1000L, 32000L, 2.0);
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
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(mockConnectionManager,0, 1000L, 32000L, 2.0);
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
        RetryInterceptor interceptor = new RetryInterceptor(mockConnectionManager);
        Response response = createResponse(500, "Internal Server Error");

        boolean shouldRetry = interceptor.shouldRetry(response);

        assertTrue(shouldRetry);
    }

    @Test
    public void testShouldRetry_503_returnsTrue() {
        RetryInterceptor interceptor = new RetryInterceptor(mockConnectionManager);
        Response response = createResponse(503, "Service Unavailable");

        boolean shouldRetry = interceptor.shouldRetry(response);

        assertTrue(shouldRetry);
    }

    @Test
    public void testShouldRetry_408_returnsTrue() {
        RetryInterceptor interceptor = new RetryInterceptor(mockConnectionManager);
        Response response = createResponse(408, "Request Timeout");

        boolean shouldRetry = interceptor.shouldRetry(response);

        assertTrue(shouldRetry);
    }

    @Test
    public void testShouldRetry_429_returnsTrue() {
        RetryInterceptor interceptor = new RetryInterceptor(mockConnectionManager);
        Response response = createResponse(429, "Too Many Requests");

        boolean shouldRetry = interceptor.shouldRetry(response);

        assertTrue(shouldRetry);
    }

    @Test
    public void testShouldRetry_400_returnsFalse() {
        RetryInterceptor interceptor = new RetryInterceptor(mockConnectionManager);
        Response response = createResponse(400, "Bad Request");

        boolean shouldRetry = interceptor.shouldRetry(response);

        assertFalse(shouldRetry);
    }

    @Test
    public void testShouldRetry_404_returnsFalse() {
        RetryInterceptor interceptor = new RetryInterceptor(mockConnectionManager);
        Response response = createResponse(404, "Not Found");

        boolean shouldRetry = interceptor.shouldRetry(response);

        assertFalse(shouldRetry);
    }

    @Test
    public void testShouldRetry_200_returnsFalse() {
        RetryInterceptor interceptor = new RetryInterceptor(mockConnectionManager);
        Response response = createResponse(200, "OK");

        boolean shouldRetry = interceptor.shouldRetry(response);

        assertFalse(shouldRetry);
    }

    // ========== Network availability tests ==========

    @Test
    public void testConstructor_nullConnectionManager_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new RetryInterceptor(null));
    }

    @Test
    public void testIntercept_noNetworkAtStart_throwsImmediately() {
        when(mockConnectionManager.isNetworkAvailable()).thenReturn(false);
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(mockConnectionManager);
        Response successResponse = createResponse(200, "OK");
        when(mockChain.proceed(any(Request.class))).thenReturn(successResponse);

        IOException exception = assertThrows(IOException.class,
                () -> interceptor.intercept(mockChain));

        assertEquals("No network connectivity", exception.getMessage());
        verify(mockChain, times(0)).proceed(any(Request.class));
        assertEquals(0, interceptor.getSleepCallCount());
    }

    @Test
    public void testIntercept_networkLostDuringRetry_throwsException() throws IOException {
        // Network available for first attempt, then lost
        when(mockConnectionManager.isNetworkAvailable())
                .thenReturn(true)   // Initial check
                .thenReturn(false); // Before first retry
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(mockConnectionManager);
        when(mockChain.proceed(any(Request.class)))
                .thenAnswer(inv -> createResponse(500, "Internal Server Error"));

        IOException exception = assertThrows(IOException.class,
                () -> interceptor.intercept(mockChain));

        assertEquals("Network lost during retry attempts", exception.getMessage());
        // Should attempt once, fail with 500, then check network before retry
        verify(mockChain, times(1)).proceed(testRequest);
        assertEquals(0, interceptor.getSleepCallCount());
    }

    @Test
    public void testIntercept_networkAvailableThroughoutRetries_completesAllRetries() throws IOException {
        // Network available throughout
        when(mockConnectionManager.isNetworkAvailable()).thenReturn(true);
        TestableRetryInterceptor interceptor = new TestableRetryInterceptor(mockConnectionManager);
        when(mockChain.proceed(any(Request.class)))
                .thenAnswer(inv -> createResponse(500, "Internal Server Error"));

        Response response = interceptor.intercept(mockChain);

        assertNotNull(response);
        assertEquals(500, response.code());
        // Should verify network: once at start + 3 times before retries = 4 total
        verify(mockConnectionManager, times(4)).isNetworkAvailable();
        verify(mockChain, times(4)).proceed(testRequest);
        assertEquals(3, interceptor.getSleepCallCount());
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
