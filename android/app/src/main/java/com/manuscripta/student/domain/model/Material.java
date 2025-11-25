package com.manuscripta.student.domain.model;

import androidx.annotation.NonNull;

import com.manuscripta.student.data.model.MaterialType;

/**
 * Domain model representing educational material in the Manuscripta system.
 * This is a clean domain object without persistence annotations, used in the business logic layer.
 *
 * <p>Materials can be lessons, quizzes, worksheets, or polls delivered from the teacher's Windows app.</p>
 */
public class Material {

    @NonNull
    private final String id;

    @NonNull
    private final MaterialType type;

    @NonNull
    private final String title;

    @NonNull
    private final String content;

    @NonNull
    private final String metadata;

    @NonNull
    private final String vocabularyTerms;

    private final long timestamp;

    /**
     * Constructor with all fields.
     *
     * @param id               Unique identifier (UUID)
     * @param type             Type of material (LESSON, QUIZ, WORKSHEET, POLL)
     * @param title            Title of the material
     * @param content          Main content of the material
     * @param metadata         Additional metadata in JSON format
     * @param vocabularyTerms  Key vocabulary terms in JSON array format
     * @param timestamp        Timestamp when the material was created or last modified (Unix epoch milliseconds)
     */
    public Material(@NonNull String id,
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
