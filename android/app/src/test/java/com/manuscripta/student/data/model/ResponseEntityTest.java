package com.manuscripta.student.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link ResponseEntity} entity.
 */
public class ResponseEntityTest {

    @Test
    public void testRoomConstructor() {
        ResponseEntity response = new ResponseEntity(
                "response-1",
                "q-1",
                "4",
                true,
                1000L,
                true
        );

        assertNotNull(response);
        assertEquals("response-1", response.getId());
        assertEquals("q-1", response.getQuestionId());
        assertEquals("4", response.getSelectedAnswer());
        assertTrue(response.isCorrect());
        assertEquals(1000L, response.getTimestamp());
        assertTrue(response.isSynced());
    }

    @Test
    public void testAppConstructor() {
        ResponseEntity response = new ResponseEntity("q-1", "4");

        assertNotNull(response);
        assertNotNull(response.getId());
        assertFalse(response.getId().isEmpty());
        assertEquals("q-1", response.getQuestionId());
        assertEquals("4", response.getSelectedAnswer());
        assertFalse(response.isCorrect());
        assertTrue(response.getTimestamp() > 0);
        assertFalse(response.isSynced());
    }

    @Test
    public void testAppConstructorGeneratesUniqueIds() {
        ResponseEntity response1 = new ResponseEntity("q-1", "A");
        ResponseEntity response2 = new ResponseEntity("q-1", "B");

        assertNotEquals(response1.getId(), response2.getId());
    }

    @Test
    public void testSettersAndGetters() {
        ResponseEntity response = new ResponseEntity(
                "response-1",
                "q-1",
                "3",
                false,
                1000L,
                false
        );

        response.setQuestionId("q-2");
        response.setSelectedAnswer("4");
        response.setCorrect(true);
        response.setTimestamp(2000L);
        response.setSynced(true);

        assertEquals("response-1", response.getId());
        assertEquals("q-2", response.getQuestionId());
        assertEquals("4", response.getSelectedAnswer());
        assertTrue(response.isCorrect());
        assertEquals(2000L, response.getTimestamp());
        assertTrue(response.isSynced());
    }

    @Test
    public void testSyncedStatus() {
        ResponseEntity response = new ResponseEntity("q-1", "4");
        assertFalse(response.isSynced());

        response.setSynced(true);
        assertTrue(response.isSynced());

        response.setSynced(false);
        assertFalse(response.isSynced());
    }

    @Test
    public void testCorrectStatus() {
        ResponseEntity response = new ResponseEntity("q-1", "4");
        assertFalse(response.isCorrect());

        response.setCorrect(true);
        assertTrue(response.isCorrect());

        response.setCorrect(false);
        assertFalse(response.isCorrect());
    }
}
