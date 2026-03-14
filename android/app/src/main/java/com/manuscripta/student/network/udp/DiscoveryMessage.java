package com.manuscripta.student.network.udp;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

/**
 * DiscoveryMessage is a class that represents a discovery message.
 * @author William Stephen
 *  */
public class DiscoveryMessage {

    /**
     * Regex pattern for validating IPv4 addresses.
     * Matches xxx.xxx.xxx.xxx where each octet is 0-255.
     */
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}"
            + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    /**
     * The IP address of the device.
     */
    @NonNull private final String ipAddress;

    /**
     * The HTTP port of the device.
     */
    private final int httpPort;

    /**
     * The TCP port of the device.
     */
    private final int tcpPort;

    /**
     * Constructor for DiscoveryMessage.
     * @param ipAddress The IP address of the device.
     * @param httpPort The HTTP port of the device.
     * @param tcpPort The TCP port of the device.
     */
    public DiscoveryMessage(@NonNull String ipAddress,
                            int httpPort,
                            int tcpPort) {
        // Constructor validation
        if (ipAddress == null) {
            throw new IllegalArgumentException("IP Address cannot be Null");
        }
        if (ipAddress.isEmpty()) {
            throw new IllegalArgumentException("IP Address cannot be Empty");
        }
        if (!IPV4_PATTERN.matcher(ipAddress).matches()) {
            throw new IllegalArgumentException("IP Address must be a valid IPv4 address (xxx.xxx.xxx.xxx)");
        }
        if (httpPort < 0 || httpPort > 65535) {
            throw new IllegalArgumentException("HTTP Port must be between 0 and 65535 inclusive");
        }
        if (tcpPort < 0 || tcpPort > 65535) {
            throw new IllegalArgumentException("TCP Port must be between 0 and 65535 inclusive");
        }
        this.ipAddress = ipAddress;
        this.httpPort = httpPort;
        this.tcpPort = tcpPort;
    }

    /**
     * Returns the IP address of the device.
     * @return The IP address of the device.
     */
    @NonNull
    public String getIpAddress() {
        return this.ipAddress;
    }

    /**
     * Returns the HTTP port of the device.
     * @return The HTTP port of the device.
     */
    public int getHttpPort() {
        return this.httpPort;
    }

    /**
     * Returns the TCP port of the device.
     * @return The TCP port of the device.
     */
    public int getTcpPort() {
        return this.tcpPort;
    }
}
