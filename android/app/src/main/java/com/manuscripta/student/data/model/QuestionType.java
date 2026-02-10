package com.manuscripta.student.data.model;

/**
 * Enum representing different types of questions that can appear in worksheets and polls.
 * Used to determine how questions should be displayed and answered.
 */
public enum QuestionType {
    /**
     * Multiple choice question with several options (A, B, C, D).
     */
    MULTIPLE_CHOICE,

    /**
     * Written answer.
     * Student types their response.
     */
    WRITTEN_ANSWER;

    /**
     * Check if this question type requires predefined options.
     * Written answer does not require options as student types their response.
     *
     * @return true if options are needed (multiple choice)
     */
    public boolean requiresOptions() {
        return this == MULTIPLE_CHOICE;
    }

    /**
     * Get a human-readable display name for this question type.
     *
     * @return Display name suitable for UI
     */
    public String getDisplayName() {
        return switch (this) {
            case MULTIPLE_CHOICE -> "Multiple Choice";
            case WRITTEN_ANSWER  -> "Written Answer";
        };
    }
}