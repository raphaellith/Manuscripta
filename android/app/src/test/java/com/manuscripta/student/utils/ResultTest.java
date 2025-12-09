package com.manuscripta.student.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the Result wrapper class.
 */
public class ResultTest {

    // ==================== Success Tests ====================

    @Test
    public void testSuccess_withData_createsSuccessResult() {
        String data = "test data";
        Result<String> result = Result.success(data);

        assertTrue(result.isSuccess());
        assertFalse(result.isError());
        assertEquals(data, result.getData());
        assertNull(result.getErrorMessage());
        assertNull(result.getException());
    }

    @Test
    public void testSuccess_withNullData_createsSuccessResult() {
        Result<String> result = Result.success(null);

        assertTrue(result.isSuccess());
        assertFalse(result.isError());
        assertNull(result.getData());
        assertNull(result.getErrorMessage());
        assertNull(result.getException());
    }

    @Test
    public void testSuccess_isSuccessInstance() {
        Result<String> result = Result.success("data");
        assertTrue(result instanceof Result.Success);
    }

    @Test
    public void testSuccess_toString_containsData() {
        Result<String> result = Result.success("test");
        String toString = result.toString();
        assertTrue(toString.contains("Success"));
        assertTrue(toString.contains("test"));
    }

    @Test
    public void testSuccess_toString_withNullData() {
        Result<String> result = Result.success(null);
        String toString = result.toString();
        assertTrue(toString.contains("Success"));
        assertTrue(toString.contains("null"));
    }

    // ==================== Error Tests ====================

    @Test
    public void testError_withMessage_createsErrorResult() {
        String message = "error message";
        Result<String> result = Result.error(message);

        assertFalse(result.isSuccess());
        assertTrue(result.isError());
        assertNull(result.getData());
        assertEquals(message, result.getErrorMessage());
        assertNull(result.getException());
    }

    @Test
    public void testError_withMessageAndException_createsErrorResult() {
        String message = "error message";
        Exception exception = new RuntimeException("test exception");
        Result<String> result = Result.error(message, exception);

        assertFalse(result.isSuccess());
        assertTrue(result.isError());
        assertNull(result.getData());
        assertEquals(message, result.getErrorMessage());
        assertEquals(exception, result.getException());
    }

    @Test
    public void testError_withException_usesExceptionMessage() {
        String exceptionMessage = "exception message";
        Exception exception = new RuntimeException(exceptionMessage);
        Result<String> result = Result.error(exception);

        assertFalse(result.isSuccess());
        assertTrue(result.isError());
        assertNull(result.getData());
        assertEquals(exceptionMessage, result.getErrorMessage());
        assertEquals(exception, result.getException());
    }

    @Test
    public void testError_withExceptionWithNullMessage_usesClassName() {
        Exception exception = new RuntimeException((String) null);
        Result<String> result = Result.error(exception);

        assertFalse(result.isSuccess());
        assertTrue(result.isError());
        assertEquals("RuntimeException", result.getErrorMessage());
        assertEquals(exception, result.getException());
    }

    @Test
    public void testError_isErrorInstance() {
        Result<String> result = Result.error("error");
        assertTrue(result instanceof Result.Error);
    }

    @Test
    public void testError_toString_containsMessage() {
        Result<String> result = Result.error("test error");
        String toString = result.toString();
        assertTrue(toString.contains("Error"));
        assertTrue(toString.contains("test error"));
    }

    @Test
    public void testError_toString_containsException() {
        Exception exception = new RuntimeException("exception");
        Result<String> result = Result.error("error", exception);
        String toString = result.toString();
        assertTrue(toString.contains("Error"));
        assertTrue(toString.contains("error"));
    }

    // ==================== Equals and HashCode Tests ====================

    @Test
    public void testSuccess_equals_sameData() {
        Result<String> result1 = Result.success("data");
        Result<String> result2 = Result.success("data");
        assertEquals(result1, result2);
    }

    @Test
    public void testSuccess_equals_differentData() {
        Result<String> result1 = Result.success("data1");
        Result<String> result2 = Result.success("data2");
        assertNotEquals(result1, result2);
    }

    @Test
    public void testSuccess_equals_nullData() {
        Result<String> result1 = Result.success(null);
        Result<String> result2 = Result.success(null);
        assertEquals(result1, result2);
    }

