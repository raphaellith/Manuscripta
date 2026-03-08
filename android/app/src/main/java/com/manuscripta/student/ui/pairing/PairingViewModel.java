package com.manuscripta.student.ui.pairing;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.network.dto.DeviceInfoDto;
import com.manuscripta.student.network.tcp.PairingCallback;
import com.manuscripta.student.network.tcp.PairingManager;
import com.manuscripta.student.network.tcp.PairingState;
import com.manuscripta.student.network.udp.DiscoveryMessage;
import com.manuscripta.student.network.udp.DiscoveryState;
import com.manuscripta.student.network.udp.OnServerDiscoveredListener;
import com.manuscripta.student.network.udp.UdpDiscoveryManager;

import java.util.UUID;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel orchestrating the full pairing flow:
 * UDP discovery → TCP handshake → HTTP registration.
 *
 * <p>Per Pairing Process §2, all three phases must succeed for pairing
 * to be considered complete. If any phase fails, the entire process
 * can be retried.</p>
 */
@HiltViewModel
public class PairingViewModel extends ViewModel {

    /** Tag for logging. */
    private static final String TAG = "PairingViewModel";

    /** The UDP discovery manager. */
    private final UdpDiscoveryManager discoveryManager;

    /** The TCP pairing manager. */
    private final PairingManager pairingManager;

    /** The Retrofit API service for HTTP registration. */
    private final ApiService apiService;

    /** Overall pairing phase exposed to the UI. */
    private final MutableLiveData<PairingPhase> pairingPhase =
            new MutableLiveData<>(PairingPhase.IDLE);

    /** Human-readable status message for display. */
    private final MutableLiveData<String> statusMessage =
            new MutableLiveData<>("Tap 'Pair' to begin");

    /** Error message, non-null only when in ERROR phase. */
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    /** Whether pairing completed successfully, triggers navigation. */
    private final MutableLiveData<Boolean> pairingComplete = new MutableLiveData<>(false);

    /** Merged LiveData for observing discovery state changes. */
    private final MediatorLiveData<DiscoveryState> discoveryStateMerger = new MediatorLiveData<>();

    /** The device ID generated for this pairing attempt. */
    @Nullable
    private String deviceId;

    /** The user-provided device name. */
    @Nullable
    private String deviceName;

    /** The discovered server info, retained for retry. */
    @Nullable
    private DiscoveryMessage discoveredServer;

    /** Listener for UDP discovery events. */
    private final OnServerDiscoveredListener discoveryListener = this::onServerDiscovered;

    /** Callback for TCP pairing events. */
    private final PairingCallback pairingCallback = new PairingCallback() {
        @Override
        public void onTcpPairingSuccess() {
            handleTcpPairingSuccess();
        }

        @Override
        public void onPairingFailed(@NonNull String reason) {
            handlePairingError("TCP pairing failed: " + reason);
        }

        @Override
        public void onPairingTimeout() {
            handlePairingError("TCP pairing timed out");
        }
    };

    /**
     * Creates a new PairingViewModel with Hilt-injected dependencies.
     *
     * @param discoveryManager The UDP discovery manager
     * @param pairingManager   The TCP pairing manager
     * @param apiService       The Retrofit API service
     */
    @Inject
    public PairingViewModel(@NonNull UdpDiscoveryManager discoveryManager,
                            @NonNull PairingManager pairingManager,
                            @NonNull ApiService apiService) {
        this.discoveryManager = discoveryManager;
        this.pairingManager = pairingManager;
        this.apiService = apiService;

        pairingManager.setPairingCallback(pairingCallback);
        discoveryManager.addListener(discoveryListener);

        // Mirror discovery state LiveData into our merger so the Activity can observe it
        discoveryStateMerger.addSource(discoveryManager.getDiscoveryState(),
                discoveryStateMerger::setValue);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        discoveryManager.removeListener(discoveryListener);
        discoveryManager.stopDiscovery();
        pairingManager.setPairingCallback(null);
    }

    // ========== Public API ==========

    /**
     * Returns the current pairing phase for UI observation.
     *
     * @return LiveData containing the current pairing phase
     */
    @NonNull
    public LiveData<PairingPhase> getPairingPhase() {
        return pairingPhase;
    }

    /**
     * Returns the human-readable status message.
     *
     * @return LiveData containing the status message
     */
    @NonNull
    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    /**
     * Returns the error message, non-null only when in ERROR phase.
     *
     * @return LiveData containing the error message
     */
    @NonNull
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns whether pairing has completed successfully.
     *
     * @return LiveData that emits true when pairing is complete
     */
    @NonNull
    public LiveData<Boolean> getPairingComplete() {
        return pairingComplete;
    }

