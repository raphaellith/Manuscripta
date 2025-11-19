package com.manuscripta.student.data.model;

/**
 * Enum representing different types of materials that can be delivered to students.
 * Used to categorize educational content in the Manuscripta system.
 */
public enum MaterialType {
    /**
     * Reading lesson content - informational text for students to read and learn from
     */
    LESSON,

    /**
     * Assessment with questions - tests student understanding
     */
    QUIZ,

    /**
     * Practice exercises - activities for students to complete
     */
    WORKSHEET,

    /**
     * Quick survey or poll - gather student opinions or check understanding
     */
    POLL;

    /**
     * Get a human-readable display name for this material type.
     *
     * @return Display name suitable for UI
     */
    public String getDisplayName() {
        return switch (this) {
            case LESSON    -> "Reading Material";
            case QUIZ      -> "Quiz";
            case WORKSHEET -> "Worksheet";
            case POLL      -> "Poll";
        };
    }
}