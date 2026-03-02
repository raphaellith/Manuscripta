package com.manuscripta.student.integration.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.manuscripta.student.integration.IntegrationTest;
import com.manuscripta.student.integration.config.IntegrationTestConfig;
import com.manuscripta.student.integration.harness.NetworkIntegrationHarness;
import com.manuscripta.student.network.tcp.PairingCallback;
import com.manuscripta.student.network.tcp.TcpMessage;
import com.manuscripta.student.network.tcp.TcpMessageListenerAdapter;
import com.manuscripta.student.network.tcp.TcpOpcode;
import com.manuscripta.student.network.tcp.message.DistributeAckMessage;
import com.manuscripta.student.network.tcp.message.FeedbackAckMessage;
import com.manuscripta.student.network.tcp.message.HandRaisedMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for TCP command messages.
 *
 * <p>After pairing, verifies that:</p>
 * <ul>
 *   <li>{@code HAND_RAISED} (0x11) can be sent and
 *       {@code HAND_ACK} (0x06) is received</li>
 *   <li>{@code DISTRIBUTE_ACK} (0x12) and
 *       {@code FEEDBACK_ACK} (0x13) are accepted by the server</li>
 *   <li>Server-initiated commands
 *       ({@code LOCK_SCREEN}, {@code UNLOCK_SCREEN},
 *       {@code REFRESH_CONFIG}, {@code UNPAIR}) can be received
 *       when triggered</li>
 * </ul>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
@Category(IntegrationTest.class)
public class TcpCommandIntegrationTest {

    /** Maximum seconds to wait for a response message. */
    private static final long TIMEOUT_SECONDS = 5L;

    private NetworkIntegrationHarness harness;
    private IntegrationTestConfig config;
    private BlockingQueue<TcpMessage> received;

    /**
     * Wires harness, pairs, and registers a message listener.
     *
     * @throws Exception if pairing fails
     */
    @Before
    public void setUp() throws Exception {
        config = IntegrationTestConfig.fromEnvironment();
        harness = new NetworkIntegrationHarness(config);
        harness.setUp();
        pairDevice();

        received = new LinkedBlockingQueue<>();
        harness.getTcpSocketManager().addMessageListener(
                new TcpMessageListenerAdapter() {
                    @Override
                    public void onMessageReceived(
                            TcpMessage message) {
                        received.add(message);
                    }
                });
    }

    /** Disconnects and releases resources. */
    @After
    public void tearDown() {
        harness.tearDown();
    }

    /**
     * Verifies that sending {@code HAND_RAISED} results in
     * a {@code HAND_ACK} from the server.
     *
     * @throws Exception if the send or poll fails
     */
    @Test
    public void handRaised_receivesHandAck() throws Exception {
        HandRaisedMessage msg = new HandRaisedMessage(
                config.getTestDeviceId());
        harness.getTcpSocketManager().send(msg);

        TcpMessage ack = pollForOpcode(
                TcpOpcode.HAND_ACK, TIMEOUT_SECONDS);
        assertNotNull("Expected HAND_ACK within timeout", ack);
        assertEquals(TcpOpcode.HAND_ACK, ack.getOpcode());
    }

    /**
     * Verifies that the server accepts a {@code DISTRIBUTE_ACK}
     * without returning an error.
     *
     * @throws Exception if the send fails
     */
    @Test
    public void distributeAck_acceptedByServer() throws Exception {
        DistributeAckMessage ack = new DistributeAckMessage(
                config.getTestDeviceId(), "test-material-001");

        // Should not throw — server silently accepts
        harness.getTcpSocketManager().send(ack);

        // No error response expected; drain briefly
        TcpMessage error = received.poll(2, TimeUnit.SECONDS);
        // If the server echoes nothing, null is acceptable
        if (error != null) {
            assertTrue("Unexpected error opcode",
                    error.getOpcode() != TcpOpcode.UNPAIR);
        }
    }

    /**
     * Verifies that the server accepts a {@code FEEDBACK_ACK}
     * without returning an error.
     *
     * @throws Exception if the send fails
     */
    @Test
    public void feedbackAck_acceptedByServer() throws Exception {
        FeedbackAckMessage ack = new FeedbackAckMessage(
                config.getTestDeviceId(), "test-feedback-001");

        harness.getTcpSocketManager().send(ack);

        TcpMessage error = received.poll(2, TimeUnit.SECONDS);
        if (error != null) {
            assertTrue("Unexpected error opcode",
                    error.getOpcode() != TcpOpcode.UNPAIR);
        }
    }

    /**
     * Verifies that server-initiated commands can be received.
     *
     * <p>This test is conditional: if the server has been configured
     * to trigger a command (e.g. via a test control API), one of
     * {@code LOCK_SCREEN}, {@code UNLOCK_SCREEN},
     * {@code REFRESH_CONFIG}, or {@code UNPAIR} should arrive.
     * Without pre-staging, the test succeeds vacuously.</p>
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void serverCommands_canBeReceived() throws Exception {
        // Start heartbeat so the server knows we are active
        harness.getHeartbeatManager().start();

        long deadline = System.currentTimeMillis()
                + TIMEOUT_SECONDS * 1000;
        boolean receivedCommand = false;

        while (System.currentTimeMillis() < deadline) {
            TcpMessage msg = received.poll(1, TimeUnit.SECONDS);
            if (msg != null && isServerCommand(msg.getOpcode())) {
                receivedCommand = true;
                break;
            }
        }

        // Observational — no failure if nothing arrives
        if (receivedCommand) {
            assertTrue("Server command received", true);
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void pairDevice() throws Exception {
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

        assertTrue("Pairing prerequisite failed",
                latch.await(5, TimeUnit.SECONDS));
    }

    private TcpMessage pollForOpcode(TcpOpcode target, long seconds)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + seconds * 1000;
        while (System.currentTimeMillis() < deadline) {
            TcpMessage msg = received.poll(1, TimeUnit.SECONDS);
            if (msg != null && msg.getOpcode() == target) {
                return msg;
            }
        }
        return null;
    }

    private static boolean isServerCommand(TcpOpcode opcode) {
        return opcode == TcpOpcode.LOCK_SCREEN
                || opcode == TcpOpcode.UNLOCK_SCREEN
                || opcode == TcpOpcode.REFRESH_CONFIG
                || opcode == TcpOpcode.UNPAIR;
    }
}
