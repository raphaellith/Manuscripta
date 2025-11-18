package com.manuscripta.student.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.manuscripta.student.data.model.QuestionEntity;

import java.util.List;

/**
 * Data Access Object for {@link QuestionEntity}.
 * Provides methods for CRUD operations on the questions table.
 */
@Dao
public interface QuestionDao {

    /**
     * Get all questions from the database.
     *
     * @return List of all questions
     */
    @Query("SELECT * FROM questions")
    List<QuestionEntity> getAll();

    /**
     * Get a question by its unique identifier.
     *
     * @param id The UUID of the question
     * @return The question entity, or null if not found
     */
    @Query("SELECT * FROM questions WHERE id = :id")
    QuestionEntity getById(String id);

    /**
     * Get all questions for a specific material.
     *
     * @param materialId The UUID of the parent material
     * @return List of questions belonging to the material
     */
    @Query("SELECT * FROM questions WHERE materialId = :materialId")
    List<QuestionEntity> getByMaterialId(String materialId);

    /**
     * Insert a new question into the database.
     * If a question with the same ID already exists, it will be replaced.
     *
     * @param question The question to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(QuestionEntity question);

    /**
     * Insert multiple questions into the database.
     * Existing questions with the same IDs will be replaced.
     *
     * @param questions The questions to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<QuestionEntity> questions);

    /**
     * Update an existing question in the database.
     *
     * @param question The question with updated values
     */
    @Update
    void update(QuestionEntity question);

    /**
     * Delete a question from the database.
     *
     * @param question The question to delete
     */
    @Delete
    void delete(QuestionEntity question);

    /**
     * Delete a question by its unique identifier.
     *
     * @param id The UUID of the question to delete
     */
    @Query("DELETE FROM questions WHERE id = :id")
    void deleteById(String id);

    /**
     * Delete all questions for a specific material.
     *
     * @param materialId The UUID of the parent material
     */
    @Query("DELETE FROM questions WHERE materialId = :materialId")
    void deleteByMaterialId(String materialId);

    /**
     * Delete all questions from the database.
     */
    @Query("DELETE FROM questions")
    void deleteAll();

    /**
     * Get the count of all questions.
     *
     * @return The total number of questions
     */
    @Query("SELECT COUNT(*) FROM questions")
    int getCount();

    /**
     * Get the count of questions for a specific material.
     *
     * @param materialId The UUID of the parent material
     * @return The number of questions for that material
     */
    @Query("SELECT COUNT(*) FROM questions WHERE materialId = :materialId")
    int getCountByMaterialId(String materialId);
}
