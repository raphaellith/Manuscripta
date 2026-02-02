package com.manuscripta.student.network.interceptor;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * OkHttp interceptor that provides standardized error handling.
 * Logs HTTP errors (4xx and 5xx status codes) with detailed information
 * to help debug API communication issues.
 */
public class ErrorInterceptor implements Interceptor {

    /** Tag for logging. */
    private static final String TAG = "ErrorInterceptor";

    /** Maximum error body size to log (1 MB). */
    private static final long MAX_ERROR_BODY_SIZE = 1024 * 1024;

    /**
     * Intercepts HTTP responses to detect and log errors.
     *
     * @param chain The interceptor chain
     * @return The HTTP response
     * @throws IOException if network error occurs
     */
    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);

        // Check for HTTP errors
        if (!response.isSuccessful()) {
            handleError(request, response);
        }

        return response;
    }

    /**
     * Handles HTTP error responses by logging detailed error information.
     *
     * @param request  The HTTP request that resulted in an error
     * @param response The error response
     */
    private void handleError(@NonNull Request request, @NonNull Response response) {
        int statusCode = response.code();
        String method = request.method();
        String url = request.url().toString();

        if (statusCode >= 400 && statusCode < 500) {
            // Client errors (4xx)
            logClientError(method, url, statusCode, response);
        } else if (statusCode >= 500) {
            // Server errors (5xx)
            logServerError(method, url, statusCode, response);
        }
    }

    /**
     * Logs client error details (4xx status codes).
     *
     * @param method     The HTTP method
     * @param url        The request URL
     * @param statusCode The HTTP status code
     * @param response   The error response
     */
    private void logClientError(@NonNull String method, @NonNull String url,
                                 int statusCode, @NonNull Response response) {
        String errorMessage = getErrorMessage(statusCode);
        String body = readResponseBody(response);

        Log.e(TAG, String.format("Client Error: %s %s - HTTP %d %s",
                method, url, statusCode, errorMessage));

        if (body != null && !body.isEmpty()) {
            Log.e(TAG, "Error body: " + body);
        }
    }

    /**
     * Logs server error details (5xx status codes).
     *
     * @param method     The HTTP method
     * @param url        The request URL
     * @param statusCode The HTTP status code
     * @param response   The error response
     */
    private void logServerError(@NonNull String method, @NonNull String url,
                                 int statusCode, @NonNull Response response) {
        String errorMessage = getErrorMessage(statusCode);
        String body = readResponseBody(response);

        Log.e(TAG, String.format("Server Error: %s %s - HTTP %d %s",
                method, url, statusCode, errorMessage));

        if (body != null && !body.isEmpty()) {
            Log.e(TAG, "Error body: " + body);
        }
    }

    /**
     * Reads the response body safely without consuming it.
     * Uses peekBody() to read up to a maximum size without consuming the original body.
     *
     * @param response The HTTP response
     * @return The response body as a string, or null if reading failed
     */
    private String readResponseBody(@NonNull Response response) {
        ResponseBody body = response.body();
        if (body == null) {
            return null;
        }

        try {
            // Peek at the body without consuming it, with a reasonable size cap
            ResponseBody peekedBody = response.peekBody(MAX_ERROR_BODY_SIZE);
            return peekedBody.string();
        } catch (IOException e) {
            // Catch any IO exceptions during body reading
            Log.e(TAG, "Failed to read error response body: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets a human-readable error message for common HTTP status codes.
     *
     * @param statusCode The HTTP status code
     * @return A descriptive error message
     */
    @NonNull
    private String getErrorMessage(int statusCode) {
        switch (statusCode) {
            case 400:
                return "Bad Request";
            case 401:
                return "Unauthorized";
            case 403:
                return "Forbidden";
            case 404:
                return "Not Found";
            case 405:
                return "Method Not Allowed";
            case 408:
                return "Request Timeout";
            case 409:
                return "Conflict";
            case 422:
                return "Unprocessable Entity";
            case 429:
                return "Too Many Requests";
            case 500:
                return "Internal Server Error";
            case 502:
                return "Bad Gateway";
            case 503:
                return "Service Unavailable";
            case 504:
                return "Gateway Timeout";
            default:
                return "Unknown Error";
        }
    }
}
