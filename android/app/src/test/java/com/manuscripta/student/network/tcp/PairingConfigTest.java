package com.manuscripta.student.network.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Unit tests for {@link PairingConfig}.
 */
public class PairingConfigTest {

    @Test
    public void defaultConstructor_usesDefaultValues() {
        PairingConfig config = new PairingConfig();

        assertEquals(PairingConfig.DEFAULT_TIMEOUT_MS, config.getTimeoutMs());
        assertEquals(PairingConfig.DEFAULT_RETRY_COUNT, config.getRetryCount());
    }

    @Test
    public void constructor_setsValues() {
        PairingConfig config = new PairingConfig(5000L, 2);

        assertEquals(5000L, config.getTimeoutMs());
        assertEquals(2, config.getRetryCount());
    }

    @Test
    public void constructor_clampsTimeoutToMinimum() {
        PairingConfig config = new PairingConfig(100L, 3);

        assertEquals(PairingConfig.MIN_TIMEOUT_MS, config.getTimeoutMs());
    }

    @Test
    public void constructor_clampsTimeoutToMaximum() {
        PairingConfig config = new PairingConfig(100000L, 3);

        assertEquals(PairingConfig.MAX_TIMEOUT_MS, config.getTimeoutMs());
    }

    @Test
    public void constructor_clampsNegativeRetryToZero() {
        PairingConfig config = new PairingConfig(5000L, -5);

        assertEquals(0, config.getRetryCount());
    }

    @Test
    public void withTimeout_returnsNewConfigWithUpdatedTimeout() {
        PairingConfig original = new PairingConfig(5000L, 2);

        PairingConfig updated = original.withTimeout(8000L);

        assertEquals(8000L, updated.getTimeoutMs());
        assertEquals(2, updated.getRetryCount());
        // Original unchanged
        assertEquals(5000L, original.getTimeoutMs());
    }

    @Test
    public void withRetryCount_returnsNewConfigWithUpdatedRetryCount() {
        PairingConfig original = new PairingConfig(5000L, 2);

        PairingConfig updated = original.withRetryCount(5);

        assertEquals(5000L, updated.getTimeoutMs());
        assertEquals(5, updated.getRetryCount());
        // Original unchanged
        assertEquals(2, original.getRetryCount());
    }

    @Test
    public void toString_containsAllFields() {
        PairingConfig config = new PairingConfig(5000L, 3);

        String str = config.toString();

        assertNotNull(str);
        assertEquals("PairingConfig{timeoutMs=5000, retryCount=3}", str);
    }

    @Test
    public void constants_haveExpectedValues() {
        assertEquals(10000L, PairingConfig.DEFAULT_TIMEOUT_MS);
        assertEquals(1000L, PairingConfig.MIN_TIMEOUT_MS);
        assertEquals(60000L, PairingConfig.MAX_TIMEOUT_MS);
        assertEquals(3, PairingConfig.DEFAULT_RETRY_COUNT);
    }
}
