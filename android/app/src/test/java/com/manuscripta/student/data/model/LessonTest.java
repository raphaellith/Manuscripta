package com.manuscripta.student.data.model;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.versioning.AndroidVersions;

/**
 * Unit tests for {@link Lesson} entity.
 */
public class LessonTest {
    private Lesson lesson;

    @Before
    public void setup() {
        Lesson lesson = new Lesson();
        this.lesson = lesson;
    }

    @Test
    public void getAndSetID(){
        lesson.setId(1L);
        assertEquals(1L, lesson.getId());
    }

    @Test
    public void getAndSetTitle(){
        lesson.setTitle("Lesson Title");
        assertEquals("Lesson Title", lesson.getTitle());
    }
}
