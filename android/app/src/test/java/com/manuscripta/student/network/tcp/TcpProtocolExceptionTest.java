package com.manuscripta.student.network.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link TcpProtocolException}.
 */
public class TcpProtocolExceptionTest {

    @Test
    public void constructor_withErrorTypeAndMessage_setsFieldsCorrectly() {
        TcpProtocolException exception = new TcpProtocolException(
                TcpProtocolException.ErrorType.EMPTY_DATA,
                "Test message");

        assertEquals(TcpProtocolException.ErrorType.EMPTY_DATA, exception.getErrorType());
        assertEquals("Test message", exception.getMessage());
        assertNull(exception.getInvalidOpcode());
    }

    @Test
    public void constructor_withInvalidOpcode_setsFieldsCorrectly() {
        TcpProtocolException exception = new TcpProtocolException((byte) 0xFF);

        assertEquals(TcpProtocolException.ErrorType.UNKNOWN_OPCODE, exception.getErrorType());
        assertNotNull(exception.getInvalidOpcode());
        assertEquals((byte) 0xFF, exception.getInvalidOpcode().byteValue());
        assertTrue(exception.getMessage().contains("0xFF"));
    }

    @Test
    public void constructor_withZeroOpcode_setsFieldsCorrectly() {
        TcpProtocolException exception = new TcpProtocolException((byte) 0x00);

        assertEquals(TcpProtocolException.ErrorType.UNKNOWN_OPCODE, exception.getErrorType());
        assertNotNull(exception.getInvalidOpcode());
        assertEquals((byte) 0x00, exception.getInvalidOpcode().byteValue());
    }

    @Test
    public void getErrorType_returnsCorrectType() {
        TcpProtocolException emptyData = new TcpProtocolException(
                TcpProtocolException.ErrorType.EMPTY_DATA, "Empty");
        TcpProtocolException malformed = new TcpProtocolException(
                TcpProtocolException.ErrorType.MALFORMED_DATA, "Malformed");
        TcpProtocolException nullMsg = new TcpProtocolException(
                TcpProtocolException.ErrorType.NULL_MESSAGE, "Null");
        TcpProtocolException unknown = new TcpProtocolException((byte) 0x99);

        assertEquals(TcpProtocolException.ErrorType.EMPTY_DATA, emptyData.getErrorType());
        assertEquals(TcpProtocolException.ErrorType.MALFORMED_DATA, malformed.getErrorType());
        assertEquals(TcpProtocolException.ErrorType.NULL_MESSAGE, nullMsg.getErrorType());
        assertEquals(TcpProtocolException.ErrorType.UNKNOWN_OPCODE, unknown.getErrorType());
    }

    @Test
    public void getInvalidOpcode_returnsNullForNonOpcodeErrors() {
        TcpProtocolException exception = new TcpProtocolException(
                TcpProtocolException.ErrorType.EMPTY_DATA,
                "Empty data");

        assertNull(exception.getInvalidOpcode());
    }

    @Test
    public void getInvalidOpcode_returnsOpcodeForOpcodeErrors() {
        TcpProtocolException exception = new TcpProtocolException((byte) 0xAB);

        assertNotNull(exception.getInvalidOpcode());
        assertEquals((byte) 0xAB, exception.getInvalidOpcode().byteValue());
    }

    @Test
    public void toString_withOpcode_containsOpcodeHex() {
        TcpProtocolException exception = new TcpProtocolException((byte) 0xCD);

        String result = exception.toString();

        assertTrue(result.contains("UNKNOWN_OPCODE"));
        assertTrue(result.contains("0xCD"));
    }

    @Test
    public void toString_withoutOpcode_containsErrorTypeAndMessage() {
        TcpProtocolException exception = new TcpProtocolException(
                TcpProtocolException.ErrorType.MALFORMED_DATA,
                "Bad data");

        String result = exception.toString();

        assertTrue(result.contains("MALFORMED_DATA"));
        assertTrue(result.contains("Bad data"));
    }

    @Test
    public void errorType_allValuesExist() {
        TcpProtocolException.ErrorType[] values = TcpProtocolException.ErrorType.values();

        assertEquals(5, values.length);
        assertNotNull(TcpProtocolException.ErrorType.valueOf("EMPTY_DATA"));
        assertNotNull(TcpProtocolException.ErrorType.valueOf("UNKNOWN_OPCODE"));
        assertNotNull(TcpProtocolException.ErrorType.valueOf("MALFORMED_DATA"));
        assertNotNull(TcpProtocolException.ErrorType.valueOf("NULL_MESSAGE"));
        assertNotNull(TcpProtocolException.ErrorType.valueOf("CONNECTION_ERROR"));
    }

    @Test
    public void exception_extendsException() {
        TcpProtocolException exception = new TcpProtocolException(
                TcpProtocolException.ErrorType.EMPTY_DATA, "Test");

        assertTrue(exception instanceof Exception);
    }

    @Test
    public void constructor_withMessageOnly_setsConnectionError() {
        TcpProtocolException exception = new TcpProtocolException("Connection failed");

        assertEquals(TcpProtocolException.ErrorType.CONNECTION_ERROR, exception.getErrorType());
        assertEquals("Connection failed", exception.getMessage());
        assertNull(exception.getInvalidOpcode());
        assertNull(exception.getCause());
    }

    @Test
    public void constructor_withMessageAndCause_setsConnectionErrorAndCause() {
        Exception cause = new RuntimeException("Underlying error");
        TcpProtocolException exception = new TcpProtocolException("Connection failed", cause);

        assertEquals(TcpProtocolException.ErrorType.CONNECTION_ERROR, exception.getErrorType());
        assertEquals("Connection failed", exception.getMessage());
        assertNull(exception.getInvalidOpcode());
        assertEquals(cause, exception.getCause());
    }
}
