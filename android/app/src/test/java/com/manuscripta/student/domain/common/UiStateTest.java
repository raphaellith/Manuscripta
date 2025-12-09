package com.manuscripta.student.domain.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link UiState} wrapper class.
 */
public class UiStateTest {

    // Loading state tests

    @Test
    public void testLoading_createsLoadingState() {
        // When
        UiState<String> state = UiState.loading();

        // Then
        assertTrue(state.isLoading());
        assertFalse(state.isSuccess());
        assertFalse(state.isError());
    }

    @Test
    public void testLoading_isInstanceOfLoading() {
        // When
        UiState<String> state = UiState.loading();

        // Then
        assertTrue(state instanceof UiState.Loading);
    }

    @Test
    public void testLoading_getData_returnsNull() {
        // When
        UiState<String> state = UiState.loading();

        // Then
        assertNull(state.getData());
    }

    @Test
    public void testLoading_getError_returnsNull() {
        // When
        UiState<String> state = UiState.loading();

        // Then
        assertNull(state.getError());
    }

    @Test
    public void testLoading_getDataOrDefault_returnsDefault() {
        // Given
        UiState<String> state = UiState.loading();

        // When & Then
        assertEquals("default", state.getDataOrDefault("default"));
    }

    // Success state tests

    @Test
    public void testSuccess_withData() {
        // Given
        String data = "test data";

        // When
        UiState<String> state = UiState.success(data);

        // Then
        assertFalse(state.isLoading());
        assertTrue(state.isSuccess());
        assertFalse(state.isError());
        assertEquals(data, state.getData());
        assertNull(state.getError());
    }

    @Test
    public void testSuccess_withNullData() {
        // When
        UiState<String> state = UiState.success(null);

        // Then
        assertTrue(state.isSuccess());
        assertNull(state.getData());
    }

    @Test
    public void testSuccess_isInstanceOfSuccess() {
        // When
        UiState<String> state = UiState.success("data");

        // Then
        assertTrue(state instanceof UiState.Success);
    }

    @Test
    public void testSuccess_getDataOrDefault_returnsData() {
        // Given
        String data = "actual data";
        UiState<String> state = UiState.success(data);

        // When & Then
        assertEquals(data, state.getDataOrDefault("default"));
    }

    @Test
    public void testSuccess_withNullData_getDataOrDefault_returnsNull() {
        // Given
        UiState<String> state = UiState.success(null);

        // When & Then
        assertNull(state.getDataOrDefault("default"));
    }

    // Error state tests

    @Test
    public void testError_withException() {
        // Given
        Exception exception = new RuntimeException("test error");

        // When
        UiState<String> state = UiState.error(exception);

        // Then
        assertFalse(state.isLoading());
        assertFalse(state.isSuccess());
        assertTrue(state.isError());
        assertNull(state.getData());
        assertEquals(exception, state.getError());
    }

    @Test
    public void testError_isInstanceOfError() {
        // When
        UiState<String> state = UiState.error(new RuntimeException("error"));

        // Then
        assertTrue(state instanceof UiState.Error);
    }

    @Test
    public void testError_withNullException_throwsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> UiState.error(null)
        );
        assertEquals("Error cannot be null", exception.getMessage());
    }

    @Test
    public void testError_getDataOrDefault_returnsDefault() {
        // Given
        UiState<String> state = UiState.error(new RuntimeException("error"));

        // When & Then
        assertEquals("default", state.getDataOrDefault("default"));
    }

    @Test
    public void testError_getDataOrDefault_withNullDefault_returnsNull() {
        // Given
        UiState<String> state = UiState.error(new RuntimeException("error"));

        // When & Then
        assertNull(state.getDataOrDefault(null));
    }

    @Test
    public void testError_preservesExceptionDetails() {
        // Given
        String errorMessage = "Detailed error message";
        Exception cause = new IllegalStateException("root cause");
        RuntimeException exception = new RuntimeException(errorMessage, cause);

        // When
        UiState<String> state = UiState.error(exception);

        // Then
        assertNotNull(state.getError());
        assertEquals(errorMessage, state.getError().getMessage());
        assertEquals(cause, state.getError().getCause());
    }

    // Complex type tests

    @Test
    public void testSuccess_withComplexType() {
        // Given
        TestObject data = new TestObject("name", 42);

        // When
        UiState<TestObject> state = UiState.success(data);

        // Then
        assertTrue(state.isSuccess());
        assertNotNull(state.getData());
        assertEquals("name", state.getData().getName());
        assertEquals(42, state.getData().getValue());
    }

    @Test
    public void testSuccess_withPrimitiveWrapper() {
        // When
        UiState<Integer> state = UiState.success(123);

        // Then
        assertTrue(state.isSuccess());
        assertEquals(Integer.valueOf(123), state.getData());
    }

    @Test
    public void testError_withCheckedExceptionType() {
        // Given
        Exception checkedException = new Exception("checked exception");

        // When
        UiState<String> state = UiState.error(checkedException);

        // Then
        assertTrue(state.isError());
        assertEquals(checkedException, state.getError());
    }

    // State transition simulation tests

    @Test
    public void testStateTransition_loadingToSuccess() {
        // Given - simulate loading state
        UiState<String> loadingState = UiState.loading();
        assertTrue(loadingState.isLoading());

        // When - transition to success
        UiState<String> successState = UiState.success("loaded data");

        // Then
        assertFalse(loadingState.isSuccess());
        assertTrue(successState.isSuccess());
        assertEquals("loaded data", successState.getData());
    }

    @Test
    public void testStateTransition_loadingToError() {
        // Given - simulate loading state
        UiState<String> loadingState = UiState.loading();
        assertTrue(loadingState.isLoading());

        // When - transition to error
        RuntimeException exception = new RuntimeException("network error");
        UiState<String> errorState = UiState.error(exception);

        // Then
        assertFalse(loadingState.isError());
        assertTrue(errorState.isError());
        assertEquals(exception, errorState.getError());
    }

    /**
     * Helper class for testing complex types.
     */
    private static class TestObject {
        private final String name;
        private final int value;

        TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        String getName() {
            return name;
        }

        int getValue() {
            return value;
        }
    }
}
