package com.manuscripta.student.domain.model;

import androidx.annotation.NonNull;

import java.util.UUID;

/**
 * Domain model representing a student's response to a question.
 * This is a clean domain object without persistence annotations, used in the business logic layer.
 *
 * <p>Responses are linked to a parent Question.</p>
 */
public class Response {

    @NonNull
    private final String id;

    @NonNull
    private final String questionId;

    @NonNull
    private final String selectedAnswer;

    private final boolean isCorrect;

    private final long timestamp;

    private final boolean synced;

    /**
     * Factory method for creating a NEW response.
     * Generates a random UUID, captures the current timestamp, and sets default values.
     *
     * @param questionId     UUID of the parent question
     * @param selectedAnswer The student's selected answer
     * @return A new Response instance with generated ID and default values
     * @throws IllegalArgumentException if questionId is null or empty
     * @throws IllegalArgumentException if selectedAnswer is null
     */
    @NonNull
    public static Response create(@NonNull String questionId, @NonNull String selectedAnswer) {
        if (questionId == null || questionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Response questionId cannot be null or empty");
        }
        if (selectedAnswer == null) {
            throw new IllegalArgumentException("Response selectedAnswer cannot be null");
        }
        return new Response(
                UUID.randomUUID().toString(),
                questionId,
                selectedAnswer,
                false,
                System.currentTimeMillis(),
                false
        );
    }

    /**
     * Constructor with all fields.
     *
     * @param id             Unique identifier (UUID)
     * @param questionId     UUID of the parent question
     * @param selectedAnswer The student's selected answer
     * @param isCorrect      Whether the response is correct
     * @param timestamp      When the response was recorded (Unix epoch milliseconds)
     * @param synced         Whether response has been synced to teacher's Windows app
     * @throws IllegalArgumentException if id is null or empty
     * @throws IllegalArgumentException if questionId is null or empty
     * @throws IllegalArgumentException if selectedAnswer is null
     * @throws IllegalArgumentException if timestamp is negative
     */
    public Response(@NonNull String id,
                    @NonNull String questionId,
                    @NonNull String selectedAnswer,
                    boolean isCorrect,
                    long timestamp,
                    boolean synced) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Response id cannot be null or empty");
        }
        if (questionId == null || questionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Response questionId cannot be null or empty");
        }
        if (selectedAnswer == null) {
            throw new IllegalArgumentException("Response selectedAnswer cannot be null");
        }
        if (timestamp < 0) {
            throw new IllegalArgumentException("Response timestamp cannot be negative");
        }

        this.id = id;
        this.questionId = questionId;
        this.selectedAnswer = selectedAnswer;
        this.isCorrect = isCorrect;
        this.timestamp = timestamp;
        this.synced = synced;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getQuestionId() {
        return questionId;
    }

    @NonNull
    public String getSelectedAnswer() {
        return selectedAnswer;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isSynced() {
        return synced;
    }
}
