package com.manuscripta.student.domain.model;

import androidx.annotation.NonNull;

import com.manuscripta.student.data.model.MaterialType;

/**
 * Domain model representing educational material in the Manuscripta system.
 * This is a clean domain object without persistence annotations, used in the business logic layer.
 *
 * <p>Materials can be readings, quizzes, worksheets, or polls delivered from the teacher's Windows app.</p>
 */
public class Material {

    /** The unique identifier for the material. */
    @NonNull
    private final String id;

    /** The type of material (READING, QUIZ, WORKSHEET, POLL). */
    @NonNull
    private final MaterialType type;

    /** The title of the material. */
    @NonNull
    private final String title;

    /** The main content of the material. */
    @NonNull
    private final String content;

    /** Additional metadata in JSON format. */
    @NonNull
    private final String metadata;

    /** Key vocabulary terms in JSON array format. */
    @NonNull
    private final String vocabularyTerms;

    /** Timestamp when the material was created or last modified (Unix epoch milliseconds). */
    private final long timestamp;

    /**
     * Constructor with all fields.
     *
     * @param id               Unique identifier (UUID)
     * @param type             Type of material (READING, QUIZ, WORKSHEET, POLL)
     * @param title            Title of the material
     * @param content          Main content of the material
     * @param metadata         Additional metadata in JSON format
     * @param vocabularyTerms  Key vocabulary terms in JSON array format
     * @param timestamp        Timestamp when the material was created or last modified (Unix epoch milliseconds)
     * @throws IllegalArgumentException if id is null or empty
     * @throws IllegalArgumentException if type is null
     * @throws IllegalArgumentException if title is null or empty
     * @throws IllegalArgumentException if content is null
     * @throws IllegalArgumentException if metadata is null
     * @throws IllegalArgumentException if vocabularyTerms is null
     * @throws IllegalArgumentException if timestamp is negative
     */
    public Material(@NonNull String id,
                    @NonNull MaterialType type,
                    @NonNull String title,
                    @NonNull String content,
                    @NonNull String metadata,
                    @NonNull String vocabularyTerms,
                    long timestamp) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Material id cannot be null or empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("Material type cannot be null");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Material title cannot be null or empty");
        }
        if (content == null) {
            throw new IllegalArgumentException("Material content cannot be null");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Material metadata cannot be null");
        }
        if (vocabularyTerms == null) {
            throw new IllegalArgumentException("Material vocabularyTerms cannot be null");
        }
        if (timestamp < 0) {
            throw new IllegalArgumentException("Material timestamp cannot be negative");
        }

        this.id = id;
        this.type = type;
        this.title = title;
        this.content = content;
        this.metadata = metadata;
        this.vocabularyTerms = vocabularyTerms;
        this.timestamp = timestamp;
    }

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

    @NonNull
    public String getContent() {
        return content;
    }

    @NonNull
    public String getMetadata() {
        return metadata;
    }

    @NonNull
    public String getVocabularyTerms() {
        return vocabularyTerms;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
