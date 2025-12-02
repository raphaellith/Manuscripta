package com.manuscripta.student.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link ResponseEntity} entity.
 * Tests immutable entity construction and getters.
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
                true,
                "device-id-1"
        );

        assertNotNull(response);
        assertEquals("response-1", response.getId());
        assertEquals("q-1", response.getQuestionId());
        assertEquals("4", response.getSelectedAnswer());
        assertTrue(response.isCorrect());
        assertEquals(1000L, response.getTimestamp());
        assertTrue(response.isSynced());
        assertEquals("device-id-1", response.getDeviceId());
    }

    @Test
    public void testGetters() {
        ResponseEntity response = new ResponseEntity(
                "response-2",
                "q-2",
                "3",
                false,
                2000L,
                false,
                "device-id-2"
        );

        assertEquals("response-2", response.getId());
        assertEquals("q-2", response.getQuestionId());
        assertEquals("3", response.getSelectedAnswer());
        assertFalse(response.isCorrect());
        assertEquals(2000L, response.getTimestamp());
        assertFalse(response.isSynced());
        assertEquals("device-id-2", response.getDeviceId());
    }

    @Test
    public void testSyncedValues() {
        // Test creating entity with synced = true
        ResponseEntity syncedResponse = new ResponseEntity(
                "response-sync-true",
                "q-1",
                "4",
                false,
                1000L,
                true,
                "device-synced"
        );
        assertTrue(syncedResponse.isSynced());

        // Test creating entity with synced = false
        ResponseEntity unsyncedResponse = new ResponseEntity(
                "response-sync-false",
                "q-1",
                "4",
                false,
                1000L,
                false,
                "device-unsynced"
        );
        assertFalse(unsyncedResponse.isSynced());
    }

    @Test
    public void testCorrectValues() {
        // Test creating entity with correct = true
        ResponseEntity correctResponse = new ResponseEntity(
                "response-correct-true",
                "q-1",
                "4",
                true,
                1000L,
                false,
                "device-correct"
        );
        assertTrue(correctResponse.isCorrect());

        // Test creating entity with correct = false
        ResponseEntity incorrectResponse = new ResponseEntity(
                "response-correct-false",
                "q-1",
                "4",
                false,
                1000L,
                false,
                "device-incorrect"
        );
        assertFalse(incorrectResponse.isCorrect());
    }
}
