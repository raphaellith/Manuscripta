package com.manuscripta.student.network.interceptor;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp interceptor that implements retry logic with exponential backoff.
 * Automatically retries failed network requests based on HTTP status codes
 * and network errors.
 *
 * <p>Retry behavior:</p>
 * <ul>
 *   <li>5xx errors: Retry with exponential backoff</li>
 *   <li>4xx errors: Fail immediately (client errors), except for transient errors
 *       (408 Request Timeout, 429 Too Many Requests) which are retried</li>
 *   <li>Network errors (IOException): Retry with exponential backoff</li>
 * </ul>
 */
public class RetryInterceptor implements Interceptor {

    /** Tag for logging. */
    private static final String TAG = "RetryInterceptor";

    /** Default maximum number of retry attempts. */
    private static final int DEFAULT_MAX_RETRIES = 3;

    /** Default initial backoff delay in milliseconds. */
    private static final long DEFAULT_INITIAL_BACKOFF_MS = 1000L;

    /** Default maximum backoff delay in milliseconds. */
    private static final long DEFAULT_MAX_BACKOFF_MS = 32000L;

    /** Default backoff multiplier for exponential growth. */
    private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;

    /** Maximum number of retry attempts. */
    private final int maxRetries;

    /** Initial backoff delay in milliseconds. */
    private final long initialBackoffMs;

    /** Maximum backoff delay in milliseconds. */
    private final long maxBackoffMs;

    /** Backoff multiplier for exponential growth. */
    private final double backoffMultiplier;

    /**
     * Creates a RetryInterceptor with default retry policy.
     */
    public RetryInterceptor() {
        this(DEFAULT_MAX_RETRIES, DEFAULT_INITIAL_BACKOFF_MS,
                DEFAULT_MAX_BACKOFF_MS, DEFAULT_BACKOFF_MULTIPLIER);
    }

    /**
     * Creates a RetryInterceptor with custom retry policy.
     *
     * @param maxRetries         Maximum number of retry attempts
     * @param initialBackoffMs   Initial backoff delay in milliseconds
     * @param maxBackoffMs       Maximum backoff delay in milliseconds
     * @param backoffMultiplier  Backoff multiplier for exponential growth
     * @throws IllegalArgumentException if parameters are invalid
     */
    public RetryInterceptor(int maxRetries, long initialBackoffMs,
                             long maxBackoffMs, double backoffMultiplier) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be non-negative");
        }
        if (initialBackoffMs <= 0) {
            throw new IllegalArgumentException("initialBackoffMs must be positive");
        }
        if (maxBackoffMs < initialBackoffMs) {
            throw new IllegalArgumentException(
                    "maxBackoffMs must be >= initialBackoffMs");
        }
        if (backoffMultiplier <= 1.0) {
            throw new IllegalArgumentException("backoffMultiplier must be > 1.0");
        }

        this.maxRetries = maxRetries;
        this.initialBackoffMs = initialBackoffMs;
        this.maxBackoffMs = maxBackoffMs;
        this.backoffMultiplier = backoffMultiplier;
    }

    /**
     * Intercepts HTTP requests to add retry logic with exponential backoff.
     *
     * @param chain The interceptor chain
     * @return The HTTP response
     * @throws IOException if all retry attempts fail
     */
    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        Response response = null;
        IOException lastException = null;
        long backoffMs = initialBackoffMs;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                // Close previous response if exists
                if (response != null) {
                    response.close();
                }

                // Execute request
                response = chain.proceed(request);

                // Check if response is successful or should not be retried
                if (response.isSuccessful() || !shouldRetry(response)) {
                    return response;
                }

                // Log retry attempt for server errors
                Log.w(TAG, String.format("Request failed with HTTP %d, attempt %d/%d: %s %s",
                        response.code(), attempt + 1, maxRetries + 1,
                        request.method(), request.url()));

            } catch (IOException e) {
                lastException = e;
                Log.w(TAG, String.format("Request failed with IOException, attempt %d/%d: %s %s - %s",
                        attempt + 1, maxRetries + 1,
                        request.method(), request.url(), e.getMessage()));
            }

            // Don't sleep after the last attempt
            if (attempt < maxRetries) {
                sleep(backoffMs);
                backoffMs = calculateNextBackoff(backoffMs);
            }
        }

        // All retries exhausted
        if (response != null) {
            // Return the last failed response
            Log.e(TAG, String.format("All retry attempts exhausted for %s %s - HTTP %d",
                    request.method(), request.url(), response.code()));
            return response;
        } else {
            // Throw the last IOException
            Log.e(TAG, String.format("All retry attempts exhausted for %s %s",
                    request.method(), request.url()));
            throw lastException;
        }
    }

    /**
     * Determines whether a response should be retried based on HTTP status code.
     *
     * @param response The HTTP response
     * @return true if the request should be retried, false otherwise
     */
    @VisibleForTesting
    boolean shouldRetry(@NonNull Response response) {
        int statusCode = response.code();

        // Retry server errors (5xx)
        if (statusCode >= 500 && statusCode < 600) {
            return true;
        }

        // Retry specific client errors that may be transient
        if (statusCode == 408 || statusCode == 429) {
            return true;
        }

        // Don't retry other client errors (4xx) or successful responses
        return false;
    }

    /**
     * Calculates the next backoff delay using exponential backoff.
     *
     * @param currentBackoff The current backoff delay
     * @return The next backoff delay, capped at maxBackoffMs
     */
    @VisibleForTesting
    long calculateNextBackoff(long currentBackoff) {
        long nextBackoff = (long) (currentBackoff * backoffMultiplier);
        return Math.min(nextBackoff, maxBackoffMs);
    }

    /**
     * Sleeps for the specified duration.
     * This method is protected to allow testing without actual delays.
     *
     * @param millis The duration to sleep in milliseconds
     */
    @VisibleForTesting
    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.w(TAG, "Sleep interrupted during backoff");
        }
    }

    /**
     * Gets the maximum number of retry attempts.
     *
     * @return Maximum retry attempts
     */
    @VisibleForTesting
    int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Gets the initial backoff delay in milliseconds.
     *
     * @return Initial backoff delay
     */
    @VisibleForTesting
    long getInitialBackoffMs() {
        return initialBackoffMs;
    }

    /**
     * Gets the maximum backoff delay in milliseconds.
     *
     * @return Maximum backoff delay
     */
    @VisibleForTesting
    long getMaxBackoffMs() {
        return maxBackoffMs;
    }

    /**
     * Gets the backoff multiplier.
     *
     * @return Backoff multiplier
     */
    @VisibleForTesting
    double getBackoffMultiplier() {
        return backoffMultiplier;
    }
}
