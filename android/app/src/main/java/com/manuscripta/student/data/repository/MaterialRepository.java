package com.manuscripta.student.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.manuscripta.student.data.model.MaterialType;
import com.manuscripta.student.domain.model.Material;

import java.util.List;

/**
 * Repository interface for managing educational materials.
 * Provides abstraction over local storage and material distribution from the server.
 *
 * <p>This repository acts as a facade for:</p>
 * <ul>
 *   <li>Local persistence of materials via Room</li>
 *   <li>Observable material state for UI via LiveData</li>
 *   <li>Integration with TCP layer for DISTRIBUTE_MATERIAL notifications</li>
 *   <li>Orchestration of HTTP material fetching and attachment downloads</li>
 * </ul>
 *
 * <p><b>Material Distribution Flow:</b></p>
 * <ol>
 *   <li>HeartbeatManager sends STATUS_UPDATE to server</li>
 *   <li>Server responds with DISTRIBUTE_MATERIAL (0x05) if new materials available</li>
 *   <li>MaterialRepository receives callback and triggers HTTP fetch</li>
 *   <li>Materials and attachments are downloaded and stored locally</li>
 *   <li>DISTRIBUTE_ACK is sent to confirm receipt</li>
 * </ol>
 */
public interface MaterialRepository {

    /**
     * Gets a material by its unique identifier.
     *
     * @param materialId The unique identifier of the material
     * @return The material, or null if not found
     * @throws IllegalArgumentException if materialId is null or empty
     */
    @Nullable
    Material getMaterialById(@NonNull String materialId);

    /**
     * Gets all materials from local storage.
     *
     * @return List of all materials, ordered by timestamp (newest first)
     */
    @NonNull
    List<Material> getAllMaterials();

    /**
     * Gets all materials of a specific type.
     *
     * @param type The type of materials to retrieve
     * @return List of materials of the specified type
     * @throws IllegalArgumentException if type is null
     */
    @NonNull
    List<Material> getMaterialsByType(@NonNull MaterialType type);

    /**
     * Gets observable LiveData for all materials.
     * The LiveData emits updates whenever materials change.
     *
     * @return LiveData containing the list of all materials
     */
    @NonNull
    LiveData<List<Material>> getMaterialsLiveData();

    /**
     * Saves a material to local storage.
     * If a material with the same ID exists, it will be replaced.
     *
     * @param material The material to save
     * @throws IllegalArgumentException if material is null
     */
    void saveMaterial(@NonNull Material material);

    /**
     * Saves multiple materials to local storage.
     * Existing materials with the same IDs will be replaced.
     *
     * @param materials The materials to save
     * @throws IllegalArgumentException if materials is null
     */
    void saveMaterials(@NonNull List<Material> materials);

    /**
     * Deletes a material from local storage.
     * Also cleans up any associated attachment files.
     *
     * @param materialId The unique identifier of the material to delete
     * @throws IllegalArgumentException if materialId is null or empty
     */
    void deleteMaterial(@NonNull String materialId);

    /**
     * Deletes all materials from local storage.
     * Also cleans up all attachment files.
     */
    void deleteAllMaterials();

    /**
     * Gets the count of materials in local storage.
     *
     * @return The total number of materials
     */
    int getMaterialCount();

    /**
     * Callback interface for material availability notifications.
     * Implement this to receive notifications when DISTRIBUTE_MATERIAL
     * signal is received from the server.
     */
    interface MaterialAvailableCallback {
        /**
         * Called when the server signals that new materials are available.
         * The implementation should trigger HTTP material fetch.
         */
        void onMaterialsAvailable();
    }

    /**
     * Sets the callback for material availability notifications.
     *
     * @param callback The callback to receive notifications, or null to remove
     */
    void setMaterialAvailableCallback(@Nullable MaterialAvailableCallback callback);

    /**
     * Triggers a material sync from the server.
     * This is called when DISTRIBUTE_MATERIAL signal is received.
     *
     * <p>The sync process:</p>
     * <ol>
     *   <li>Fetches materials via HTTP GET /distribution/{deviceId}</li>
     *   <li>Downloads any attachment files referenced in content</li>
     *   <li>Saves materials to local storage</li>
     *   <li>Sends DISTRIBUTE_ACK to confirm receipt</li>
     * </ol>
     *
     * @param deviceId The device ID to fetch materials for
     * @throws IllegalArgumentException if deviceId is null or empty
     */
    void syncMaterials(@NonNull String deviceId);

    /**
     * Checks if materials are currently being synced.
     *
     * @return true if a sync operation is in progress
     */
    boolean isSyncing();
}
