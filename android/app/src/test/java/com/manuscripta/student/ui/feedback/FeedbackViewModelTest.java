package com.manuscripta.student.ui.feedback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.manuscripta.student.data.model.QuestionType;
import com.manuscripta.student.domain.model.Question;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Unit tests for {@link FeedbackViewModel}.
 */
public class FeedbackViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private FeedbackViewModel viewModel;
    private Question testQuestion;

    @Before
    public void setUp() {
        viewModel = new FeedbackViewModel();
        testQuestion = new Question("q-1", "mat-1", "What is 2+2?",
                QuestionType.MULTIPLE_CHOICE, "[\"3\",\"4\",\"5\"]", "4", null);
    }

    @Test
    public void testInitialIsCorrectIsNull() {
        assertNull(viewModel.getIsCorrect().getValue());
    }

    @Test
    public void testInitialExplanationIsNull() {
        assertNull(viewModel.getExplanation().getValue());
    }

    @Test
    public void testInitialQuestionIsNull() {
        assertNull(viewModel.getQuestion().getValue());
    }

    @Test
    public void testHasFeedbackInitiallyFalse() {
        assertFalse(viewModel.hasFeedback());
    }

    @Test
    public void testSetCorrectFeedback() {
        viewModel.setCorrectFeedback(testQuestion, "Well done!");

        assertTrue(viewModel.getIsCorrect().getValue());
        assertEquals("Well done!", viewModel.getExplanation().getValue());
        assertEquals(testQuestion, viewModel.getQuestion().getValue());
    }

    @Test
    public void testSetIncorrectFeedback() {
        viewModel.setIncorrectFeedback(testQuestion, "The correct answer is 4.");

        assertFalse(viewModel.getIsCorrect().getValue());
        assertEquals("The correct answer is 4.", viewModel.getExplanation().getValue());
        assertEquals(testQuestion, viewModel.getQuestion().getValue());
    }

    @Test
    public void testSetFeedbackCorrect() {
        viewModel.setFeedback(true, testQuestion, "Correct!");

        assertTrue(viewModel.getIsCorrect().getValue());
        assertEquals("Correct!", viewModel.getExplanation().getValue());
        assertNotNull(viewModel.getQuestion().getValue());
    }

    @Test
    public void testSetFeedbackIncorrect() {
        viewModel.setFeedback(false, testQuestion, "Try again.");

        assertFalse(viewModel.getIsCorrect().getValue());
        assertEquals("Try again.", viewModel.getExplanation().getValue());
    }

    @Test
    public void testHasFeedbackAfterSetting() {
        assertFalse(viewModel.hasFeedback());

        viewModel.setCorrectFeedback(testQuestion, "Great!");

        assertTrue(viewModel.hasFeedback());
    }

    @Test
    public void testOverwriteFeedback() {
        viewModel.setCorrectFeedback(testQuestion, "First");
        viewModel.setIncorrectFeedback(testQuestion, "Second");

        assertFalse(viewModel.getIsCorrect().getValue());
        assertEquals("Second", viewModel.getExplanation().getValue());
    }

    @Test
    public void testFeedbackPreservesQuestionData() {
        viewModel.setCorrectFeedback(testQuestion, "Explanation");

        Question result = viewModel.getQuestion().getValue();
        assertNotNull(result);
        assertEquals("q-1", result.getId());
        assertEquals("What is 2+2?", result.getQuestionText());
        assertEquals("4", result.getCorrectAnswer());
    }

    @Test
    public void testSetFeedbackWithNullExplanation() {
        viewModel.setFeedback(true, testQuestion, null);

        assertTrue(viewModel.getIsCorrect().getValue());
        assertNull(viewModel.getExplanation().getValue());
        assertTrue(viewModel.hasFeedback());
    }

    @Test
    public void testLiveDataObjectsNotNull() {
        assertNotNull(viewModel.getIsCorrect());
        assertNotNull(viewModel.getExplanation());
        assertNotNull(viewModel.getQuestion());
    }
}
