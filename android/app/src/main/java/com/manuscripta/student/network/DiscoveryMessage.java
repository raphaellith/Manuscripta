package com.manuscripta.student.network;

import androidx.annotation.NonNull;

public class DiscoveryMessage {

    @NonNull private final String ipAddress;

    private final int httpPort;

    private final int tcpPort;

    public DiscoveryMessage(@NonNull String ipAddress,
                            int httpPort,
                            int tcpPort){

        if (ipAddress == null || ipAddress.isEmpty()){
            throw new IllegalArgumentException("IP Address cannot be Null or Empty");
        }
        if (httpPort < 0 || httpPort > 65535){
            throw new IllegalArgumentException("HTTP Port must be between 0 and 65535 inclusive");
        }
        if (tcpPort < 0 || tcpPort > 65535){
            throw new IllegalArgumentException("HTTP Port must be between 0 and 65535 inclusive");
        }
        this.ipAddress = ipAddress;
        this.httpPort = httpPort;
        this.tcpPort = tcpPort;
    }

    @NonNull
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
