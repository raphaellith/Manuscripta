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
                false
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
    }

    @Test
    public void testCreateFactoryMethod() {
        Response newResponse = Response.create("q-123", "Answer A");

        assertNotNull(newResponse);
        assertNotNull(newResponse.getId());
        assertFalse(newResponse.getId().isEmpty());
        assertEquals("q-123", newResponse.getQuestionId());
        assertEquals("Answer A", newResponse.getSelectedAnswer());
        assertFalse(newResponse.isCorrect());
        assertTrue(newResponse.getTimestamp() > 0);
        assertFalse(newResponse.isSynced());
    }

    @Test
    public void testCreateFactoryMethodGeneratesUniqueIds() {
        Response response1 = Response.create("q-1", "A");
        Response response2 = Response.create("q-1", "B");

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
                true
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
                false
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
                        false
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
                        false
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
                        false
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
                        false
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
                        false
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
                        false
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
                        false
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
                        false
                )
        );
        assertEquals("Response timestamp cannot be negative", exception.getMessage());
    }

    @Test
    public void testCreate_nullQuestionId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Response.create(null, "Answer")
        );
        assertEquals("Response questionId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testCreate_emptyQuestionId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Response.create("", "Answer")
        );
        assertEquals("Response questionId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testCreate_blankQuestionId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Response.create("   ", "Answer")
        );
        assertEquals("Response questionId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testCreate_nullSelectedAnswer_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Response.create("q-id", null)
        );
        assertEquals("Response selectedAnswer cannot be null", exception.getMessage());
    }

    @Test
    public void testCreateFactoryMethodWithEmptyAnswer() {
        // Empty answer is valid for create
        Response r = Response.create("q-id", "");
        assertEquals("", r.getSelectedAnswer());
    }
}
