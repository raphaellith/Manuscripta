package com.manuscripta.student.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * Manages the acquisition and release of multicast locks for UDP broadcast reception.
 *
 * <p>Android devices filter out multicast/broadcast packets by default to save power.
 * A multicast lock must be held to receive UDP broadcast packets. This utility
 * encapsulates the lock management and permission checking.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * MulticastLockManager lockManager = new MulticastLockManager();
 * if (lockManager.hasRequiredPermissions(context)) {
 *     lockManager.acquire(context);
 *     // ... perform UDP discovery ...
 *     lockManager.release();
 * }
 * </pre>
 *
 * @author William Stephen
 */
public class MulticastLockManager {

    /**
     * Tag for the multicast lock.
     */
    private static final String LOCK_TAG = "ManuscriptaMulticastLock";

    /**
     * The multicast lock, if acquired.
     */
    @Nullable
    private WifiManager.MulticastLock multicastLock;

    /**
     * Checks if the ACCESS_WIFI_STATE permission is granted.
     *
     * @param context The context to check permissions with.
     * @return true if the permission is granted, false otherwise.
     */
    public boolean hasWifiPermission(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Checks if the CHANGE_WIFI_MULTICAST_STATE permission is granted.
     *
     * @param context The context to check permissions with.
     * @return true if the permission is granted, false otherwise.
     */
    public boolean hasMulticastPermission(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.CHANGE_WIFI_MULTICAST_STATE)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Checks if all required permissions for multicast reception are granted.
     *
     * @param context The context to check permissions with.
     * @return true if all required permissions are granted, false otherwise.
     */
    public boolean hasRequiredPermissions(@NonNull Context context) {
        return hasWifiPermission(context) && hasMulticastPermission(context);
    }

    /**
     * Acquires a multicast lock to enable UDP broadcast reception.
     *
     * <p>This method is idempotent and thread-safe; calling it when a lock is already held
     * does nothing.</p>
     *
     * @param context The context to acquire the lock with.
     * @return true if the lock was acquired successfully, false otherwise.
     */
    public synchronized boolean acquire(@NonNull Context context) {
        if (multicastLock != null && multicastLock.isHeld()) {
            return true;
        }

        // Ensure required permissions are granted before attempting to acquire the lock
        if (!hasRequiredPermissions(context)) {
            return false;
        }

        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return false;
        }

        try {
            multicastLock = wifiManager.createMulticastLock(LOCK_TAG);
            multicastLock.setReferenceCounted(true);
            multicastLock.acquire();
            return true;
        } catch (SecurityException e) {
            // Permissions may have been revoked; ensure no stale lock reference
            multicastLock = null;
            return false;
        }
    }

    /**
     * Releases the multicast lock if held.
     *
     * <p>This method is idempotent and thread-safe; calling it when no lock is held
     * does nothing.</p>
     */
    public synchronized void release() {
        if (multicastLock != null && multicastLock.isHeld()) {
            multicastLock.release();
        }
        multicastLock = null;
    }

    /**
     * Checks if a multicast lock is currently held.
     *
     * <p>This method is thread-safe.</p>
     *
     * @return true if a lock is held, false otherwise.
     */
    public synchronized boolean isHeld() {
        return multicastLock != null && multicastLock.isHeld();
    }
}
