package com.manuscripta.student.network;

import androidx.annotation.NonNull;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * DiscoveryMessageParser is a class that parses a discovery message.
 * 
 * <pre>
 * Protocol Reference (API Contract Section 3.3):
 * +------------+--------+--------+------------------------------------------------+
 * | Field      | Offset | Size   | Description                                    |
 * +------------+--------+--------+------------------------------------------------+
 * | Opcode     | 0      | 1 byte | 0x00 = DISCOVERY                               |
 * | IP Address | 1      | 4 bytes| IPv4 address (network byte order, big-endian)  |
 * | HTTP Port  | 5      | 2 bytes| Unsigned, little-endian                        |
 * | TCP Port   | 7      | 2 bytes| Unsigned, little-endian                        |
 * +------------+--------+--------+------------------------------------------------+
 * </pre>
 * 
 * @author William Stephen
 */
public class DiscoveryMessageParser {
    /**
     * Parses a discovery message.
     * @param data The discovery message to parse.
     * @return The parsed discovery message.
     */
    @NonNull
    public static DiscoveryMessage parse(@NonNull byte[] data){
        // Validate length
        if (data.length != 9){
            throw new IllegalArgumentException("Message length must be 9 bytes. Message length received: " + data.length + " bytes");
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
        
        // Return the parsed discovery message
        return new DiscoveryMessage(
                ipAddress,
                httpPort,
                tcpPort
        );
    }
}
