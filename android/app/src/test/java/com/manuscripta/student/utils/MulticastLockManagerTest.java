package com.manuscripta.student.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link MulticastLockManager}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class MulticastLockManagerTest {

    private MulticastLockManager lockManager;
    private Application context;

    @Before
    public void setUp() {
        lockManager = new MulticastLockManager();
        context = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void hasWifiPermission_returnsTrueWhenGranted() {
        // Given
        Shadows.shadowOf(context).grantPermissions(Manifest.permission.ACCESS_WIFI_STATE);

        // When
        boolean result = lockManager.hasWifiPermission(context);

        // Then
        assertTrue(result);
    }

    @Test
    public void hasWifiPermission_returnsFalseWhenDenied() {
        // Given
        Shadows.shadowOf(context).denyPermissions(Manifest.permission.ACCESS_WIFI_STATE);

        // When
        boolean result = lockManager.hasWifiPermission(context);

        // Then
        assertFalse(result);
    }

    @Test
    public void hasMulticastPermission_returnsTrueWhenGranted() {
        // Given
        Shadows.shadowOf(context).grantPermissions(Manifest.permission.CHANGE_WIFI_MULTICAST_STATE);

        // When
        boolean result = lockManager.hasMulticastPermission(context);

        // Then
        assertTrue(result);
    }

    @Test
    public void hasMulticastPermission_returnsFalseWhenDenied() {
        // Given
        Shadows.shadowOf(context).denyPermissions(Manifest.permission.CHANGE_WIFI_MULTICAST_STATE);

        // When
        boolean result = lockManager.hasMulticastPermission(context);

        // Then
        assertFalse(result);
    }

    @Test
    public void hasRequiredPermissions_returnsTrueWhenAllGranted() {
        // Given
        Shadows.shadowOf(context).grantPermissions(
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_MULTICAST_STATE
        );

        // When
        boolean result = lockManager.hasRequiredPermissions(context);

        // Then
        assertTrue(result);
    }

    @Test
    public void hasRequiredPermissions_returnsFalseWhenWifiDenied() {
        // Given
        Shadows.shadowOf(context).denyPermissions(Manifest.permission.ACCESS_WIFI_STATE);
        Shadows.shadowOf(context).grantPermissions(Manifest.permission.CHANGE_WIFI_MULTICAST_STATE);

        // When
        boolean result = lockManager.hasRequiredPermissions(context);

        // Then
        assertFalse(result);
    }

    @Test
    public void hasRequiredPermissions_returnsFalseWhenMulticastDenied() {
        // Given
        Shadows.shadowOf(context).grantPermissions(Manifest.permission.ACCESS_WIFI_STATE);
        Shadows.shadowOf(context).denyPermissions(Manifest.permission.CHANGE_WIFI_MULTICAST_STATE);

        // When
        boolean result = lockManager.hasRequiredPermissions(context);

        // Then
        assertFalse(result);
    }

    @Test
    public void isHeld_returnsFalseInitially() {
        // When
        boolean result = lockManager.isHeld();

        // Then
        assertFalse(result);
    }

    @Test
    public void release_doesNothingWhenNoLockHeld() {
        // When - should not throw
        lockManager.release();

        // Then
        assertFalse(lockManager.isHeld());
    }

    @Test
    public void acquire_returnsTrue() {
        // When
        boolean result = lockManager.acquire(context);

        // Then
        assertTrue(result);

        // Cleanup
        lockManager.release();
    }

    @Test
    public void isHeld_returnsTrueAfterAcquire() {
        // Given
        lockManager.acquire(context);

        // When
        boolean result = lockManager.isHeld();

        // Then
        assertTrue(result);

        // Cleanup
        lockManager.release();
    }

    @Test
    public void isHeld_returnsFalseAfterRelease() {
        // Given
        lockManager.acquire(context);
        assertTrue(lockManager.isHeld());

        // When
        lockManager.release();

        // Then
        assertFalse(lockManager.isHeld());
    }

    @Test
    public void acquire_isIdempotent() {
        // Given
        lockManager.acquire(context);
        assertTrue(lockManager.isHeld());

        // When - call acquire again
        boolean result = lockManager.acquire(context);

        // Then - still held, no exception
        assertTrue(result);
        assertTrue(lockManager.isHeld());

        // Cleanup
        lockManager.release();
    }
}
