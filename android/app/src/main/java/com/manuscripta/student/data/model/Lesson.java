package com.manuscripta.student.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Temporary entity for Lesson.
 * This is a placeholder entity to satisfy Room database requirements.
 */
@Entity(tableName = "lessons")
public class Lesson {
    /**
     * The unique identifier for this lesson.
     */
    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * The title of the lesson.
     */
    private String title;

    public Lesson() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
