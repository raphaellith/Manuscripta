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
import com.manuscripta.student.utils.FileStorageManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

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
 * <p><b>Note:</b> HTTP network operations for material fetching are not yet implemented.
 * The syncMaterials method is a placeholder for when the HTTP layer is ready.</p>
 */
@Singleton
public class MaterialRepositoryImpl implements MaterialRepository {

    /** Tag for logging. */
    private static final String TAG = "MaterialRepository";

    /** The DAO for material persistence. */
    private final MaterialDao materialDao;

    /** The file storage manager for attachments. */
    private final FileStorageManager fileStorageManager;

    /** Lock object for thread-safe operations. */
    private final Object lock = new Object();

    /** LiveData for observable material list. */
    private final MutableLiveData<List<Material>> materialsLiveData;

    /** Flag indicating if a sync operation is in progress. */
    private final AtomicBoolean syncing = new AtomicBoolean(false);

    /** Callback for material availability notifications. */
    @Nullable
    private MaterialAvailableCallback materialAvailableCallback;

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
     * @throws IllegalArgumentException if materialDao or fileStorageManager is null
     */
    @Inject
    public MaterialRepositoryImpl(@NonNull MaterialDao materialDao,
                                  @NonNull FileStorageManager fileStorageManager) {
        if (materialDao == null) {
            throw new IllegalArgumentException("MaterialDao cannot be null");
        }
        if (fileStorageManager == null) {
            throw new IllegalArgumentException("FileStorageManager cannot be null");
        }
        this.materialDao = materialDao;
        this.fileStorageManager = fileStorageManager;
        this.materialsLiveData = new MutableLiveData<>(new ArrayList<>());

        // Initialize LiveData with existing materials from database.
        // Safe to call here as all fields are initialized and getAll() is a pure read.
        refreshMaterialsLiveData();
    }

    @Override
    @Nullable
    public Material getMaterialById(@NonNull String materialId) {
        validateNotEmpty(materialId, "Material ID");

        synchronized (lock) {
            MaterialEntity entity = materialDao.getById(materialId);
            if (entity == null) {
                return null;
            }
            return MaterialMapper.toDomain(entity);
        }
    }

    @Override
    @NonNull
    public List<Material> getAllMaterials() {
        synchronized (lock) {
            List<MaterialEntity> entities = materialDao.getAll();
            return mapEntitiesToDomain(entities);
        }
    }

    @Override
    @NonNull
    public List<Material> getMaterialsByType(@NonNull MaterialType type) {
        if (type == null) {
            throw new IllegalArgumentException("Material type cannot be null");
        }

        synchronized (lock) {
            List<MaterialEntity> entities = materialDao.getByType(type);
            return mapEntitiesToDomain(entities);
        }
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
            for (Material material : materials) {
                entities.add(MaterialMapper.toEntity(material));
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
        synchronized (lock) {
            return materialDao.getCount();
        }
    }

    @Override
    public void setMaterialAvailableCallback(@Nullable MaterialAvailableCallback callback) {
        synchronized (lock) {
            this.materialAvailableCallback = callback;
        }
    }

    @Override
    public void syncMaterials(@NonNull String deviceId) {
        validateNotEmpty(deviceId, "Device ID");

        if (!syncing.compareAndSet(false, true)) {
            Log.w(TAG, "Sync already in progress, ignoring request");
            return;
        }

        Log.d(TAG, "Starting material sync for device: " + deviceId);

        try {
            // TODO: Implement HTTP material fetch when ApiService is ready
            // The flow should be:
            // 1. HTTP GET /distribution/{deviceId} to fetch materials
            // 2. Parse content for attachment references (/attachments/{id})
            // 3. Download each attachment via HTTP
            // 4. Save attachment bytes via FileStorageManager
            // 5. Save materials to database
            // 6. Send DISTRIBUTE_ACK via TcpSocketManager

            // For now, just log that sync was requested
            Log.i(TAG, "Material sync requested for device " + deviceId
                    + " - HTTP fetch not yet implemented");

            // Notify callback that materials may be available
            notifyMaterialsAvailable();

        } finally {
            syncing.set(false);
            Log.d(TAG, "Material sync completed for device: " + deviceId);
        }
    }

    @Override
    public boolean isSyncing() {
        return syncing.get();
    }

    /**
     * Notifies the callback that materials are available.
     * Called when DISTRIBUTE_MATERIAL signal is received.
     */
    public void notifyMaterialsAvailable() {
        MaterialAvailableCallback callback;
        synchronized (lock) {
            callback = this.materialAvailableCallback;
        }

        if (callback != null) {
            Log.d(TAG, "Notifying callback: materials available");
            callback.onMaterialsAvailable();
        }
    }

    /**
     * Refreshes the materials LiveData with current database contents.
     *
     * <p>Must be called within synchronized block for thread safety.</p>
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
