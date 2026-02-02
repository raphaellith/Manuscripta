package com.manuscripta.student.network.interceptor;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

/**
 * OkHttp interceptor that logs HTTP requests and responses.
 * Provides detailed logging of request/response details including headers,
 * body content, and timing information for debugging network operations.
 * Sensitive headers (Authorization, Cookie, etc.) are redacted for security.
 */
public class LoggingInterceptor implements Interceptor {

    /** Tag for logging. */
    private static final String TAG = "LoggingInterceptor";

    /** Maximum body size to log (1 MB). */
    private static final long MAX_BODY_SIZE = 1024 * 1024;

    /** Sensitive headers that should be redacted. */
    private static final Set<String> SENSITIVE_HEADERS = new HashSet<>(Arrays.asList(
            "authorization",
            "proxy-authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "x-auth-token",
            "x-csrf-token"
    ));

    /**
     * Intercepts HTTP requests and responses to log details.
     *
     * @param chain The interceptor chain
     * @return The HTTP response
     * @throws IOException if network error occurs
     */
    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();

        // Log request details
        logRequest(request);

        // Execute request and measure time
        long startTime = System.currentTimeMillis();
        Response response;
        try {
            response = chain.proceed(request);
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            Log.e(TAG, String.format("Request failed after %dms: %s %s - %s",
                    duration, request.method(), request.url(), e.getMessage()));
            throw e;
        }
        long duration = System.currentTimeMillis() - startTime;

        // Log response details and return reconstructed response
        return logResponse(response, duration);
    }

    /**
     * Logs request details including method, URL, headers, and body.
     * Sensitive headers are redacted for security.
     *
     * @param request The HTTP request
     */
    private void logRequest(@NonNull Request request) {
        Log.d(TAG, "┌─────────────────────────────────────────────────────");
        Log.d(TAG, String.format("│ Request: %s %s", request.method(), request.url()));
        Log.d(TAG, "│ Headers:");

        // Log headers with redaction for sensitive ones
        for (int i = 0; i < request.headers().size(); i++) {
            String name = request.headers().name(i);
            String value = request.headers().value(i);
            String redactedValue = redactSensitiveHeader(name, value);
            Log.d(TAG, String.format("│   %s: %s", name, redactedValue));
        }

        // Log request body if present and within size limit
        if (request.body() != null) {
            try {
                long contentLength = request.body().contentLength();
                if (contentLength > MAX_BODY_SIZE) {
                    Log.d(TAG, String.format("│ Body: <too large to log: %d bytes>", contentLength));
                } else {
                    Buffer buffer = new Buffer();
                    request.body().writeTo(buffer);
                    String body = buffer.readUtf8();
                    Log.d(TAG, "│ Body:");
                    Log.d(TAG, "│   " + body);
                }
            } catch (IOException e) {
                Log.e(TAG, "│ Failed to read request body: " + e.getMessage());
            }
        }
    }

    /**
     * Logs response details including status code, headers, body, and duration.
     * Sensitive headers are redacted for security.
     *
     * @param response The HTTP response
     * @param duration Request duration in milliseconds
     * @return Response with reconstructed body (if body was logged)
     */
    @NonNull
    private Response logResponse(@NonNull Response response, long duration) {
        Log.d(TAG, "│");
        Log.d(TAG, String.format("│ Response: %d %s (%dms)",
                response.code(), response.message(), duration));
        Log.d(TAG, "│ Headers:");

        // Log headers with redaction for sensitive ones
        for (int i = 0; i < response.headers().size(); i++) {
            String name = response.headers().name(i);
            String value = response.headers().value(i);
            String redactedValue = redactSensitiveHeader(name, value);
            Log.d(TAG, String.format("│   %s: %s", name, redactedValue));
        }

        // Log response body if present and within size limit
        ResponseBody responseBody = response.body();
        if (responseBody != null) {
            try {
                long contentLength = responseBody.contentLength();
                if (contentLength > MAX_BODY_SIZE) {
                    Log.d(TAG, String.format("│ Body: <too large to log: %d bytes>", contentLength));
                } else {
                    // Use peekBody to read without consuming the original
                    ResponseBody peekedBody = response.peekBody(MAX_BODY_SIZE);
                    String bodyString = peekedBody.string();
                    Log.d(TAG, "│ Body:");
                    Log.d(TAG, "│   " + bodyString);
                }
            } catch (IOException e) {
                Log.e(TAG, "│ Failed to read response body: " + e.getMessage());
            }
        }

        Log.d(TAG, "└─────────────────────────────────────────────────────");
        return response;
    }

    /**
     * Redacts sensitive header values for security.
     *
     * @param name  The header name
     * @param value The header value
     * @return The original value or "██████" if sensitive
     */
    @NonNull
    private String redactSensitiveHeader(@NonNull String name, @NonNull String value) {
        if (SENSITIVE_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
            return "██████";
        }
        return value;
    }
}
