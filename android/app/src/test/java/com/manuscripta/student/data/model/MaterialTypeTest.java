package com.manuscripta.student.data.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link MaterialType} enum.
 */
public class MaterialTypeTest {

    @Test
    public void testGetDisplayName() {
        assertEquals("Reading Material", MaterialType.READING.getDisplayName());
        assertEquals("Quiz", MaterialType.QUIZ.getDisplayName());
        assertEquals("Worksheet", MaterialType.WORKSHEET.getDisplayName());
        assertEquals("Poll", MaterialType.POLL.getDisplayName());
    }
}
