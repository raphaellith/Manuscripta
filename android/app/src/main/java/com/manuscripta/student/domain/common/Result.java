package com.manuscripta.student.domain.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A discriminated union that encapsulates a successful outcome with a value of type T
 * or a failure with an exception.
 *
 * <p>This class is used to represent the result of operations that may succeed or fail,
 * providing a type-safe way to handle both cases without throwing exceptions.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * Result&lt;User&gt; result = repository.getUser(id);
 * if (result.isSuccess()) {
 *     User user = result.getData();
 *     // handle success
 * } else {
 *     Throwable error = result.getError();
 *     // handle error
 * }
 * </pre>
 *
 * @param <T> The type of the success value
 */
public abstract class Result<T> {

    /**
     * Private constructor to prevent external instantiation.
     * Use factory methods {@link #success(Object)} and {@link #error(Throwable)} instead.
     */
    private Result() {
        // Private constructor to simulate sealed class
    }

    /**
     * Creates a successful result containing the given data.
     *
     * @param data The success value
     * @param <T>  The type of the success value
     * @return A Result representing success with the given data
     */
    @NonNull
    public static <T> Result<T> success(@Nullable T data) {
        return new Success<>(data);
    }

    /**
     * Creates a failed result containing the given error.
     *
     * @param error The error that caused the failure (must not be null)
     * @param <T>   The type of the success value (not used for errors)
     * @return A Result representing failure with the given error
     * @throws IllegalArgumentException if error is null
     */
    @NonNull
    public static <T> Result<T> error(@NonNull Throwable error) {
        if (error == null) {
            throw new IllegalArgumentException("Error cannot be null");
        }
        return new Error<>(error);
    }

    /**
     * Returns whether this result represents a successful outcome.
     *
     * @return true if this is a success result, false otherwise
     */
    public abstract boolean isSuccess();

    /**
     * Returns whether this result represents a failed outcome.
     *
     * @return true if this is an error result, false otherwise
     */
    public boolean isError() {
        return !isSuccess();
    }

    /**
     * Returns the success data if this is a success result, or null if this is an error result.
     *
     * @return The success data, or null
     */
    @Nullable
    public abstract T getData();

    /**
     * Returns the error if this is an error result, or null if this is a success result.
     *
     * @return The error, or null
     */
    @Nullable
    public abstract Throwable getError();

    /**
     * Returns the success data if this is a success result, or the given default value otherwise.
     *
     * @param defaultValue The value to return if this is an error result
     * @return The success data or the default value
     */
    @Nullable
    public T getDataOrDefault(@Nullable T defaultValue) {
        if (isSuccess()) {
            return getData();
        }
        return defaultValue;
    }

    /**
     * Represents a successful result containing data.
     *
     * @param <T> The type of the success value
     */
    public static final class Success<T> extends Result<T> {

        /** The success data. */
        @Nullable
        private final T data;

        /**
         * Creates a successful result with the given data.
         *
         * @param data The success value (may be null)
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
        public Throwable getError() {
            return null;
        }
    }

    /**
     * Represents a failed result containing an error.
     *
     * @param <T> The type of the success value (not used for errors)
     */
    public static final class Error<T> extends Result<T> {

        /** The error that caused the failure. */
        @NonNull
        private final Throwable error;

        /**
         * Creates an error result with the given throwable.
         *
         * @param error The error that caused the failure
         */
        private Error(@NonNull Throwable error) {
            this.error = error;
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
        public Throwable getError() {
            return error;
        }
    }
}
