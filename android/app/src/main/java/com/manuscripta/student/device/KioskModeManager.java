package com.manuscripta.student.device;

import androidx.annotation.Nullable;

/**
 * Manages kiosk mode state to prevent students from accessing other apps.
 *
 * <p>Per {@code Project Specification.md} §8, the app must support kiosk mode.
 * This manager tracks whether kiosk mode is active and notifies listeners
 * of state changes. The actual Android Lock Task Mode integration is
 * handled at the Activity level using the state provided by this manager.</p>
 *
 * <p>Related requirements: SYS1 (e-ink tablet compatibility).</p>
 */
public class KioskModeManager {

    /** Lock for thread-safe operations. */
    private final Object lock = new Object();

    /** Whether kiosk mode is currently enabled. */
    private boolean kioskEnabled;

    /** Listener for kiosk mode state changes. */
    @Nullable
    private KioskModeListener listener;

    /**
     * Listener interface for kiosk mode state changes.
     */
    public interface KioskModeListener {
        /**
         * Called when the kiosk mode state changes.
         *
         * @param enabled true if kiosk mode is now enabled, false if disabled
         */
        void onKioskModeChanged(boolean enabled);
    }

    /**
     * Creates a new KioskModeManager with kiosk mode initially disabled.
     */
    public KioskModeManager() {
        this.kioskEnabled = false;
    }

    /**
     * Enables kiosk mode, preventing the student from leaving the app.
     *
     * @return true if kiosk mode was enabled successfully
     */
    public boolean enableKioskMode() {
        KioskModeListener currentListener;
        synchronized (lock) {
            if (kioskEnabled) {
                return true;
            }
            kioskEnabled = true;
            currentListener = this.listener;
        }
        if (currentListener != null) {
            currentListener.onKioskModeChanged(true);
        }
        return true;
    }

    /**
     * Disables kiosk mode, allowing the student to leave the app.
     *
     * @return true if kiosk mode was disabled successfully
     */
    public boolean disableKioskMode() {
        KioskModeListener currentListener;
        synchronized (lock) {
            if (!kioskEnabled) {
                return true;
            }
            kioskEnabled = false;
            currentListener = this.listener;
        }
        if (currentListener != null) {
            currentListener.onKioskModeChanged(false);
        }
        return true;
    }

    /**
     * Returns whether kiosk mode is currently enabled.
     *
     * @return true if kiosk mode is enabled
     */
    public boolean isKioskModeEnabled() {
        synchronized (lock) {
            return kioskEnabled;
        }
    }

    /**
     * Sets the listener for kiosk mode state changes.
     *
     * @param listener The listener, or null to remove the current listener
     */
    public void setKioskModeListener(@Nullable KioskModeListener listener) {
        synchronized (lock) {
            this.listener = listener;
        }
    }

    /**
     * Removes the current kiosk mode listener.
     */
    public void removeKioskModeListener() {
        synchronized (lock) {
            this.listener = null;
        }
    }
}
