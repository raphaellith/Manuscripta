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
     */
    @NonNull
    public static Response create(@NonNull String questionId, @NonNull String selectedAnswer) {
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
     */
    public Response(@NonNull String id,
                    @NonNull String questionId,
                    @NonNull String selectedAnswer,
                    boolean isCorrect,
                    long timestamp,
                    boolean synced) {
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
