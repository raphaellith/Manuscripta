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
    private MaterialEntity defaultMaterial;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, ManuscriptaDatabase.class)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();
        materialDao = database.materialDao();

        defaultMaterial = new MaterialEntity(
                "mat-1",
                MaterialType.QUIZ,
                "Test Quiz",
                "Sample Content",
                "{\"author\": \"Teacher\"}",
                "[\"term1\", \"term2\"]",
                System.currentTimeMillis()
        );
    }

    @After
    public void tearDown() {
        if (database != null) {
            database.close();
        }
    }

    @Test
    public void testInsertAndGetById() {
        materialDao.insert(defaultMaterial);

        MaterialEntity retrieved = materialDao.getById("mat-1");
        assertNotNull(retrieved);
        assertEquals("mat-1", retrieved.getId());
        assertEquals("Test Quiz", retrieved.getTitle());
        assertEquals(MaterialType.QUIZ, retrieved.getType());
        assertEquals("Sample Content", retrieved.getContent());
    }

    @Test
    public void testGetAll() {
        materialDao.insert(defaultMaterial);
        
        MaterialEntity material2 = new MaterialEntity(
                "mat-2", 
                MaterialType.LESSON, 
                "Lesson 1",
                "Lesson Content",
                "{}",
                "[]",
                System.currentTimeMillis()
        );
        materialDao.insert(material2);

        List<MaterialEntity> materials = materialDao.getAll();
        assertEquals(2, materials.size());
    }

    @Test
    public void testGetByType() {
        MaterialEntity quiz = new MaterialEntity(
                "mat-1", 
                MaterialType.QUIZ, 
                "Quiz",
                "Content", "{}", "[]", 0
        );
        MaterialEntity lesson = new MaterialEntity(
                "mat-2", 
                MaterialType.LESSON, 
                "Lesson",
                "Content", "{}", "[]", 0
        );
        materialDao.insert(quiz);
        materialDao.insert(lesson);

        List<MaterialEntity> quizzes = materialDao.getByType(MaterialType.QUIZ);
        assertEquals(1, quizzes.size());
        assertEquals("Quiz", quizzes.get(0).getTitle());
    }

    @Test
    public void testUpdate() {
        materialDao.insert(defaultMaterial);

        MaterialEntity updatedMaterial = new MaterialEntity(
                defaultMaterial.getId(),
                defaultMaterial.getType(),
                "Updated",
                defaultMaterial.getContent(),
                defaultMaterial.getMetadata(),
                defaultMaterial.getVocabularyTerms(),
                defaultMaterial.getTimestamp()
        );
        materialDao.update(updatedMaterial);

        MaterialEntity retrieved = materialDao.getById("mat-1");
        assertEquals("Updated", retrieved.getTitle());
    }

    @Test
    public void testDelete() {
        materialDao.insert(defaultMaterial);
        materialDao.delete(defaultMaterial);

        assertNull(materialDao.getById("mat-1"));
    }

    @Test
    public void testDeleteById() {
        materialDao.insert(defaultMaterial);
        materialDao.deleteById("mat-1");

        assertNull(materialDao.getById("mat-1"));
    }

    @Test
    public void testDeleteAll() {
        materialDao.insert(defaultMaterial);
        materialDao.insert(new MaterialEntity("mat-2", MaterialType.QUIZ, "Quiz 2", "C", "{}", "[]", 0));
        materialDao.deleteAll();

        assertEquals(0, materialDao.getCount());
    }

    @Test
    public void testGetCount() {
        assertEquals(0, materialDao.getCount());

        materialDao.insert(defaultMaterial);
        assertEquals(1, materialDao.getCount());

        materialDao.insert(new MaterialEntity("mat-2", MaterialType.QUIZ, "Quiz 2", "C", "{}", "[]", 0));
        assertEquals(2, materialDao.getCount());
    }

    @Test
    public void testInsertAll() {
        MaterialEntity mat2 = new MaterialEntity("mat-2", MaterialType.LESSON, "Lesson 1", "C", "{}", "[]", 0);
        materialDao.insertAll(Arrays.asList(defaultMaterial, mat2));

        assertEquals(2, materialDao.getCount());
    }

    @Test
    public void testInsertReplaceOnConflict() {
        materialDao.insert(defaultMaterial);

        MaterialEntity updated = new MaterialEntity(
                "mat-1", 
                MaterialType.QUIZ, 
                "Replaced",
                "New Content",
                "{}",
                "[]",
                System.currentTimeMillis()
        );
        materialDao.insert(updated);

        MaterialEntity retrieved = materialDao.getById("mat-1");
        assertEquals("Replaced", retrieved.getTitle());
        assertEquals("New Content", retrieved.getContent());
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
