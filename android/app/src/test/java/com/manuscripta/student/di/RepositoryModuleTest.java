package com.manuscripta.student.di;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.manuscripta.student.data.local.DeviceStatusDao;
import com.manuscripta.student.data.local.FeedbackDao;
import com.manuscripta.student.data.local.ManuscriptaDatabase;
import com.manuscripta.student.data.local.MaterialDao;
import com.manuscripta.student.data.local.QuestionDao;
import com.manuscripta.student.data.local.ResponseDao;
import com.manuscripta.student.data.local.SessionDao;
import com.manuscripta.student.data.repository.ConfigRepository;
import com.manuscripta.student.data.repository.DeviceStatusRepository;
import com.manuscripta.student.data.repository.DeviceStatusRepositoryImpl;
import com.manuscripta.student.data.repository.FeedbackRepository;
import com.manuscripta.student.data.repository.FeedbackRepositoryImpl;
import com.manuscripta.student.data.repository.MaterialRepository;
import com.manuscripta.student.data.repository.MaterialRepositoryImpl;
import com.manuscripta.student.data.repository.ResponseRepository;
import com.manuscripta.student.data.repository.ResponseRepositoryImpl;
import com.manuscripta.student.data.repository.SessionRepository;
import com.manuscripta.student.data.repository.SessionRepositoryImpl;
import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.network.tcp.AckRetrySender;
import com.manuscripta.student.network.tcp.HeartbeatManager;
import com.manuscripta.student.network.tcp.PairingManager;
import com.manuscripta.student.network.tcp.TcpSocketManager;
import com.manuscripta.student.network.tcp.message.DistributeMaterialMessage;
import com.manuscripta.student.network.tcp.message.LockScreenMessage;
import com.manuscripta.student.network.tcp.message.ReturnFeedbackMessage;
import com.manuscripta.student.network.tcp.message.UnlockScreenMessage;
import com.manuscripta.student.network.tcp.message.UnpairMessage;
import com.manuscripta.student.utils.FileStorageManager;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link RepositoryModule}.
 */
public class RepositoryModuleTest {

    private RepositoryModule repositoryModule;
    private ManuscriptaDatabase mockDatabase;
    private SessionDao mockSessionDao;
    private ResponseDao mockResponseDao;
    private DeviceStatusDao mockDeviceStatusDao;
    private FeedbackDao mockFeedbackDao;
    private MaterialDao mockMaterialDao;
    private QuestionDao mockQuestionDao;
    private FileStorageManager mockFileStorageManager;
    private ApiService mockApiService;
    private TcpSocketManager mockTcpSocketManager;
    private AckRetrySender mockAckRetrySender;
    private PairingManager mockPairingManager;
    private DeviceStatusRepository mockDeviceStatusRepository;
    private FeedbackRepository mockFeedbackRepository;
    private MaterialRepository mockMaterialRepository;
    private SessionRepository mockSessionRepository;
    private ConfigRepository mockConfigRepository;

    @Before
    public void setUp() {
        repositoryModule = new RepositoryModule();
        mockDatabase = mock(ManuscriptaDatabase.class);
        mockSessionDao = mock(SessionDao.class);
        mockResponseDao = mock(ResponseDao.class);
        mockDeviceStatusDao = mock(DeviceStatusDao.class);
        mockFeedbackDao = mock(FeedbackDao.class);
        when(mockFeedbackDao.getAllLive())
                .thenReturn(new MutableLiveData<>(java.util.Collections.emptyList()));
        mockMaterialDao = mock(MaterialDao.class);
        mockQuestionDao = mock(QuestionDao.class);
        mockFileStorageManager = mock(FileStorageManager.class);
        mockApiService = mock(ApiService.class);
        mockTcpSocketManager = mock(TcpSocketManager.class);
        mockAckRetrySender = mock(AckRetrySender.class);
        mockPairingManager = mock(PairingManager.class);
        mockDeviceStatusRepository = mock(DeviceStatusRepository.class);
        mockFeedbackRepository = mock(FeedbackRepository.class);
        mockMaterialRepository = mock(MaterialRepository.class);
        mockSessionRepository = mock(SessionRepository.class);
        mockConfigRepository = mock(ConfigRepository.class);
    }

    @Test
    public void testProvideSessionDao_returnsDao() {
        when(mockDatabase.sessionDao()).thenReturn(mockSessionDao);

        SessionDao result = repositoryModule.provideSessionDao(mockDatabase);

        assertNotNull(result);
        verify(mockDatabase).sessionDao();
    }

