package com.manuscripta.student.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the UiState wrapper class.
 */
public class UiStateTest {

    // ==================== Loading Tests ====================

    @Test
    public void testLoading_createsLoadingState() {
        UiState<String> state = UiState.loading();

        assertTrue(state.isLoading());
        assertFalse(state.isSuccess());
        assertFalse(state.isError());
        assertNull(state.getData());
        assertNull(state.getErrorMessage());
        assertNull(state.getException());
    }

    @Test
    public void testLoading_isLoadingInstance() {
        UiState<String> state = UiState.loading();
        assertTrue(state instanceof UiState.Loading);
    }

    @Test
    public void testLoading_toString() {
        UiState<String> state = UiState.loading();
        String toString = state.toString();
        assertTrue(toString.contains("Loading"));
    }

    @Test
    public void testLoading_equals_sameType() {
        UiState<String> state1 = UiState.loading();
        UiState<String> state2 = UiState.loading();
        assertEquals(state1, state2);
    }

    @Test
    public void testLoading_equals_sameInstance() {
        UiState<String> state = UiState.loading();
        assertEquals(state, state);
    }

    @Test
    public void testLoading_equals_null() {
        UiState<String> state = UiState.loading();
        assertNotEquals(state, null);
    }

    @Test
    public void testLoading_equals_differentType() {
        UiState<String> state = UiState.loading();
        assertNotEquals(state, "loading");
    }

    @Test
    public void testLoading_hashCode() {
        UiState<String> state1 = UiState.loading();
        UiState<String> state2 = UiState.loading();
        assertEquals(state1.hashCode(), state2.hashCode());
    }

    // ==================== Success Tests ====================

    @Test
    public void testSuccess_withData_createsSuccessState() {
        String data = "test data";
        UiState<String> state = UiState.success(data);

        assertFalse(state.isLoading());
        assertTrue(state.isSuccess());
        assertFalse(state.isError());
        assertEquals(data, state.getData());
        assertNull(state.getErrorMessage());
        assertNull(state.getException());
    }

    @Test
    public void testSuccess_withNullData_createsSuccessState() {
        UiState<String> state = UiState.success(null);

        assertFalse(state.isLoading());
        assertTrue(state.isSuccess());
        assertFalse(state.isError());
        assertNull(state.getData());
        assertNull(state.getErrorMessage());
        assertNull(state.getException());
    }

    @Test
    public void testSuccess_isSuccessInstance() {
        UiState<String> state = UiState.success("data");
        assertTrue(state instanceof UiState.Success);
    }

    @Test
    public void testSuccess_toString_containsData() {
        UiState<String> state = UiState.success("test");
        String toString = state.toString();
        assertTrue(toString.contains("Success"));
        assertTrue(toString.contains("test"));
    }

    @Test
    public void testSuccess_toString_withNullData() {
        UiState<String> state = UiState.success(null);
        String toString = state.toString();
        assertTrue(toString.contains("Success"));
        assertTrue(toString.contains("null"));
    }

    @Test
    public void testSuccess_equals_sameData() {
        UiState<String> state1 = UiState.success("data");
        UiState<String> state2 = UiState.success("data");
        assertEquals(state1, state2);
    }

    @Test
    public void testSuccess_equals_differentData() {
        UiState<String> state1 = UiState.success("data1");
        UiState<String> state2 = UiState.success("data2");
        assertNotEquals(state1, state2);
    }

    @Test
    public void testSuccess_equals_nullData() {
        UiState<String> state1 = UiState.success(null);
        UiState<String> state2 = UiState.success(null);
        assertEquals(state1, state2);
    }

    @Test
    public void testSuccess_equals_oneNullData() {
        UiState<String> state1 = UiState.success("data");
        UiState<String> state2 = UiState.success(null);
        assertNotEquals(state1, state2);
    }

    @Test
    public void testSuccess_equals_nullDataWithNonNullData() {
        UiState<String> state1 = UiState.success(null);
        UiState<String> state2 = UiState.success("data");
        assertNotEquals(state1, state2);
    }

