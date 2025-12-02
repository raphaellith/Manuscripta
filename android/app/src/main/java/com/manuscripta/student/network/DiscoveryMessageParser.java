package com.manuscripta.student.network;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DiscoveryMessageParser {
    @NonNull
    public static DiscoveryMessage parse(@NonNull byte[] data){
        // Validate length
        if (data.length != 9){
            throw new IllegalArgumentException("Message length must be 9 Bytes. Message length received: " + data.length + " bytes");
        }

        // Wrap the bytes in a ByteBuffer
        ByteBuffer buffer = ByteBuffer.wrap(data);

        // Parse and validate opcode
        byte opcode = buffer.get();
        if (opcode != 0x00){
            throw new IllegalArgumentException("Opcode must be 0x00. Opcode received: " + opcode);
        }

        // Parse IP Address into String
        byte[] ipBytes = new byte[4];
        buffer.get(ipBytes);
        String ipAddress = String.format("%d.%d.%d.%d", ipBytes[0] & 0xFF, ipBytes[1] & 0xFF, ipBytes[2] & 0xFF, ipBytes[3] & 0xFF);

        // Parse Ports
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int httpPort = buffer.getShort() & 0xFFFF;
        int tcpPort = buffer.getShort() & 0xFFFF;

        return new DiscoveryMessage(
                ipAddress,
                httpPort,
                tcpPort
        );
    }
}
