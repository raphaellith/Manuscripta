package com.manuscripta.student.data.model;

/**
 * Enum representing the feedback style setting for quiz responses.
 * Per Validation Rules §2G(1)(b), determines how feedback is displayed to students.
 */
public enum FeedbackStyle {
    /**
     * Correct/Incorrect feedback is shown immediately after submitting.
     */
    IMMEDIATE,

    /**
     * Only "Response Submitted" confirmation is shown, without correctness feedback.
     */
    NEUTRAL
}
