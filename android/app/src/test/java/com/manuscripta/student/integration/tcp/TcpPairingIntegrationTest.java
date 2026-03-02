package com.manuscripta.student.integration.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.manuscripta.student.integration.IntegrationTest;
import com.manuscripta.student.integration.config.IntegrationTestConfig;
import com.manuscripta.student.integration.harness.NetworkIntegrationHarness;
import com.manuscripta.student.network.tcp.ConnectionState;
import com.manuscripta.student.network.tcp.PairingCallback;
import com.manuscripta.student.network.tcp.PairingState;
import com.manuscripta.student.network.tcp.TcpMessage;
import com.manuscripta.student.network.tcp.TcpMessageListener;
import com.manuscripta.student.network.tcp.TcpMessageListenerAdapter;
import com.manuscripta.student.network.tcp.TcpOpcode;
import com.manuscripta.student.network.tcp.TcpProtocolException;

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
 * Integration tests for the TCP pairing handshake.
 *
 * <p>Connects to the real Windows server via TCP and verifies that the
 * {@code PAIRING_REQUEST} / {@code PAIRING_ACK} exchange completes
 * correctly, including {@link PairingState} transitions.</p>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
@Category(IntegrationTest.class)
public class TcpPairingIntegrationTest {

    /** Maximum time to wait for a TCP message or state change. */
    private static final long TIMEOUT_SECONDS = 5L;

    private NetworkIntegrationHarness harness;
    private IntegrationTestConfig config;

    /** Wires the harness. */
    @Before
    public void setUp() {
        config = IntegrationTestConfig.fromEnvironment();
        harness = new NetworkIntegrationHarness(config);
        harness.setUp();
    }

    /** Disconnects and releases resources. */
    @After
    public void tearDown() {
        harness.tearDown();
    }

    /**
     * Verifies that sending a {@code PAIRING_REQUEST} results in a
     * {@code PAIRING_ACK} from the server and the
     * {@link PairingState} reaches {@code PAIRED}.
     *
     * @throws Exception if the latch times out or an error occurs
     */
    @Test
    public void startPairing_receivesPairingAck() throws Exception {
        CountDownLatch successLatch = new CountDownLatch(1);
        CountDownLatch failLatch = new CountDownLatch(1);

        harness.getPairingManager().setPairingCallback(
                new PairingCallback() {
                    @Override
                    public void onTcpPairingSuccess() {
                        successLatch.countDown();
                    }

                    @Override
                    public void onPairingFailed(String reason) {
                        failLatch.countDown();
                    }

                    @Override
                    public void onPairingTimeout() {
                        failLatch.countDown();
                    }
                });

        harness.getPairingManager().startPairing(
                config.getTestDeviceId(),
                config.getServerHost(),
                config.getTcpPort());

        boolean paired = successLatch.await(
                TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals("Pairing should succeed within timeout",
                true, paired);
        assertEquals(PairingState.PAIRED,
                harness.getPairingManager().getCurrentState());
    }

    /**
     * Verifies that the raw {@code PAIRING_ACK} message (0x21) is
     * received on the socket when pairing is initiated.
     *
     * @throws Exception if no message arrives within the timeout
     */
    @Test
    public void startPairing_receivesPairingAckMessage()
            throws Exception {
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

        harness.getPairingManager().startPairing(
                config.getTestDeviceId(),
                config.getServerHost(),
                config.getTcpPort());

        TcpMessage ack = received.poll(
                TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertNotNull("Expected PAIRING_ACK within timeout", ack);
        assertEquals(TcpOpcode.PAIRING_ACK, ack.getOpcode());
    }

    /**
     * Verifies pairing state transitions from
     * {@code NOT_PAIRED} through {@code PAIRING_IN_PROGRESS} to
     * {@code PAIRED}.
     *
     * @throws Exception if the latch times out
     */
    @Test
    public void startPairing_stateTransitionsCorrectly()
            throws Exception {
        assertEquals(PairingState.NOT_PAIRED,
                harness.getPairingManager().getCurrentState());

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

        // Immediately after calling startPairing, state should
        // transition to PAIRING_IN_PROGRESS
        PairingState midState =
                harness.getPairingManager().getCurrentState();
        assertEquals("Should be in progress after startPairing",
                true,
                midState == PairingState.PAIRING_IN_PROGRESS
                        || midState == PairingState.PAIRED);

        latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(PairingState.PAIRED,
                harness.getPairingManager().getCurrentState());
    }
}
