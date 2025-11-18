package com.manuscripta.student.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.manuscripta.student.data.model.Lesson;

/**
 * Room database for Manuscripta application.
 * This is the main database configuration.
 */
@Database(entities = {Lesson.class}, version = 1, exportSchema = false)
public abstract class ManuscriptaDatabase extends RoomDatabase {
    // DAO methods will be added here as entities are created
}
