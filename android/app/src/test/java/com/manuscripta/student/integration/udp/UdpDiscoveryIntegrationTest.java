package com.manuscripta.student.integration.udp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.manuscripta.student.integration.IntegrationTest;
import com.manuscripta.student.integration.config.IntegrationTestConfig;
import com.manuscripta.student.network.udp.DiscoveryMessage;
import com.manuscripta.student.network.udp.DiscoveryMessageParser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

/**
 * Integration tests for UDP discovery broadcasts.
 *
 * <p>Uses a raw {@link DatagramSocket} to receive discovery packets
 * from the Windows server, bypassing {@code UdpDiscoveryManager}
 * (which depends on Android {@code WifiManager}). This verifies the
 * wire protocol directly.</p>
 *
 * <p>Per API Contract §3.3, the discovery packet is 9 bytes:
 * <pre>
 * [1 byte opcode 0x00][4 bytes IPv4][2 bytes HTTP port LE]
 * [2 bytes TCP port LE]
 * </pre></p>
 */
@Category(IntegrationTest.class)
public class UdpDiscoveryIntegrationTest {

    /** Socket receive timeout in milliseconds (20 seconds). */
    private static final int RECEIVE_TIMEOUT_MS = 20_000;

    private IntegrationTestConfig config;
    private DatagramSocket socket;

    /** Opens a UDP socket bound to the configured port. */
    @Before
    public void setUp() throws Exception {
        config = IntegrationTestConfig.fromEnvironment();
        socket = new DatagramSocket(config.getUdpPort());
        socket.setSoTimeout(RECEIVE_TIMEOUT_MS);
    }

    /** Closes the UDP socket. */
    @After
    public void tearDown() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    /**
     * Verifies that a 9-byte discovery broadcast is received and
     * parsed successfully with the correct opcode.
     *
     * @throws Exception if the socket times out or parsing fails
     */
    @Test
    public void receiveDiscoveryBroadcast_parsesCorrectly()
            throws Exception {
        byte[] buffer = new byte[16];
        DatagramPacket packet =
                new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        byte[] data = Arrays.copyOf(
                packet.getData(), packet.getLength());
        assertEquals("Discovery packet must be 9 bytes",
                9, data.length);

        // Opcode must be 0x00
        assertEquals("Opcode must be DISCOVERY (0x00)",
                0x00, data[0]);

        DiscoveryMessage msg = DiscoveryMessageParser.parse(data);
        assertNotNull(msg);
        assertNotNull("IP address should be present",
                msg.getIpAddress());
        assertTrue("IP address should not be empty",
                !msg.getIpAddress().isEmpty());
    }

    /**
     * Verifies that the parsed discovery message contains the
     * expected server host and port values.
     *
     * @throws Exception if the socket times out or parsing fails
     */
    @Test
    public void receiveDiscoveryBroadcast_matchesExpectedConfig()
            throws Exception {
        byte[] buffer = new byte[16];
        DatagramPacket packet =
                new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        DiscoveryMessage msg = DiscoveryMessageParser.parse(
                Arrays.copyOf(packet.getData(), packet.getLength()));

        assertEquals("HTTP port should match config",
                config.getHttpPort(), msg.getHttpPort());
        assertEquals("TCP port should match config",
                config.getTcpPort(), msg.getTcpPort());
    }

    /**
     * Verifies broadcast cadence by receiving two packets and
     * asserting the interval is approximately 3 seconds.
     *
     * @throws Exception if the socket times out
     */
    @Test
    public void discoveryBroadcast_cadenceIsApprox3Seconds()
            throws Exception {
        byte[] buffer = new byte[16];
        DatagramPacket packet =
                new DatagramPacket(buffer, buffer.length);

        socket.receive(packet);
        long first = System.currentTimeMillis();

        socket.receive(packet);
        long second = System.currentTimeMillis();

        long interval = second - first;

        // Allow generous tolerance: 1.5s – 6s
        assertTrue("Interval should be roughly 3s but was "
                + interval + "ms",
                interval >= 1500 && interval <= 6000);
    }
}
