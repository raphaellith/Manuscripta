package com.manuscripta.student.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
}
