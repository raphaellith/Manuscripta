package com.manuscripta.student.data.model;

/**
 * Enum representing the status of a learning session.
 */
public enum SessionStatus {
    /**
     * Session is currently active/ongoing.
     */
    ACTIVE,

    /**
     * Session has been paused.
     */
    PAUSED,

    /**
     * Session completed normally.
     */
    COMPLETED,

    /**
     * Session was cancelled before completion.
     */
    CANCELLED
}