    @Test
    public void testSuccess_equals_oneNullData() {
        Result<String> result1 = Result.success("data");
        Result<String> result2 = Result.success(null);
        assertNotEquals(result1, result2);
    }

    @Test
    public void testSuccess_equals_nullDataWithNonNullData() {
        Result<String> result1 = Result.success(null);
        Result<String> result2 = Result.success("data");
        assertNotEquals(result1, result2);
    }

    @Test
    public void testSuccess_equals_sameInstance() {
        Result<String> result = Result.success("data");
        assertEquals(result, result);
    }

    @Test
    public void testSuccess_equals_null() {
        Result<String> result = Result.success("data");
        assertNotEquals(result, null);
    }

    @Test
    public void testSuccess_equals_differentType() {
        Result<String> result = Result.success("data");
        assertNotEquals(result, "data");
    }

    @Test
    public void testSuccess_hashCode_sameData() {
        Result<String> result1 = Result.success("data");
        Result<String> result2 = Result.success("data");
        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    public void testSuccess_hashCode_nullData() {
        Result<String> result = Result.success(null);
        assertEquals(0, result.hashCode());
    }

    @Test
    public void testError_equals_sameMessageAndException() {
        Exception exception = new RuntimeException("ex");
        Result<String> result1 = Result.error("error", exception);
        Result<String> result2 = Result.error("error", exception);
        assertEquals(result1, result2);
    }

    @Test
    public void testError_equals_differentMessage() {
        Result<String> result1 = Result.error("error1");
        Result<String> result2 = Result.error("error2");
        assertNotEquals(result1, result2);
    }

    @Test
    public void testError_equals_differentException() {
        Exception ex1 = new RuntimeException("ex1");
        Exception ex2 = new RuntimeException("ex2");
        Result<String> result1 = Result.error("error", ex1);
        Result<String> result2 = Result.error("error", ex2);
        assertNotEquals(result1, result2);
    }

    @Test
    public void testError_equals_nullException() {
        Result<String> result1 = Result.error("error", null);
        Result<String> result2 = Result.error("error", null);
        assertEquals(result1, result2);
    }

    @Test
    public void testError_equals_oneNullException() {
        Exception exception = new RuntimeException("ex");
        Result<String> result1 = Result.error("error", exception);
        Result<String> result2 = Result.error("error", null);
        assertNotEquals(result1, result2);
    }

    @Test
    public void testError_equals_nullExceptionWithNonNullException() {
        Exception exception = new RuntimeException("ex");
        Result<String> result1 = Result.error("error", null);
        Result<String> result2 = Result.error("error", exception);
        assertNotEquals(result1, result2);
    }

    @Test
    public void testError_equals_sameInstance() {
        Result<String> result = Result.error("error");
        assertEquals(result, result);
    }

    @Test
    public void testError_equals_null() {
        Result<String> result = Result.error("error");
        assertNotEquals(result, null);
    }

    @Test
    public void testError_equals_differentType() {
        Result<String> result = Result.error("error");
        assertNotEquals(result, "error");
    }

    @Test
    public void testError_hashCode_sameMessage() {
        Result<String> result1 = Result.error("error");
        Result<String> result2 = Result.error("error");
        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    public void testError_hashCode_withException() {
        Exception exception = new RuntimeException("ex");
        Result<String> result = Result.error("error", exception);
        assertNotNull(result.hashCode());
    }

    @Test
    public void testError_hashCode_withNullException() {
        Result<String> result = Result.error("error", null);
        int hash = result.hashCode();
        int expected = 31 * "error".hashCode();
        assertEquals(expected, hash);
    }

    // ==================== Type Safety Tests ====================

    @Test
    public void testSuccess_withDifferentTypes() {
        Result<Integer> intResult = Result.success(42);
        Result<Boolean> boolResult = Result.success(true);

        assertEquals(Integer.valueOf(42), intResult.getData());
        assertEquals(Boolean.TRUE, boolResult.getData());
    }

    @Test
    public void testError_withDifferentTypes() {
        Result<Integer> intResult = Result.error("int error");
        Result<Boolean> boolResult = Result.error("bool error");

        assertEquals("int error", intResult.getErrorMessage());
        assertEquals("bool error", boolResult.getErrorMessage());
    }

    // ==================== Cross-type Comparison Tests ====================

    @Test
    public void testSuccess_notEqualsError() {
        Result<String> success = Result.success("data");
        Result<String> error = Result.error("error");
        assertNotEquals(success, error);
    }
}
