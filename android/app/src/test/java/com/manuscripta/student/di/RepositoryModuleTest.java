package com.manuscripta.student.di;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.manuscripta.student.data.local.DeviceStatusDao;
import com.manuscripta.student.data.local.FeedbackDao;
import com.manuscripta.student.data.local.ManuscriptaDatabase;
import com.manuscripta.student.data.local.MaterialDao;
import com.manuscripta.student.data.local.ResponseDao;
import com.manuscripta.student.data.local.SessionDao;
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
import com.manuscripta.student.network.tcp.PairingManager;
import com.manuscripta.student.network.tcp.TcpSocketManager;
import com.manuscripta.student.utils.FileStorageManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import androidx.test.core.app.ApplicationProvider;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link RepositoryModule}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class RepositoryModuleTest {

    private RepositoryModule repositoryModule;
    private ManuscriptaDatabase mockDatabase;
    private SessionDao mockSessionDao;
    private ResponseDao mockResponseDao;
    private MaterialDao mockMaterialDao;
    private FileStorageManager mockFileStorageManager;
    private ApiService mockApiService;
    private TcpSocketManager mockTcpSocketManager;
    private PairingManager mockPairingManager;
    private DeviceStatusDao mockDeviceStatusDao;
    private FeedbackDao mockFeedbackDao;

    @Before
    public void setUp() {
        repositoryModule = new RepositoryModule();
        mockDatabase = mock(ManuscriptaDatabase.class);
        mockSessionDao = mock(SessionDao.class);
        mockResponseDao = mock(ResponseDao.class);
        mockMaterialDao = mock(MaterialDao.class);
        mockFileStorageManager = mock(FileStorageManager.class);
        mockApiService = mock(ApiService.class);
        mockTcpSocketManager = mock(TcpSocketManager.class);
        mockPairingManager = mock(PairingManager.class);
        mockDeviceStatusDao = mock(DeviceStatusDao.class);
        mockFeedbackDao = mock(FeedbackDao.class);
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
        ResponseRepository result = repositoryModule.provideResponseRepository(mockResponseDao);

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
    public void testProvideFileStorageManager_returnsManager() {
        Context context = ApplicationProvider.getApplicationContext();

        FileStorageManager result = repositoryModule.provideFileStorageManager(context);

        assertNotNull(result);
    }

    @Test
    public void testProvideMaterialRepository_returnsRepository() {
        MaterialRepository result = repositoryModule.provideMaterialRepository(
                mockMaterialDao, mockFileStorageManager, mockApiService,
                mockTcpSocketManager, mockPairingManager);

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
        ApiService mockApiService = mock(ApiService.class);
        TcpSocketManager mockTcpSocketManager = mock(TcpSocketManager.class);

        FeedbackRepository result = repositoryModule.provideFeedbackRepository(
                mockFeedbackDao, mockApiService, mockTcpSocketManager);

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
}
