package com.manuscripta.student.device;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.lifecycle.MutableLiveData;

import com.manuscripta.student.data.repository.DeviceStatusRepository;
import com.manuscripta.student.data.repository.SessionRepository;
import com.manuscripta.student.network.tcp.ConnectionState;
import com.manuscripta.student.network.tcp.TcpSocketManager;
import com.manuscripta.student.network.tcp.message.HandRaisedMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

/**
 * Unit tests for {@link DeviceManagementService}.
 */
public class DeviceManagementServiceTest {

    private static final String TEST_DEVICE_ID = "test-device-123";

    @Mock
    private DeviceStatusRepository mockDeviceStatusRepository;

    @Mock
    private SessionRepository mockSessionRepository;

    @Mock
    private TcpSocketManager mockSocketManager;

    private DeviceManagementService service;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockSocketManager.getConnectionState())
                .thenReturn(new MutableLiveData<>(ConnectionState.DISCONNECTED));
        when(mockSessionRepository.getSessionsByStatus(any()))
                .thenReturn(Collections.emptyList());
        service = new DeviceManagementService(
                mockDeviceStatusRepository,
                mockSessionRepository,
                mockSocketManager);
    }

    // ========== Constructor tests ==========

    @Test
    public void testConstructor_createsInstance() {
        assertNotNull(service);
    }

    @Test
    public void testConstructor_nullDeviceStatusRepo_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new DeviceManagementService(
                        null, mockSessionRepository, mockSocketManager));
    }

    @Test
    public void testConstructor_nullSessionRepo_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new DeviceManagementService(
                        mockDeviceStatusRepository, null, mockSocketManager));
    }

    @Test
    public void testConstructor_nullSocketManager_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new DeviceManagementService(
                        mockDeviceStatusRepository, mockSessionRepository,
                        null));
    }

    @Test
    public void testConstructor_initiallyNotInitialised() {
        assertFalse(service.isInitialised());
    }

    // ========== initialise tests ==========

    @Test
    public void testInitialise_setsInitialised() {
        service.initialise(TEST_DEVICE_ID);
        assertTrue(service.isInitialised());
    }

    @Test
    public void testInitialise_nullDeviceId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.initialise(null));
    }

    @Test
    public void testInitialise_emptyDeviceId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.initialise(""));
    }

    @Test
    public void testInitialise_blankDeviceId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.initialise("   "));
    }

    // ========== shutdown tests ==========

    @Test
    public void testShutdown_clearsInitialised() {
        service.initialise(TEST_DEVICE_ID);
        service.shutdown();
        assertFalse(service.isInitialised());
    }

    @Test
    public void testShutdown_disablesKioskMode() {
        service.initialise(TEST_DEVICE_ID);
        service.enableKioskMode();
        service.shutdown();
        assertFalse(service.isKioskModeEnabled());
    }

    // ========== Convenience method tests ==========

    @Test
    public void testRaiseHand_delegatesToHelpManager() throws Exception {
        service.raiseHand(TEST_DEVICE_ID);
        verify(mockSocketManager).send(any(HandRaisedMessage.class));
    }

    @Test
    public void testLowerHand_delegatesToHelpManager() throws Exception {
        service.raiseHand(TEST_DEVICE_ID);
        service.lowerHand();
        assertFalse(service.getHelpRequestManager().isHandRaised());
    }

    @Test
    public void testIsScreenLocked_delegatesToScreenLockManager() {
        assertFalse(service.isScreenLocked());
    }

    @Test
    public void testIsConnected_delegatesToConnectionTracker() {
        when(mockSocketManager.isConnected()).thenReturn(true);
        assertTrue(service.isConnected());
    }

    @Test
    public void testIsConnected_whenDisconnected() {
        when(mockSocketManager.isConnected()).thenReturn(false);
        assertFalse(service.isConnected());
    }

    @Test
    public void testEnableKioskMode_delegatesToKioskManager() {
        assertTrue(service.enableKioskMode());
        assertTrue(service.isKioskModeEnabled());
    }

    @Test
    public void testDisableKioskMode_delegatesToKioskManager() {
        service.enableKioskMode();
        assertTrue(service.disableKioskMode());
        assertFalse(service.isKioskModeEnabled());
    }

    @Test
    public void testOnBatteryLevelChanged_delegatesToBatteryMonitor() {
        service.onBatteryLevelChanged(75);
        verify(mockDeviceStatusRepository).updateBatteryLevel(75);
    }

    // ========== Sub-manager getter tests ==========

    @Test
    public void testGetBatteryMonitor_returnsInstance() {
        assertNotNull(service.getBatteryMonitor());
    }

    @Test
    public void testGetConnectionTracker_returnsInstance() {
        assertNotNull(service.getConnectionTracker());
    }

    @Test
    public void testGetKioskModeManager_returnsInstance() {
        assertNotNull(service.getKioskModeManager());
    }

    @Test
    public void testGetScreenLockManager_returnsInstance() {
        assertNotNull(service.getScreenLockManager());
    }

    @Test
    public void testGetHelpRequestManager_returnsInstance() {
        assertNotNull(service.getHelpRequestManager());
    }

    @Test
    public void testGetSessionEndHandler_returnsInstance() {
        assertNotNull(service.getSessionEndHandler());
    }

    // ========== Integration tests ==========

    @Test
    public void testInitialiseAndShutdown_lifecycle() {
        assertFalse(service.isInitialised());
        service.initialise(TEST_DEVICE_ID);
        assertTrue(service.isInitialised());
        service.shutdown();
        assertFalse(service.isInitialised());
    }

    @Test
    public void testKioskModeToggle_beforeInitialise() {
        // Kiosk mode should work even before initialise
        assertTrue(service.enableKioskMode());
        assertTrue(service.isKioskModeEnabled());
        assertTrue(service.disableKioskMode());
        assertFalse(service.isKioskModeEnabled());
    }

    @Test
    public void testBatteryLevel_afterInitialise() {
        service.initialise(TEST_DEVICE_ID);
        service.onBatteryLevelChanged(50);
        verify(mockDeviceStatusRepository).updateBatteryLevel(50);
    }
}
