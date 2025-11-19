package com.manuscripta.student.data.model;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link QuestionEntity} entity.
 */
public class QuestionEntityTest {
    private QuestionEntity questionEntity;

    @Before
    public void setUp() {
        this.questionEntity = new QuestionEntity(
                "q-1",
                "mat-1",
                "What is 2+2?",
                QuestionType.MULTIPLE_CHOICE,
                "[\"3\", \"4\", \"5\"]",
                "4"
        );
    }

    @Test
    public void testConstructorAndGetters() {
        assertEquals("q-1", questionEntity.getId());
        assertEquals("mat-1", questionEntity.getMaterialId());
        assertEquals("What is 2+2?", questionEntity.getQuestionText());
        assertEquals(QuestionType.MULTIPLE_CHOICE, questionEntity.getQuestionType());
        assertEquals("[\"3\", \"4\", \"5\"]", questionEntity.getOptions());
        assertEquals("4", questionEntity.getCorrectAnswer());
    }
}