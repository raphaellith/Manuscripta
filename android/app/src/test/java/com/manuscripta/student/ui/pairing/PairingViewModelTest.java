package com.manuscripta.student.ui.pairing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.manuscripta.student.data.local.ManuscriptaDatabase;
import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.network.dto.DeviceInfoDto;
import com.manuscripta.student.network.tcp.PairingCallback;
import com.manuscripta.student.network.tcp.PairingManager;
import com.manuscripta.student.network.tcp.PairingState;
import com.manuscripta.student.network.udp.DiscoveryMessage;
import com.manuscripta.student.network.udp.DiscoveryState;
import com.manuscripta.student.network.udp.OnServerDiscoveredListener;
import com.manuscripta.student.network.udp.UdpDiscoveryManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Unit tests for {@link PairingViewModel}.
 */
public class PairingViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private UdpDiscoveryManager mockDiscoveryManager;

    @Mock
    private PairingManager mockPairingManager;

    @Mock
    private ApiService mockApiService;

    @Mock
    private ManuscriptaDatabase mockDatabase;

    @Mock
    private Call<Void> mockCall;

    private PairingViewModel viewModel;

    /** Captured callbacks for driving the pairing flow in tests. */
    private PairingCallback capturedPairingCallback;
    private OnServerDiscoveredListener capturedDiscoveryListener;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        MutableLiveData<DiscoveryState> discoveryState =
                new MutableLiveData<>(DiscoveryState.IDLE);
        when(mockDiscoveryManager.getDiscoveryState()).thenReturn(discoveryState);

        MutableLiveData<PairingState> pairingState =
                new MutableLiveData<>(PairingState.NOT_PAIRED);
        when(mockPairingManager.getPairingState()).thenReturn(pairingState);

        viewModel = new PairingViewModel(mockDiscoveryManager, mockPairingManager,
                mockApiService, mockDatabase);

        // Capture the callbacks set by the ViewModel
        ArgumentCaptor<PairingCallback> callbackCaptor =
                ArgumentCaptor.forClass(PairingCallback.class);
        verify(mockPairingManager).setPairingCallback(callbackCaptor.capture());
        capturedPairingCallback = callbackCaptor.getValue();

        ArgumentCaptor<OnServerDiscoveredListener> listenerCaptor =
                ArgumentCaptor.forClass(OnServerDiscoveredListener.class);
        verify(mockDiscoveryManager).addListener(listenerCaptor.capture());
        capturedDiscoveryListener = listenerCaptor.getValue();
    }

    // ========== Initial state ==========

    @Test
    public void initialState_isIdle() {
        assertEquals(PairingPhase.IDLE, viewModel.getPairingPhase().getValue());
    }

    @Test
    public void initialState_pairingNotComplete() {
        assertFalse(Boolean.TRUE.equals(viewModel.getPairingComplete().getValue()));
    }

    @Test
    public void initialState_notInProgress() {
        assertFalse(viewModel.isInProgress());
    }

    @Test
    public void initialState_noError() {
        assertNull(viewModel.getErrorMessage().getValue());
    }

    // ========== startPairing ==========

    @Test
    public void startPairing_setsPhaseToDiscovering() {
        viewModel.startPairing("Test Student");

        assertEquals(PairingPhase.DISCOVERING, viewModel.getPairingPhase().getValue());
    }

    @Test
    public void startPairing_startsUdpDiscovery() {
        viewModel.startPairing("Test Student");

        verify(mockDiscoveryManager).startDiscovery();
    }

    @Test
    public void startPairing_isInProgress() {
        viewModel.startPairing("Test Student");

        assertTrue(viewModel.isInProgress());
    }

    @Test
    public void startPairing_clearsError() {
        // Force an error first
        viewModel.startPairing("Test Student");
        capturedPairingCallback.onPairingFailed("test error");
        assertNotNull(viewModel.getErrorMessage().getValue());

        // Retry should clear error
        viewModel.startPairing("Test Student");
        assertNull(viewModel.getErrorMessage().getValue());
    }

    // ========== Discovery → TCP pairing ==========

    @Test
    public void onServerDiscovered_setsPhaseToTcpPairing() {
        viewModel.startPairing("Test Student");
        DiscoveryMessage server = new DiscoveryMessage("192.168.1.100", 5911, 5912);

        capturedDiscoveryListener.onServerDiscovered(server);

        assertEquals(PairingPhase.TCP_PAIRING, viewModel.getPairingPhase().getValue());
    }

    @Test
    public void onServerDiscovered_stopsDiscovery() {
        viewModel.startPairing("Test Student");
        DiscoveryMessage server = new DiscoveryMessage("192.168.1.100", 5911, 5912);

        capturedDiscoveryListener.onServerDiscovered(server);

        verify(mockDiscoveryManager).stopDiscovery();
    }

    @Test
    public void onServerDiscovered_storesHttpPort() {
        viewModel.startPairing("Test Student");
        DiscoveryMessage server = new DiscoveryMessage("192.168.1.100", 5911, 5912);

        capturedDiscoveryListener.onServerDiscovered(server);

        verify(mockPairingManager).setServerHttpPort(5911);
    }

    @Test
    public void onServerDiscovered_startsTcpPairing() {
        viewModel.startPairing("Test Student");
        DiscoveryMessage server = new DiscoveryMessage("192.168.1.100", 5911, 5912);

        capturedDiscoveryListener.onServerDiscovered(server);

        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> hostCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> portCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockPairingManager).startPairing(
                idCaptor.capture(), hostCaptor.capture(), portCaptor.capture());

        assertNotNull(idCaptor.getValue()); // UUID was generated
        assertEquals("192.168.1.100", hostCaptor.getValue());
        assertEquals(5912, (int) portCaptor.getValue());
    }

    // ========== TCP pairing → HTTP registration ==========

    @Test
    public void onTcpPairingSuccess_setsPhaseToHttpRegistering() {
        viewModel.startPairing("Test Student");
        when(mockApiService.registerDevice(any())).thenReturn(mockCall);
        DiscoveryMessage server = new DiscoveryMessage("192.168.1.100", 5911, 5912);
        capturedDiscoveryListener.onServerDiscovered(server);

        capturedPairingCallback.onTcpPairingSuccess();

        assertEquals(PairingPhase.HTTP_REGISTERING, viewModel.getPairingPhase().getValue());
    }

    @Test
    public void onTcpPairingSuccess_callsRegisterDevice() {
        viewModel.startPairing("Test Student");
        when(mockApiService.registerDevice(any())).thenReturn(mockCall);
        DiscoveryMessage server = new DiscoveryMessage("192.168.1.100", 5911, 5912);
        capturedDiscoveryListener.onServerDiscovered(server);

        capturedPairingCallback.onTcpPairingSuccess();

        ArgumentCaptor<DeviceInfoDto> dtoCaptor =
                ArgumentCaptor.forClass(DeviceInfoDto.class);
        verify(mockApiService).registerDevice(dtoCaptor.capture());

        DeviceInfoDto captured = dtoCaptor.getValue();
        assertNotNull(captured.getDeviceId());
        assertEquals("Test Student", captured.getName());
    }

    // ========== HTTP registration success ==========

    @SuppressWarnings("unchecked")
    @Test
    public void onHttpSuccess_setsPhaseToPaired() {
        viewModel.startPairing("Test Student");
        when(mockApiService.registerDevice(any())).thenReturn(mockCall);
        DiscoveryMessage server = new DiscoveryMessage("192.168.1.100", 5911, 5912);
        capturedDiscoveryListener.onServerDiscovered(server);
        capturedPairingCallback.onTcpPairingSuccess();

        // Simulate Retrofit success callback
        ArgumentCaptor<Callback<Void>> callbackCaptor =
                ArgumentCaptor.forClass(Callback.class);
        verify(mockCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(mockCall, Response.success(null));

        assertEquals(PairingPhase.PAIRED, viewModel.getPairingPhase().getValue());
        assertTrue(Boolean.TRUE.equals(viewModel.getPairingComplete().getValue()));
    }

    // ========== Error handling ==========

    @Test
    public void onTcpPairingFailed_setsErrorPhase() {
        viewModel.startPairing("Test Student");

        capturedPairingCallback.onPairingFailed("Connection refused");

        assertEquals(PairingPhase.ERROR, viewModel.getPairingPhase().getValue());
        assertNotNull(viewModel.getErrorMessage().getValue());
    }

    @Test
    public void onTcpPairingTimeout_setsErrorPhase() {
        viewModel.startPairing("Test Student");

        capturedPairingCallback.onPairingTimeout();

        assertEquals(PairingPhase.ERROR, viewModel.getPairingPhase().getValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onHttpFailure_setsErrorPhase() {
        viewModel.startPairing("Test Student");
        when(mockApiService.registerDevice(any())).thenReturn(mockCall);
        DiscoveryMessage server = new DiscoveryMessage("192.168.1.100", 5911, 5912);
        capturedDiscoveryListener.onServerDiscovered(server);
        capturedPairingCallback.onTcpPairingSuccess();

        ArgumentCaptor<Callback<Void>> callbackCaptor =
                ArgumentCaptor.forClass(Callback.class);
        verify(mockCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(mockCall, new RuntimeException("Network error"));

        assertEquals(PairingPhase.ERROR, viewModel.getPairingPhase().getValue());
        assertNotNull(viewModel.getErrorMessage().getValue());
    }

    // ========== cancelPairing ==========

    @Test
    public void cancelPairing_stopsDiscovery() {
        viewModel.startPairing("Test Student");

        viewModel.cancelPairing();

        verify(mockDiscoveryManager).stopDiscovery();
    }

    @Test
    public void cancelPairing_cancelsTcpPairing() {
        viewModel.startPairing("Test Student");

        viewModel.cancelPairing();

        verify(mockPairingManager).cancelPairing();
    }

    @Test
    public void cancelPairing_setsPhaseToIdle() {
        viewModel.startPairing("Test Student");

        viewModel.cancelPairing();

        assertEquals(PairingPhase.IDLE, viewModel.getPairingPhase().getValue());
    }

    // ========== retry ==========

    @Test
    public void retry_restartsFromDiscovery() {
        viewModel.startPairing("Test Student");
        capturedPairingCallback.onPairingFailed("test");

        viewModel.retry();

        assertEquals(PairingPhase.DISCOVERING, viewModel.getPairingPhase().getValue());
    }

    // ========== LiveData exposure ==========

    @Test
    public void getDiscoveryState_isNotNull() {
        assertNotNull(viewModel.getDiscoveryState());
    }

    @Test
    public void getTcpPairingState_isNotNull() {
        assertNotNull(viewModel.getTcpPairingState());
    }

    @Test
    public void getStatusMessage_isNotNull() {
        assertNotNull(viewModel.getStatusMessage());
    }

    // ========== Lifecycle ==========

    @Test
    public void startPairing_whileInProgress_isIgnored() {
        viewModel.startPairing("Test Student");
        // Reset mock to track second call
        org.mockito.Mockito.reset(mockDiscoveryManager);
        MutableLiveData<DiscoveryState> state = new MutableLiveData<>(DiscoveryState.SEARCHING);
        when(mockDiscoveryManager.getDiscoveryState()).thenReturn(state);

        viewModel.startPairing("Another Name");

        verify(mockDiscoveryManager, never()).startDiscovery();
    }
}