    @Test
    public void testSuccess_equals_sameInstance() {
        UiState<String> state = UiState.success("data");
        assertEquals(state, state);
    }

    @Test
    public void testSuccess_equals_null() {
        UiState<String> state = UiState.success("data");
        assertNotEquals(state, null);
    }

    @Test
    public void testSuccess_equals_differentType() {
        UiState<String> state = UiState.success("data");
        assertNotEquals(state, "data");
    }

    @Test
    public void testSuccess_hashCode_sameData() {
        UiState<String> state1 = UiState.success("data");
        UiState<String> state2 = UiState.success("data");
        assertEquals(state1.hashCode(), state2.hashCode());
    }

    @Test
    public void testSuccess_hashCode_nullData() {
        UiState<String> state = UiState.success(null);
        assertEquals(0, state.hashCode());
    }

    // ==================== Error Tests ====================

    @Test
    public void testError_withMessage_createsErrorState() {
        String message = "error message";
        UiState<String> state = UiState.error(message);

        assertFalse(state.isLoading());
        assertFalse(state.isSuccess());
        assertTrue(state.isError());
        assertNull(state.getData());
        assertEquals(message, state.getErrorMessage());
        assertNull(state.getException());
    }

    @Test
    public void testError_withMessageAndException_createsErrorState() {
        String message = "error message";
        Exception exception = new RuntimeException("test exception");
        UiState<String> state = UiState.error(message, exception);

        assertFalse(state.isLoading());
        assertFalse(state.isSuccess());
        assertTrue(state.isError());
        assertNull(state.getData());
        assertEquals(message, state.getErrorMessage());
        assertEquals(exception, state.getException());
    }

    @Test
    public void testError_withException_usesExceptionMessage() {
        String exceptionMessage = "exception message";
        Exception exception = new RuntimeException(exceptionMessage);
        UiState<String> state = UiState.error(exception);

        assertFalse(state.isLoading());
        assertFalse(state.isSuccess());
        assertTrue(state.isError());
        assertNull(state.getData());
        assertEquals(exceptionMessage, state.getErrorMessage());
        assertEquals(exception, state.getException());
    }

    @Test
    public void testError_withExceptionWithNullMessage_usesClassName() {
        Exception exception = new RuntimeException((String) null);
        UiState<String> state = UiState.error(exception);

        assertFalse(state.isLoading());
        assertFalse(state.isSuccess());
        assertTrue(state.isError());
        assertEquals("RuntimeException", state.getErrorMessage());
        assertEquals(exception, state.getException());
    }

    @Test
    public void testError_isErrorInstance() {
        UiState<String> state = UiState.error("error");
        assertTrue(state instanceof UiState.Error);
    }

    @Test
    public void testError_toString_containsMessage() {
        UiState<String> state = UiState.error("test error");
        String toString = state.toString();
        assertTrue(toString.contains("Error"));
        assertTrue(toString.contains("test error"));
    }

    @Test
    public void testError_toString_containsException() {
        Exception exception = new RuntimeException("exception");
        UiState<String> state = UiState.error("error", exception);
        String toString = state.toString();
        assertTrue(toString.contains("Error"));
        assertTrue(toString.contains("error"));
    }

    @Test
    public void testError_equals_sameMessageAndException() {
        Exception exception = new RuntimeException("ex");
        UiState<String> state1 = UiState.error("error", exception);
        UiState<String> state2 = UiState.error("error", exception);
        assertEquals(state1, state2);
    }

    @Test
    public void testError_equals_differentMessage() {
        UiState<String> state1 = UiState.error("error1");
        UiState<String> state2 = UiState.error("error2");
        assertNotEquals(state1, state2);
    }

    @Test
    public void testError_equals_differentException() {
        Exception ex1 = new RuntimeException("ex1");
        Exception ex2 = new RuntimeException("ex2");
        UiState<String> state1 = UiState.error("error", ex1);
        UiState<String> state2 = UiState.error("error", ex2);
        assertNotEquals(state1, state2);
    }

    @Test
    public void testError_equals_nullException() {
        UiState<String> state1 = UiState.error("error", null);
        UiState<String> state2 = UiState.error("error", null);
        assertEquals(state1, state2);
    }

