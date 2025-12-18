package com.manuscripta.student.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;

import com.manuscripta.student.data.local.MaterialDao;
import com.manuscripta.student.data.model.MaterialEntity;
import com.manuscripta.student.data.model.MaterialType;
import com.manuscripta.student.domain.model.Material;
import com.manuscripta.student.utils.FileStorageManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for {@link MaterialRepositoryImpl}.
 */
public class MaterialRepositoryImplTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private MaterialDao mockDao;

    @Mock
    private FileStorageManager mockFileStorageManager;

    private MaterialRepositoryImpl repository;

    private static final String TEST_MATERIAL_ID = "test-material-123";
    private static final String TEST_DEVICE_ID = "test-device-456";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockDao.getAll()).thenReturn(new ArrayList<>());
        repository = new MaterialRepositoryImpl(mockDao, mockFileStorageManager);
    }

    // ========== Constructor tests ==========

    @Test
    public void constructor_createsInstance() {
        assertNotNull(repository);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nullDao_throwsException() {
        new MaterialRepositoryImpl(null, mockFileStorageManager);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nullFileStorageManager_throwsException() {
        new MaterialRepositoryImpl(mockDao, null);
    }

    // ========== getMaterialById tests ==========

    @Test
    public void getMaterialById_existingMaterial_returnsMaterial() {
        MaterialEntity entity = createTestEntity(TEST_MATERIAL_ID);
        when(mockDao.getById(TEST_MATERIAL_ID)).thenReturn(entity);

        Material result = repository.getMaterialById(TEST_MATERIAL_ID);

        assertNotNull(result);
        assertEquals(TEST_MATERIAL_ID, result.getId());
    }

    @Test
    public void getMaterialById_nonExistingMaterial_returnsNull() {
        when(mockDao.getById(TEST_MATERIAL_ID)).thenReturn(null);

        Material result = repository.getMaterialById(TEST_MATERIAL_ID);

        assertNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getMaterialById_nullId_throwsException() {
        repository.getMaterialById(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getMaterialById_emptyId_throwsException() {
        repository.getMaterialById("");
    }

    // ========== getAllMaterials tests ==========

    @Test
    public void getAllMaterials_returnsMaterials() {
        List<MaterialEntity> entities = Arrays.asList(
                createTestEntity("mat1"),
                createTestEntity("mat2")
        );
        when(mockDao.getAll()).thenReturn(entities);

        List<Material> result = repository.getAllMaterials();

        assertEquals(2, result.size());
    }

    @Test
    public void getAllMaterials_emptyDatabase_returnsEmptyList() {
        when(mockDao.getAll()).thenReturn(new ArrayList<>());

        List<Material> result = repository.getAllMaterials();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== getMaterialsByType tests ==========

    @Test
    public void getMaterialsByType_returnsMaterialsOfType() {
        List<MaterialEntity> entities = Collections.singletonList(
                createTestEntity(TEST_MATERIAL_ID, MaterialType.READING)
        );
        when(mockDao.getByType(MaterialType.READING)).thenReturn(entities);

        List<Material> result = repository.getMaterialsByType(MaterialType.READING);

        assertEquals(1, result.size());
        assertEquals(MaterialType.READING, result.get(0).getType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getMaterialsByType_nullType_throwsException() {
        repository.getMaterialsByType(null);
    }

    // ========== getMaterialsLiveData tests ==========

    @Test
    public void getMaterialsLiveData_returnsNonNullLiveData() {
        LiveData<List<Material>> liveData = repository.getMaterialsLiveData();

        assertNotNull(liveData);
    }

    @Test
    public void getMaterialsLiveData_initialValue_isEmpty() {
        LiveData<List<Material>> liveData = repository.getMaterialsLiveData();

        assertNotNull(liveData.getValue());
        assertTrue(liveData.getValue().isEmpty());
    }

    // ========== saveMaterial tests ==========

    @Test
    public void saveMaterial_insertsIntoDao() {
        Material material = createTestDomainMaterial(TEST_MATERIAL_ID);

        repository.saveMaterial(material);

        ArgumentCaptor<MaterialEntity> captor = ArgumentCaptor.forClass(MaterialEntity.class);
        verify(mockDao).insert(captor.capture());
        assertEquals(TEST_MATERIAL_ID, captor.getValue().getId());
    }

    @Test
    public void saveMaterial_refreshesLiveData() {
        Material material = createTestDomainMaterial(TEST_MATERIAL_ID);
        List<MaterialEntity> entities = Collections.singletonList(
                createTestEntity(TEST_MATERIAL_ID)
        );
        when(mockDao.getAll()).thenReturn(entities);

        repository.saveMaterial(material);

        List<Material> liveDataValue = repository.getMaterialsLiveData().getValue();
        assertNotNull(liveDataValue);
        assertEquals(1, liveDataValue.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveMaterial_nullMaterial_throwsException() {
        repository.saveMaterial(null);
    }

    // ========== saveMaterials tests ==========

    @Test
    public void saveMaterials_insertsAllIntoDao() {
        List<Material> materials = Arrays.asList(
                createTestDomainMaterial("mat1"),
                createTestDomainMaterial("mat2")
        );

        repository.saveMaterials(materials);

        verify(mockDao).insertAll(anyList());
    }

    @Test
    public void saveMaterials_emptyList_doesNotInsert() {
        repository.saveMaterials(new ArrayList<>());

        verify(mockDao, never()).insertAll(anyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveMaterials_nullList_throwsException() {
        repository.saveMaterials(null);
    }

    // ========== deleteMaterial tests ==========

    @Test
    public void deleteMaterial_deletesFromDao() {
        repository.deleteMaterial(TEST_MATERIAL_ID);

        verify(mockDao).deleteById(TEST_MATERIAL_ID);
    }

    @Test
    public void deleteMaterial_deletesAttachments() {
        repository.deleteMaterial(TEST_MATERIAL_ID);

        verify(mockFileStorageManager).deleteAttachmentsForMaterial(TEST_MATERIAL_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteMaterial_nullId_throwsException() {
        repository.deleteMaterial(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteMaterial_emptyId_throwsException() {
        repository.deleteMaterial("");
    }

    // ========== deleteAllMaterials tests ==========

    @Test
    public void deleteAllMaterials_deletesAllFromDao() {
        repository.deleteAllMaterials();

        verify(mockDao).deleteAll();
    }

    @Test
    public void deleteAllMaterials_clearsAllAttachments() {
        repository.deleteAllMaterials();

        verify(mockFileStorageManager).clearAllAttachments();
    }

    // ========== getMaterialCount tests ==========

    @Test
    public void getMaterialCount_returnsCount() {
        when(mockDao.getCount()).thenReturn(5);

        int count = repository.getMaterialCount();

        assertEquals(5, count);
    }

    // ========== setMaterialAvailableCallback tests ==========

    @Test
    public void setMaterialAvailableCallback_setsCallback() {
        MaterialRepository.MaterialAvailableCallback callback =
                () -> { };

        repository.setMaterialAvailableCallback(callback);

        // No exception means success
    }

    @Test
    public void setMaterialAvailableCallback_nullCallback_doesNotThrow() {
        repository.setMaterialAvailableCallback(null);

        // No exception means success
    }

    // ========== syncMaterials tests ==========

    @Test
    public void syncMaterials_setsAndClearsSyncingFlag() {
        assertFalse(repository.isSyncing());

        repository.syncMaterials(TEST_DEVICE_ID);

        // After sync completes, syncing should be false
        assertFalse(repository.isSyncing());
    }

    @Test(expected = IllegalArgumentException.class)
    public void syncMaterials_nullDeviceId_throwsException() {
        repository.syncMaterials(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void syncMaterials_emptyDeviceId_throwsException() {
        repository.syncMaterials("");
    }

    @Test
    public void syncMaterials_notifiesCallback() {
        final boolean[] callbackCalled = {false};
        repository.setMaterialAvailableCallback(() -> callbackCalled[0] = true);

        repository.syncMaterials(TEST_DEVICE_ID);

        assertTrue(callbackCalled[0]);
    }

    // ========== isSyncing tests ==========

    @Test
    public void isSyncing_initiallyFalse() {
        assertFalse(repository.isSyncing());
    }

    // ========== notifyMaterialsAvailable tests ==========

    @Test
    public void notifyMaterialsAvailable_callsCallback() {
        final boolean[] callbackCalled = {false};
        repository.setMaterialAvailableCallback(() -> callbackCalled[0] = true);

        repository.notifyMaterialsAvailable();

        assertTrue(callbackCalled[0]);
    }

    @Test
    public void notifyMaterialsAvailable_noCallback_doesNotThrow() {
        repository.setMaterialAvailableCallback(null);

        repository.notifyMaterialsAvailable();

        // No exception means success
    }

    // ========== Thread safety tests ==========

    @Test
    public void concurrentSaves_areThreadSafe() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                repository.saveMaterial(createTestDomainMaterial("material-" + index));
                latch.countDown();
            }).start();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void concurrentDeletes_areThreadSafe() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                repository.deleteMaterial("material-" + index);
                latch.countDown();
            }).start();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // ========== Helper methods ==========

    private MaterialEntity createTestEntity(String id) {
        return createTestEntity(id, MaterialType.READING);
    }

    private MaterialEntity createTestEntity(String id, MaterialType type) {
        return new MaterialEntity(
                id,
                type,
                "Test Title",
                "Test content",
                "{}",
                "[]",
                System.currentTimeMillis()
        );
    }

    private Material createTestDomainMaterial(String id) {
        return new Material(
                id,
                MaterialType.READING,
                "Test Title",
                "Test content",
                "{}",
                "[]",
                System.currentTimeMillis()
        );
    }
}
