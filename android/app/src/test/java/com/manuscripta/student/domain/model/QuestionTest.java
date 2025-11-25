package com.manuscripta.student.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.manuscripta.student.data.model.QuestionType;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link Question} domain model.
 */
public class QuestionTest {
    private Question question;

    @Before
    public void setUp() {
        this.question = new Question(
                "question-id",
                "material-id",
                "What is the question?",
                QuestionType.MULTIPLE_CHOICE,
                "[\"A\",\"B\",\"C\",\"D\"]",
                "B"
        );
    }

    @Test
    public void testConstructorAndGetters() {
        assertNotNull(question);
        assertEquals("question-id", question.getId());
        assertEquals("material-id", question.getMaterialId());
        assertEquals("What is the question?", question.getQuestionText());
        assertEquals(QuestionType.MULTIPLE_CHOICE, question.getQuestionType());
        assertEquals("[\"A\",\"B\",\"C\",\"D\"]", question.getOptions());
        assertEquals("B", question.getCorrectAnswer());
    }
}
