package com.manuscripta.student.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link MulticastLockManager}.
 */
public class MulticastLockManagerTest {

    private MulticastLockManager multicastLockManager;
    private Context mockContext;

    @Before
    public void setUp() {
        multicastLockManager = new MulticastLockManager();
        mockContext = mock(Context.class);
        when(mockContext.checkPermission(anyString(), anyInt(), anyInt()))
                .thenReturn(PackageManager.PERMISSION_DENIED);
    }

    @Test
    public void testIsHeld_initiallyFalse() {
        assertFalse(multicastLockManager.isHeld());
    }

    @Test
    public void testRelease_whenNoLockHeld_doesNotThrow() {
        multicastLockManager.release();

        assertFalse(multicastLockManager.isHeld());
    }

    @Test
    public void testHasWifiPermission_whenPermissionDenied_returnsFalse() {
        when(mockContext.checkPermission(
                eq(Manifest.permission.ACCESS_WIFI_STATE), anyInt(), anyInt()))
                .thenReturn(PackageManager.PERMISSION_DENIED);

        assertFalse(multicastLockManager.hasWifiPermission(mockContext));
    }

    @Test
    public void testHasWifiPermission_whenPermissionGranted_returnsTrue() {
        when(mockContext.checkPermission(
                eq(Manifest.permission.ACCESS_WIFI_STATE), anyInt(), anyInt()))
                .thenReturn(PackageManager.PERMISSION_GRANTED);

        assertTrue(multicastLockManager.hasWifiPermission(mockContext));
    }

    @Test
    public void testHasMulticastPermission_whenPermissionDenied_returnsFalse() {
        when(mockContext.checkPermission(
                eq(Manifest.permission.CHANGE_WIFI_MULTICAST_STATE), anyInt(), anyInt()))
                .thenReturn(PackageManager.PERMISSION_DENIED);

        assertFalse(multicastLockManager.hasMulticastPermission(mockContext));
    }

    @Test
    public void testHasMulticastPermission_whenPermissionGranted_returnsTrue() {
        when(mockContext.checkPermission(
                eq(Manifest.permission.CHANGE_WIFI_MULTICAST_STATE), anyInt(), anyInt()))
                .thenReturn(PackageManager.PERMISSION_GRANTED);

        assertTrue(multicastLockManager.hasMulticastPermission(mockContext));
    }

    @Test
    public void testHasRequiredPermissions_whenNoPermissions_returnsFalse() {
        assertFalse(multicastLockManager.hasRequiredPermissions(mockContext));
    }

    @Test
    public void testHasRequiredPermissions_whenOnlyWifiPermission_returnsFalse() {
        when(mockContext.checkPermission(
                eq(Manifest.permission.ACCESS_WIFI_STATE), anyInt(), anyInt()))
                .thenReturn(PackageManager.PERMISSION_GRANTED);

        assertFalse(multicastLockManager.hasRequiredPermissions(mockContext));
    }

    @Test
    public void testHasRequiredPermissions_whenOnlyMulticastPermission_returnsFalse() {
        when(mockContext.checkPermission(
                eq(Manifest.permission.CHANGE_WIFI_MULTICAST_STATE), anyInt(), anyInt()))
                .thenReturn(PackageManager.PERMISSION_GRANTED);

        assertFalse(multicastLockManager.hasRequiredPermissions(mockContext));
    }

    @Test
    public void testHasRequiredPermissions_whenBothGranted_returnsTrue() {
        when(mockContext.checkPermission(anyString(), anyInt(), anyInt()))
                .thenReturn(PackageManager.PERMISSION_GRANTED);

        assertTrue(multicastLockManager.hasRequiredPermissions(mockContext));
    }

    @Test
    public void testAcquire_whenPermissionsDenied_returnsFalse() {
        boolean result = multicastLockManager.acquire(mockContext);

        assertFalse(result);
    }
}
