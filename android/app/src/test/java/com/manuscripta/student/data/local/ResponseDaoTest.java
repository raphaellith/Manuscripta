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
        MaterialEntity material = new MaterialEntity("mat-1", MaterialType.QUIZ, "Test Quiz");
        materialDao.insert(material);

        QuestionEntity question = new QuestionEntity();
        question.setId("q-1");
        question.setMaterialId("mat-1");
        question.setQuestionText("What is 2+2?");
        question.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        question.setOptions("[\"3\", \"4\", \"5\"]");
        question.setCorrectAnswer("4");
        questionDao.insert(question);
    }

    @After
    public void tearDown() {
        if (database != null) {
            database.close();
        }
    }

    private ResponseEntity createResponse(String id, String questionId) {
        ResponseEntity response = new ResponseEntity();
        response.setId(id);
        response.setQuestionId(questionId);
        response.setSelectedAnswer("4");
        response.setCorrect(true);
        response.setTimestamp(System.currentTimeMillis());
        response.setSynced(false);
        return response;
    }

    @Test
    public void testInsertAndGetById() {
        ResponseEntity response = createResponse("r-1", "q-1");
        responseDao.insert(response);

        ResponseEntity retrieved = responseDao.getById("r-1");
        assertNotNull(retrieved);
        assertEquals("r-1", retrieved.getId());
        assertEquals("q-1", retrieved.getQuestionId());
        assertEquals("4", retrieved.getSelectedAnswer());
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
        QuestionEntity q2 = new QuestionEntity();
        q2.setId("q-2");
        q2.setMaterialId("mat-1");
        q2.setQuestionText("What is 3+3?");
        q2.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        q2.setOptions("[\"5\", \"6\", \"7\"]");
        q2.setCorrectAnswer("6");
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
        ResponseEntity synced = createResponse("r-1", "q-1");
        synced.setSynced(true);
        responseDao.insert(synced);

        ResponseEntity unsynced = createResponse("r-2", "q-1");
        unsynced.setSynced(false);
        responseDao.insert(unsynced);

        List<ResponseEntity> unsyncedList = responseDao.getUnsynced();
        assertEquals(1, unsyncedList.size());
        assertEquals("r-2", unsyncedList.get(0).getId());
    }

    @Test
    public void testGetUnsyncedCount() {
        ResponseEntity synced = createResponse("r-1", "q-1");
        synced.setSynced(true);
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

        response.setSelectedAnswer("3");
        response.setCorrect(false);
        responseDao.update(response);

        ResponseEntity retrieved = responseDao.getById("r-1");
        assertEquals("3", retrieved.getSelectedAnswer());
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
        ResponseEntity response = createResponse("r-1", "q-1");
        response.setSelectedAnswer("3");
        responseDao.insert(response);

        ResponseEntity updated = createResponse("r-1", "q-1");
        updated.setSelectedAnswer("4");
        responseDao.insert(updated);

        ResponseEntity retrieved = responseDao.getById("r-1");
        assertEquals("4", retrieved.getSelectedAnswer());
        assertEquals(1, responseDao.getCount());
    }
}
