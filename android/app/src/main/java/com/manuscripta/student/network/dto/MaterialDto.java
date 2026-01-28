package com.manuscripta.student.network.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Data Transfer Object for Material API communication.
 * Materials may reference attachment files (PDFs, images) via URLs in the content field.
 *
 * <p>Entity IDs are assigned by the Windows teacher application and must be preserved
 * exactly as received, without modification or regeneration.</p>
 *
 * <p>Related requirements: MAT1, MAT6, MAT8 (Teacher), MAT15</p>
 */
public class MaterialDto {

    /**
     * Unique identifier for the material (UUID format).
     * Assigned by the Windows teacher application and preserved across services.
     */
    @SerializedName("Id")
    @Nullable
    private String id;

    /**
     * The type of material (e.g., "READING", "QUIZ", "WORKSHEET", "POLL").
     */
    @SerializedName("Type")
    @Nullable
    private String type;

    /**
     * The title of the material.
     */
    @SerializedName("Title")
    @Nullable
    private String title;

    /**
     * The main content of the material.
     * May contain references to attachments in the format "/attachments/{id}".
     */
    @SerializedName("Content")
    @Nullable
    private String content;

    /**
     * Additional metadata in JSON format (e.g., author, subject, grade level).
     */
    @SerializedName("Metadata")
    @Nullable
    private String metadata;

    /**
     * Key vocabulary terms for accessibility support (MAT6).
     * Used for text-to-speech and content simplification features.
     */
    @SerializedName("VocabularyTerms")
    @Nullable
    private List<VocabularyTermDto> vocabularyTerms;

    /**
     * Timestamp when the material was created or last modified (Unix epoch milliseconds).
     */
    @SerializedName("Timestamp")
    @Nullable
    private Long timestamp;

    /**
     * Default constructor for Gson deserialization.
     */
    public MaterialDto() {
    }

    /**
     * Constructor with all fields.
     *
     * @param id              Unique identifier (UUID)
     * @param type            Type of material
     * @param title           Title of the material
     * @param content         Main content (may contain attachment references)
     * @param metadata        Additional metadata in JSON format
     * @param vocabularyTerms Key vocabulary terms for accessibility
     * @param timestamp       Timestamp when the material was created or last modified
     */
    public MaterialDto(@Nullable String id,
                       @Nullable String type,
                       @Nullable String title,
                       @Nullable String content,
                       @Nullable String metadata,
                       @Nullable List<VocabularyTermDto> vocabularyTerms,
                       @Nullable Long timestamp) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.content = content;
        this.metadata = metadata;
        this.vocabularyTerms = vocabularyTerms;
        this.timestamp = timestamp;
    }

    /**
     * Gets the unique identifier.
     *
     * @return The material ID
     */
    @Nullable
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier.
     *
     * @param id The material ID
     */
    public void setId(@Nullable String id) {
        this.id = id;
    }

    /**
     * Gets the material type.
     *
     * @return The type (e.g., "READING", "QUIZ")
     */
    @Nullable
    public String getType() {
        return type;
    }

    /**
     * Sets the material type.
     *
     * @param type The type (e.g., "READING", "QUIZ")
     */
    public void setType(@Nullable String type) {
        this.type = type;
    }

    /**
     * Gets the material title.
     *
     * @return The title
     */
    @Nullable
    public String getTitle() {
        return title;
    }

    /**
     * Sets the material title.
     *
     * @param title The title
     */
    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    /**
     * Gets the main content.
     *
     * @return The content (may contain attachment references)
     */
    @Nullable
    public String getContent() {
        return content;
    }

    /**
     * Sets the main content.
     *
     * @param content The content (may contain attachment references)
     */
    public void setContent(@Nullable String content) {
        this.content = content;
    }

    /**
     * Gets the additional metadata.
     *
     * @return The metadata in JSON format
     */
    @Nullable
    public String getMetadata() {
        return metadata;
    }

    /**
     * Sets the additional metadata.
     *
     * @param metadata The metadata in JSON format
     */
    public void setMetadata(@Nullable String metadata) {
        this.metadata = metadata;
    }

    /**
     * Gets the vocabulary terms.
     *
     * @return The list of vocabulary terms
     */
    @Nullable
    public List<VocabularyTermDto> getVocabularyTerms() {
        return vocabularyTerms;
    }

    /**
     * Sets the vocabulary terms.
     *
     * @param vocabularyTerms The list of vocabulary terms
     */
    public void setVocabularyTerms(@Nullable List<VocabularyTermDto> vocabularyTerms) {
        this.vocabularyTerms = vocabularyTerms;
    }

    /**
     * Gets the timestamp.
     *
     * @return The timestamp (Unix epoch milliseconds)
     */
    @Nullable
    public Long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp.
     *
     * @param timestamp The timestamp (Unix epoch milliseconds)
     */
    public void setTimestamp(@Nullable Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    @NonNull
    public String toString() {
        return "MaterialDto{"
                + "id='" + id + '\''
                + ", type='" + type + '\''
                + ", title='" + title + '\''
                + ", content='" + content + '\''
                + ", metadata='" + metadata + '\''
                + ", vocabularyTerms=" + vocabularyTerms
                + ", timestamp=" + timestamp
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MaterialDto that = (MaterialDto) o;
        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (type != null ? !type.equals(that.type) : that.type != null) {
            return false;
        }
        if (title != null ? !title.equals(that.title) : that.title != null) {
            return false;
        }
        if (content != null ? !content.equals(that.content) : that.content != null) {
            return false;
        }
        if (metadata != null ? !metadata.equals(that.metadata) : that.metadata != null) {
            return false;
        }
        if (vocabularyTerms != null ? !vocabularyTerms.equals(that.vocabularyTerms)
                : that.vocabularyTerms != null) {
            return false;
        }
        return timestamp != null ? timestamp.equals(that.timestamp) : that.timestamp == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        result = 31 * result + (vocabularyTerms != null ? vocabularyTerms.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }
}
