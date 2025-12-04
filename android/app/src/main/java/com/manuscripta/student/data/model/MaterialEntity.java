package com.manuscripta.student.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room entity representing educational material in the Manuscripta system.
 * Materials can be readings, quizzes, worksheets, or polls delivered from the teacher's Windows app.
 *
 * <p>Entity IDs are persistent across services - materials created by the Windows teacher
 * application will have IDs assigned by that application, and the Android client preserves
 * these IDs when receiving materials via the network layer.</p>
 */
@Entity(tableName = "materials")
public class MaterialEntity {

    /**
     * Unique identifier for this material (UUID format).
     * Assigned by the creator (Windows teacher app) and preserved across services.
     */
    @PrimaryKey
    @NonNull
    private final String id;

    /**
     * The type of material (READING, QUIZ, WORKSHEET, POLL).
     */
    @NonNull
    private final MaterialType type;

    /**
     * The title of the material.
     */
    @NonNull
    private final String title;

    /**
     * The main content of the material (can include formatted text, HTML, etc.).
     */
    private final String content;

    /**
     * Additional metadata in JSON format (e.g., author, subject, grade level).
     */
    private final String metadata;

    /**
     * Key vocabulary terms as a JSON array for MAT6 accessibility support.
     * Used for text-to-speech and content simplification features.
     */
    private final String vocabularyTerms;

    /**
     * Timestamp when the material was created or last modified (Unix epoch milliseconds).
     */
    private final long timestamp;

    /**
     * Constructor with required fields.
     *
     * @param id    Unique identifier (UUID)
     * @param type  Type of material
     * @param title Title of the material
     * @param content Main content of the material
     * @param metadata Additional metadata in JSON format
     * @param vocabularyTerms Key vocabulary terms in JSON array
     * @param timestamp Timestamp when the material was created or last modified
     */
    public MaterialEntity(@NonNull String id,
                          @NonNull MaterialType type,
                          @NonNull String title,
                          @NonNull String content,
                          @NonNull String metadata,
                          @NonNull String vocabularyTerms,
                          long timestamp) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.content = content;
        this.metadata = metadata;
        this.vocabularyTerms = vocabularyTerms;
        this.timestamp = timestamp;
    }

    // Getters and Setters

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public MaterialType getType() {
        return type;
    }

    @NonNull
    public String getTitle() {
        return title;
    }


    public String getContent() {
        return content;
    }

    public String getMetadata() {
        return metadata;
    }

    public String getVocabularyTerms() {
        return vocabularyTerms;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
