package com.manuscripta.student.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.manuscripta.student.data.local.MaterialDao;
import com.manuscripta.student.data.model.MaterialEntity;
import com.manuscripta.student.data.model.MaterialType;
import com.manuscripta.student.domain.mapper.MaterialMapper;
import com.manuscripta.student.domain.model.Material;
import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.network.dto.DistributionBundleDto;
import com.manuscripta.student.network.dto.MaterialDto;
import com.manuscripta.student.network.tcp.AckRetrySender;
import com.manuscripta.student.network.tcp.PairingManager;
import com.manuscripta.student.network.tcp.TcpMessage;
import com.manuscripta.student.network.tcp.TcpMessageListenerAdapter;
import com.manuscripta.student.network.tcp.TcpOpcode;
import com.manuscripta.student.network.tcp.TcpSocketManager;
import com.manuscripta.student.network.tcp.message.DistributeAckMessage;
import com.manuscripta.student.utils.ContentParser;
import com.manuscripta.student.utils.FileStorageManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Implementation of {@link MaterialRepository} that manages educational materials
 * with local persistence and network synchronization.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Local persistence via Room DAO</li>
 *   <li>Observable material state via LiveData for UI updates</li>
 *   <li>Integration with FileStorageManager for attachment file storage</li>
 *   <li>Callback interface for TCP DISTRIBUTE_MATERIAL signal handling</li>
 *   <li>Thread-safe operations</li>
 * </ul>
 *
 */
@Singleton
public class MaterialRepositoryImpl implements MaterialRepository {

    /** Tag for logging. */
    private static final String TAG = "MaterialRepository";

    /** The DAO for material persistence. */
    private final MaterialDao materialDao;

    /** The file storage manager for attachments. */
    private final FileStorageManager fileStorageManager;

    /** The API service for network operations. */
    private final ApiService apiService;

    /** The TCP socket manager for receiving DISTRIBUTE_MATERIAL signals. */
    private final TcpSocketManager tcpSocketManager;

    /** Handles retry logic for sending ACK messages over TCP. */
    private final AckRetrySender ackRetrySender;

    /** The pairing manager for retrieving the current device ID. */
    private final PairingManager pairingManager;

    /** Executor for running sync operations on a background thread. */
    private final ExecutorService syncExecutor = Executors.newSingleThreadExecutor();

    /** Lock object guarding database writes and LiveData updates. */
    private final Object lock = new Object();

    /** LiveData for observable material list. */
    private final MutableLiveData<List<Material>> materialsLiveData;

    /** Flag indicating if a sync operation is in progress. */
    private final AtomicBoolean syncing = new AtomicBoolean(false);

    /** Callback for material availability notifications. */
    @Nullable
    private volatile MaterialAvailableCallback materialAvailableCallback;

