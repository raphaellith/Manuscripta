package com.manuscripta.student.domain.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Represents the state of a UI operation that can be in one of three states:
 * Loading, Success, or Error.
 *
 * <p>This class is typically used to represent the state of data being fetched
 * or operations being performed, allowing the UI to react appropriately to each state.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * UiState&lt;List&lt;User&gt;&gt; state = UiState.loading();
 * // later...
 * state = UiState.success(userList);
 * // or on error...
 * state = UiState.error(exception);
 * </pre>
 *
 * @param <T> The type of data associated with the success state
 */
public abstract class UiState<T> {

    /**
     * Private constructor to prevent external instantiation.
     * Use factory methods {@link #loading()}, {@link #success(Object)},
     * and {@link #error(Throwable)} instead.
     */
    private UiState() {
        // Private constructor to simulate sealed class
    }

    /**
     * Creates a loading state with no data.
     *
     * @param <T> The type of the success value
     * @return A UiState representing the loading state
     */
    @NonNull
    public static <T> UiState<T> loading() {
        return new Loading<>();
    }

    /**
     * Creates a success state containing the given data.
     *
     * @param data The success value (may be null)
     * @param <T>  The type of the success value
     * @return A UiState representing success with the given data
     */
    @NonNull
    public static <T> UiState<T> success(@Nullable T data) {
        return new Success<>(data);
    }

    /**
     * Creates an error state containing the given error.
     *
     * @param error The error that occurred (must not be null)
     * @param <T>   The type of the success value (not used for errors)
     * @return A UiState representing the error state
     * @throws IllegalArgumentException if error is null
     */
    @NonNull
    public static <T> UiState<T> error(@NonNull Throwable error) {
        if (error == null) {
            throw new IllegalArgumentException("Error cannot be null");
        }
        return new Error<>(error);
    }

    /**
     * Returns whether this state represents a loading state.
     *
     * @return true if this is a loading state, false otherwise
     */
    public abstract boolean isLoading();

    /**
     * Returns whether this state represents a successful outcome.
     *
     * @return true if this is a success state, false otherwise
     */
    public abstract boolean isSuccess();

    /**
     * Returns whether this state represents an error.
     *
     * @return true if this is an error state, false otherwise
     */
    public abstract boolean isError();

    /**
     * Returns the success data if this is a success state, or null otherwise.
     *
     * @return The success data, or null
     */
    @Nullable
    public abstract T getData();

    /**
     * Returns the error if this is an error state, or null otherwise.
     *
     * @return The error, or null
     */
    @Nullable
    public abstract Throwable getError();

    /**
     * Returns the success data if this is a success state, or the given default value otherwise.
     *
     * @param defaultValue The value to return if this is not a success state
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
     * Represents the loading state.
     *
     * @param <T> The type of the success value
     */
    public static final class Loading<T> extends UiState<T> {

        /**
         * Creates a loading state.
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
        public Throwable getError() {
            return null;
        }
    }

    /**
     * Represents a successful state containing data.
     *
     * @param <T> The type of the success value
     */
    public static final class Success<T> extends UiState<T> {

        /** The success data. */
        @Nullable
        private final T data;

        /**
         * Creates a success state with the given data.
         *
         * @param data The success value (may be null)
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
        public Throwable getError() {
            return null;
        }
    }

    /**
     * Represents an error state containing an exception.
     *
     * @param <T> The type of the success value (not used for errors)
     */
    public static final class Error<T> extends UiState<T> {

        /** The error that occurred. */
        @NonNull
        private final Throwable error;

        /**
         * Creates an error state with the given throwable.
         *
         * @param error The error that occurred
         */
        private Error(@NonNull Throwable error) {
            this.error = error;
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
        @NonNull
        public Throwable getError() {
            return error;
        }
    }
}
