package com.manuscripta.student.network.tcp;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.manuscripta.student.network.tcp.message.FeedbackAckMessage;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Unit tests for {@link AckRetrySender}.
 */
public class AckRetrySenderTest {

    private TcpSocketManager mockSocketManager;
    private AckRetrySender sender;

    @Before
    public void setUp() {
        mockSocketManager = mock(TcpSocketManager.class);
        // Override sleep to a no-op so tests run instantly
        sender = new AckRetrySender(mockSocketManager) {
            @Override
            protected boolean sleep(long millis) {
                return true;
            }
        };
    }

    @Test
    public void testConstants() {
        assertEquals(3, AckRetrySender.MAX_ATTEMPTS);
        assertEquals(500L, AckRetrySender.RETRY_DELAY_MS);
    }

    @Test
    public void testSend_succeedsOnFirstAttempt() throws Exception {
        FeedbackAckMessage message = new FeedbackAckMessage("device-1", "fb-1");

        sender.send(message, "TestTag");

        verify(mockSocketManager, times(1)).send(message);
    }

    @Test
    public void testSend_succeedsOnSecondAttempt_stopsRetrying() throws Exception {
        FeedbackAckMessage message = new FeedbackAckMessage("device-1", "fb-1");
        doThrow(new IOException("fail"))
                .doNothing()
                .when(mockSocketManager).send(message);

        sender.send(message, "TestTag");

        verify(mockSocketManager, times(2)).send(message);
    }

    @Test
    public void testSend_allAttemptsFail_retriesMaxTimes() throws Exception {
        FeedbackAckMessage message = new FeedbackAckMessage("device-1", "fb-1");
        doThrow(new IOException("fail"))
                .when(mockSocketManager).send(message);

        sender.send(message, "TestTag");

        verify(mockSocketManager, times(AckRetrySender.MAX_ATTEMPTS)).send(message);
    }

    @Test
    public void testSend_tcpProtocolException_retries() throws Exception {
        FeedbackAckMessage message = new FeedbackAckMessage("device-1", "fb-1");
        doThrow(new TcpProtocolException("encoding error"))
                .doNothing()
                .when(mockSocketManager).send(message);

        sender.send(message, "TestTag");

        verify(mockSocketManager, times(2)).send(message);
    }

    @Test
    public void testSend_interruptDuringSleep_abortsRetries() throws Exception {
        FeedbackAckMessage message = new FeedbackAckMessage("device-1", "fb-1");
        doThrow(new IOException("fail"))
                .when(mockSocketManager).send(message);

        // Override sleep to simulate interrupt
        AckRetrySender interruptedSender = new AckRetrySender(mockSocketManager) {
            @Override
            protected boolean sleep(long millis) {
                return false; // simulate interrupted
            }
        };

        interruptedSender.send(message, "TestTag");

        // Should have attempted once, then been interrupted during sleep
        verify(mockSocketManager, times(1)).send(message);
    }
}
