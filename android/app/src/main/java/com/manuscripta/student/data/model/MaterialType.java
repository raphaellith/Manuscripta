package com.manuscripta.student.data.model;

/**
 * Enum representing different types of materials that can be delivered to students.
 * Used to categorize educational content in the Manuscripta system.
 */
public enum MaterialType {
    /**
     * Reading material content.
     * Informational text for students to read and learn from.
     */
    READING,

    /**
     * Assessment with questions.
     * Tests student understanding.
     */
    QUIZ,

    /**
     * Practice exercises.
     * Activities for students to complete.
     */
    WORKSHEET,

    /**
     * Quick survey or poll.
     * Gathers student opinions or checks understanding.
     */
    POLL;

    /**
     * Get a human-readable display name for this material type.
     *
     * @return Display name suitable for UI
     */
    public String getDisplayName() {
        return switch (this) {
            case READING   -> "Reading Material";
            case QUIZ      -> "Quiz";
            case WORKSHEET -> "Worksheet";
            case POLL      -> "Poll";
        };
    }
}