    /**
     * Returns the discovery state for observation.
     *
     * @return LiveData containing the UDP discovery state
     */
    @NonNull
    public LiveData<DiscoveryState> getDiscoveryState() {
        return discoveryStateMerger;
    }

    /**
     * Returns the TCP pairing state for observation.
     *
     * @return LiveData containing the TCP pairing state
     */
    @NonNull
    public LiveData<PairingState> getTcpPairingState() {
        return pairingManager.getPairingState();
    }

    /**
     * Starts the full pairing flow. Generates a new device ID and
     * begins UDP discovery.
     *
     * @param name The user-provided device name for display in the teacher app
     */
    public void startPairing(@NonNull String name) {
        if (isInProgress()) {
            Log.w(TAG, "Pairing already in progress");
            return;
        }

        this.deviceName = name;
        this.deviceId = UUID.randomUUID().toString();
        this.discoveredServer = null;

        Log.i(TAG, "Starting pairing with deviceId=" + deviceId + ", name=" + name);

        pairingPhase.setValue(PairingPhase.DISCOVERING);
        statusMessage.setValue("Searching for teacher server\u2026");
        errorMessage.setValue(null);

        discoveryManager.startDiscovery();
    }

    /**
     * Retries the pairing flow from the beginning.
     */
    public void retry() {
        if (deviceName != null) {
            startPairing(deviceName);
        }
    }

    /**
     * Cancels any in-progress pairing and returns to idle state.
     */
    public void cancelPairing() {
        discoveryManager.stopDiscovery();
        pairingManager.cancelPairing();
        pairingPhase.setValue(PairingPhase.IDLE);
        statusMessage.setValue("Pairing cancelled");
        errorMessage.setValue(null);
    }

    /**
     * Returns whether any pairing phase is currently in progress.
     *
     * @return true if pairing is in progress
     */
    public boolean isInProgress() {
        PairingPhase phase = pairingPhase.getValue();
        return phase == PairingPhase.DISCOVERING
                || phase == PairingPhase.TCP_PAIRING
                || phase == PairingPhase.HTTP_REGISTERING;
    }

    // ========== Internal handlers ==========

    /**
     * Called when a server is discovered via UDP.
     *
     * @param message The discovery message containing server connection details
     */
    private void onServerDiscovered(@NonNull DiscoveryMessage message) {
        Log.i(TAG, "Server discovered: " + message.getIpAddress()
                + " HTTP:" + message.getHttpPort() + " TCP:" + message.getTcpPort());

        this.discoveredServer = message;
        discoveryManager.stopDiscovery();

        // Store the HTTP port in PairingManager for BaseUrlInterceptor
        pairingManager.setServerHttpPort(message.getHttpPort());

        pairingPhase.postValue(PairingPhase.TCP_PAIRING);
        statusMessage.postValue("Connecting to server\u2026");

        // Start TCP pairing handshake
        pairingManager.startPairing(deviceId, message.getIpAddress(), message.getTcpPort());
    }

    /**
     * Called when TCP pairing succeeds. Proceeds to HTTP registration.
     */
    private void handleTcpPairingSuccess() {
        Log.i(TAG, "TCP pairing successful, proceeding to HTTP registration");

        pairingPhase.postValue(PairingPhase.HTTP_REGISTERING);
        statusMessage.postValue("Registering device\u2026");

        DeviceInfoDto deviceInfo = new DeviceInfoDto(deviceId, deviceName);
        apiService.registerDevice(deviceInfo).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call,
                                   @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    handleHttpRegistrationSuccess();
                } else {
                    handlePairingError("HTTP registration failed: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                handlePairingError("HTTP registration failed: " + t.getMessage());
            }
        });
    }

    /**
     * Called when HTTP registration succeeds. Pairing is now complete.
     */
    private void handleHttpRegistrationSuccess() {
        Log.i(TAG, "HTTP registration successful — pairing complete");

        pairingPhase.postValue(PairingPhase.PAIRED);
        statusMessage.postValue("Paired successfully");
        pairingComplete.postValue(true);
    }

    /**
     * Handles a pairing error at any stage.
     *
     * @param error The error message to display
     */
    private void handlePairingError(@NonNull String error) {
        Log.e(TAG, "Pairing error: " + error);

        pairingPhase.postValue(PairingPhase.ERROR);
        statusMessage.postValue("Pairing failed");
        errorMessage.postValue(error);
    }
}
