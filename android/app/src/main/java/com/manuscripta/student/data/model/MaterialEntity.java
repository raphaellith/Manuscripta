package com.manuscripta.student.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room entity representing educational material in the Manuscripta system.
 * Materials can be lessons, quizzes, worksheets, or polls delivered from the teacher's Windows app.
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
    private String id;

    /**
     * The type of material (LESSON, QUIZ, WORKSHEET, POLL).
     */
    @NonNull
    private MaterialType type;

    /**
     * The title of the material.
     */
    @NonNull
    private String title;

    /**
     * The main content of the material (can include formatted text, HTML, etc.).
     */
    private String content;

    /**
     * Additional metadata in JSON format (e.g., author, subject, grade level).
     */
    private String metadata;

    /**
     * Key vocabulary terms as a JSON array for MAT6 accessibility support.
     * Used for text-to-speech and content simplification features.
     */
    private String vocabularyTerms;

    /**
     * Timestamp when the material was created or last modified (Unix epoch milliseconds).
     */
    private long timestamp;

    /**
     * Default constructor required by Room.
     */
    public MaterialEntity() {
        this.id = "";
        this.type = MaterialType.LESSON;
        this.title = "";
    }

    /**
     * Constructor with required fields.
     *
     * @param id    Unique identifier (UUID)
     * @param type  Type of material
     * @param title Title of the material
     */
    public MaterialEntity(@NonNull String id,
                          @NonNull MaterialType type,
                          @NonNull String title,
                          @NonNull String content,
                          @NonNull String metadata,
                          @NonNull String vocabularyTerms) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.content = content;
        this.metadata = metadata;
        this.vocabularyTerms = vocabularyTerms;
        this.timestamp = System.currentTimeMillis();
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

    public void setType(@NonNull MaterialType type) {
        this.type = type;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getVocabularyTerms() {
        return vocabularyTerms;
    }

    public void setVocabularyTerms(String vocabularyTerms) {
        this.vocabularyTerms = vocabularyTerms;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
