package com.manuscripta.student.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A generic wrapper class representing the state of UI operations.
 * Can be Loading, Success (containing data), or Error (containing error information).
 * Useful for representing asynchronous operation states in ViewModels.
 *
 * @param <T> The type of data contained in a successful state.
 */
public abstract class UiState<T> {

    /**
     * Private constructor to prevent external instantiation.
     * Only Loading, Success, and Error subclasses can be created.
     */
    private UiState() {
        // Sealed class pattern - only inner classes can extend
    }

    /**
     * Checks if this state represents loading.
     *
     * @return true if this is a Loading state, false otherwise.
     */
    public abstract boolean isLoading();

    /**
     * Checks if this state represents a success.
     *
     * @return true if this is a Success state, false otherwise.
     */
    public abstract boolean isSuccess();

    /**
     * Checks if this state represents an error.
     *
     * @return true if this is an Error state, false otherwise.
     */
    public abstract boolean isError();

    /**
     * Gets the data from a Success state.
     *
     * @return The data if this is a Success state, null otherwise.
     */
    @Nullable
    public abstract T getData();

    /**
     * Gets the error message from an Error state.
     *
     * @return The error message if this is an Error state, null otherwise.
     */
    @Nullable
    public abstract String getErrorMessage();

    /**
     * Gets the exception from an Error state.
     *
     * @return The exception if this is an Error state, null otherwise.
     */
    @Nullable
    public abstract Throwable getException();

    /**
     * Creates a Loading state.
     *
     * @param <T> The type parameter for the state.
     * @return A new Loading state.
     */
    @NonNull
    public static <T> UiState<T> loading() {
        return new Loading<>();
    }

    /**
     * Creates a Success state containing the given data.
     *
     * @param data The data to wrap in a Success state.
     * @param <T>  The type of the data.
     * @return A new Success state containing the data.
     */
    @NonNull
    public static <T> UiState<T> success(@Nullable T data) {
        return new Success<>(data);
    }

    /**
     * Creates an Error state with the given message.
     *
     * @param message The error message.
     * @param <T>     The type parameter for the state.
     * @return A new Error state with the given message.
     */
    @NonNull
    public static <T> UiState<T> error(@NonNull String message) {
        return new Error<>(message, null);
    }

    /**
     * Creates an Error state with the given message and exception.
     *
     * @param message   The error message.
     * @param exception The exception that caused the error.
     * @param <T>       The type parameter for the state.
     * @return A new Error state with the given message and exception.
     */
    @NonNull
    public static <T> UiState<T> error(@NonNull String message, @Nullable Throwable exception) {
        return new Error<>(message, exception);
    }

    /**
     * Creates an Error state from an exception.
     *
     * @param exception The exception that caused the error.
     * @param <T>       The type parameter for the state.
     * @return A new Error state with the exception's message.
     */
    @NonNull
    public static <T> UiState<T> error(@NonNull Throwable exception) {
        String message = exception.getMessage();
        if (message == null) {
            message = exception.getClass().getSimpleName();
        }
        return new Error<>(message, exception);
    }

    /**
     * Creates a UiState from a Result.
     *
     * @param result The result to convert.
     * @param <T>    The type of data.
     * @return A Success or Error UiState based on the Result.
     */
    @NonNull
    public static <T> UiState<T> fromResult(@NonNull Result<T> result) {
        if (result.isSuccess()) {
            return success(result.getData());
        } else {
            return error(result.getErrorMessage(), result.getException());
        }
    }

    /**
     * Represents a loading state.
     *
     * @param <T> The type parameter (for consistency with other states).
     */
    public static final class Loading<T> extends UiState<T> {

        /**
         * Creates a new Loading state.
         */
        private Loading() {
            // Private constructor
        }

        @Override
        public boolean isLoading() {
            return true;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public boolean isError() {
            return false;
        }

        @Override
        @Nullable
        public T getData() {
            return null;
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
            return obj != null && getClass() == obj.getClass();
        }

        @Override
        public int hashCode() {
            return Loading.class.hashCode();
        }

        @Override
        public String toString() {
            return "Loading{}";
        }
    }

    /**
     * Represents a successful state containing data.
     *
     * @param <T> The type of data contained in this state.
     */
    public static final class Success<T> extends UiState<T> {

        /**
         * The data contained in this successful state.
         */
        private final T data;

        /**
         * Creates a new Success state with the given data.
         *
         * @param data The data to store in this state.
         */
        private Success(@Nullable T data) {
            this.data = data;
        }

        @Override
        public boolean isLoading() {
            return false;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public boolean isError() {
            return false;
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
     * Represents an error state containing error information.
     *
     * @param <T> The type parameter (for consistency with Success).
     */
    public static final class Error<T> extends UiState<T> {

        /**
         * The error message describing what went wrong.
         */
        private final String message;

        /**
         * The exception that caused the error, if any.
         */
        private final Throwable exception;

        /**
         * Creates a new Error state with the given message and exception.
         *
         * @param message   The error message.
         * @param exception The exception that caused the error.
         */
        private Error(@NonNull String message, @Nullable Throwable exception) {
            this.message = message;
            this.exception = exception;
        }

        @Override
        public boolean isLoading() {
            return false;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public boolean isError() {
            return true;
        }

        @Override
        @Nullable
        public T getData() {
            return null;
        }

        @Override
        @Nullable
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
