package com.manuscripta.student.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.manuscripta.student.data.model.FeedbackEntity;
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
 * Unit tests for {@link FeedbackDao}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class FeedbackDaoTest {

    private ManuscriptaDatabase database;
    private MaterialDao materialDao;
    private QuestionDao questionDao;
    private ResponseDao responseDao;
    private FeedbackDao feedbackDao;

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
        feedbackDao = database.feedbackDao();

        // Insert parent material, question, and response for foreign key constraints
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

        ResponseEntity response = new ResponseEntity(
                "r-1",
                "q-1",
                "4",
                true,
                System.currentTimeMillis(),
                true,
                "device-1"
        );
        responseDao.insert(response);
    }

    @After
    public void tearDown() {
        if (database != null) {
            database.close();
        }
    }

    private FeedbackEntity createFeedback(String id, String responseId) {
        return new FeedbackEntity(id, responseId, "Good work!", 85);
    }

    @Test
    public void testInsertAndGetById() {
        FeedbackEntity feedback = createFeedback("f-1", "r-1");
        feedbackDao.insert(feedback);

        FeedbackEntity retrieved = feedbackDao.getById("f-1");
        assertNotNull(retrieved);
        assertEquals("f-1", retrieved.getId());
        assertEquals("r-1", retrieved.getResponseId());
        assertEquals("Good work!", retrieved.getText());
        assertEquals(Integer.valueOf(85), retrieved.getMarks());
    }

    @Test
    public void testGetAll() {
        // Create another response for the second feedback
        ResponseEntity response2 = new ResponseEntity(
                "r-2", "q-1", "3", false, System.currentTimeMillis(), true, "device-2");
        responseDao.insert(response2);

        feedbackDao.insert(createFeedback("f-1", "r-1"));
        feedbackDao.insert(createFeedback("f-2", "r-2"));

        List<FeedbackEntity> feedbackList = feedbackDao.getAll();
        assertEquals(2, feedbackList.size());
    }

    @Test
    public void testGetByResponseId() {
        feedbackDao.insert(createFeedback("f-1", "r-1"));

        FeedbackEntity retrieved = feedbackDao.getByResponseId("r-1");
        assertNotNull(retrieved);
        assertEquals("f-1", retrieved.getId());
    }

    @Test
    public void testGetByResponseIdNotFound() {
        FeedbackEntity retrieved = feedbackDao.getByResponseId("nonexistent");
        assertNull(retrieved);
    }

    @Test
    public void testGetAllByDeviceId() {
        // Create responses for two different devices
        ResponseEntity response2 = new ResponseEntity(
                "r-2", "q-1", "3", false, System.currentTimeMillis(), true, "device-1");
        responseDao.insert(response2);

        ResponseEntity response3 = new ResponseEntity(
                "r-3", "q-1", "5", false, System.currentTimeMillis(), true, "device-2");
        responseDao.insert(response3);

        feedbackDao.insert(createFeedback("f-1", "r-1")); // device-1
        feedbackDao.insert(createFeedback("f-2", "r-2")); // device-1
        feedbackDao.insert(createFeedback("f-3", "r-3")); // device-2

        List<FeedbackEntity> device1Feedback = feedbackDao.getAllByDeviceId("device-1");
        assertEquals(2, device1Feedback.size());

        List<FeedbackEntity> device2Feedback = feedbackDao.getAllByDeviceId("device-2");
        assertEquals(1, device2Feedback.size());
    }

    @Test
    public void testInsertAll() {
        ResponseEntity response2 = new ResponseEntity(
                "r-2", "q-1", "3", false, System.currentTimeMillis(), true, "device-2");
        responseDao.insert(response2);

        FeedbackEntity f1 = createFeedback("f-1", "r-1");
        FeedbackEntity f2 = createFeedback("f-2", "r-2");
        feedbackDao.insertAll(Arrays.asList(f1, f2));

        assertEquals(2, feedbackDao.getCount());
    }

    @Test
    public void testDelete() {
        FeedbackEntity feedback = createFeedback("f-1", "r-1");
        feedbackDao.insert(feedback);
        feedbackDao.delete(feedback);

        assertNull(feedbackDao.getById("f-1"));
    }

    @Test
    public void testDeleteById() {
        feedbackDao.insert(createFeedback("f-1", "r-1"));
        feedbackDao.deleteById("f-1");

        assertNull(feedbackDao.getById("f-1"));
    }

    @Test
    public void testDeleteByResponseId() {
        feedbackDao.insert(createFeedback("f-1", "r-1"));
        feedbackDao.deleteByResponseId("r-1");

        assertNull(feedbackDao.getByResponseId("r-1"));
    }

    @Test
    public void testDeleteAll() {
        ResponseEntity response2 = new ResponseEntity(
                "r-2", "q-1", "3", false, System.currentTimeMillis(), true, "device-2");
        responseDao.insert(response2);

        feedbackDao.insert(createFeedback("f-1", "r-1"));
        feedbackDao.insert(createFeedback("f-2", "r-2"));
        feedbackDao.deleteAll();

        assertEquals(0, feedbackDao.getCount());
    }

    @Test
    public void testGetCount() {
        assertEquals(0, feedbackDao.getCount());

        feedbackDao.insert(createFeedback("f-1", "r-1"));
        assertEquals(1, feedbackDao.getCount());

        ResponseEntity response2 = new ResponseEntity(
                "r-2", "q-1", "3", false, System.currentTimeMillis(), true, "device-2");
        responseDao.insert(response2);
        feedbackDao.insert(createFeedback("f-2", "r-2"));
        assertEquals(2, feedbackDao.getCount());
    }

    @Test
    public void testCascadeDeleteOnResponseDelete() {
        feedbackDao.insert(createFeedback("f-1", "r-1"));

        // Delete the parent response
        responseDao.deleteById("r-1");

        // Feedback should be deleted due to CASCADE
        assertEquals(0, feedbackDao.getCount());
    }

    @Test
    public void testGetByIdNotFound() {
        assertNull(feedbackDao.getById("nonexistent"));
    }

    @Test
    public void testGetAllEmpty() {
        List<FeedbackEntity> feedbackList = feedbackDao.getAll();
        assertNotNull(feedbackList);
        assertTrue(feedbackList.isEmpty());
    }

    @Test
    public void testInsertReplaceOnConflict() {
        FeedbackEntity original = new FeedbackEntity("f-1", "r-1", "Original", 50);
        feedbackDao.insert(original);

        FeedbackEntity updated = new FeedbackEntity("f-1", "r-1", "Updated", 75);
        feedbackDao.insert(updated);

        FeedbackEntity retrieved = feedbackDao.getById("f-1");
        assertEquals("Updated", retrieved.getText());
        assertEquals(Integer.valueOf(75), retrieved.getMarks());
        assertEquals(1, feedbackDao.getCount());
    }

    @Test
    public void testFeedbackWithTextOnly() {
        FeedbackEntity feedback = new FeedbackEntity("f-text", "r-1", "Great job!", null);
        feedbackDao.insert(feedback);

        FeedbackEntity retrieved = feedbackDao.getById("f-text");
        assertEquals("Great job!", retrieved.getText());
        assertNull(retrieved.getMarks());
    }

    @Test
    public void testFeedbackWithMarksOnly() {
        FeedbackEntity feedback = new FeedbackEntity("f-marks", "r-1", null, 90);
        feedbackDao.insert(feedback);

        FeedbackEntity retrieved = feedbackDao.getById("f-marks");
        assertNull(retrieved.getText());
        assertEquals(Integer.valueOf(90), retrieved.getMarks());
    }

    @Test
    public void testGetAllByDeviceIdEmpty() {
        List<FeedbackEntity> feedbackList = feedbackDao.getAllByDeviceId("nonexistent-device");
        assertNotNull(feedbackList);
        assertTrue(feedbackList.isEmpty());
    }
}
