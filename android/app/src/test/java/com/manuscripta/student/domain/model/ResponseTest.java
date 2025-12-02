package com.manuscripta.student.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link Response} domain model.
 */
public class ResponseTest {
    private Response response;

    @Before
    public void setUp() {
        this.response = new Response(
                "response-id",
                "question-id",
                "Answer B",
                true,
                1234567890L,
                false,
                "device-test-id"
        );
    }

    @Test
    public void testConstructorAndGetters() {
        assertNotNull(response);
        assertEquals("response-id", response.getId());
        assertEquals("question-id", response.getQuestionId());
        assertEquals("Answer B", response.getSelectedAnswer());
        assertTrue(response.isCorrect());
        assertEquals(1234567890L, response.getTimestamp());
        assertFalse(response.isSynced());
        assertEquals("device-test-id", response.getDeviceId());
    }

    @Test
    public void testCreateFactoryMethod() {
        Response newResponse = Response.create("q-123", "Answer A", "device-factory-id");

        assertNotNull(newResponse);
        assertNotNull(newResponse.getId());
        assertFalse(newResponse.getId().isEmpty());
        assertEquals("q-123", newResponse.getQuestionId());
        assertEquals("Answer A", newResponse.getSelectedAnswer());
        assertFalse(newResponse.isCorrect());
        assertTrue(newResponse.getTimestamp() > 0);
        assertFalse(newResponse.isSynced());
        assertEquals("device-factory-id", newResponse.getDeviceId());
    }

    @Test
    public void testCreateFactoryMethodGeneratesUniqueIds() {
        Response response1 = Response.create("q-1", "A", "device-unique-1");
        Response response2 = Response.create("q-1", "B", "device-unique-2");

        assertNotEquals(response1.getId(), response2.getId());
    }

    @Test
    public void testConstructorWithEmptyAnswer() {
        // Empty answer is valid (e.g., skipped question)
        Response r = new Response(
                "id",
                "q-id",
                "",
                false,
                0L,
                true,
                "device-empty-ans"
        );
        assertEquals("", r.getSelectedAnswer());
    }

    @Test
    public void testConstructorWithZeroTimestamp() {
        // Zero timestamp is valid
        Response r = new Response(
                "id",
                "q-id",
                "answer",
                false,
                0L,
                false,
                "device-zero-ts"
        );
        assertEquals(0L, r.getTimestamp());
    }

    @Test
    public void testConstructor_nullId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Response(
                        null,
                        "question-id",
                        "Answer",
                        false,
                        0L,
                        false,
                        "device-null-id"
                )
        );
        assertEquals("Response id cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_emptyId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Response(
                        "",
                        "question-id",
                        "Answer",
                        false,
                        0L,
                        false,
                        "device-empty-id"
                )
        );
        assertEquals("Response id cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_blankId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Response(
                        "   ",
                        "question-id",
                        "Answer",
                        false,
                        0L,
                        false,
                        "device-blank-id"
                )
        );
        assertEquals("Response id cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_nullQuestionId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Response(
                        "id",
                        null,
                        "Answer",
                        false,
                        0L,
                        false,
                        "device-null-qid"
                )
        );
        assertEquals("Response questionId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_emptyQuestionId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Response(
                        "id",
                        "",
                        "Answer",
                        false,
                        0L,
                        false,
                        "device-empty-qid"
                )
        );
        assertEquals("Response questionId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_blankQuestionId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Response(
                        "id",
                        "   ",
                        "Answer",
                        false,
                        0L,
                        false,
                        "device-blank-qid"
                )
        );
        assertEquals("Response questionId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_nullSelectedAnswer_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Response(
                        "id",
                        "question-id",
                        null,
                        false,
                        0L,
                        false,
                        "device-null-ans"
                )
        );
        assertEquals("Response selectedAnswer cannot be null", exception.getMessage());
    }

    @Test
    public void testConstructor_negativeTimestamp_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Response(
                        "id",
                        "question-id",
                        "Answer",
                        false,
                        -1L,
                        false,
                        "device-neg-ts"
                )
        );
        assertEquals("Response timestamp cannot be negative", exception.getMessage());
    }

    @Test
    public void testCreate_nullQuestionId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Response.create(null, "Answer", "device-create-null-qid")
        );
        assertEquals("Response questionId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testCreate_emptyQuestionId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Response.create("", "Answer", "device-create-empty-qid")
        );
        assertEquals("Response questionId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testCreate_blankQuestionId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Response.create("   ", "Answer", "device-create-blank-qid")
        );
        assertEquals("Response questionId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testCreate_nullSelectedAnswer_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Response.create("q-id", null, "device-create-null-ans")
        );
        assertEquals("Response selectedAnswer cannot be null", exception.getMessage());
    }

    @Test
    public void testCreateFactoryMethodWithEmptyAnswer() {
        // Empty answer is valid for create
        Response r = Response.create("q-id", "", "device-empty-create-ans");
        assertEquals("", r.getSelectedAnswer());
    }

    @Test
    public void testConstructor_nullDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Response(
                        "id",
                        "question-id",
                        "Answer",
                        false,
                        0L,
                        false,
                        null
                )
        );
        assertEquals("Response deviceId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_emptyDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Response(
                        "id",
                        "question-id",
                        "Answer",
                        false,
                        0L,
                        false,
                        ""
                )
        );
        assertEquals("Response deviceId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_blankDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Response(
                        "id",
                        "question-id",
                        "Answer",
                        false,
                        0L,
                        false,
                        "   "
                )
        );
        assertEquals("Response deviceId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testCreate_nullDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Response.create("q-id", "Answer", null)
        );
        assertEquals("Response deviceId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testCreate_emptyDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Response.create("q-id", "Answer", "")
        );
        assertEquals("Response deviceId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testCreate_blankDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Response.create("q-id", "Answer", "   ")
        );
        assertEquals("Response deviceId cannot be null or empty", exception.getMessage());
    }
}
