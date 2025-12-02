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

    /** The unique identifier for the response. */
    @NonNull
    private final String id;

    /** The ID of the parent question. */
    @NonNull
    private final String questionId;

    /** The answer selected by the student. */
    @NonNull
    private final String selectedAnswer;

    /** Whether the response is correct. */
    private final boolean isCorrect;

    /** The timestamp when the response was recorded (Unix epoch milliseconds). */
    private final long timestamp;

    /** Whether the response has been synced to the teacher's Windows app. */
    private final boolean synced;

    /** The device identifier for the tablet that submitted this response. */
    @NonNull
    private final String deviceId;

    /**
     * Factory method for creating a NEW response.
     * Generates a random UUID, captures the current timestamp, and sets default values.
     *
     * @param questionId     UUID of the parent question
     * @param selectedAnswer The student's selected answer
     * @param deviceId       The device identifier for the tablet submitting this response
     * @return A new Response instance with generated ID and default values
     * @throws IllegalArgumentException if questionId is null or empty
     * @throws IllegalArgumentException if selectedAnswer is null
     * @throws IllegalArgumentException if deviceId is null or empty
     */
    @NonNull
    public static Response create(@NonNull String questionId,
                                  @NonNull String selectedAnswer,
                                  @NonNull String deviceId) {
        if (questionId == null || questionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Response questionId cannot be null or empty");
        }
        if (selectedAnswer == null) {
            throw new IllegalArgumentException("Response selectedAnswer cannot be null");
        }
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Response deviceId cannot be null or empty");
        }
        return new Response(
                UUID.randomUUID().toString(),
                questionId,
                selectedAnswer,
                false,
                System.currentTimeMillis(),
                false,
                deviceId
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
     * @param deviceId       The device identifier for the tablet that submitted this response
     * @throws IllegalArgumentException if id is null or empty
     * @throws IllegalArgumentException if questionId is null or empty
     * @throws IllegalArgumentException if selectedAnswer is null
     * @throws IllegalArgumentException if timestamp is negative
     * @throws IllegalArgumentException if deviceId is null or empty
     */
    public Response(@NonNull String id,
                    @NonNull String questionId,
                    @NonNull String selectedAnswer,
                    boolean isCorrect,
                    long timestamp,
                    boolean synced,
                    @NonNull String deviceId) {
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
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Response deviceId cannot be null or empty");
        }

        this.id = id;
        this.questionId = questionId;
        this.selectedAnswer = selectedAnswer;
        this.isCorrect = isCorrect;
        this.timestamp = timestamp;
        this.synced = synced;
        this.deviceId = deviceId;
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

    @NonNull
    public String getDeviceId() {
        return deviceId;
    }
}
