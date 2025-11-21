package com.manuscripta.student.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.manuscripta.student.data.model.MaterialEntity;
import com.manuscripta.student.data.model.MaterialType;

import java.util.List;

/**
 * Data Access Object for {@link MaterialEntity}.
 * Provides methods for CRUD operations on the materials table.
 */
@Dao
public interface MaterialDao {

    /**
     * Get all materials from the database.
     *
     * @return List of all materials ordered by timestamp (newest first)
     */
    @Query("SELECT * FROM materials ORDER BY timestamp DESC")
    List<MaterialEntity> getAll();

    /**
     * Get a material by its unique identifier.
     *
     * @param id The UUID of the material
     * @return The material entity, or null if not found
     */
    @Query("SELECT * FROM materials WHERE id = :id")
    MaterialEntity getById(String id);

    /**
     * Get all materials of a specific type.
     *
     * @param type The type of material to filter by
     * @return List of materials of the specified type
     */
    @Query("SELECT * FROM materials WHERE type = :type ORDER BY timestamp DESC")
    List<MaterialEntity> getByType(MaterialType type);

    /**
     * Insert a new material into the database.
     * If a material with the same ID already exists, it will be replaced.
     *
     * @param material The material to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MaterialEntity material);

    /**
     * Insert multiple materials into the database.
     * Existing materials with the same IDs will be replaced.
     *
     * @param materials The materials to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<MaterialEntity> materials);

    /**
     * Update an existing material in the database.
     *
     * @param material The material with updated values
     */
    @Update
    void update(MaterialEntity material);

    /**
     * Delete a material from the database.
     *
     * @param material The material to delete
     */
    @Delete
    void delete(MaterialEntity material);

    /**
     * Delete a material by its unique identifier.
     *
     * @param id The UUID of the material to delete
     */
    @Query("DELETE FROM materials WHERE id = :id")
    void deleteById(String id);

    /**
     * Delete all materials from the database.
     */
    @Query("DELETE FROM materials")
    void deleteAll();

    /**
     * Get the count of all materials.
     *
     * @return The total number of materials
     */
    @Query("SELECT COUNT(*) FROM materials")
    int getCount();
}
