package com.manuscripta.student.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A generic wrapper class representing the result of an operation.
 * Can be either Success (containing data) or Error (containing error information).
 *
 * @param <T> The type of data contained in a successful result.
 */
public abstract class Result<T> {

    /**
     * Private constructor to prevent external instantiation.
     * Only Success and Error subclasses can be created.
     */
    private Result() {
        // Sealed class pattern - only inner classes can extend
    }

    /**
     * Checks if this result represents a success.
     *
     * @return true if this is a Success result, false otherwise.
     */
    public abstract boolean isSuccess();

    /**
     * Checks if this result represents an error.
     *
     * @return true if this is an Error result, false otherwise.
     */
    public boolean isError() {
        return !isSuccess();
    }

    /**
     * Gets the data from a Success result.
     *
     * @return The data if this is a Success result, null otherwise.
     */
    @Nullable
    public abstract T getData();

    /**
     * Gets the error message from an Error result.
     *
     * @return The error message if this is an Error result, null otherwise.
     */
    @Nullable
    public abstract String getErrorMessage();

    /**
     * Gets the exception from an Error result.
     *
     * @return The exception if this is an Error result, null otherwise.
     */
    @Nullable
    public abstract Throwable getException();

    /**
     * Creates a Success result containing the given data.
     *
     * @param data The data to wrap in a Success result.
     * @param <T>  The type of the data.
     * @return A new Success result containing the data.
     */
    @NonNull
    public static <T> Result<T> success(@Nullable T data) {
        return new Success<>(data);
    }

    /**
     * Creates an Error result with the given message.
     *
     * @param message The error message.
     * @param <T>     The type parameter for the result.
     * @return A new Error result with the given message.
     */
    @NonNull
    public static <T> Result<T> error(@NonNull String message) {
        return new Error<>(message, null);
    }

    /**
     * Creates an Error result with the given message and exception.
     *
     * @param message   The error message.
     * @param exception The exception that caused the error.
     * @param <T>       The type parameter for the result.
     * @return A new Error result with the given message and exception.
     */
    @NonNull
    public static <T> Result<T> error(@NonNull String message, @Nullable Throwable exception) {
        return new Error<>(message, exception);
    }

    /**
     * Creates an Error result from an exception.
     *
     * @param exception The exception that caused the error.
     * @param <T>       The type parameter for the result.
     * @return A new Error result with the exception's message.
     */
    @NonNull
    public static <T> Result<T> error(@NonNull Throwable exception) {
        String message = exception.getMessage();
        if (message == null) {
            message = exception.getClass().getSimpleName();
        }
        return new Error<>(message, exception);
    }

    /**
     * Represents a successful result containing data.
     *
     * @param <T> The type of data contained in this result.
     */
    public static final class Success<T> extends Result<T> {

        /**
         * The data contained in this successful result.
         */
        private final T data;

        /**
         * Creates a new Success result with the given data.
         *
         * @param data The data to store in this result.
         */
        private Success(@Nullable T data) {
            this.data = data;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        @Nullable
        public T getData() {
            return data;
        }

        @Override
        @Nullable
        public String getErrorMessage() {
            return null;
        }

        @Override
        @Nullable
        public Throwable getException() {
            return null;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Success<?> success = (Success<?>) obj;
            if (data == null) {
                return success.data == null;
            }
            return data.equals(success.data);
        }

        @Override
        public int hashCode() {
            return data != null ? data.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "Success{data=" + data + "}";
        }
    }

    /**
     * Represents an error result containing error information.
     *
     * @param <T> The type parameter (for consistency with Success).
     */
    public static final class Error<T> extends Result<T> {

        /**
         * The error message describing what went wrong.
         */
        private final String message;

        /**
         * The exception that caused the error, if any.
         */
        private final Throwable exception;

        /**
         * Creates a new Error result with the given message and exception.
         *
         * @param message   The error message.
         * @param exception The exception that caused the error.
         */
        private Error(@NonNull String message, @Nullable Throwable exception) {
            this.message = message;
            this.exception = exception;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        @Nullable
        public T getData() {
            return null;
        }

        @Override
        @NonNull
        public String getErrorMessage() {
            return message;
        }

        @Override
        @Nullable
        public Throwable getException() {
            return exception;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Error<?> error = (Error<?>) obj;
            if (!message.equals(error.message)) {
                return false;
            }
            if (exception == null) {
                return error.exception == null;
            }
            return exception.equals(error.exception);
        }

        @Override
        public int hashCode() {
            int result = message.hashCode();
            result = 31 * result + (exception != null ? exception.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Error{message='" + message + "', exception=" + exception + "}";
        }
    }
}
