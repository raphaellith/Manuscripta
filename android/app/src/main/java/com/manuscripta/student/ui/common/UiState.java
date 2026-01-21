package com.manuscripta.student.ui.common;

import androidx.annotation.Nullable;

/**
 * Represents the state of a UI operation (loading, success, error).
 *
 * @param <T> The type of data in the success state
 */
public class UiState<T> {

    private final Status status;
    @Nullable
    private final T data;
    @Nullable
    private final String errorMessage;

    private UiState(Status status, @Nullable T data, @Nullable String errorMessage) {
        this.status = status;
        this.data = data;
        this.errorMessage = errorMessage;
    }

    /**
     * Creates a loading state.
     *
     * @param <T> The type of data
     * @return A loading UI state
     */
    public static <T> UiState<T> loading() {
        return new UiState<>(Status.LOADING, null, null);
    }

    /**
     * Creates a success state with data.
     *
     * @param data The data
     * @param <T>  The type of data
     * @return A success UI state
     */
    public static <T> UiState<T> success(T data) {
        return new UiState<>(Status.SUCCESS, data, null);
    }

    /**
     * Creates an error state with a message.
     *
     * @param message The error message
     * @param <T>     The type of data
     * @return An error UI state
     */
    public static <T> UiState<T> error(String message) {
        return new UiState<>(Status.ERROR, null, message);
    }

    /**
     * Returns the status.
     *
     * @return The status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Returns the data (may be null).
     *
     * @return The data
     */
    @Nullable
    public T getData() {
        return data;
    }

    /**
     * Returns the error message (may be null).
     *
     * @return The error message
     */
    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns whether this is a loading state.
     *
     * @return true if loading
     */
    public boolean isLoading() {
        return status == Status.LOADING;
    }

    /**
     * Returns whether this is a success state.
     *
     * @return true if success
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * Returns whether this is an error state.
     *
     * @return true if error
     */
    public boolean isError() {
        return status == Status.ERROR;
    }

    /**
     * Status enum for UI states.
     */
    public enum Status {
        /** Loading state. */
        LOADING,
        /** Success state. */
        SUCCESS,
        /** Error state. */
        ERROR
    }
}
