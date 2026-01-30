package com.manuscripta.student.network.interceptor;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

/**
 * OkHttp interceptor that logs HTTP requests and responses.
 * Provides detailed logging of request/response details including headers,
 * body content, and timing information for debugging network operations.
 */
public class LoggingInterceptor implements Interceptor {

    private static final String TAG = "LoggingInterceptor";

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
     *
     * @param request The HTTP request
     */
    private void logRequest(@NonNull Request request) {
        Log.d(TAG, "┌─────────────────────────────────────────────────────");
        Log.d(TAG, String.format("│ Request: %s %s", request.method(), request.url()));
        Log.d(TAG, "│ Headers:");

        // Log headers
        for (int i = 0; i < request.headers().size(); i++) {
            String name = request.headers().name(i);
            String value = request.headers().value(i);
            Log.d(TAG, String.format("│   %s: %s", name, value));
        }

        // Log request body if present
        if (request.body() != null) {
            try {
                Buffer buffer = new Buffer();
                request.body().writeTo(buffer);
                String body = buffer.readUtf8();
                Log.d(TAG, "│ Body:");
                Log.d(TAG, "│   " + body);
            } catch (IOException e) {
                Log.e(TAG, "│ Failed to read request body: " + e.getMessage());
            }
        }
    }

    /**
     * Logs response details including status code, headers, body, and duration.
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

        // Log headers
        for (int i = 0; i < response.headers().size(); i++) {
            String name = response.headers().name(i);
            String value = response.headers().value(i);
            Log.d(TAG, String.format("│   %s: %s", name, value));
        }

        // Log response body if present
        ResponseBody responseBody = response.body();
        if (responseBody != null) {
            try {
                // Read body content
                String bodyString = responseBody.string();
                Log.d(TAG, "│ Body:");
                Log.d(TAG, "│   " + bodyString);

                // Reconstruct response with buffered body since we consumed it
                ResponseBody newBody = ResponseBody.create(
                        bodyString,
                        responseBody.contentType()
                );
                response = response.newBuilder().body(newBody).build();
            } catch (IOException e) {
                Log.e(TAG, "│ Failed to read response body: " + e.getMessage());
            }
        }

        Log.d(TAG, "└─────────────────────────────────────────────────────");
        return response;
    }
}
