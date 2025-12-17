package com.manuscripta.student.network.udp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import org.junit.Test;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Unit tests for {@link DiscoveryMessageParser}.
 */
public class DiscoveryMessageParserTest {

    @Test
    public void testPrivateConstructor_throwsAssertionError() throws Exception {
        // Given: Access to the private constructor via reflection
        java.lang.reflect.Constructor<DiscoveryMessageParser> constructor =
                DiscoveryMessageParser.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When/Then: Attempting to instantiate throws InvocationTargetException wrapping AssertionError
        java.lang.reflect.InvocationTargetException exception = assertThrows(
                java.lang.reflect.InvocationTargetException.class,
                constructor::newInstance
        );
        
        // Verify the cause is AssertionError with correct message
        assertNotNull(exception.getCause());
        assertEquals(AssertionError.class, exception.getCause().getClass());
        assertEquals("Utility class should not be instantiated", exception.getCause().getMessage());
    }

    @Test
    public void testParse_validMessage_returnsDiscoveryMessage() {
        // Given: Valid discovery message with IP 192.168.1.100, HTTP port 8080, TCP port 9090
        byte[] data = createValidDiscoveryMessage(
                new byte[]{(byte) 192, (byte) 168, 1, 100},
                8080,
                9090
        );

        // When
        DiscoveryMessage message = DiscoveryMessageParser.parse(data);

        // Then
        assertNotNull(message);
        assertEquals("192.168.1.100", message.getIpAddress());
        assertEquals(8080, message.getHttpPort());
        assertEquals(9090, message.getTcpPort());
    }

    @Test
    public void testParse_localhostIP_parsesCorrectly() {
        // Given: Localhost IP (127.0.0.1)
        byte[] data = createValidDiscoveryMessage(
                new byte[]{127, 0, 0, 1},
                443,
                8443
        );

        // When
        DiscoveryMessage message = DiscoveryMessageParser.parse(data);

        // Then
        assertEquals("127.0.0.1", message.getIpAddress());
        assertEquals(443, message.getHttpPort());
        assertEquals(8443, message.getTcpPort());
    }

    @Test
    public void testParse_minimumPortValues_parsesCorrectly() {
        // Given: Minimum port values (0)
        byte[] data = createValidDiscoveryMessage(
                new byte[]{10, 0, 0, 1},
                0,
                0
        );

        // When
        DiscoveryMessage message = DiscoveryMessageParser.parse(data);

        // Then
        assertEquals("10.0.0.1", message.getIpAddress());
        assertEquals(0, message.getHttpPort());
        assertEquals(0, message.getTcpPort());
    }

    @Test
    public void testParse_maximumPortValues_parsesCorrectly() {
        // Given: Maximum port values (65535)
        byte[] data = createValidDiscoveryMessage(
                new byte[]{(byte) 172, 16, 0, 1},
                65535,
                65535
        );

        // When
        DiscoveryMessage message = DiscoveryMessageParser.parse(data);

        // Then
        assertEquals("172.16.0.1", message.getIpAddress());
        assertEquals(65535, message.getHttpPort());
        assertEquals(65535, message.getTcpPort());
    }

    @Test
    public void testParse_unsignedByteValues_parsesCorrectly() {
        // Given: IP address with bytes > 127 (tests unsigned byte handling)
        byte[] data = createValidDiscoveryMessage(
                new byte[]{(byte) 255, (byte) 255, (byte) 255, (byte) 255},
                1234,
                5678
        );

        // When
        DiscoveryMessage message = DiscoveryMessageParser.parse(data);

        // Then
        assertEquals("255.255.255.255", message.getIpAddress());
        assertEquals(1234, message.getHttpPort());
        assertEquals(5678, message.getTcpPort());
    }

    @Test
    public void testParse_privateNetworkIP_parsesCorrectly() {
        // Given: Private network IP (10.x.x.x)
        byte[] data = createValidDiscoveryMessage(
                new byte[]{10, 20, 30, 40},
                8080,
                9090
        );

        // When
        DiscoveryMessage message = DiscoveryMessageParser.parse(data);

        // Then
        assertEquals("10.20.30.40", message.getIpAddress());
    }

