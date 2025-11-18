package com.manuscripta.student.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.manuscripta.student.data.model.MaterialEntity;
import com.manuscripta.student.data.model.QuestionEntity;

/**
 * Room database for Manuscripta application.
 * This is the main database configuration.
 */
@Database(entities = {MaterialEntity.class, QuestionEntity.class}, version = 4, exportSchema = false)
public abstract class ManuscriptaDatabase extends RoomDatabase {

    /**
     * Get the Material DAO for database operations on materials.
     *
     * @return MaterialDao instance
     */
    public abstract MaterialDao materialDao();

    /**
     * Get the Question DAO for database operations on questions.
     *
     * @return QuestionDao instance
     */
    public abstract QuestionDao questionDao();
}
