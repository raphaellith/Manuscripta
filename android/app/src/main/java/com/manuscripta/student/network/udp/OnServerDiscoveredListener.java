package com.manuscripta.student.network.udp;

import androidx.annotation.NonNull;

/**
 * Callback interface for server discovery events.
 * Used by PairingManager to receive notifications when a teacher server is discovered.
 */
public interface OnServerDiscoveredListener {

    /**
     * Called when a server is successfully discovered via UDP broadcast.
     *
     * @param message The discovery message containing server details (IP, ports).
     */
    void onServerDiscovered(@NonNull DiscoveryMessage message);
}