    @Test
    public void testProvideSessionRepository_returnsRepository() {
        SessionRepository result = repositoryModule.provideSessionRepository(mockSessionDao);

        assertNotNull(result);
        assertTrue(result instanceof SessionRepositoryImpl);
    }

    @Test
    public void testProvideResponseDao_returnsDao() {
        when(mockDatabase.responseDao()).thenReturn(mockResponseDao);

        ResponseDao result = repositoryModule.provideResponseDao(mockDatabase);

        assertNotNull(result);
        verify(mockDatabase).responseDao();
    }

    @Test
    public void testProvideResponseRepository_returnsRepository() {
        ResponseRepository result = repositoryModule.provideResponseRepository(mockResponseDao, mockApiService);

        assertNotNull(result);
        assertTrue(result instanceof ResponseRepositoryImpl);
    }

    @Test
    public void testProvideMaterialDao_returnsDao() {
        when(mockDatabase.materialDao()).thenReturn(mockMaterialDao);

        MaterialDao result = repositoryModule.provideMaterialDao(mockDatabase);

        assertNotNull(result);
        verify(mockDatabase).materialDao();
    }

    @Test
    public void testProvideFileStorageManager_returnsFileStorageManager() {
        Context context = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(context);
        when(context.getFilesDir()).thenReturn(new java.io.File("/tmp/test"));

        FileStorageManager result = repositoryModule.provideFileStorageManager(context);

        assertNotNull(result);
    }

    @Test
    public void testProvideAckRetrySender_returnsSender() {
        AckRetrySender result = repositoryModule.provideAckRetrySender(mockTcpSocketManager);

        assertNotNull(result);
    }

    @Test
    public void testProvideMaterialRepository_returnsRepository() {
        when(mockMaterialDao.getAll()).thenReturn(new java.util.ArrayList<>());

        MaterialRepository result = repositoryModule.provideMaterialRepository(
                mockMaterialDao, mockQuestionDao, mockFileStorageManager, mockApiService,
                mockTcpSocketManager, mockAckRetrySender, mockSessionRepository);

        assertNotNull(result);
        assertTrue(result instanceof MaterialRepositoryImpl);
    }

    @Test
    public void testProvideFeedbackDao_returnsDao() {
        when(mockDatabase.feedbackDao()).thenReturn(mockFeedbackDao);

        FeedbackDao result = repositoryModule.provideFeedbackDao(mockDatabase);

        assertNotNull(result);
        verify(mockDatabase).feedbackDao();
    }

    @Test
    public void testProvideFeedbackRepository_returnsRepository() {
        FeedbackRepository result = repositoryModule.provideFeedbackRepository(
                mockFeedbackDao, mockApiService, mockAckRetrySender);

        assertNotNull(result);
        assertTrue(result instanceof FeedbackRepositoryImpl);
    }

    @Test
    public void testProvideDeviceStatusDao_returnsDao() {
        when(mockDatabase.deviceStatusDao()).thenReturn(mockDeviceStatusDao);

        DeviceStatusDao result = repositoryModule.provideDeviceStatusDao(mockDatabase);

        assertNotNull(result);
        verify(mockDatabase).deviceStatusDao();
    }

    @Test
    public void testProvideDeviceStatusRepository_returnsRepository() {
        DeviceStatusRepository result = repositoryModule.provideDeviceStatusRepository(mockDeviceStatusDao);

        assertNotNull(result);
        assertTrue(result instanceof DeviceStatusRepositoryImpl);
    }

    @Test
    public void testProvideHeartbeatManager_returnsWiredManager() {
        HeartbeatManager result = repositoryModule.provideHeartbeatManager(
                mockTcpSocketManager, mockPairingManager,
                mockMaterialRepository, mockFeedbackRepository,
                mockDeviceStatusRepository, mockSessionRepository,
                mockConfigRepository);

        try {
            assertNotNull(result);
        } finally {
            result.destroy();
        }
    }

    @Test
    public void testProvideHeartbeatManager_materialCallback_triggersSync()
            throws InterruptedException {
        when(mockPairingManager.getDeviceId()).thenReturn("device-1");

        HeartbeatManager hm = repositoryModule.provideHeartbeatManager(
                mockTcpSocketManager, mockPairingManager,
                mockMaterialRepository, mockFeedbackRepository,
                mockDeviceStatusRepository, mockSessionRepository,
                mockConfigRepository);

        try {
            hm.onMessageReceived(new DistributeMaterialMessage());

            verify(mockMaterialRepository, timeout(2000)).syncMaterials("device-1");
        } finally {
            hm.destroy();
        }
    }

