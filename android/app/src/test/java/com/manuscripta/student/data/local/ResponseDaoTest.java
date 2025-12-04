package com.manuscripta.student.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.manuscripta.student.data.model.MaterialEntity;
import com.manuscripta.student.data.model.MaterialType;
import com.manuscripta.student.data.model.QuestionEntity;
import com.manuscripta.student.data.model.QuestionType;
import com.manuscripta.student.data.model.ResponseEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link ResponseDao}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class ResponseDaoTest {

    private ManuscriptaDatabase database;
    private MaterialDao materialDao;
    private QuestionDao questionDao;
    private ResponseDao responseDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, ManuscriptaDatabase.class)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();
        materialDao = database.materialDao();
        questionDao = database.questionDao();
        responseDao = database.responseDao();

        // Insert parent material and question for foreign key constraints
        MaterialEntity material = new MaterialEntity(
                "mat-1",
                MaterialType.QUIZ,
                "Test Quiz",
                "Content",
                "{}",
                "[]",
                System.currentTimeMillis()
        );
        materialDao.insert(material);

        QuestionEntity question = new QuestionEntity(
                "q-1",
                "mat-1",
                "What is 2+2?",
                QuestionType.MULTIPLE_CHOICE,
                "[\"3\", \"4\", \"5\"]",
                "4"
        );
        questionDao.insert(question);
    }

    @After
    public void tearDown() {
        if (database != null) {
            database.close();
        }
    }

    private ResponseEntity createResponse(String id, String questionId) {
        return new ResponseEntity(
                id,
                questionId,
                "4",
                true,
                System.currentTimeMillis(),
                false,
                "device-test-123"
        );
    }

    @Test
    public void testInsertAndGetById() {
        ResponseEntity response = createResponse("r-1", "q-1");
        responseDao.insert(response);

        ResponseEntity retrieved = responseDao.getById("r-1");
        assertNotNull(retrieved);
        assertEquals("r-1", retrieved.getId());
        assertEquals("q-1", retrieved.getQuestionId());
        assertEquals("4", retrieved.getAnswer());
        assertTrue(retrieved.isCorrect());
    }

    @Test
    public void testGetAll() {
        responseDao.insert(createResponse("r-1", "q-1"));
        responseDao.insert(createResponse("r-2", "q-1"));

        List<ResponseEntity> responses = responseDao.getAll();
        assertEquals(2, responses.size());
    }

    @Test
    public void testGetByQuestionId() {
        // Add another question
        QuestionEntity q2 = new QuestionEntity(
                "q-2",
                "mat-1",
                "What is 3+3?",
                QuestionType.MULTIPLE_CHOICE,
                "[\"5\", \"6\", \"7\"]",
                "6"
        );
        questionDao.insert(q2);

        responseDao.insert(createResponse("r-1", "q-1"));
        responseDao.insert(createResponse("r-2", "q-1"));
        responseDao.insert(createResponse("r-3", "q-2"));

        List<ResponseEntity> q1Responses = responseDao.getByQuestionId("q-1");
        assertEquals(2, q1Responses.size());

        List<ResponseEntity> q2Responses = responseDao.getByQuestionId("q-2");
        assertEquals(1, q2Responses.size());
    }

    @Test
    public void testGetUnsynced() {
        // Create synced response using constructor
        ResponseEntity synced = new ResponseEntity(
                "r-1", "q-1", "4", true, System.currentTimeMillis(), true, "device-1");
        responseDao.insert(synced);

        // Create unsynced response using constructor
        ResponseEntity unsynced = new ResponseEntity(
                "r-2", "q-1", "4", true, System.currentTimeMillis(), false, "device-2");
        responseDao.insert(unsynced);

        List<ResponseEntity> unsyncedList = responseDao.getUnsynced();
        assertEquals(1, unsyncedList.size());
        assertEquals("r-2", unsyncedList.get(0).getId());
    }

    @Test
    public void testGetUnsyncedCount() {
        // Create synced response using constructor
        ResponseEntity synced = new ResponseEntity(
                "r-1", "q-1", "4", true, System.currentTimeMillis(), true, "device-synced");
        responseDao.insert(synced);

        responseDao.insert(createResponse("r-2", "q-1"));
        responseDao.insert(createResponse("r-3", "q-1"));

        assertEquals(2, responseDao.getUnsyncedCount());
    }

    @Test
    public void testMarkSynced() {
        ResponseEntity response = createResponse("r-1", "q-1");
        responseDao.insert(response);

        assertFalse(responseDao.getById("r-1").isSynced());

        responseDao.markSynced("r-1");

        assertTrue(responseDao.getById("r-1").isSynced());
    }

    @Test
    public void testMarkAllSynced() {
        responseDao.insert(createResponse("r-1", "q-1"));
        responseDao.insert(createResponse("r-2", "q-1"));
        responseDao.insert(createResponse("r-3", "q-1"));

        assertEquals(3, responseDao.getUnsyncedCount());

        responseDao.markAllSynced(Arrays.asList("r-1", "r-2"));

        assertEquals(1, responseDao.getUnsyncedCount());
        assertTrue(responseDao.getById("r-1").isSynced());
        assertTrue(responseDao.getById("r-2").isSynced());
        assertFalse(responseDao.getById("r-3").isSynced());
    }

    @Test
    public void testUpdate() {
        ResponseEntity response = createResponse("r-1", "q-1");
        responseDao.insert(response);

        // Create a new entity with updated values since entities are immutable
        ResponseEntity updatedResponse = new ResponseEntity(
                "r-1", "q-1", "3", false, response.getTimestamp(), response.isSynced(),
                "device-test-123");
        responseDao.update(updatedResponse);

        ResponseEntity retrieved = responseDao.getById("r-1");
        assertEquals("3", retrieved.getAnswer());
        assertFalse(retrieved.isCorrect());
    }

    @Test
    public void testDelete() {
        ResponseEntity response = createResponse("r-1", "q-1");
        responseDao.insert(response);
        responseDao.delete(response);

        assertNull(responseDao.getById("r-1"));
    }

    @Test
    public void testDeleteById() {
        responseDao.insert(createResponse("r-1", "q-1"));
        responseDao.deleteById("r-1");

        assertNull(responseDao.getById("r-1"));
    }

    @Test
    public void testDeleteByQuestionId() {
        responseDao.insert(createResponse("r-1", "q-1"));
        responseDao.insert(createResponse("r-2", "q-1"));
        responseDao.deleteByQuestionId("q-1");

        assertEquals(0, responseDao.getCountByQuestionId("q-1"));
    }

    @Test
    public void testDeleteAll() {
        responseDao.insert(createResponse("r-1", "q-1"));
        responseDao.insert(createResponse("r-2", "q-1"));
        responseDao.deleteAll();

        assertEquals(0, responseDao.getCount());
    }

    @Test
    public void testGetCount() {
        assertEquals(0, responseDao.getCount());

        responseDao.insert(createResponse("r-1", "q-1"));
        assertEquals(1, responseDao.getCount());

        responseDao.insert(createResponse("r-2", "q-1"));
        assertEquals(2, responseDao.getCount());
    }

    @Test
    public void testGetCountByQuestionId() {
        responseDao.insert(createResponse("r-1", "q-1"));
        responseDao.insert(createResponse("r-2", "q-1"));

        assertEquals(2, responseDao.getCountByQuestionId("q-1"));
    }

    @Test
    public void testInsertAll() {
        ResponseEntity r1 = createResponse("r-1", "q-1");
        ResponseEntity r2 = createResponse("r-2", "q-1");
        responseDao.insertAll(Arrays.asList(r1, r2));

        assertEquals(2, responseDao.getCount());
    }

    @Test
    public void testCascadeDeleteOnQuestionDelete() {
        responseDao.insert(createResponse("r-1", "q-1"));
        responseDao.insert(createResponse("r-2", "q-1"));

        // Delete the parent question
        questionDao.deleteById("q-1");

        // Responses should be deleted due to CASCADE
        assertEquals(0, responseDao.getCount());
    }

    @Test
    public void testGetByIdNotFound() {
        assertNull(responseDao.getById("nonexistent"));
    }

    @Test
    public void testGetAllEmpty() {
        List<ResponseEntity> responses = responseDao.getAll();
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    public void testInsertReplaceOnConflict() {
        // Create response with answer "3"
        ResponseEntity response = new ResponseEntity(
                "r-1", "q-1", "3", true, System.currentTimeMillis(), false, "device-conflict");
        responseDao.insert(response);

        // Create new response with same ID but different answer
        ResponseEntity updated = new ResponseEntity(
                "r-1", "q-1", "4", true, response.getTimestamp(), false, "device-conflict");
        responseDao.insert(updated);

        ResponseEntity retrieved = responseDao.getById("r-1");
        assertEquals("4", retrieved.getAnswer());
        assertEquals(1, responseDao.getCount());
    }
}
