package com.manuscripta.student.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Domain model representing teacher feedback on a student's response.
 * This is a clean domain object without persistence annotations, used in the business logic layer.
 *
 * <p>Per Validation Rules.md §2F, at least one of {@code text} or {@code marks}
 * must be non-null for valid feedback.</p>
 */
public class Feedback {

    /** The unique identifier for the feedback (UUID assigned by Windows). */
    @NonNull
    private final String id;

    /** The ID of the parent response. */
    @NonNull
    private final String responseId;

    /** Optional textual feedback from the teacher. */
    @Nullable
    private final String text;

    /** Optional numerical marks awarded. */
    @Nullable
    private final Integer marks;

    /**
     * Constructor with all fields.
     *
     * <p>Per Validation Rules.md §2F(1)(b), at least one of {@code text} or {@code marks}
     * must be non-null.</p>
     *
     * @param id         Unique identifier (UUID assigned by Windows)
     * @param responseId UUID of the parent response
     * @param text       Optional textual feedback
     * @param marks      Optional numerical marks
     * @throws IllegalArgumentException if id is null or empty
     * @throws IllegalArgumentException if responseId is null or empty
     * @throws IllegalArgumentException if both text and marks are null
     * @throws IllegalArgumentException if marks is negative
     */
    public Feedback(@NonNull String id,
                    @NonNull String responseId,
                    @Nullable String text,
                    @Nullable Integer marks) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Feedback id cannot be null or empty");
        }
        if (responseId == null || responseId.trim().isEmpty()) {
            throw new IllegalArgumentException("Feedback responseId cannot be null or empty");
        }
        boolean hasText = text != null && !text.trim().isEmpty();
        boolean hasMarks = marks != null;
        if (!hasText && !hasMarks) {
            throw new IllegalArgumentException(
                    "Feedback must have at least one of text or marks");
        }
        if (marks != null && marks < 0) {
            throw new IllegalArgumentException("Feedback marks cannot be negative");
        }

        this.id = id;
        this.responseId = responseId;
        this.text = text;
        this.marks = marks;
    }

    /**
     * Returns the unique identifier for this feedback.
     *
     * @return The feedback ID (UUID)
     */
    @NonNull
    public String getId() {
        return id;
    }

    /**
     * Returns the ID of the response this feedback is for.
     *
     * @return The response ID (UUID)
     */
    @NonNull
    public String getResponseId() {
        return responseId;
    }

    /**
     * Returns the optional textual feedback.
     *
     * @return The text feedback, or null if not provided
     */
    @Nullable
    public String getText() {
        return text;
    }

    /**
     * Returns the optional numerical marks.
     *
     * @return The marks awarded, or null if not provided
     */
    @Nullable
    public Integer getMarks() {
        return marks;
    }

    /**
     * Checks if this feedback has textual content.
     *
     * @return true if text is present and non-empty, false otherwise
     */
    public boolean hasText() {
        return text != null && !text.trim().isEmpty();
    }

    /**
     * Checks if this feedback has numerical marks.
     *
     * @return true if marks are present, false otherwise
     */
    public boolean hasMarks() {
        return marks != null;
    }
}
