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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link MaterialDao}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class MaterialDaoTest {

    private ManuscriptaDatabase database;
    private MaterialDao materialDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, ManuscriptaDatabase.class)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();
        materialDao = database.materialDao();
    }

    @After
    public void tearDown() {
        if (database != null) {
            database.close();
        }
    }

    @Test
    public void testInsertAndGetById() {
        MaterialEntity material = new MaterialEntity("mat-1", MaterialType.QUIZ, "Test Quiz");
        materialDao.insert(material);

        MaterialEntity retrieved = materialDao.getById("mat-1");
        assertNotNull(retrieved);
        assertEquals("mat-1", retrieved.getId());
        assertEquals("Test Quiz", retrieved.getTitle());
        assertEquals(MaterialType.QUIZ, retrieved.getType());
    }

    @Test
    public void testGetAll() {
        MaterialEntity material1 = new MaterialEntity("mat-1", MaterialType.QUIZ, "Quiz 1");
        MaterialEntity material2 = new MaterialEntity("mat-2", MaterialType.LESSON, "Lesson 1");
        materialDao.insert(material1);
        materialDao.insert(material2);

        List<MaterialEntity> materials = materialDao.getAll();
        assertEquals(2, materials.size());
    }

    @Test
    public void testGetByType() {
        MaterialEntity quiz = new MaterialEntity("mat-1", MaterialType.QUIZ, "Quiz");
        MaterialEntity lesson = new MaterialEntity("mat-2", MaterialType.LESSON, "Lesson");
        materialDao.insert(quiz);
        materialDao.insert(lesson);

        List<MaterialEntity> quizzes = materialDao.getByType(MaterialType.QUIZ);
        assertEquals(1, quizzes.size());
        assertEquals("Quiz", quizzes.get(0).getTitle());
    }

    @Test
    public void testUpdate() {
        MaterialEntity material = new MaterialEntity("mat-1", MaterialType.QUIZ, "Original");
        materialDao.insert(material);

        material.setTitle("Updated");
        materialDao.update(material);

        MaterialEntity retrieved = materialDao.getById("mat-1");
        assertEquals("Updated", retrieved.getTitle());
    }

    @Test
    public void testDelete() {
        MaterialEntity material = new MaterialEntity("mat-1", MaterialType.QUIZ, "Quiz");
        materialDao.insert(material);
        materialDao.delete(material);

        assertNull(materialDao.getById("mat-1"));
    }

    @Test
    public void testDeleteById() {
        MaterialEntity material = new MaterialEntity("mat-1", MaterialType.QUIZ, "Quiz");
        materialDao.insert(material);
        materialDao.deleteById("mat-1");

        assertNull(materialDao.getById("mat-1"));
    }

    @Test
    public void testDeleteAll() {
        materialDao.insert(new MaterialEntity("mat-1", MaterialType.QUIZ, "Quiz 1"));
        materialDao.insert(new MaterialEntity("mat-2", MaterialType.QUIZ, "Quiz 2"));
        materialDao.deleteAll();

        assertEquals(0, materialDao.getCount());
    }

    @Test
    public void testGetCount() {
        assertEquals(0, materialDao.getCount());

        materialDao.insert(new MaterialEntity("mat-1", MaterialType.QUIZ, "Quiz 1"));
        assertEquals(1, materialDao.getCount());

        materialDao.insert(new MaterialEntity("mat-2", MaterialType.QUIZ, "Quiz 2"));
        assertEquals(2, materialDao.getCount());
    }

    @Test
    public void testInsertAll() {
        MaterialEntity mat1 = new MaterialEntity("mat-1", MaterialType.QUIZ, "Quiz 1");
        MaterialEntity mat2 = new MaterialEntity("mat-2", MaterialType.LESSON, "Lesson 1");
        materialDao.insertAll(Arrays.asList(mat1, mat2));

        assertEquals(2, materialDao.getCount());
    }

    @Test
    public void testInsertReplaceOnConflict() {
        MaterialEntity material = new MaterialEntity("mat-1", MaterialType.QUIZ, "Original");
        materialDao.insert(material);

        MaterialEntity updated = new MaterialEntity("mat-1", MaterialType.QUIZ, "Replaced");
        materialDao.insert(updated);

        MaterialEntity retrieved = materialDao.getById("mat-1");
        assertEquals("Replaced", retrieved.getTitle());
        assertEquals(1, materialDao.getCount());
    }

    @Test
    public void testGetByIdNotFound() {
        assertNull(materialDao.getById("nonexistent"));
    }

    @Test
    public void testGetAllEmpty() {
        List<MaterialEntity> materials = materialDao.getAll();
        assertNotNull(materials);
        assertTrue(materials.isEmpty());
    }
}
