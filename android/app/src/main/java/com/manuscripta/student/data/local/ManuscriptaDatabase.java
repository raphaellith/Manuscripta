package com.manuscripta.student.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.manuscripta.student.data.model.MaterialEntity;
import com.manuscripta.student.data.model.QuestionEntity;
import com.manuscripta.student.data.model.SessionEntity;
import com.manuscripta.student.data.model.ResponseEntity;
import com.manuscripta.student.data.model.DeviceStatusEntity;

/**
 * Room database for Manuscripta application.
 * This is the main database configuration.
 */
@Database(
    entities = {
        MaterialEntity.class,
        QuestionEntity.class,
        ResponseEntity.class,
        SessionEntity.class,
        DeviceStatusEntity.class
    },
    version = 7,
    exportSchema = true
)
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

    /**
     * Get the Session DAO for database operations on sessions.
     *
     * @return SessionDao instance
     */
    public abstract SessionDao sessionDao();

    /**
     * Get the Response DAO for database operations on responses.
     *
     * @return ResponseDao instance
     */
    public abstract ResponseDao responseDao();

    /**
     * Get the DeviceStatus DAO for database operations on device status.
     *
     * @return DeviceStatusDao instance
     */
    public abstract DeviceStatusDao deviceStatusDao();
}
