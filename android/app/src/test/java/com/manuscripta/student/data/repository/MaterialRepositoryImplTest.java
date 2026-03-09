package com.manuscripta.student.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;

import com.manuscripta.student.data.local.MaterialDao;
import com.manuscripta.student.data.local.QuestionDao;
import com.manuscripta.student.data.model.MaterialEntity;
import com.manuscripta.student.data.model.MaterialType;
import com.manuscripta.student.domain.model.Material;
import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.network.dto.DistributionBundleDto;
import com.manuscripta.student.network.dto.MaterialDto;
import com.manuscripta.student.network.tcp.AckRetrySender;
import com.manuscripta.student.network.tcp.TcpSocketManager;
import com.manuscripta.student.network.tcp.message.DistributeAckMessage;
import com.manuscripta.student.utils.FileStorageManager;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

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
    private QuestionDao mockQuestionDao;

    @Mock
    private FileStorageManager mockFileStorageManager;

    @Mock
    private ApiService mockApiService;

    @Mock
    private TcpSocketManager mockTcpSocketManager;

    @Mock
    private AckRetrySender mockAckRetrySender;

    @Mock
    private SessionRepository mockSessionRepository;

    @Mock
    private Call<DistributionBundleDto> mockDistributionCall;

    @Mock
    private Call<ResponseBody> mockAttachmentCall;

    private MaterialRepositoryImpl repository;

    private static final String TEST_MATERIAL_ID = "test-material-123";
    private static final String TEST_DEVICE_ID = "test-device-456";
    private static final String TEST_ATTACHMENT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String CONTENT_WITH_ATTACHMENT =
            "Content /attachments/" + TEST_ATTACHMENT_ID;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockDao.getAll()).thenReturn(new ArrayList<>());
        repository = new MaterialRepositoryImpl(mockDao, mockQuestionDao, mockFileStorageManager,
                mockApiService, mockTcpSocketManager, mockAckRetrySender, mockSessionRepository);
    }

    // ========== Constructor tests ==========

    @Test
    public void testConstructor_createsInstance() {
        assertNotNull(repository);
    }

    @Test
    public void testConstructor_nullDao_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new MaterialRepositoryImpl(null, mockQuestionDao, mockFileStorageManager,
                        mockApiService, mockTcpSocketManager, mockAckRetrySender,
                        mockSessionRepository));
    }

    @Test
    public void testConstructor_nullQuestionDao_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new MaterialRepositoryImpl(mockDao, null, mockFileStorageManager,
                        mockApiService, mockTcpSocketManager, mockAckRetrySender,
                        mockSessionRepository));
    }

    @Test
    public void testConstructor_nullFileStorageManager_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new MaterialRepositoryImpl(mockDao, mockQuestionDao, null,
                        mockApiService, mockTcpSocketManager, mockAckRetrySender,
                        mockSessionRepository));
    }

    @Test
    public void testConstructor_nullApiService_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new MaterialRepositoryImpl(mockDao, mockQuestionDao, mockFileStorageManager,
                        null, mockTcpSocketManager, mockAckRetrySender,
                        mockSessionRepository));
    }

    @Test
    public void testConstructor_nullTcpSocketManager_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new MaterialRepositoryImpl(mockDao, mockQuestionDao, mockFileStorageManager,
                        mockApiService, null, mockAckRetrySender,
                        mockSessionRepository));
    }

    @Test
    public void testConstructor_nullAckRetrySender_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new MaterialRepositoryImpl(mockDao, mockQuestionDao, mockFileStorageManager,
                        mockApiService, mockTcpSocketManager, null,
                        mockSessionRepository));
    }

    @Test
    public void testConstructor_nullSessionRepository_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new MaterialRepositoryImpl(mockDao, mockQuestionDao, mockFileStorageManager,
                        mockApiService, mockTcpSocketManager, mockAckRetrySender, null));
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
        doReturn(entities).when(mockDao).getByType(MaterialType.READING);

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

        verify(mockDao, never()).insertAll(anyList());
    }

    @Test
    public void testSaveMaterials_nullList_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> repository.saveMaterials(null));
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
    public void testSyncMaterials_setsAndClearsSyncingFlag() throws IOException {
        DistributionBundleDto emptyBundle = new DistributionBundleDto();
        when(mockApiService.getDistribution(TEST_DEVICE_ID)).thenReturn(mockDistributionCall);
        when(mockDistributionCall.execute()).thenReturn(Response.success(emptyBundle));

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
    public void testSyncMaterials_notifiesCallback() throws IOException {
        MaterialDto dto = new MaterialDto("mat-1", "READING", "Title", null, null, null, 0L);
        DistributionBundleDto bundle = new DistributionBundleDto(
                Collections.singletonList(dto), Collections.emptyList());
        when(mockApiService.getDistribution(TEST_DEVICE_ID)).thenReturn(mockDistributionCall);
        when(mockDistributionCall.execute()).thenReturn(Response.success(bundle));

        final boolean[] callbackCalled = {false};
        repository.setMaterialAvailableCallback(() -> callbackCalled[0] = true);

        repository.syncMaterials(TEST_DEVICE_ID);

        assertTrue(callbackCalled[0]);
    }

    @Test
    public void testSyncMaterials_httpFailure_doesNotNotifyCallback() throws IOException {
        when(mockApiService.getDistribution(TEST_DEVICE_ID)).thenReturn(mockDistributionCall);
        when(mockDistributionCall.execute()).thenReturn(Response.error(500,
                okhttp3.ResponseBody.create(null, "")));

        final boolean[] callbackCalled = {false};
        repository.setMaterialAvailableCallback(() -> callbackCalled[0] = true);

        repository.syncMaterials(TEST_DEVICE_ID);

        assertFalse(callbackCalled[0]);
    }

    @Test
    public void testSyncMaterials_emptyBundle_doesNotNotifyCallback() throws IOException {
        DistributionBundleDto emptyBundle = new DistributionBundleDto();
        when(mockApiService.getDistribution(TEST_DEVICE_ID)).thenReturn(mockDistributionCall);
        when(mockDistributionCall.execute()).thenReturn(Response.success(emptyBundle));

        final boolean[] callbackCalled = {false};
        repository.setMaterialAvailableCallback(() -> callbackCalled[0] = true);

        repository.syncMaterials(TEST_DEVICE_ID);

        assertFalse(callbackCalled[0]);
    }

    @Test
    public void testSyncMaterials_ioException_doesNotNotifyCallback() throws IOException {
        when(mockApiService.getDistribution(TEST_DEVICE_ID)).thenReturn(mockDistributionCall);
        when(mockDistributionCall.execute()).thenThrow(new IOException("Network error"));

        final boolean[] callbackCalled = {false};
        repository.setMaterialAvailableCallback(() -> callbackCalled[0] = true);

        repository.syncMaterials(TEST_DEVICE_ID);

        assertFalse(callbackCalled[0]);
    }

    // ========== DISTRIBUTE_ACK delegation tests ==========

    @Test
    public void testSyncMaterials_delegatesAckToRetrySender() throws Exception {
        MaterialDto dto = new MaterialDto("mat-1", "READING", "Title", null, null, null, 0L);
        DistributionBundleDto bundle = new DistributionBundleDto(
                Collections.singletonList(dto), Collections.emptyList());
        when(mockApiService.getDistribution(TEST_DEVICE_ID)).thenReturn(mockDistributionCall);
        when(mockDistributionCall.execute()).thenReturn(Response.success(bundle));

        repository.syncMaterials(TEST_DEVICE_ID);

        verify(mockAckRetrySender).send(any(DistributeAckMessage.class), anyString());
    }

    @Test
    public void testSyncMaterials_callbackStillFiresRegardlessOfAck() throws Exception {
        MaterialDto dto = new MaterialDto("mat-1", "READING", "Title", null, null, null, 0L);
        DistributionBundleDto bundle = new DistributionBundleDto(
                Collections.singletonList(dto), Collections.emptyList());
        when(mockApiService.getDistribution(TEST_DEVICE_ID)).thenReturn(mockDistributionCall);
        when(mockDistributionCall.execute()).thenReturn(Response.success(bundle));

        final boolean[] callbackCalled = {false};
        repository.setMaterialAvailableCallback(() -> callbackCalled[0] = true);

        repository.syncMaterials(TEST_DEVICE_ID);

        // Materials still saved and callback fires
        verify(mockDao).insert(any(MaterialEntity.class));
        assertTrue(callbackCalled[0]);
    }

    @Test
    public void testSyncMaterials_multipleMaterials_acksEachIndividually() throws Exception {
        MaterialDto dto1 = new MaterialDto("mat-1", "READING", "Title1", null, null, null, 0L);
        MaterialDto dto2 = new MaterialDto("mat-2", "READING", "Title2", null, null, null, 0L);
        DistributionBundleDto bundle = new DistributionBundleDto(
                Arrays.asList(dto1, dto2), Collections.emptyList());
        when(mockApiService.getDistribution(TEST_DEVICE_ID)).thenReturn(mockDistributionCall);
        when(mockDistributionCall.execute()).thenReturn(Response.success(bundle));

        repository.syncMaterials(TEST_DEVICE_ID);

        verify(mockAckRetrySender, times(2)).send(any(DistributeAckMessage.class), anyString());
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
        verify(mockDao, org.mockito.Mockito.times(threadCount))
                .insert(any(MaterialEntity.class));
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
        verify(mockDao, org.mockito.Mockito.times(threadCount)).deleteById(any(String.class));
        verify(mockFileStorageManager, org.mockito.Mockito.times(threadCount))
                .deleteAttachmentsForMaterial(any(String.class));
    }

    // ========== Null element validation tests ==========

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

    // ========== Concurrent sync prevention ==========

    /**
     * Verifies that a second concurrent syncMaterials call is
     * ignored when a sync is already in progress.
     *
     * @throws IOException if mock setup fails
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSyncMaterials_concurrentCall_ignored()
            throws IOException {
        // First call will take a while (simulate)
        DistributionBundleDto emptyBundle =
                new DistributionBundleDto();
        when(mockApiService.getDistribution(TEST_DEVICE_ID))
                .thenReturn(mockDistributionCall);
        when(mockDistributionCall.execute())
                .thenReturn(Response.success(emptyBundle));

        repository.syncMaterials(TEST_DEVICE_ID);

        // Syncing just completed (flag reset), call again to
        // ensure it works after the first one finishes
        repository.syncMaterials(TEST_DEVICE_ID);

        // Two complete syncs should call execute twice
        verify(mockDistributionCall, times(2)).execute();
    }

    // ========== Attachment download tests ==========

    /**
     * Material content with attachment references triggers
     * download and ACK is sent when all succeed.
     *
     * @throws IOException if mock setup fails
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSyncMaterials_withAttachments_downloadsAndAcks()
            throws IOException {
        // Material content containing an attachment reference
        String attId =
                "550e8400-e29b-41d4-a716-446655440000";
        String content =
                "![img](/attachments/" + attId + ")";
        MaterialDto dto = new MaterialDto(
                "mat-1", "READING", "Title", content,
                null, null, 0L);
        DistributionBundleDto bundle =
                new DistributionBundleDto(
                        Collections.singletonList(dto),
                        Collections.emptyList());
        when(mockApiService.getDistribution(TEST_DEVICE_ID))
                .thenReturn(mockDistributionCall);
        when(mockDistributionCall.execute())
                .thenReturn(Response.success(bundle));

        // Mock attachment download
        byte[] imageBytes = new byte[]{1, 2, 3};
        ResponseBody attachBody = ResponseBody.create(
                MediaType.parse("image/png"), imageBytes);
        when(mockApiService.getAttachment(attId))
                .thenReturn(mockAttachmentCall);
        when(mockAttachmentCall.execute())
                .thenReturn(Response.success(attachBody));

        repository.syncMaterials(TEST_DEVICE_ID);

        verify(mockFileStorageManager).saveAttachment(
                eq("mat-1"), eq(attId),
                eq("png"), eq(imageBytes));
        verify(mockAckRetrySender).send(
                any(DistributeAckMessage.class),
                anyString());
    }

    /**
     * When attachment download fails, ACK is still sent.
     *
     * @throws IOException if mock setup fails
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSyncMaterials_attachmentFails_stillSendsAck()
            throws IOException {
        String attId =
                "660e8400-e29b-41d4-a716-446655440001";
        String content =
                "![img](/attachments/" + attId + ")";
        MaterialDto dto = new MaterialDto(
                "mat-1", "READING", "Title", content,
                null, null, 0L);
        DistributionBundleDto bundle =
                new DistributionBundleDto(
                        Collections.singletonList(dto),
                        Collections.emptyList());
        when(mockApiService.getDistribution(TEST_DEVICE_ID))
                .thenReturn(mockDistributionCall);
        when(mockDistributionCall.execute())
                .thenReturn(Response.success(bundle));

        // Attachment returns 404
        when(mockApiService.getAttachment(attId))
                .thenReturn(mockAttachmentCall);
        when(mockAttachmentCall.execute())
                .thenReturn(Response.error(404,
                        ResponseBody.create(null, "")));

        repository.syncMaterials(TEST_DEVICE_ID);

        // Material is still saved to DB
        verify(mockDao).insert(any(MaterialEntity.class));
        // ACK is sent despite attachment failure
        verify(mockAckRetrySender).send(
                any(DistributeAckMessage.class),
                anyString());
    }

    /**
     * When attachment download throws IOException, ACK is still sent.
     *
     * @throws IOException if mock setup fails
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSyncMaterials_attachmentIOException_stillSendsAck()
            throws IOException {
        String attId =
                "770e8400-e29b-41d4-a716-446655440002";
        String content =
                "![img](/attachments/" + attId + ")";
        MaterialDto dto = new MaterialDto(
                "mat-1", "READING", "Title", content,
                null, null, 0L);
        DistributionBundleDto bundle =
                new DistributionBundleDto(
                        Collections.singletonList(dto),
                        Collections.emptyList());
        when(mockApiService.getDistribution(TEST_DEVICE_ID))
                .thenReturn(mockDistributionCall);
        when(mockDistributionCall.execute())
                .thenReturn(Response.success(bundle));

        when(mockApiService.getAttachment(attId))
                .thenReturn(mockAttachmentCall);
        when(mockAttachmentCall.execute())
                .thenThrow(new IOException("timeout"));

        repository.syncMaterials(TEST_DEVICE_ID);

        verify(mockDao).insert(any(MaterialEntity.class));
        verify(mockAckRetrySender).send(
                any(DistributeAckMessage.class),
                anyString());
    }

    /**
     * Attachment with null content type uses "bin" extension.
     *
     * @throws IOException if mock setup fails
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSyncMaterials_attachmentNullContentType()
            throws IOException {
        String attId =
                "880e8400-e29b-41d4-a716-446655440003";
        String content =
                "![img](/attachments/" + attId + ")";
        MaterialDto dto = new MaterialDto(
                "mat-1", "READING", "Title", content,
                null, null, 0L);
        DistributionBundleDto bundle =
                new DistributionBundleDto(
                        Collections.singletonList(dto),
                        Collections.emptyList());
        when(mockApiService.getDistribution(TEST_DEVICE_ID))
                .thenReturn(mockDistributionCall);
        when(mockDistributionCall.execute())
                .thenReturn(Response.success(bundle));

        // Create ResponseBody with null content type
        byte[] bytes = new byte[]{1};
        ResponseBody body = ResponseBody.create(null, bytes);
        when(mockApiService.getAttachment(attId))
                .thenReturn(mockAttachmentCall);
        when(mockAttachmentCall.execute())
                .thenReturn(Response.success(body));

        repository.syncMaterials(TEST_DEVICE_ID);

        verify(mockFileStorageManager).saveAttachment(
                eq("mat-1"), eq(attId),
                eq("bin"), any(byte[].class));
    }

    /**
     * Successful response with null body returns failure.
     *
     * @throws IOException if mock setup fails
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSyncMaterials_nullResponseBody_doesNotCrash()
            throws IOException {
        when(mockApiService.getDistribution(TEST_DEVICE_ID))
                .thenReturn(mockDistributionCall);
        when(mockDistributionCall.execute())
                .thenReturn(Response.success(null));

        repository.syncMaterials(TEST_DEVICE_ID);

        // Should return early without processing
        verify(mockDao, never()).insert(
                any(MaterialEntity.class));
    }

    /**
     * Unexpected runtime exception during sync does not crash
     * and resets the syncing flag.
     *
     * @throws IOException if mock setup fails
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSyncMaterials_unexpectedException_resetsSyncing()
            throws IOException {
        when(mockApiService.getDistribution(TEST_DEVICE_ID))
                .thenReturn(mockDistributionCall);
        when(mockDistributionCall.execute())
                .thenThrow(new RuntimeException("DB error"));

        repository.syncMaterials(TEST_DEVICE_ID);

        assertFalse(repository.isSyncing());
    }

    /**
     * Callback that throws runtime exception does not
     * propagate to the caller.
     *
     * @throws IOException if mock setup fails
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSyncMaterials_callbackThrows_doesNotPropagate()
            throws IOException {
        MaterialDto dto = new MaterialDto(
                "mat-1", "READING", "Title", null,
                null, null, 0L);
        DistributionBundleDto bundle =
                new DistributionBundleDto(
                        Collections.singletonList(dto),
                        Collections.emptyList());
        when(mockApiService.getDistribution(TEST_DEVICE_ID))
                .thenReturn(mockDistributionCall);
        when(mockDistributionCall.execute())
                .thenReturn(Response.success(bundle));

        repository.setMaterialAvailableCallback(
                () -> {
                    throw new RuntimeException("fail");
                });

        // Should not throw
        repository.syncMaterials(TEST_DEVICE_ID);

        assertFalse(repository.isSyncing());
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
