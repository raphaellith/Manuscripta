package com.manuscripta.student.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

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

    @Test
    public void testConstructorWithEmptyOptionsAndAnswer() {
        // Empty options and correct answer are valid (e.g., for WRITTEN_ANSWER type)
        Question q = new Question(
                "id",
                "mat-id",
                "Write an essay",
                QuestionType.WRITTEN_ANSWER,
                "",
                ""
        );
        assertEquals("", q.getOptions());
        assertEquals("", q.getCorrectAnswer());
    }

    @Test
    public void testConstructor_nullId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Question(
                        null,
                        "material-id",
                        "Question?",
                        QuestionType.MULTIPLE_CHOICE,
                        "[]",
                        "A"
                )
        );
        assertEquals("Question id cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_emptyId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Question(
                        "",
                        "material-id",
                        "Question?",
                        QuestionType.MULTIPLE_CHOICE,
                        "[]",
                        "A"
                )
        );
        assertEquals("Question id cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_blankId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Question(
                        "   ",
                        "material-id",
                        "Question?",
                        QuestionType.MULTIPLE_CHOICE,
                        "[]",
                        "A"
                )
        );
        assertEquals("Question id cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_nullMaterialId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Question(
                        "id",
                        null,
                        "Question?",
                        QuestionType.MULTIPLE_CHOICE,
                        "[]",
                        "A"
                )
        );
        assertEquals("Question materialId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_emptyMaterialId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Question(
                        "id",
                        "",
                        "Question?",
                        QuestionType.MULTIPLE_CHOICE,
                        "[]",
                        "A"
                )
        );
        assertEquals("Question materialId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_blankMaterialId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Question(
                        "id",
                        "   ",
                        "Question?",
                        QuestionType.MULTIPLE_CHOICE,
                        "[]",
                        "A"
                )
        );
        assertEquals("Question materialId cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_nullQuestionText_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Question(
                        "id",
                        "material-id",
                        null,
                        QuestionType.MULTIPLE_CHOICE,
                        "[]",
                        "A"
                )
        );
        assertEquals("Question questionText cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_emptyQuestionText_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Question(
                        "id",
                        "material-id",
                        "",
                        QuestionType.MULTIPLE_CHOICE,
                        "[]",
                        "A"
                )
        );
        assertEquals("Question questionText cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_blankQuestionText_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Question(
                        "id",
                        "material-id",
                        "   ",
                        QuestionType.MULTIPLE_CHOICE,
                        "[]",
                        "A"
                )
        );
        assertEquals("Question questionText cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testConstructor_nullQuestionType_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Question(
                        "id",
                        "material-id",
                        "Question?",
                        null,
                        "[]",
                        "A"
                )
        );
        assertEquals("Question questionType cannot be null", exception.getMessage());
    }

    @Test
    public void testConstructor_nullOptions_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Question(
                        "id",
                        "material-id",
                        "Question?",
                        QuestionType.MULTIPLE_CHOICE,
                        null,
                        "A"
                )
        );
        assertEquals("Question options cannot be null", exception.getMessage());
    }

    @Test
    public void testConstructor_nullCorrectAnswer_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Question(
                        "id",
                        "material-id",
                        "Question?",
                        QuestionType.MULTIPLE_CHOICE,
                        "[]",
                        null
                )
        );
        assertEquals("Question correctAnswer cannot be null", exception.getMessage());
    }
}