    @Test
    public void testError_equals_oneNullException() {
        Exception exception = new RuntimeException("ex");
        UiState<String> state1 = UiState.error("error", exception);
        UiState<String> state2 = UiState.error("error", null);
        assertNotEquals(state1, state2);
    }

    @Test
    public void testError_equals_nullExceptionWithNonNullException() {
        Exception exception = new RuntimeException("ex");
        UiState<String> state1 = UiState.error("error", null);
        UiState<String> state2 = UiState.error("error", exception);
        assertNotEquals(state1, state2);
    }

    @Test
    public void testError_equals_sameInstance() {
        UiState<String> state = UiState.error("error");
        assertEquals(state, state);
    }

    @Test
    public void testError_equals_null() {
        UiState<String> state = UiState.error("error");
        assertNotEquals(state, null);
    }

    @Test
    public void testError_equals_differentType() {
        UiState<String> state = UiState.error("error");
        assertNotEquals(state, "error");
    }

    @Test
    public void testError_hashCode_sameMessage() {
        UiState<String> state1 = UiState.error("error");
        UiState<String> state2 = UiState.error("error");
        assertEquals(state1.hashCode(), state2.hashCode());
    }

    @Test
    public void testError_hashCode_withException() {
        Exception exception = new RuntimeException("ex");
        UiState<String> state = UiState.error("error", exception);
        assertNotNull(state.hashCode());
    }

    @Test
    public void testError_hashCode_withNullException() {
        UiState<String> state = UiState.error("error", null);
        int hash = state.hashCode();
        int expected = 31 * "error".hashCode();
        assertEquals(expected, hash);
    }

    // ==================== fromResult Tests ====================

    @Test
    public void testFromResult_success_createsSuccessState() {
        Result<String> result = Result.success("data");
        UiState<String> state = UiState.fromResult(result);

        assertTrue(state.isSuccess());
        assertFalse(state.isLoading());
        assertFalse(state.isError());
        assertEquals("data", state.getData());
    }

    @Test
    public void testFromResult_error_createsErrorState() {
        Exception exception = new RuntimeException("ex");
        Result<String> result = Result.error("error message", exception);
        UiState<String> state = UiState.fromResult(result);

        assertTrue(state.isError());
        assertFalse(state.isLoading());
        assertFalse(state.isSuccess());
        assertEquals("error message", state.getErrorMessage());
        assertEquals(exception, state.getException());
    }

    @Test
    public void testFromResult_errorWithNullData_createsErrorState() {
        Result<String> result = Result.error("error");
        UiState<String> state = UiState.fromResult(result);

        assertTrue(state.isError());
        assertEquals("error", state.getErrorMessage());
        assertNull(state.getException());
    }

    // ==================== Type Safety Tests ====================

    @Test
    public void testSuccess_withDifferentTypes() {
        UiState<Integer> intState = UiState.success(42);
        UiState<Boolean> boolState = UiState.success(true);

        assertEquals(Integer.valueOf(42), intState.getData());
        assertEquals(Boolean.TRUE, boolState.getData());
    }

    @Test
    public void testError_withDifferentTypes() {
        UiState<Integer> intState = UiState.error("int error");
        UiState<Boolean> boolState = UiState.error("bool error");

        assertEquals("int error", intState.getErrorMessage());
        assertEquals("bool error", boolState.getErrorMessage());
    }

    // ==================== Cross-type Comparison Tests ====================

    @Test
    public void testLoading_notEqualsSuccess() {
        UiState<String> loading = UiState.loading();
        UiState<String> success = UiState.success("data");
        assertNotEquals(loading, success);
    }

    @Test
    public void testLoading_notEqualsError() {
        UiState<String> loading = UiState.loading();
        UiState<String> error = UiState.error("error");
        assertNotEquals(loading, error);
    }

    @Test
    public void testSuccess_notEqualsError() {
        UiState<String> success = UiState.success("data");
        UiState<String> error = UiState.error("error");
        assertNotEquals(success, error);
    }
}
