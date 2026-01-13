package com.manuscripta.student.network.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link HeartbeatConfig}.
 */
public class HeartbeatConfigTest {

    // ========== Constructor tests ==========

    @Test
    public void defaultConstructor_usesDefaultInterval() {
        HeartbeatConfig config = new HeartbeatConfig();

        assertEquals(HeartbeatConfig.DEFAULT_INTERVAL_MS, config.getIntervalMs());
    }

    @Test
    public void defaultConstructor_isEnabled() {
        HeartbeatConfig config = new HeartbeatConfig();

        assertTrue(config.isEnabled());
    }

    @Test
    public void constructor_setsIntervalAndEnabled() {
        HeartbeatConfig config = new HeartbeatConfig(3000L, false);

        assertEquals(3000L, config.getIntervalMs());
        assertFalse(config.isEnabled());
    }

    // ========== Interval clamping tests ==========

    @Test
    public void constructor_clampsIntervalToMinimum() {
        HeartbeatConfig config = new HeartbeatConfig(100L, true);

        assertEquals(HeartbeatConfig.MIN_INTERVAL_MS, config.getIntervalMs());
    }

    @Test
    public void constructor_clampsIntervalToMaximum() {
        HeartbeatConfig config = new HeartbeatConfig(120000L, true);

        assertEquals(HeartbeatConfig.MAX_INTERVAL_MS, config.getIntervalMs());
    }

    @Test
    public void constructor_acceptsValidInterval() {
        HeartbeatConfig config = new HeartbeatConfig(10000L, true);

        assertEquals(10000L, config.getIntervalMs());
    }

    @Test
    public void constructor_acceptsMinimumInterval() {
        HeartbeatConfig config = new HeartbeatConfig(HeartbeatConfig.MIN_INTERVAL_MS, true);

        assertEquals(HeartbeatConfig.MIN_INTERVAL_MS, config.getIntervalMs());
    }

    @Test
    public void constructor_acceptsMaximumInterval() {
        HeartbeatConfig config = new HeartbeatConfig(HeartbeatConfig.MAX_INTERVAL_MS, true);

        assertEquals(HeartbeatConfig.MAX_INTERVAL_MS, config.getIntervalMs());
    }

    // ========== withEnabled tests ==========

    @Test
    public void withEnabled_returnsNewInstance() {
        HeartbeatConfig original = new HeartbeatConfig(5000L, false);
        HeartbeatConfig enabled = original.withEnabled();

        assertNotSame(original, enabled);
    }

    @Test
    public void withEnabled_setsEnabledToTrue() {
        HeartbeatConfig original = new HeartbeatConfig(5000L, false);
        HeartbeatConfig enabled = original.withEnabled();

        assertTrue(enabled.isEnabled());
    }

    @Test
    public void withEnabled_preservesInterval() {
        HeartbeatConfig original = new HeartbeatConfig(7000L, false);
        HeartbeatConfig enabled = original.withEnabled();

        assertEquals(7000L, enabled.getIntervalMs());
    }

    // ========== withDisabled tests ==========

    @Test
    public void withDisabled_returnsNewInstance() {
        HeartbeatConfig original = new HeartbeatConfig(5000L, true);
        HeartbeatConfig disabled = original.withDisabled();

        assertNotSame(original, disabled);
    }

    @Test
    public void withDisabled_setsEnabledToFalse() {
        HeartbeatConfig original = new HeartbeatConfig(5000L, true);
        HeartbeatConfig disabled = original.withDisabled();

        assertFalse(disabled.isEnabled());
    }

    @Test
    public void withDisabled_preservesInterval() {
        HeartbeatConfig original = new HeartbeatConfig(8000L, true);
        HeartbeatConfig disabled = original.withDisabled();

        assertEquals(8000L, disabled.getIntervalMs());
    }

    // ========== withInterval tests ==========

    @Test
    public void withInterval_returnsNewInstance() {
        HeartbeatConfig original = new HeartbeatConfig(5000L, true);
        HeartbeatConfig updated = original.withInterval(10000L);

        assertNotSame(original, updated);
    }

    @Test
    public void withInterval_setsNewInterval() {
        HeartbeatConfig original = new HeartbeatConfig(5000L, true);
        HeartbeatConfig updated = original.withInterval(10000L);

        assertEquals(10000L, updated.getIntervalMs());
    }

    @Test
    public void withInterval_preservesEnabled() {
        HeartbeatConfig original = new HeartbeatConfig(5000L, false);
        HeartbeatConfig updated = original.withInterval(10000L);

        assertFalse(updated.isEnabled());
    }

    @Test
    public void withInterval_clampsToMinimum() {
        HeartbeatConfig original = new HeartbeatConfig(5000L, true);
        HeartbeatConfig updated = original.withInterval(100L);

        assertEquals(HeartbeatConfig.MIN_INTERVAL_MS, updated.getIntervalMs());
    }

    @Test
    public void withInterval_clampsToMaximum() {
        HeartbeatConfig original = new HeartbeatConfig(5000L, true);
        HeartbeatConfig updated = original.withInterval(100000L);

        assertEquals(HeartbeatConfig.MAX_INTERVAL_MS, updated.getIntervalMs());
    }

    // ========== toString tests ==========

    @Test
    public void toString_returnsNonNull() {
        HeartbeatConfig config = new HeartbeatConfig();

        assertNotNull(config.toString());
    }

    @Test
    public void toString_containsInterval() {
        HeartbeatConfig config = new HeartbeatConfig(7500L, true);

        assertTrue(config.toString().contains("7500"));
    }

    @Test
    public void toString_containsEnabled() {
        HeartbeatConfig config = new HeartbeatConfig(5000L, true);

        assertTrue(config.toString().contains("true"));
    }

    // ========== Constants tests ==========

    @Test
    public void defaultIntervalMs_isFiveSeconds() {
        assertEquals(5000L, HeartbeatConfig.DEFAULT_INTERVAL_MS);
    }

    @Test
    public void minIntervalMs_isOneSecond() {
        assertEquals(1000L, HeartbeatConfig.MIN_INTERVAL_MS);
    }

    @Test
    public void maxIntervalMs_isSixtySeconds() {
        assertEquals(60000L, HeartbeatConfig.MAX_INTERVAL_MS);
    }
}
