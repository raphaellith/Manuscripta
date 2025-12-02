package com.manuscripta.student.data.model;

/**
 * Enum representing different types of questions that can appear in quizzes.
 * Used to determine how questions should be displayed and answered.
 */
public enum QuestionType {
    /**
     * Multiple choice question with several options (A, B, C, D).
     */
    MULTIPLE_CHOICE,

    /**
     * True or False question.
     */
    TRUE_FALSE,

    /**
     * Written answer - student types their response.
     */
    WRITTEN_ANSWER;

    /**
     * Check if this question type requires predefined options.
     *
     * @return true if options are needed (multiple choice, true/false)
     */
    public boolean requiresOptions() {
        return this == MULTIPLE_CHOICE || this == TRUE_FALSE;
    }

    /**
     * Get a human-readable display name for this question type.
     *
     * @return Display name suitable for UI
     */
    public String getDisplayName() {
        return switch (this) {
            case MULTIPLE_CHOICE -> "Multiple Choice";
            case TRUE_FALSE      -> "True/False";
            case WRITTEN_ANSWER  -> "Written Answer";
        };
    }
}
