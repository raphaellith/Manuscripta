package com.manuscripta.student.domain.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link Result} wrapper class.
 */
public class ResultTest {

    @Test
    public void testSuccess_withData() {
        // Given
        String data = "test data";

        // When
        Result<String> result = Result.success(data);

        // Then
        assertTrue(result.isSuccess());
        assertFalse(result.isError());
        assertEquals(data, result.getData());
        assertNull(result.getError());
    }

    @Test
    public void testSuccess_withNullData() {
        // When
        Result<String> result = Result.success(null);

        // Then
        assertTrue(result.isSuccess());
        assertFalse(result.isError());
        assertNull(result.getData());
        assertNull(result.getError());
    }

    @Test
    public void testSuccess_isInstanceOfSuccess() {
        // When
        Result<String> result = Result.success("data");

        // Then
        assertTrue(result instanceof Result.Success);
    }

    @Test
    public void testError_withException() {
        // Given
        Exception exception = new RuntimeException("test error");

        // When
        Result<String> result = Result.error(exception);

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.isError());
        assertNull(result.getData());
        assertEquals(exception, result.getError());
    }

    @Test
    public void testError_isInstanceOfError() {
        // When
        Result<String> result = Result.error(new RuntimeException("error"));

        // Then
        assertTrue(result instanceof Result.Error);
    }

    @Test
    public void testError_withNullException_throwsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Result.error(null)
        );
        assertEquals("Error cannot be null", exception.getMessage());
    }

    @Test
    public void testGetDataOrDefault_success_returnsData() {
        // Given
        String data = "actual data";
        Result<String> result = Result.success(data);

        // When & Then
        assertEquals(data, result.getDataOrDefault("default"));
    }

    @Test
    public void testGetDataOrDefault_successWithNull_returnsNull() {
        // Given
        Result<String> result = Result.success(null);

        // When & Then
        assertNull(result.getDataOrDefault("default"));
    }

    @Test
    public void testGetDataOrDefault_error_returnsDefault() {
        // Given
        Result<String> result = Result.error(new RuntimeException("error"));

        // When & Then
        assertEquals("default", result.getDataOrDefault("default"));
    }

    @Test
    public void testGetDataOrDefault_error_withNullDefault_returnsNull() {
        // Given
        Result<String> result = Result.error(new RuntimeException("error"));

        // When & Then
        assertNull(result.getDataOrDefault(null));
    }

    @Test
    public void testSuccess_withComplexType() {
        // Given
        TestObject data = new TestObject("name", 42);

        // When
        Result<TestObject> result = Result.success(data);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("name", result.getData().getName());
        assertEquals(42, result.getData().getValue());
    }

    @Test
    public void testError_preservesExceptionDetails() {
        // Given
        String errorMessage = "Detailed error message";
        Exception cause = new IllegalStateException("root cause");
        RuntimeException exception = new RuntimeException(errorMessage, cause);

        // When
        Result<String> result = Result.error(exception);

        // Then
        assertNotNull(result.getError());
        assertEquals(errorMessage, result.getError().getMessage());
        assertEquals(cause, result.getError().getCause());
    }

    @Test
    public void testError_getData_returnsNull() {
        // Given
        Result<String> result = Result.error(new RuntimeException("error"));

        // When & Then
        assertNull(result.getData());
    }

    @Test
    public void testSuccess_getError_returnsNull() {
        // Given
        Result<String> result = Result.success("data");

        // When & Then
        assertNull(result.getError());
    }

    @Test
    public void testSuccess_withPrimitiveWrapper() {
        // When
        Result<Integer> result = Result.success(123);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(Integer.valueOf(123), result.getData());
    }

    @Test
    public void testError_withCheckedExceptionType() {
        // Given
        Exception checkedException = new Exception("checked exception");

        // When
        Result<String> result = Result.error(checkedException);

        // Then
        assertTrue(result.isError());
        assertEquals(checkedException, result.getError());
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
