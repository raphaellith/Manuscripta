package com.manuscripta.student.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.manuscripta.student.data.model.MaterialEntity;

/**
 * Room database for Manuscripta application.
 * This is the main database configuration.
 */
@Database(entities = {MaterialEntity.class}, version = 3, exportSchema = false)
public abstract class ManuscriptaDatabase extends RoomDatabase {

    /**
     * Get the Material DAO for database operations on materials.
     *
     * @return MaterialDao instance
     */
    public abstract MaterialDao materialDao();
}
