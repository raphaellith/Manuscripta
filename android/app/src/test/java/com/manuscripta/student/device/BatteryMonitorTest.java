package com.manuscripta.student.device;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.manuscripta.student.data.repository.DeviceStatusRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link BatteryMonitor}.
 */
public class BatteryMonitorTest {

    @Mock
    private DeviceStatusRepository mockRepository;

    private BatteryMonitor batteryMonitor;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        batteryMonitor = new BatteryMonitor(mockRepository);
    }

    // ========== Constructor tests ==========

    @Test
    public void testConstructor_createsInstance() {
        assertNotNull(batteryMonitor);
    }

    @Test
    public void testConstructor_nullRepository_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new BatteryMonitor(null));
    }

    // ========== onBatteryLevelChanged tests ==========

    @Test
    public void testOnBatteryLevelChanged_validLevel_updatesRepository() {
        batteryMonitor.onBatteryLevelChanged(75);
        verify(mockRepository).updateBatteryLevel(75);
    }

    @Test
    public void testOnBatteryLevelChanged_zero_updatesRepository() {
        batteryMonitor.onBatteryLevelChanged(0);
        verify(mockRepository).updateBatteryLevel(0);
    }

    @Test
    public void testOnBatteryLevelChanged_hundred_updatesRepository() {
        batteryMonitor.onBatteryLevelChanged(100);
        verify(mockRepository).updateBatteryLevel(100);
    }

    @Test
    public void testOnBatteryLevelChanged_negativeLevel_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> batteryMonitor.onBatteryLevelChanged(-1));
    }

    @Test
    public void testOnBatteryLevelChanged_overHundred_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> batteryMonitor.onBatteryLevelChanged(101));
    }

    @Test
    public void testOnBatteryLevelChanged_boundary_updatesRepository() {
        batteryMonitor.onBatteryLevelChanged(1);
        verify(mockRepository).updateBatteryLevel(1);
    }

    @Test
    public void testOnBatteryLevelChanged_ninetynine_updatesRepository() {
        batteryMonitor.onBatteryLevelChanged(99);
        verify(mockRepository).updateBatteryLevel(99);
    }

    // ========== isLowBattery(int) tests ==========

    @Test
    public void testIsLowBattery_belowThreshold_returnsTrue() {
        assertTrue(batteryMonitor.isLowBattery(10));
    }

    @Test
    public void testIsLowBattery_atThreshold_returnsFalse() {
        assertFalse(batteryMonitor.isLowBattery(
                BatteryMonitor.LOW_BATTERY_THRESHOLD));
    }

    @Test
    public void testIsLowBattery_aboveThreshold_returnsFalse() {
        assertFalse(batteryMonitor.isLowBattery(50));
    }

    @Test
    public void testIsLowBattery_zero_returnsTrue() {
        assertTrue(batteryMonitor.isLowBattery(0));
    }

    @Test
    public void testIsLowBattery_justBelowThreshold_returnsTrue() {
        assertTrue(batteryMonitor.isLowBattery(
                BatteryMonitor.LOW_BATTERY_THRESHOLD - 1));
    }

    @Test
    public void testIsLowBattery_negativeLevel_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> batteryMonitor.isLowBattery(-1));
    }

    @Test
    public void testIsLowBattery_overHundred_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> batteryMonitor.isLowBattery(101));
    }

    // ========== isLowBattery() no-arg tests ==========

    @Test
    public void testIsLowBattery_noArg_lowLevel_returnsTrue() {
        when(mockRepository.getCurrentBatteryLevel()).thenReturn(5);
        assertTrue(batteryMonitor.isLowBattery());
    }

    @Test
    public void testIsLowBattery_noArg_normalLevel_returnsFalse() {
        when(mockRepository.getCurrentBatteryLevel()).thenReturn(80);
        assertFalse(batteryMonitor.isLowBattery());
    }

    @Test
    public void testIsLowBattery_noArg_atThreshold_returnsFalse() {
        when(mockRepository.getCurrentBatteryLevel()).thenReturn(
                BatteryMonitor.LOW_BATTERY_THRESHOLD);
        assertFalse(batteryMonitor.isLowBattery());
    }

    // ========== getCurrentBatteryLevel tests ==========

    @Test
    public void testGetCurrentBatteryLevel_delegatesToRepository() {
        when(mockRepository.getCurrentBatteryLevel()).thenReturn(42);
        assertEquals(42, batteryMonitor.getCurrentBatteryLevel());
    }

    @Test
    public void testGetCurrentBatteryLevel_fullBattery() {
        when(mockRepository.getCurrentBatteryLevel()).thenReturn(100);
        assertEquals(100, batteryMonitor.getCurrentBatteryLevel());
    }

    // ========== Constants tests ==========

    @Test
    public void testConstants_thresholdIsPositive() {
        assertTrue(BatteryMonitor.LOW_BATTERY_THRESHOLD > 0);
    }

    @Test
    public void testConstants_minLevelIsZero() {
        assertEquals(0, BatteryMonitor.MIN_BATTERY_LEVEL);
    }

    @Test
    public void testConstants_maxLevelIsHundred() {
        assertEquals(100, BatteryMonitor.MAX_BATTERY_LEVEL);
    }

    @Test
    public void testConstants_thresholdIsFifteen() {
        assertEquals(15, BatteryMonitor.LOW_BATTERY_THRESHOLD);
    }
}
