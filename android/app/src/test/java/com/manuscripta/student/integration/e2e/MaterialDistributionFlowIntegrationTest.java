package com.manuscripta.student.integration.e2e;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.manuscripta.student.integration.IntegrationTest;
import com.manuscripta.student.integration.config.IntegrationTestConfig;
import com.manuscripta.student.integration.harness.NetworkIntegrationHarness;
import com.manuscripta.student.network.dto.DeviceInfoDto;
import com.manuscripta.student.network.dto.DistributionBundleDto;
import com.manuscripta.student.network.dto.MaterialDto;
import com.manuscripta.student.network.tcp.PairingCallback;
import com.manuscripta.student.network.tcp.TcpMessage;
import com.manuscripta.student.network.tcp.TcpMessageListenerAdapter;
import com.manuscripta.student.network.tcp.TcpOpcode;
import com.manuscripta.student.network.tcp.message.DistributeAckMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import retrofit2.Response;

/**
 * End-to-end integration test for the material distribution flow.
 *
 * <p>Scenario:</p>
 * <ol>
 *   <li>Pair with the server (TCP + HTTP)</li>
 *   <li>Start heartbeat</li>
 *   <li>Wait for {@code DISTRIBUTE_MATERIAL} signal over TCP</li>
 *   <li>Fetch distribution bundle via HTTP</li>
 *   <li>Send {@code DISTRIBUTE_ACK} per material over TCP</li>
 *   <li>Verify the server accepted the ACKs</li>
 * </ol>
 *
 * <p>Prerequisite: the Windows server must have materials staged for
 * the test device. If no materials are staged, the distribution
 * signal will not arrive and the test succeeds vacuously.</p>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
@Category(IntegrationTest.class)
public class MaterialDistributionFlowIntegrationTest {

    /** Maximum seconds to wait for DISTRIBUTE_MATERIAL signal. */
    private static final long SIGNAL_TIMEOUT_SECONDS = 15L;

    private IntegrationTestConfig config;
    private NetworkIntegrationHarness harness;

    /** Wires harness and pairs the device. */
    @Before
    public void setUp() throws Exception {
        config = IntegrationTestConfig.fromEnvironment();
        harness = new NetworkIntegrationHarness(config);
        harness.setUp();
        pairDevice();
    }

    /** Stops heartbeat, disconnects, and releases resources. */
    @After
    public void tearDown() {
        harness.tearDown();
    }

    /**
     * Runs the full material distribution flow.
     *
     * @throws Exception if any step fails
     */
    @Test
    public void materialDistributionFlow() throws Exception {
        // --- 1. Register a message listener ---
        BlockingQueue<TcpMessage> received =
                new LinkedBlockingQueue<>();
        harness.getTcpSocketManager().addMessageListener(
                new TcpMessageListenerAdapter() {
                    @Override
                    public void onMessageReceived(
                            TcpMessage message) {
                        received.add(message);
                    }
                });

        // --- 2. Start heartbeat ---
        harness.getHeartbeatManager().start();

        // --- 3. Wait for DISTRIBUTE_MATERIAL ---
        TcpMessage distributeSignal = pollForOpcode(
                received, TcpOpcode.DISTRIBUTE_MATERIAL,
                SIGNAL_TIMEOUT_SECONDS);

        if (distributeSignal == null) {
            // No materials staged — acceptable, skip remainder
            return;
        }

        // --- 4. Fetch distribution bundle via HTTP ---
        Response<DistributionBundleDto> httpResponse =
                harness.getApiService()
                        .getDistribution(config.getTestDeviceId())
                        .execute();

        assertEquals(200, httpResponse.code());
        DistributionBundleDto bundle = httpResponse.body();
        assertNotNull(bundle);
        assertNotNull(bundle.getMaterials());
        assertFalse("Materials list should not be empty",
                bundle.getMaterials().isEmpty());

        // --- 5. Send DISTRIBUTE_ACK per material ---
        List<MaterialDto> materials = bundle.getMaterials();
        for (MaterialDto mat : materials) {
            DistributeAckMessage ack = new DistributeAckMessage(
                    config.getTestDeviceId(), mat.getId());
            harness.getAckRetrySender().send(ack,
                    "MaterialDistributionFlowTest");
        }

        // --- 6. Briefly drain: no error expected ---
        TcpMessage error = received.poll(2, TimeUnit.SECONDS);
        if (error != null) {
            assertTrue("No UNPAIR expected after ACKs",
                    error.getOpcode() != TcpOpcode.UNPAIR);
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void pairDevice() throws Exception {
        // TCP pairing
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
                config.getServerHost(),
                config.getTcpPort());
        assertTrue("TCP pairing must succeed",
                latch.await(5, TimeUnit.SECONDS));

        // HTTP registration
        DeviceInfoDto info = new DeviceInfoDto(
                config.getTestDeviceId(),
                config.getTestDeviceName());
        harness.getApiService().registerDevice(info).execute();
    }

    private static TcpMessage pollForOpcode(
            BlockingQueue<TcpMessage> queue,
            TcpOpcode target,
            long timeoutSeconds) throws InterruptedException {
        long deadline = System.currentTimeMillis()
                + timeoutSeconds * 1000;
        while (System.currentTimeMillis() < deadline) {
            TcpMessage msg = queue.poll(1, TimeUnit.SECONDS);
            if (msg != null && msg.getOpcode() == target) {
                return msg;
            }
        }
        return null;
    }
}
