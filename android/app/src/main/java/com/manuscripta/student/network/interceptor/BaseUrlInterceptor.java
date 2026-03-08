package com.manuscripta.student.network.interceptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp interceptor that dynamically rewrites the request URL's host and port
 * to point at the paired teacher server discovered via UDP.
 *
 * <p>Before pairing completes, requests proceed with the placeholder base URL
 * (which will fail, but no HTTP calls should happen before pairing).</p>
 */
public class BaseUrlInterceptor implements Interceptor {

    /**
     * Provides the current server connection details.
     */
    public interface ServerInfoProvider {
        /**
         * Returns the server host (IP address), or null if not yet paired.
         *
         * @return The server host, or null.
         */
        @Nullable
        String getServerHost();

        /**
         * Returns the server HTTP port, or 0 if not yet paired.
         *
         * @return The server HTTP port.
         */
        int getServerHttpPort();
    }

    /** Provider for server connection details. */
    @NonNull
    private final ServerInfoProvider serverInfoProvider;

    /**
     * Creates a new BaseUrlInterceptor with the given server info provider.
     *
     * @param serverInfoProvider Provider for obtaining the server host and port
     */
    public BaseUrlInterceptor(@NonNull ServerInfoProvider serverInfoProvider) {
        if (serverInfoProvider == null) {
            throw new IllegalArgumentException("ServerInfoProvider cannot be null");
        }
        this.serverInfoProvider = serverInfoProvider;
    }

    /**
     * Intercepts HTTP requests to rewrite the host and port to the paired server.
     *
     * @param chain The interceptor chain
     * @return The HTTP response
     * @throws IOException if a network error occurs
     */
    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();

        String host = serverInfoProvider.getServerHost();
        int port = serverInfoProvider.getServerHttpPort();

        if (host == null || host.isEmpty() || port <= 0) {
            return chain.proceed(originalRequest);
        }

        HttpUrl newUrl = originalRequest.url().newBuilder()
                .scheme("http")
                .host(host)
                .port(port)
                .build();

        Request newRequest = originalRequest.newBuilder()
                .url(newUrl)
                .build();

        return chain.proceed(newRequest);
    }
}
