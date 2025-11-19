package com.manuscripta.student.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ResponseEntity} entity.
 */
public class ResponseEntityTest {
    private ResponseEntity responseEntity;

    @Before
    public void setUp() {
        this.responseEntity = new ResponseEntity();
    }

    @Test
    public void testDefaultConstructor() {
        assertNotNull(responseEntity);
        assertEquals("", responseEntity.getId());
        assertEquals("", responseEntity.getQuestionId());
        assertEquals("", responseEntity.getSelectedAnswer());
        assertFalse(responseEntity.isCorrect());
        assertFalse(responseEntity.isSynced());
    }

    @Test
    public void testSettersAndGetters() {
        responseEntity.setId("response-1");
        responseEntity.setQuestionId("q-1");
        responseEntity.setSelectedAnswer("4");
        responseEntity.setCorrect(true);
        responseEntity.setTimestamp(1000L);
        responseEntity.setSynced(true);

        assertEquals("response-1", responseEntity.getId());
        assertEquals("q-1", responseEntity.getQuestionId());
        assertEquals("4", responseEntity.getSelectedAnswer());
        assertTrue(responseEntity.isCorrect());
        assertEquals(1000L, responseEntity.getTimestamp());
        assertTrue(responseEntity.isSynced());
    }

    @Test
    public void testSyncedStatus() {
        assertFalse(responseEntity.isSynced());

        responseEntity.setSynced(true);
        assertTrue(responseEntity.isSynced());

        responseEntity.setSynced(false);
        assertFalse(responseEntity.isSynced());
    }

    @Test
    public void testCorrectStatus() {
        assertFalse(responseEntity.isCorrect());

        responseEntity.setCorrect(true);
        assertTrue(responseEntity.isCorrect());

        responseEntity.setCorrect(false);
        assertFalse(responseEntity.isCorrect());
    }
}