    /**
     * Creates a new MaterialRepositoryImpl with the given dependencies.
     *
     * <p>Note: Null checks are performed despite @NonNull annotations because annotations
     * are only compile-time hints in Java and do not prevent null values at runtime.
     * This defensive approach ensures fail-fast behaviour for invalid constructor calls.</p>
     *
     * <p>The constructor calls {@link #refreshMaterialsLiveData()} to initialize LiveData
     * with existing materials. This is safe because all fields are assigned before the call
     * and the DAO read operation has no side effects.</p>
     *
     * @param materialDao        The DAO for material persistence
     * @param fileStorageManager The file storage manager for attachments
     * @param apiService         The API service for network operations
     * @param tcpSocketManager   The TCP socket manager for DISTRIBUTE_MATERIAL signals
     * @param ackRetrySender     The retry sender for ACK messages
     * @param pairingManager     The pairing manager for retrieving the current device ID
     * @throws IllegalArgumentException if any dependency is null
     */
    @Inject
    public MaterialRepositoryImpl(@NonNull MaterialDao materialDao,
                                  @NonNull FileStorageManager fileStorageManager,
                                  @NonNull ApiService apiService,
                                  @NonNull TcpSocketManager tcpSocketManager,
                                  @NonNull AckRetrySender ackRetrySender,
                                  @NonNull PairingManager pairingManager) {
        if (materialDao == null) {
            throw new IllegalArgumentException("MaterialDao cannot be null");
        }
        if (fileStorageManager == null) {
            throw new IllegalArgumentException("FileStorageManager cannot be null");
        }
        if (apiService == null) {
            throw new IllegalArgumentException("ApiService cannot be null");
        }
        if (tcpSocketManager == null) {
            throw new IllegalArgumentException("TcpSocketManager cannot be null");
        }
        if (ackRetrySender == null) {
            throw new IllegalArgumentException("AckRetrySender cannot be null");
        }
        if (pairingManager == null) {
            throw new IllegalArgumentException("PairingManager cannot be null");
        }
        this.materialDao = materialDao;
        this.fileStorageManager = fileStorageManager;
        this.apiService = apiService;
        this.tcpSocketManager = tcpSocketManager;
        this.ackRetrySender = ackRetrySender;
        this.pairingManager = pairingManager;
        this.materialsLiveData = new MutableLiveData<>(new ArrayList<>());

        // Register as a listener for DISTRIBUTE_MATERIAL signals from the TCP layer.
        // When the server signals that new materials are available, trigger a sync.
        tcpSocketManager.addMessageListener(new TcpMessageListenerAdapter() {
            @Override
            public void onMessageReceived(@NonNull TcpMessage message) {
                if (message.getOpcode() == TcpOpcode.DISTRIBUTE_MATERIAL) {
                    String deviceId = pairingManager.getDeviceId();
                    if (deviceId != null && !deviceId.trim().isEmpty()) {
                        Log.d(TAG, "DISTRIBUTE_MATERIAL signal received, triggering sync");
                        syncExecutor.execute(() -> syncMaterials(deviceId));
                    } else {
                        Log.w(TAG, "DISTRIBUTE_MATERIAL received but device ID not available");
                    }
                }
            }
        });

        // Initialize LiveData with existing materials from database on a background thread.
        // This avoids blocking the main thread during Hilt injection at app startup.
        syncExecutor.execute(this::refreshMaterialsLiveData);
    }

    @Override
    @Nullable
    public Material getMaterialById(@NonNull String materialId) {
        validateNotEmpty(materialId, "Material ID");

        MaterialEntity entity = materialDao.getById(materialId);
        if (entity == null) {
            return null;
        }
        return MaterialMapper.toDomain(entity);
    }

    @Override
    @NonNull
    public List<Material> getAllMaterials() {
        List<MaterialEntity> entities = materialDao.getAll();
        return mapEntitiesToDomain(entities);
    }

    @Override
    @NonNull
    public List<Material> getMaterialsByType(@NonNull MaterialType type) {
        if (type == null) {
            throw new IllegalArgumentException("Material type cannot be null");
        }

        List<MaterialEntity> entities = materialDao.getByType(type);
        return mapEntitiesToDomain(entities);
    }

    @Override
    @NonNull
    public LiveData<List<Material>> getMaterialsLiveData() {
        return materialsLiveData;
    }

    @Override
    public void saveMaterial(@NonNull Material material) {
        if (material == null) {
            throw new IllegalArgumentException("Material cannot be null");
        }

        synchronized (lock) {
            MaterialEntity entity = MaterialMapper.toEntity(material);
            materialDao.insert(entity);
            refreshMaterialsLiveData();

            Log.d(TAG, "Saved material: " + material.getId());
        }
    }

    @Override
    public void saveMaterials(@NonNull List<Material> materials) {
        if (materials == null) {
            throw new IllegalArgumentException("Materials list cannot be null");
        }

        if (materials.isEmpty()) {
            Log.d(TAG, "No materials to save, skipping insert");
            return;
        }

        synchronized (lock) {
            List<MaterialEntity> entities = new ArrayList<>(materials.size());
            int index = 0;
            for (Material material : materials) {
                if (material == null) {
                    throw new IllegalArgumentException(
                            "Materials list cannot contain null elements (null at index " + index + ")");
                }
                entities.add(MaterialMapper.toEntity(material));
                index++;
            }
            materialDao.insertAll(entities);
            refreshMaterialsLiveData();

            Log.d(TAG, "Saved " + materials.size() + " materials");
        }
    }