    @Test
    public void testParse_messageTooShort_throwsException() {
        // Given: Message with only 8 bytes (should be 9)
        byte[] data = new byte[8];

        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> DiscoveryMessageParser.parse(data)
        );
        assertEquals(
                "Message length must be 9 bytes. Message length received: 8 bytes",
                exception.getMessage()
        );
    }

    @Test
    public void testParse_messageTooLong_throwsException() {
        // Given: Message with 10 bytes (should be 9)
        byte[] data = new byte[10];

        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> DiscoveryMessageParser.parse(data)
        );
        assertEquals(
                "Message length must be 9 bytes. Message length received: 10 bytes",
                exception.getMessage()
        );
    }

    @Test
    public void testParse_emptyMessage_throwsException() {
        // Given: Empty byte array
        byte[] data = new byte[0];

        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> DiscoveryMessageParser.parse(data)
        );
        assertEquals(
                "Message length must be 9 bytes. Message length received: 0 bytes",
                exception.getMessage()
        );
    }

    @Test
    public void testParse_invalidOpcode_throwsException() {
        // Given: Message with invalid opcode (0x01 instead of 0x00)
        byte[] data = createDiscoveryMessageWithOpcode(
                (byte) 0x01,
                new byte[]{(byte) 192, (byte) 168, 1, 100},
                8080,
                9090
        );

        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> DiscoveryMessageParser.parse(data)
        );
        assertEquals("Opcode must be 0x00. Opcode received: 1", exception.getMessage());
    }

    @Test
    public void testParse_opcodeFF_throwsException() {
        // Given: Message with opcode 0xFF
        byte[] data = createDiscoveryMessageWithOpcode(
                (byte) 0xFF,
                new byte[]{(byte) 192, (byte) 168, 1, 100},
                8080,
                9090
        );

        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> DiscoveryMessageParser.parse(data)
        );
        assertEquals("Opcode must be 0x00. Opcode received: -1", exception.getMessage());
    }

    @Test
    public void testParse_littleEndianPortEncoding_parsesCorrectly() {
        // Given: Manually construct message to verify little-endian port encoding
        // Port 256 (0x0100) in little-endian is [0x00, 0x01]
        byte[] data = new byte[9];
        data[0] = 0x00; // Opcode
        data[1] = (byte) 192;  // IP: 192
        data[2] = (byte) 168; // IP: 168
        data[3] = 1;    // IP: 1
        data[4] = 1;    // IP: 1
        data[5] = 0x00; // HTTP port low byte
        data[6] = 0x01; // HTTP port high byte (256 = 0x0100)
        data[7] = (byte) 0xFF; // TCP port low byte
        data[8] = (byte) 0xFF; // TCP port high byte (65535 = 0xFFFF)

        // When
        DiscoveryMessage message = DiscoveryMessageParser.parse(data);

        // Then
        assertEquals("192.168.1.1", message.getIpAddress());
        assertEquals(256, message.getHttpPort());
        assertEquals(65535, message.getTcpPort());
    }

    /**
     * Helper method to create a valid discovery message byte array.
     * 
     * @param ipBytes The IP address as 4 bytes
     * @param httpPort The HTTP port (0-65535)
     * @param tcpPort The TCP port (0-65535)
     * @return The discovery message as byte array
     */
    private byte[] createValidDiscoveryMessage(byte[] ipBytes, int httpPort, int tcpPort) {
        return createDiscoveryMessageWithOpcode((byte) 0x00, ipBytes, httpPort, tcpPort);
    }

    /**
     * Helper method to create a discovery message with custom opcode.
     * 
     * @param opcode The opcode byte
     * @param ipBytes The IP address as 4 bytes
     * @param httpPort The HTTP port (0-65535)
     * @param tcpPort The TCP port (0-65535)
     * @return The discovery message as byte array
     */
    private byte[] createDiscoveryMessageWithOpcode(byte opcode,
                                                    byte[] ipBytes,
                                                    int httpPort,
                                                    int tcpPort) {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.put(opcode);
        buffer.put(ipBytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) httpPort);
        buffer.putShort((short) tcpPort);
        return buffer.array();
    }
}
