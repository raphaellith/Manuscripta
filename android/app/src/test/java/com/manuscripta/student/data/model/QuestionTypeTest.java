package com.manuscripta.student.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link QuestionType} enum.
 */
public class QuestionTypeTest {

    @Test
    public void testRequiresOptions() {
        assertTrue(QuestionType.MULTIPLE_CHOICE.requiresOptions());
        assertTrue(QuestionType.TRUE_FALSE.requiresOptions());
        assertFalse(QuestionType.SHORT_ANSWER.requiresOptions());
    }

    @Test
    public void testGetDisplayName() {
        assertEquals("Multiple Choice", QuestionType.MULTIPLE_CHOICE.getDisplayName());
        assertEquals("True/False", QuestionType.TRUE_FALSE.getDisplayName());
        assertEquals("Short Answer", QuestionType.SHORT_ANSWER.getDisplayName());
    }
}
