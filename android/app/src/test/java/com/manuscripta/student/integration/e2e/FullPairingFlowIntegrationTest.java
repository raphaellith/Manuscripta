package com.manuscripta.student.integration.e2e;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.manuscripta.student.integration.IntegrationTest;
import com.manuscripta.student.integration.config.IntegrationTestConfig;
import com.manuscripta.student.integration.harness.NetworkIntegrationHarness;
import com.manuscripta.student.network.dto.DeviceInfoDto;
import com.manuscripta.student.network.tcp.PairingCallback;
import com.manuscripta.student.network.tcp.PairingState;
import com.manuscripta.student.network.udp.DiscoveryMessage;
import com.manuscripta.student.network.udp.DiscoveryMessageParser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import retrofit2.Response;

/**
 * End-to-end integration test for the full pairing flow.
 *
 * <p>Chains UDP discovery, TCP pairing handshake, and HTTP device
 * registration into a single scenario:</p>
 * <ol>
 *   <li>Listen on UDP &rarr; receive discovery message</li>
 *   <li>Connect TCP to discovered address/port</li>
 *   <li>Send PAIRING_REQUEST &rarr; receive PAIRING_ACK</li>
 *   <li>Call POST /pair with device info &rarr; assert 201</li>
 *   <li>Assert device is now paired end-to-end</li>
 * </ol>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
@Category(IntegrationTest.class)
public class FullPairingFlowIntegrationTest {

    /** UDP receive timeout in milliseconds. */
    private static final int UDP_TIMEOUT_MS = 20_000;

    /** TCP pairing timeout in seconds. */
    private static final long PAIR_TIMEOUT_SECONDS = 5L;

    private IntegrationTestConfig config;
    private NetworkIntegrationHarness harness;
    private DatagramSocket udpSocket;

    /** Builds harness and opens a UDP socket. */
    @Before
    public void setUp() throws Exception {
        config = IntegrationTestConfig.fromEnvironment();
        harness = new NetworkIntegrationHarness(config);
        harness.setUp();

        udpSocket = new DatagramSocket(config.getUdpPort());
        udpSocket.setSoTimeout(UDP_TIMEOUT_MS);
    }

    /** Releases all resources. */
    @After
    public void tearDown() {
        harness.tearDown();
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
    }

    /**
     * Runs the full pairing flow end-to-end:
     * UDP discovery &rarr; TCP pairing &rarr; HTTP registration.
     *
     * @throws Exception if any step fails
     */
    @Test
    public void fullPairingFlow() throws Exception {
        // 1. Receive UDP discovery broadcast
        byte[] buffer = new byte[16];
        DatagramPacket packet =
                new DatagramPacket(buffer, buffer.length);
        udpSocket.receive(packet);

        DiscoveryMessage discovery = DiscoveryMessageParser.parse(
                Arrays.copyOf(packet.getData(), packet.getLength()));
        assertNotNull("Discovery message parsed", discovery);

        // 2–3. TCP pairing handshake using discovered address
        CountDownLatch latch = new CountDownLatch(1);
        harness.getPairingManager().setPairingCallback(
                new PairingCallback() {
                    @Override
                    public void onTcpPairingSuccess() {
                        latch.countDown();
                    }

                    @Override
                    public void onPairingFailed(String reason) {
                        latch.countDown();
                    }

                    @Override
                    public void onPairingTimeout() {
                        latch.countDown();
                    }
                });

        harness.getPairingManager().startPairing(
                config.getTestDeviceId(),
                discovery.getIpAddress(),
                discovery.getTcpPort());

        assertTrue("TCP pairing should complete within timeout",
                latch.await(PAIR_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertEquals(PairingState.PAIRED,
                harness.getPairingManager().getCurrentState());

        // 4. HTTP device registration
        DeviceInfoDto info = new DeviceInfoDto(
                config.getTestDeviceId(),
                config.getTestDeviceName());
        Response<Void> httpResponse = harness.getApiService()
                .registerDevice(info).execute();
        assertEquals("HTTP registration should return 201",
                201, httpResponse.code());

        // 5. Device is now fully paired
        assertNotNull(
                harness.getPairingManager().getDeviceId());
    }
}
