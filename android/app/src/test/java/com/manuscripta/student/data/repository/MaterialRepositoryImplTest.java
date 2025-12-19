package com.manuscripta.student.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;

import com.manuscripta.student.data.local.MaterialDao;
import com.manuscripta.student.data.model.MaterialEntity;
import com.manuscripta.student.data.model.MaterialType;
import com.manuscripta.student.domain.model.Material;
import com.manuscripta.student.network.tcp.TcpProtocolException;
import com.manuscripta.student.network.tcp.TcpSocketManager;
import com.manuscripta.student.network.tcp.message.DistributeAckMessage;
import com.manuscripta.student.utils.FileStorageManager;

import java.io.IOException;

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

    @Mock
    private TcpSocketManager mockTcpSocketManager;

    private MaterialRepositoryImpl repository;
    private MaterialRepositoryImpl repositoryWithTcp;

    private static final String TEST_MATERIAL_ID = "test-material-123";
    private static final String TEST_DEVICE_ID = "test-device-456";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockDao.getAll()).thenReturn(new ArrayList<>());
        // Repository without TcpSocketManager (for backward compatibility tests)
        repository = new MaterialRepositoryImpl(mockDao, mockFileStorageManager, null);
        // Repository with TcpSocketManager (for ACK tests)
        repositoryWithTcp = new MaterialRepositoryImpl(mockDao, mockFileStorageManager,
                mockTcpSocketManager);
    }

    // ========== Constructor tests ==========

    @Test
    public void testConstructor_createsInstance() {
        assertNotNull(repository);
    }

    @Test
    public void testConstructor_withTcpSocketManager_createsInstance() {
        assertNotNull(repositoryWithTcp);
    }

    @Test
    public void testConstructor_nullDao_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new MaterialRepositoryImpl(null, mockFileStorageManager, null));
    }

    @Test
    public void testConstructor_nullFileStorageManager_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new MaterialRepositoryImpl(mockDao, null, null));
    }

    @Test
    public void testConstructor_nullTcpSocketManager_doesNotThrow() {
        MaterialRepositoryImpl repo = new MaterialRepositoryImpl(mockDao, mockFileStorageManager,
                null);
        assertNotNull(repo);
    }

    // ========== getMaterialById tests ==========

    @Test
    public void testGetMaterialById_existingMaterial_returnsMaterial() {
        MaterialEntity entity = createTestEntity(TEST_MATERIAL_ID);
        when(mockDao.getById(TEST_MATERIAL_ID)).thenReturn(entity);

        Material result = repository.getMaterialById(TEST_MATERIAL_ID);

        assertNotNull(result);
        assertEquals(TEST_MATERIAL_ID, result.getId());
    }

    @Test
    public void testGetMaterialById_nonExistingMaterial_returnsNull() {
        when(mockDao.getById(TEST_MATERIAL_ID)).thenReturn(null);

        Material result = repository.getMaterialById(TEST_MATERIAL_ID);

        assertNull(result);
    }

    @Test
    public void testGetMaterialById_nullId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> repository.getMaterialById(null));
    }

    @Test
    public void testGetMaterialById_emptyId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> repository.getMaterialById(""));
    }

    @Test
    public void testGetMaterialById_blankId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> repository.getMaterialById("   "));
    }

    // ========== getAllMaterials tests ==========

    @Test
    public void testGetAllMaterials_returnsMaterials() {
        List<MaterialEntity> entities = Arrays.asList(
                createTestEntity("mat1"),
                createTestEntity("mat2")
        );
        when(mockDao.getAll()).thenReturn(entities);

        List<Material> result = repository.getAllMaterials();

        assertEquals(2, result.size());
    }

    @Test
    public void testGetAllMaterials_emptyDatabase_returnsEmptyList() {
        when(mockDao.getAll()).thenReturn(new ArrayList<>());

        List<Material> result = repository.getAllMaterials();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== getMaterialsByType tests ==========

    @Test
    public void testGetMaterialsByType_returnsMaterialsOfType() {
        List<MaterialEntity> entities = Collections.singletonList(
                createTestEntity(TEST_MATERIAL_ID, MaterialType.READING)
        );
        when(mockDao.getByType(MaterialType.READING)).thenReturn(entities);

        List<Material> result = repository.getMaterialsByType(MaterialType.READING);

        assertEquals(1, result.size());
        assertEquals(MaterialType.READING, result.get(0).getType());
    }

    @Test
    public void testGetMaterialsByType_nullType_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> repository.getMaterialsByType(null));
    }

    // ========== getMaterialsLiveData tests ==========

    @Test
    public void testGetMaterialsLiveData_returnsNonNullLiveData() {
        LiveData<List<Material>> liveData = repository.getMaterialsLiveData();

        assertNotNull(liveData);
    }

    @Test
    public void testGetMaterialsLiveData_initialValue_isEmpty() {
        LiveData<List<Material>> liveData = repository.getMaterialsLiveData();

        assertNotNull(liveData.getValue());
        assertTrue(liveData.getValue().isEmpty());
    }

    // ========== saveMaterial tests ==========

    @Test
    public void testSaveMaterial_insertsIntoDao() {
        Material material = createTestDomainMaterial(TEST_MATERIAL_ID);

        repository.saveMaterial(material);

        ArgumentCaptor<MaterialEntity> captor = ArgumentCaptor.forClass(MaterialEntity.class);
        verify(mockDao).insert(captor.capture());
        assertEquals(TEST_MATERIAL_ID, captor.getValue().getId());
    }

    @Test
    public void testSaveMaterial_refreshesLiveData() {
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

    @Test
    public void testSaveMaterial_nullMaterial_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> repository.saveMaterial(null));
    }

    // ========== saveMaterials tests ==========

    @Test
    public void testSaveMaterials_insertsAllIntoDao() {
        List<Material> materials = Arrays.asList(
                createTestDomainMaterial("mat1"),
                createTestDomainMaterial("mat2")
        );

        repository.saveMaterials(materials);

        verify(mockDao).insertAll(anyList());
    }

    @Test
    public void testSaveMaterials_emptyList_doesNotInsert() {
        repository.saveMaterials(new ArrayList<>());

        // Empty list should return early without calling insertAll
        verify(mockDao, never()).insertAll(anyList());
    }

    @Test
    public void testSaveMaterials_nullList_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> repository.saveMaterials(null));
    }

    @Test
    public void testSaveMaterials_nullElementInList_throwsException() {
        List<Material> materials = new ArrayList<>();
        materials.add(createTestDomainMaterial("mat1"));
        materials.add(null);
        materials.add(createTestDomainMaterial("mat3"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> repository.saveMaterials(materials));
        assertTrue(exception.getMessage().contains("null at index 1"));
    }

    // ========== deleteMaterial tests ==========

    @Test
    public void testDeleteMaterial_deletesFromDao() {
        repository.deleteMaterial(TEST_MATERIAL_ID);

        verify(mockDao).deleteById(TEST_MATERIAL_ID);
    }

    @Test
    public void testDeleteMaterial_deletesAttachments() {
        repository.deleteMaterial(TEST_MATERIAL_ID);

        verify(mockFileStorageManager).deleteAttachmentsForMaterial(TEST_MATERIAL_ID);
    }

    @Test
    public void testDeleteMaterial_nullId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> repository.deleteMaterial(null));
    }

    @Test
    public void testDeleteMaterial_emptyId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> repository.deleteMaterial(""));
    }

    @Test
    public void testDeleteMaterial_blankId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> repository.deleteMaterial("   "));
    }

    // ========== deleteAllMaterials tests ==========

    @Test
    public void testDeleteAllMaterials_deletesAllFromDao() {
        repository.deleteAllMaterials();

        verify(mockDao).deleteAll();
    }

    @Test
    public void testDeleteAllMaterials_clearsAllAttachments() {
        repository.deleteAllMaterials();

        verify(mockFileStorageManager).clearAllAttachments();
    }

    // ========== getMaterialCount tests ==========

    @Test
    public void testGetMaterialCount_returnsCount() {
        when(mockDao.getCount()).thenReturn(5);

        int count = repository.getMaterialCount();

        assertEquals(5, count);
    }

    // ========== setMaterialAvailableCallback tests ==========

    @Test
    public void testSetMaterialAvailableCallback_setsCallback() {
        MaterialRepository.MaterialAvailableCallback callback =
                () -> { };

        repository.setMaterialAvailableCallback(callback);

        // No exception means success
    }

    @Test
    public void testSetMaterialAvailableCallback_nullCallback_doesNotThrow() {
        repository.setMaterialAvailableCallback(null);

        // No exception means success
    }

    // ========== syncMaterials tests ==========

    @Test
    public void testSyncMaterials_setsAndClearsSyncingFlag() {
        assertFalse(repository.isSyncing());

        repository.syncMaterials(TEST_DEVICE_ID);

        // After sync completes, syncing should be false
        assertFalse(repository.isSyncing());
    }

    @Test
    public void testSyncMaterials_nullDeviceId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> repository.syncMaterials(null));
    }

    @Test
    public void testSyncMaterials_emptyDeviceId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> repository.syncMaterials(""));
    }

    @Test
    public void testSyncMaterials_blankDeviceId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> repository.syncMaterials("   "));
    }

    @Test
    public void testSyncMaterials_notifiesCallback() {
        final boolean[] callbackCalled = {false};
        repository.setMaterialAvailableCallback(() -> callbackCalled[0] = true);

        repository.syncMaterials(TEST_DEVICE_ID);

        assertTrue(callbackCalled[0]);
    }

    // ========== isSyncing tests ==========

    @Test
    public void testIsSyncing_initiallyFalse() {
        assertFalse(repository.isSyncing());
    }

    // ========== notifyMaterialsAvailable tests ==========

    @Test
    public void testNotifyMaterialsAvailable_callsCallback() {
        final boolean[] callbackCalled = {false};
        repository.setMaterialAvailableCallback(() -> callbackCalled[0] = true);

        repository.notifyMaterialsAvailable();

        assertTrue(callbackCalled[0]);
    }

    @Test
    public void testNotifyMaterialsAvailable_noCallback_doesNotThrow() {
        repository.setMaterialAvailableCallback(null);

        repository.notifyMaterialsAvailable();

        // No exception means success
    }

    // ========== DISTRIBUTE_ACK tests ==========
    // Note: Per API Contract §3.6.1, DISTRIBUTE_ACK should only be sent after
    // successfully fetching and storing materials via HTTP. Since HTTP fetch
    // is not yet implemented, syncMaterials does NOT send the ACK.

    @Test
    public void testSyncMaterials_doesNotSendAckUntilHttpImplemented()
            throws IOException, TcpProtocolException {
        // Per API Contract §3.6.1, ACK should only be sent after successful HTTP fetch
        // Since HTTP is not implemented, no ACK should be sent
        repositoryWithTcp.syncMaterials(TEST_DEVICE_ID);

        verify(mockTcpSocketManager, never()).send(any(DistributeAckMessage.class));
    }

    @Test
    public void testSyncMaterials_withoutTcpSocketManager_doesNotThrow() {
        // repository has null TcpSocketManager
        repository.syncMaterials(TEST_DEVICE_ID);

        // No exception means success
    }

    // ========== Thread safety tests ==========

    @Test
    public void testConcurrentSaves_areThreadSafe() throws InterruptedException {
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

        // Verify all saves were actually called
        verify(mockDao, times(threadCount)).insert(any(MaterialEntity.class));
    }

    @Test
    public void testConcurrentDeletes_areThreadSafe() throws InterruptedException {
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

        // Verify all deletes were actually called
        verify(mockDao, times(threadCount)).deleteById(any(String.class));
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
