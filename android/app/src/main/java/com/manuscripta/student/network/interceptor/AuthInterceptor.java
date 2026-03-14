package com.manuscripta.student.network.interceptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp interceptor that adds authentication headers to HTTP requests.
 * Includes device identification headers for API authentication.
 */
public class AuthInterceptor implements Interceptor {

    /** HTTP header name for device identification. */
    private static final String HEADER_DEVICE_ID = "X-Device-ID";

    /** Provider for device ID. */
    @NonNull
    private final DeviceIdProvider deviceIdProvider;

    /**
     * Creates a new AuthInterceptor with the given device ID provider.
     *
     * @param deviceIdProvider Provider for obtaining the device ID
     * @throws IllegalArgumentException if deviceIdProvider is null
     */
    public AuthInterceptor(@NonNull DeviceIdProvider deviceIdProvider) {
        if (deviceIdProvider == null) {
            throw new IllegalArgumentException("DeviceIdProvider cannot be null");
        }
        this.deviceIdProvider = deviceIdProvider;
    }

    /**
     * Intercepts HTTP requests to add authentication headers.
     *
     * @param chain The interceptor chain
     * @return The HTTP response
     * @throws IOException if network error occurs
     */
    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // Get device ID from provider
        String deviceId = deviceIdProvider.getDeviceId();

        // If device ID is available, add it as a header
        Request.Builder requestBuilder = originalRequest.newBuilder();
        if (deviceId != null && !deviceId.isEmpty()) {
            requestBuilder.header(HEADER_DEVICE_ID, deviceId);
        }

        Request newRequest = requestBuilder.build();
        return chain.proceed(newRequest);
    }

    /**
     * Functional interface for providing device ID.
     */
    public interface DeviceIdProvider {
        /**
         * Gets the current device ID.
         *
         * @return The device ID, or null if not available
         */
        @Nullable
        String getDeviceId();
    }
}