    @Test
    public void testProvideHeartbeatManager_feedbackCallback_triggersFetch()
            throws Exception {
        when(mockPairingManager.getDeviceId()).thenReturn("device-1");

        HeartbeatManager hm = repositoryModule.provideHeartbeatManager(
                mockTcpSocketManager, mockPairingManager,
                mockMaterialRepository, mockFeedbackRepository,
                mockDeviceStatusRepository, mockSessionRepository,
                mockConfigRepository);

        try {
            hm.onMessageReceived(new ReturnFeedbackMessage());

            verify(mockFeedbackRepository, timeout(2000))
                    .fetchAndStoreFeedback("device-1");
        } finally {
            hm.destroy();
        }
    }

    @Test
    public void testProvideHeartbeatManager_statusProvider_returnsStatus() {
        when(mockPairingManager.getDeviceId()).thenReturn("device-1");
        when(mockTcpSocketManager.isConnected()).thenReturn(true);

        HeartbeatManager hm = repositoryModule.provideHeartbeatManager(
                mockTcpSocketManager, mockPairingManager,
                mockMaterialRepository, mockFeedbackRepository,
                mockDeviceStatusRepository, mockSessionRepository,
                mockConfigRepository);

        // Trigger a heartbeat by starting and waiting for the first scheduled send
        try {
            hm.start();
            verify(mockDeviceStatusRepository, timeout(5000))
                    .getDeviceStatus("device-1");
        } finally {
            hm.destroy();
        }
    }

    @Test
    public void testProvideHeartbeatManager_nullDeviceId_skipsCallbacks() throws Exception {
        when(mockPairingManager.getDeviceId()).thenReturn(null);

        HeartbeatManager hm = repositoryModule.provideHeartbeatManager(
                mockTcpSocketManager, mockPairingManager,
                mockMaterialRepository, mockFeedbackRepository,
                mockDeviceStatusRepository, mockSessionRepository,
                mockConfigRepository);

        try {
            hm.onMessageReceived(new DistributeMaterialMessage());
            hm.onMessageReceived(new ReturnFeedbackMessage());

            // With null device ID, neither repository should be called
            verify(mockMaterialRepository, org.mockito.Mockito.never())
                    .syncMaterials(anyString());
            verify(mockFeedbackRepository, org.mockito.Mockito.never())
                    .fetchAndStoreFeedback(anyString());
        } finally {
            hm.destroy();
        }
    }

    @Test
    public void testProvideHeartbeatManager_emptyDeviceId_skipsCallbacks() throws Exception {
        when(mockPairingManager.getDeviceId()).thenReturn("   ");

        HeartbeatManager hm = repositoryModule.provideHeartbeatManager(
                mockTcpSocketManager, mockPairingManager,
                mockMaterialRepository, mockFeedbackRepository,
                mockDeviceStatusRepository, mockSessionRepository,
                mockConfigRepository);

        try {
            hm.onMessageReceived(new DistributeMaterialMessage());
            hm.onMessageReceived(new ReturnFeedbackMessage());

            verify(mockMaterialRepository, org.mockito.Mockito.never())
                    .syncMaterials(anyString());
            verify(mockFeedbackRepository, org.mockito.Mockito.never())
                    .fetchAndStoreFeedback(anyString());
        } finally {
            hm.destroy();
        }
    }

    @Test
    public void testProvideHeartbeatManager_lockCallback_setsLocked()
            throws InterruptedException {
        when(mockPairingManager.getDeviceId()).thenReturn("device-1");

        HeartbeatManager hm = repositoryModule.provideHeartbeatManager(
                mockTcpSocketManager, mockPairingManager,
                mockMaterialRepository, mockFeedbackRepository,
                mockDeviceStatusRepository, mockSessionRepository,
                mockConfigRepository);

        try {
            hm.onMessageReceived(new LockScreenMessage());

            verify(mockDeviceStatusRepository, timeout(2000))
                    .setLocked("device-1");
        } finally {
            hm.destroy();
        }
    }

