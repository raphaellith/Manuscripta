package com.manuscripta.student.network.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object for vocabulary terms received from the API.
 * Vocabulary terms provide key definitions for educational material content.
 *
 * <p>Used for MAT6 accessibility support features like text-to-speech
 * and content simplification.</p>
 */
public class VocabularyTermDto {

    /**
     * The vocabulary term word or phrase.
     */
    @SerializedName("term")
    @Nullable
    private String term;

    /**
     * The definition or explanation of the term.
     */
    @SerializedName("definition")
    @Nullable
    private String definition;

    /**
     * Default constructor for Gson deserialization.
     */
    public VocabularyTermDto() {
    }

    /**
     * Constructor with all fields.
     *
     * @param term       The vocabulary term word or phrase
     * @param definition The definition or explanation of the term
     */
    public VocabularyTermDto(@Nullable String term, @Nullable String definition) {
        this.term = term;
        this.definition = definition;
    }

    /**
     * Gets the vocabulary term.
     *
     * @return The term word or phrase
     */
    @Nullable
    public String getTerm() {
        return term;
    }

    /**
     * Sets the vocabulary term.
     *
     * @param term The term word or phrase
     */
    public void setTerm(@Nullable String term) {
        this.term = term;
    }

    /**
     * Gets the definition of the term.
     *
     * @return The definition or explanation
     */
    @Nullable
    public String getDefinition() {
        return definition;
    }

    /**
     * Sets the definition of the term.
     *
     * @param definition The definition or explanation
     */
    public void setDefinition(@Nullable String definition) {
        this.definition = definition;
    }

    @Override
    @NonNull
    public String toString() {
        return "VocabularyTermDto{"
                + "term='" + term + '\''
                + ", definition='" + definition + '\''
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
        VocabularyTermDto that = (VocabularyTermDto) o;
        if (term != null ? !term.equals(that.term) : that.term != null) {
            return false;
        }
        return definition != null ? definition.equals(that.definition) : that.definition == null;
    }

    @Override
    public int hashCode() {
        int result = term != null ? term.hashCode() : 0;
        result = 31 * result + (definition != null ? definition.hashCode() : 0);
        return result;
    }
}
