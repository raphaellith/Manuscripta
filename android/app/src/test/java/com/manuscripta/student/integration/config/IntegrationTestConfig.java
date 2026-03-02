package com.manuscripta.student.integration.config;

import androidx.annotation.NonNull;

/**
 * Configuration POJO that reads connection parameters from environment
 * variables with sensible defaults.
 *
 * <p>Environment variables:
 * <ul>
 *   <li>{@code MANUSCRIPTA_SERVER_HOST} — server IP/hostname
 *       (default {@code "localhost"})</li>
 *   <li>{@code MANUSCRIPTA_HTTP_PORT} — HTTP port
 *       (default {@code 5911})</li>
 *   <li>{@code MANUSCRIPTA_TCP_PORT} — TCP port
 *       (default {@code 5912})</li>
 *   <li>{@code MANUSCRIPTA_UDP_PORT} — UDP port
 *       (default {@code 5913})</li>
 * </ul>
 */
public final class IntegrationTestConfig {

    /** Default server hostname. */
    private static final String DEFAULT_HOST = "localhost";

    /** Default HTTP port per API Contract. */
    private static final int DEFAULT_HTTP_PORT = 5911;

    /** Default TCP port per API Contract. */
    private static final int DEFAULT_TCP_PORT = 5912;

    /** Default UDP port per API Contract. */
    private static final int DEFAULT_UDP_PORT = 5913;

    /** Fixed device ID for reproducible tests. */
    private static final String DEFAULT_DEVICE_ID =
            "inttest-00000000-0000-0000-0000-000000000001";

    /** Fixed device name for reproducible tests. */
    private static final String DEFAULT_DEVICE_NAME = "Integration Test Tablet";

    private final String serverHost;
    private final int httpPort;
    private final int tcpPort;
    private final int udpPort;
    private final String testDeviceId;
    private final String testDeviceName;

    /**
     * Creates a config with explicitly supplied values.
     *
     * @param serverHost    server hostname or IP
     * @param httpPort      HTTP port
     * @param tcpPort       TCP port
     * @param udpPort       UDP port
     * @param testDeviceId  device ID used in test requests
     * @param testDeviceName device name used in test requests
     */
    public IntegrationTestConfig(@NonNull String serverHost,
                                 int httpPort,
                                 int tcpPort,
                                 int udpPort,
                                 @NonNull String testDeviceId,
                                 @NonNull String testDeviceName) {
        this.serverHost = serverHost;
        this.httpPort = httpPort;
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        this.testDeviceId = testDeviceId;
        this.testDeviceName = testDeviceName;
    }

    /**
     * Reads configuration from {@code System.getenv()} with sensible
     * defaults for local development.
     *
     * @return a fully-populated config instance
     */
    @NonNull
    public static IntegrationTestConfig fromEnvironment() {
        String host = envOrDefault(
                "MANUSCRIPTA_SERVER_HOST", DEFAULT_HOST);
        int http = intEnvOrDefault(
                "MANUSCRIPTA_HTTP_PORT", DEFAULT_HTTP_PORT);
        int tcp = intEnvOrDefault(
                "MANUSCRIPTA_TCP_PORT", DEFAULT_TCP_PORT);
        int udp = intEnvOrDefault(
                "MANUSCRIPTA_UDP_PORT", DEFAULT_UDP_PORT);
        return new IntegrationTestConfig(
                host, http, tcp, udp,
                DEFAULT_DEVICE_ID, DEFAULT_DEVICE_NAME);
    }

    /**
     * Returns the HTTP base URL computed from host and port.
     *
     * @return base URL ending with {@code /}
     */
    @NonNull
    public String getHttpBaseUrl() {
        return "http://" + serverHost + ":" + httpPort + "/";
    }

    /** @return server hostname or IP */
    @NonNull
    public String getServerHost() {
        return serverHost;
    }

    /** @return HTTP port */
    public int getHttpPort() {
        return httpPort;
    }

    /** @return TCP port */
    public int getTcpPort() {
        return tcpPort;
    }

    /** @return UDP port */
    public int getUdpPort() {
        return udpPort;
    }

    /** @return fixed test device ID */
    @NonNull
    public String getTestDeviceId() {
        return testDeviceId;
    }

    /** @return fixed test device name */
    @NonNull
    public String getTestDeviceName() {
        return testDeviceName;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static String envOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : fallback;
    }

    private static int intEnvOrDefault(String key, int fallback) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
