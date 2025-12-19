package com.manuscripta.student.di;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.manuscripta.student.data.local.ManuscriptaDatabase;
import com.manuscripta.student.data.local.MaterialDao;
import com.manuscripta.student.data.local.ResponseDao;
import com.manuscripta.student.data.local.SessionDao;
import com.manuscripta.student.data.repository.MaterialRepository;
import com.manuscripta.student.data.repository.MaterialRepositoryImpl;
import com.manuscripta.student.data.repository.ResponseRepository;
import com.manuscripta.student.data.repository.ResponseRepositoryImpl;
import com.manuscripta.student.data.repository.SessionRepository;
import com.manuscripta.student.data.repository.SessionRepositoryImpl;
import com.manuscripta.student.network.tcp.TcpSocketManager;
import com.manuscripta.student.utils.FileStorageManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
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
    private TcpSocketManager mockTcpSocketManager;

    @Before
    public void setUp() {
        repositoryModule = new RepositoryModule();
        mockDatabase = mock(ManuscriptaDatabase.class);
        mockSessionDao = mock(SessionDao.class);
        mockResponseDao = mock(ResponseDao.class);
        mockMaterialDao = mock(MaterialDao.class);
        mockFileStorageManager = mock(FileStorageManager.class);
        mockTcpSocketManager = mock(TcpSocketManager.class);
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
        Context context = RuntimeEnvironment.getApplication();

        FileStorageManager result = repositoryModule.provideFileStorageManager(context);

        assertNotNull(result);
    }

    @Test
    public void testProvideMaterialRepository_withTcpSocketManager_returnsRepository() {
        MaterialRepository result = repositoryModule.provideMaterialRepository(
                mockMaterialDao, mockFileStorageManager, mockTcpSocketManager);

        assertNotNull(result);
        assertTrue(result instanceof MaterialRepositoryImpl);
    }

    @Test
    public void testProvideMaterialRepository_withNullTcpSocketManager_returnsRepository() {
        MaterialRepository result = repositoryModule.provideMaterialRepository(
                mockMaterialDao, mockFileStorageManager, null);

        assertNotNull(result);
        assertTrue(result instanceof MaterialRepositoryImpl);
    }
}