    @Test
    public void testProvideHeartbeatManager_unlockCallback_setsOnTask()
            throws InterruptedException {
        when(mockPairingManager.getDeviceId()).thenReturn("device-1");

        HeartbeatManager hm = repositoryModule.provideHeartbeatManager(
                mockTcpSocketManager, mockPairingManager,
                mockMaterialRepository, mockFeedbackRepository,
                mockDeviceStatusRepository, mockSessionRepository,
                mockConfigRepository);

        try {
            hm.onMessageReceived(new UnlockScreenMessage());

            verify(mockDeviceStatusRepository, timeout(2000))
                    .setOnTask("device-1", null);
        } finally {
            hm.destroy();
        }
    }

    @Test
    public void testProvideHeartbeatManager_unpairCallback_clearsAllData()
            throws InterruptedException {
        when(mockPairingManager.getDeviceId()).thenReturn("device-1");

        HeartbeatManager hm = repositoryModule.provideHeartbeatManager(
                mockTcpSocketManager, mockPairingManager,
                mockMaterialRepository, mockFeedbackRepository,
                mockDeviceStatusRepository, mockSessionRepository,
                mockConfigRepository);

        try {
            hm.onMessageReceived(new UnpairMessage());

            verify(mockSessionRepository, timeout(2000))
                    .deleteAllSessions();
            verify(mockMaterialRepository, timeout(2000))
                    .deleteAllMaterials();
            verify(mockFeedbackRepository, timeout(2000))
                    .deleteAllFeedback();
            verify(mockConfigRepository, timeout(2000))
                    .clearConfig();
            verify(mockDeviceStatusRepository, timeout(2000))
                    .clearDeviceStatus("device-1");
            verify(mockTcpSocketManager, timeout(2000))
                    .disconnect();
            verify(mockPairingManager, timeout(2000))
                    .resetPairingData();
        } finally {
            hm.destroy();
        }
    }

    @Test
    public void testProvideHeartbeatManager_unpairCallback_handlesNoActiveSession()
            throws InterruptedException {
        when(mockPairingManager.getDeviceId()).thenReturn("device-1");
        doThrow(new IllegalStateException("No active session"))
                .when(mockSessionRepository).cancelSession();

        HeartbeatManager hm = repositoryModule.provideHeartbeatManager(
                mockTcpSocketManager, mockPairingManager,
                mockMaterialRepository, mockFeedbackRepository,
                mockDeviceStatusRepository, mockSessionRepository,
                mockConfigRepository);

        try {
            hm.onMessageReceived(new UnpairMessage());

            // Should still proceed with cleanup despite cancelSession failing
            verify(mockSessionRepository, timeout(2000))
                    .deleteAllSessions();
            verify(mockPairingManager, timeout(2000))
                    .resetPairingData();
        } finally {
            hm.destroy();
        }
    }

    @Test
    public void testProvideHeartbeatManager_materialCallback_handlesException()
            throws InterruptedException {
        when(mockPairingManager.getDeviceId()).thenReturn("device-1");
        doThrow(new RuntimeException("sync failed"))
                .when(mockMaterialRepository).syncMaterials("device-1");

        HeartbeatManager hm = repositoryModule.provideHeartbeatManager(
                mockTcpSocketManager, mockPairingManager,
                mockMaterialRepository, mockFeedbackRepository,
                mockDeviceStatusRepository, mockSessionRepository,
                mockConfigRepository);

        try {
            // Should not throw — exception is caught internally
            hm.onMessageReceived(new DistributeMaterialMessage());

            verify(mockMaterialRepository, timeout(2000))
                    .syncMaterials("device-1");
        } finally {
            hm.destroy();
        }
    }

    @Test
    public void testProvideHeartbeatManager_feedbackCallback_handlesException()
            throws Exception {
        when(mockPairingManager.getDeviceId()).thenReturn("device-1");
        doThrow(new RuntimeException("fetch failed"))
                .when(mockFeedbackRepository)
                .fetchAndStoreFeedback("device-1");

        HeartbeatManager hm = repositoryModule.provideHeartbeatManager(
                mockTcpSocketManager, mockPairingManager,
                mockMaterialRepository, mockFeedbackRepository,
                mockDeviceStatusRepository, mockSessionRepository,
                mockConfigRepository);

        try {
            // Should not throw — exception is caught internally
            hm.onMessageReceived(new ReturnFeedbackMessage());

            verify(mockFeedbackRepository, timeout(2000))
                    .fetchAndStoreFeedback("device-1");
        } finally {
            hm.destroy();
        }
    }
}
