package com.manuscripta.student.data.local;

import static org.junit.Assert.assertEquals;
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link QuestionDao}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class QuestionDaoTest {

    private ManuscriptaDatabase database;
    private MaterialDao materialDao;
    private QuestionDao questionDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, ManuscriptaDatabase.class)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();
        materialDao = database.materialDao();
        questionDao = database.questionDao();

        // Insert a parent material for foreign key constraint
        MaterialEntity material = new MaterialEntity("mat-1", MaterialType.QUIZ, "Test Quiz");
        materialDao.insert(material);
    }

    @After
    public void tearDown() {
        if (database != null) {
            database.close();
        }
    }

    private QuestionEntity createQuestion(String id, String materialId) {
        QuestionEntity question = new QuestionEntity();
        question.setId(id);
        question.setMaterialId(materialId);
        question.setQuestionText("Sample question?");
        question.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        question.setOptions("[\"A\", \"B\", \"C\", \"D\"]");
        question.setCorrectAnswer("A");
        return question;
    }

    @Test
    public void testInsertAndGetById() {
        QuestionEntity question = createQuestion("q-1", "mat-1");
        questionDao.insert(question);

        QuestionEntity retrieved = questionDao.getById("q-1");
        assertNotNull(retrieved);
        assertEquals("q-1", retrieved.getId());
        assertEquals("mat-1", retrieved.getMaterialId());
        assertEquals("Sample question?", retrieved.getQuestionText());
    }

    @Test
    public void testGetAll() {
        questionDao.insert(createQuestion("q-1", "mat-1"));
        questionDao.insert(createQuestion("q-2", "mat-1"));

        List<QuestionEntity> questions = questionDao.getAll();
        assertEquals(2, questions.size());
    }

    @Test
    public void testGetByMaterialId() {
        // Add another material
        materialDao.insert(new MaterialEntity("mat-2", MaterialType.LESSON, "Lesson"));

        questionDao.insert(createQuestion("q-1", "mat-1"));
        questionDao.insert(createQuestion("q-2", "mat-1"));
        questionDao.insert(createQuestion("q-3", "mat-2"));

        List<QuestionEntity> mat1Questions = questionDao.getByMaterialId("mat-1");
        assertEquals(2, mat1Questions.size());

        List<QuestionEntity> mat2Questions = questionDao.getByMaterialId("mat-2");
        assertEquals(1, mat2Questions.size());
    }

    @Test
    public void testUpdate() {
        QuestionEntity question = createQuestion("q-1", "mat-1");
        questionDao.insert(question);

        question.setQuestionText("Updated question?");
        questionDao.update(question);

        QuestionEntity retrieved = questionDao.getById("q-1");
        assertEquals("Updated question?", retrieved.getQuestionText());
    }

    @Test
    public void testDelete() {
        QuestionEntity question = createQuestion("q-1", "mat-1");
        questionDao.insert(question);
        questionDao.delete(question);

        assertNull(questionDao.getById("q-1"));
    }

    @Test
    public void testDeleteById() {
        questionDao.insert(createQuestion("q-1", "mat-1"));
        questionDao.deleteById("q-1");

        assertNull(questionDao.getById("q-1"));
    }

    @Test
    public void testDeleteByMaterialId() {
        questionDao.insert(createQuestion("q-1", "mat-1"));
        questionDao.insert(createQuestion("q-2", "mat-1"));
        questionDao.deleteByMaterialId("mat-1");

        assertEquals(0, questionDao.getCountByMaterialId("mat-1"));
    }

    @Test
    public void testDeleteAll() {
        questionDao.insert(createQuestion("q-1", "mat-1"));
        questionDao.insert(createQuestion("q-2", "mat-1"));
        questionDao.deleteAll();

        assertEquals(0, questionDao.getCount());
    }

    @Test
    public void testGetCount() {
        assertEquals(0, questionDao.getCount());

        questionDao.insert(createQuestion("q-1", "mat-1"));
        assertEquals(1, questionDao.getCount());

        questionDao.insert(createQuestion("q-2", "mat-1"));
        assertEquals(2, questionDao.getCount());
    }

    @Test
    public void testGetCountByMaterialId() {
        materialDao.insert(new MaterialEntity("mat-2", MaterialType.LESSON, "Lesson"));

        questionDao.insert(createQuestion("q-1", "mat-1"));
        questionDao.insert(createQuestion("q-2", "mat-1"));
        questionDao.insert(createQuestion("q-3", "mat-2"));

        assertEquals(2, questionDao.getCountByMaterialId("mat-1"));
        assertEquals(1, questionDao.getCountByMaterialId("mat-2"));
    }

    @Test
    public void testInsertAll() {
        QuestionEntity q1 = createQuestion("q-1", "mat-1");
        QuestionEntity q2 = createQuestion("q-2", "mat-1");
        questionDao.insertAll(Arrays.asList(q1, q2));

        assertEquals(2, questionDao.getCount());
    }

    @Test
    public void testCascadeDeleteOnMaterialDelete() {
        questionDao.insert(createQuestion("q-1", "mat-1"));
        questionDao.insert(createQuestion("q-2", "mat-1"));

        // Delete the parent material
        materialDao.deleteById("mat-1");

        // Questions should be deleted due to CASCADE
        assertEquals(0, questionDao.getCount());
    }

    @Test
    public void testGetByIdNotFound() {
        assertNull(questionDao.getById("nonexistent"));
    }

    @Test
    public void testGetAllEmpty() {
        List<QuestionEntity> questions = questionDao.getAll();
        assertNotNull(questions);
        assertTrue(questions.isEmpty());
    }

    @Test
    public void testInsertReplaceOnConflict() {
        QuestionEntity question = createQuestion("q-1", "mat-1");
        question.setQuestionText("Original");
        questionDao.insert(question);

        QuestionEntity updated = createQuestion("q-1", "mat-1");
        updated.setQuestionText("Replaced");
        questionDao.insert(updated);

        QuestionEntity retrieved = questionDao.getById("q-1");
        assertEquals("Replaced", retrieved.getQuestionText());
        assertEquals(1, questionDao.getCount());
    }
}
