package com.manuscripta.student.network;

import org.jetbrains.annotations.NotNull;

public class DiscoveryMessage {

    @NotNull private String ipAddress;

    @NotNull private int httpPort;

    @NotNull private int tcpPort;

    public DiscoveryMessage(String ipAddress,
                            int httpPort,
                            int tcpPort){
        // Validation to come
        this.ipAddress = ipAddress;
        this.httpPort = httpPort;
        this.tcpPort = tcpPort;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public int getHttpPort() {
        return this.httpPort;
    }

    public int getTcpPort() {
        return this.tcpPort;
    }
}