    @Override
    public void deleteMaterial(@NonNull String materialId) {
        validateNotEmpty(materialId, "Material ID");

        synchronized (lock) {
            // Delete associated attachment files first
            fileStorageManager.deleteAttachmentsForMaterial(materialId);

            // Then delete the material from database
            materialDao.deleteById(materialId);
            refreshMaterialsLiveData();

            Log.d(TAG, "Deleted material: " + materialId);
        }
    }

    @Override
    public void deleteAllMaterials() {
        synchronized (lock) {
            // Clear all attachment files first
            fileStorageManager.clearAllAttachments();

            // Then clear the database
            materialDao.deleteAll();
            refreshMaterialsLiveData();

            Log.d(TAG, "Deleted all materials");
        }
    }

    @Override
    public int getMaterialCount() {
        return materialDao.getCount();
    }

    @Override
    public void setMaterialAvailableCallback(@Nullable MaterialAvailableCallback callback) {
        this.materialAvailableCallback = callback;
    }

    @Override
    public void syncMaterials(@NonNull String deviceId) {
        validateNotEmpty(deviceId, "Device ID");

        if (!syncing.compareAndSet(false, true)) {
            Log.w(TAG, "Sync already in progress, ignoring request");
            return;
        }

        Log.d(TAG, "Starting material sync for device: " + deviceId);

        boolean syncSucceeded = false;

        try {
            // 1. HTTP GET /distribution/{deviceId} to fetch materials
            Response<DistributionBundleDto> response =
                    apiService.getDistribution(deviceId).execute();

            if (!response.isSuccessful() || response.body() == null) {
                Log.e(TAG, "Failed to fetch distribution bundle. HTTP " + response.code());
                return;
            }

            DistributionBundleDto bundle = response.body();
            List<MaterialDto> materialDtos = bundle.getMaterials();

            if (materialDtos == null || materialDtos.isEmpty()) {
                Log.i(TAG, "No materials in distribution bundle");
                return;
            }

            Log.i(TAG, "Received " + materialDtos.size() + " materials");

            // 2. Convert MaterialDtos to MaterialEntities, download attachments, and save
            for (MaterialDto dto : materialDtos) {
                try {
                    // 3. Parse content for attachment references and download each one.
                    // Done before acquiring the lock to avoid holding it during network I/O.
                    boolean attachmentsOk = true;
                    List<String> attachmentIds =
                            ContentParser.extractDistinctAttachmentReferences(dto.getContent());
                    for (String attachmentId : attachmentIds) {
                        if (!downloadAttachment(dto.getId(), attachmentId)) {
                            attachmentsOk = false;
                            Log.w(TAG, "Attachment download failed for material: "
                                    + dto.getId());
                        }
                    }

                    // Lock only for the DB write — no I/O inside the critical section.
                    synchronized (lock) {
                        MaterialEntity entity = MaterialMapper.dtoToEntity(dto);
                        materialDao.insert(entity);
                        Log.d(TAG, "Saved material: " + entity.getId());
                    }

                    // Per API Contract §3.6.2, send one ACK per successfully received material
                    // (device ID + material ID) immediately after download and save succeed.
                    if (attachmentsOk) {
                        ackRetrySender.send(new DistributeAckMessage(deviceId, dto.getId()), TAG);
                    } else {
                        Log.w(TAG, "Skipping DISTRIBUTE_ACK for material: " + dto.getId());
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Invalid material data: " + e.getMessage());
                }
            }

            // Refresh LiveData once after all materials are saved to notify observers.
            synchronized (lock) {
                refreshMaterialsLiveData();
            }

            Log.i(TAG, "Material sync completed successfully");
            syncSucceeded = true;

        } catch (IOException e) {
            Log.e(TAG, "Network error during material sync: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during material sync: " + e.getMessage(), e);
        } finally {
            syncing.set(false);
            Log.d(TAG, "Material sync completed for device: " + deviceId);
        }

        // Only notify callback if sync actually succeeded and materials were saved.
        // This ensures the callback fires only when materials are available.
        if (syncSucceeded) {
            try {
                notifyMaterialsAvailable();
            } catch (RuntimeException e) {
                Log.e(TAG, "Error while notifying materials availability callback", e);
            }
        }
    }

    @Override
    public boolean isSyncing() {
        return syncing.get();
    }

    /**
     * Downloads a single attachment and saves it to local storage.
     *
     * @param materialId   The ID of the material the attachment belongs to
     * @param attachmentId The ID of the attachment to download
     * @return true if download and save succeeded, false otherwise
     */
    private boolean downloadAttachment(@NonNull String materialId, @NonNull String attachmentId) {
        try {
            Response<ResponseBody> attachmentResponse =
                    apiService.getAttachment(attachmentId).execute();

            if (!attachmentResponse.isSuccessful() || attachmentResponse.body() == null) {
                Log.e(TAG, "Failed to download attachment " + attachmentId
                        + ". HTTP " + attachmentResponse.code());
                return false;
            }

            ResponseBody body = attachmentResponse.body();
            byte[] bytes = body.bytes();

            // Determine file extension from content-type header (default to "bin")
            String contentType = body.contentType() != null
                    ? body.contentType().subtype()
                    : "bin";

            fileStorageManager.saveAttachment(materialId, attachmentId, contentType, bytes);
            Log.d(TAG, "Saved attachment " + attachmentId + " for material " + materialId);
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Network error downloading attachment " + attachmentId + ": "
                    + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Notifies the callback that materials are available.
     * Called when DISTRIBUTE_MATERIAL signal is received.
     *
     * <p>Package-private to prevent external components from triggering callbacks
     * outside the intended flow. Internal components in the same package can still
     * invoke this method when handling TCP signals.</p>
     */
    void notifyMaterialsAvailable() {
        MaterialAvailableCallback callback = this.materialAvailableCallback;

        if (callback != null) {
            Log.d(TAG, "Notifying callback: materials available");
            callback.onMaterialsAvailable();
        }
    }

    /**
     * Refreshes the materials LiveData with current database contents.
     *
     * <p>This method does not perform explicit synchronization; callers should ensure any
     * required thread-safety when invoking it.</p>
     *
     * <p><b>Thread safety note:</b> This method uses {@link MutableLiveData#postValue(Object)}
     * which is asynchronous. When multiple threads modify data concurrently, the order of
     * LiveData updates may not strictly reflect the order of database modifications. If strict
     * ordering is required, consider using {@link MutableLiveData#setValue(Object)} from the
     * main thread instead.</p>
     */
    private void refreshMaterialsLiveData() {
        List<MaterialEntity> entities = materialDao.getAll();
        List<Material> materials = mapEntitiesToDomain(entities);
        materialsLiveData.postValue(materials);
    }

    /**
     * Maps a list of MaterialEntity objects to Material domain objects.
     *
     * @param entities The list of entities to map
     * @return List of domain objects
     */
    @NonNull
    private List<Material> mapEntitiesToDomain(@NonNull List<MaterialEntity> entities) {
        List<Material> materials = new ArrayList<>(entities.size());
        for (MaterialEntity entity : entities) {
            materials.add(MaterialMapper.toDomain(entity));
        }
        return materials;
    }

    /**
     * Validates that a string parameter is not null or empty.
     *
     * @param value     The value to validate
     * @param fieldName The name of the field for error messages
     * @throws IllegalArgumentException if the value is null or empty
     */
    private void validateNotEmpty(@Nullable String value, @NonNull String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }
}
