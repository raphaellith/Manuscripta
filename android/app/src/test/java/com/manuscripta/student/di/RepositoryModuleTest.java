package com.manuscripta.student.di;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.manuscripta.student.data.local.DeviceStatusDao;
import com.manuscripta.student.data.local.ManuscriptaDatabase;
import com.manuscripta.student.data.local.MaterialDao;
import com.manuscripta.student.data.local.ResponseDao;
import com.manuscripta.student.data.local.SessionDao;
import com.manuscripta.student.data.repository.DeviceStatusRepository;
import com.manuscripta.student.data.repository.DeviceStatusRepositoryImpl;
import com.manuscripta.student.data.repository.MaterialRepository;
import com.manuscripta.student.data.repository.MaterialRepositoryImpl;
import com.manuscripta.student.data.repository.ResponseRepository;
import com.manuscripta.student.data.repository.ResponseRepositoryImpl;
import com.manuscripta.student.data.repository.SessionRepository;
import com.manuscripta.student.data.repository.SessionRepositoryImpl;
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
    private MaterialDao mockMaterialDao;
    private Context mockContext;
    private FileStorageManager mockFileStorageManager;

    @Before
    public void setUp() {
        repositoryModule = new RepositoryModule();
        mockDatabase = mock(ManuscriptaDatabase.class);
        mockSessionDao = mock(SessionDao.class);
        mockResponseDao = mock(ResponseDao.class);
        mockDeviceStatusDao = mock(DeviceStatusDao.class);
        mockMaterialDao = mock(MaterialDao.class);
        mockContext = mock(Context.class);
        mockFileStorageManager = mock(FileStorageManager.class);
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
    public void testProvideDeviceStatusDao_returnsDao() {
        when(mockDatabase.deviceStatusDao()).thenReturn(mockDeviceStatusDao);

        DeviceStatusDao result = repositoryModule.provideDeviceStatusDao(mockDatabase);

        assertNotNull(result);
        verify(mockDatabase).deviceStatusDao();
    }

    @Test
    public void testProvideDeviceStatusRepository_returnsRepository() {
        DeviceStatusRepository result =
                repositoryModule.provideDeviceStatusRepository(mockDeviceStatusDao);

        assertNotNull(result);
        assertTrue(result instanceof DeviceStatusRepositoryImpl);
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
        // Mock the context chain and filesDir
        when(mockContext.getApplicationContext()).thenReturn(mockContext);
        when(mockContext.getFilesDir())
                .thenReturn(new java.io.File(System.getProperty("java.io.tmpdir")));

        FileStorageManager result = repositoryModule.provideFileStorageManager(mockContext);

        assertNotNull(result);
        assertTrue(result instanceof FileStorageManager);
    }

    @Test
    public void testProvideMaterialRepository_returnsRepository() {
        // Stub getAll() to avoid NullPointerException when MaterialRepositoryImpl
        // constructor calls refreshMaterialsLiveData() which reads from the DAO
        when(mockMaterialDao.getAll()).thenReturn(new java.util.ArrayList<>());

        MaterialRepository result =
                repositoryModule.provideMaterialRepository(mockMaterialDao, mockFileStorageManager);

        assertNotNull(result);
        assertTrue(result instanceof MaterialRepositoryImpl);
    }
}
