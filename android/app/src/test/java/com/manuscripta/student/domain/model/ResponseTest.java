package com.manuscripta.student.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
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
}
