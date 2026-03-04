package com.manuscripta.student.ui.quiz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.manuscripta.student.data.model.QuestionType;
import com.manuscripta.student.data.repository.ResponseRepository;
import com.manuscripta.student.domain.model.Question;
import com.manuscripta.student.domain.model.Response;
import com.manuscripta.student.utils.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link QuizViewModel}.
 */
public class QuizViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private ResponseRepository mockResponseRepository;

    private QuizViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new QuizViewModel(mockResponseRepository);
        viewModel.setDeviceId("test-device");
    }

    @Test
    public void testInitialStateIsLoading() {
        UiState<Question> state = viewModel.getCurrentQuestion().getValue();
        assertNotNull(state);
        assertTrue(state.isLoading());
    }

    @Test
    public void testInitialSelectedAnswerIsNegativeOne() {
        assertEquals(Integer.valueOf(-1), viewModel.getSelectedAnswer().getValue());
    }

    @Test
    public void testInitialQuestionIndexIsZero() {
        assertEquals(Integer.valueOf(0), viewModel.getCurrentQuestionIndex().getValue());
    }

    @Test
    public void testSetQuestions() {
        List<Question> questions = createTestQuestions(3);

        viewModel.setQuestions(questions);

        UiState<Question> state = viewModel.getCurrentQuestion().getValue();
        assertNotNull(state);
        assertTrue(state.isSuccess());
        assertEquals("q-0", state.getData().getId());
        assertEquals(Integer.valueOf(0), viewModel.getCurrentQuestionIndex().getValue());
    }

    @Test
    public void testSetQuestionsEmptyList() {
        viewModel.setQuestions(Collections.emptyList());

        UiState<Question> state = viewModel.getCurrentQuestion().getValue();
        assertNotNull(state);
        assertTrue(state.isError());
    }

    @Test
    public void testSelectAnswer() {
        viewModel.selectAnswer(2);
        assertEquals(Integer.valueOf(2), viewModel.getSelectedAnswer().getValue());
    }

    @Test
    public void testClearSelection() {
        viewModel.selectAnswer(1);
        viewModel.clearSelection();
        assertEquals(Integer.valueOf(-1), viewModel.getSelectedAnswer().getValue());
    }

    @Test
    public void testSubmitCorrectAnswer() {
        List<Question> questions = Arrays.asList(
                new Question("q-1", "mat-1", "What is 2+2?",
                        QuestionType.MULTIPLE_CHOICE, "[\"3\",\"4\",\"5\"]", "4")
        );
        viewModel.setQuestions(questions);

        Boolean result = viewModel.submitAnswer("4");

        assertNotNull(result);
        assertTrue(result);
        verify(mockResponseRepository, times(1)).saveResponse(any(Response.class));
    }

    @Test
    public void testSubmitIncorrectAnswer() {
        List<Question> questions = Arrays.asList(
                new Question("q-1", "mat-1", "What is 2+2?",
                        QuestionType.MULTIPLE_CHOICE, "[\"3\",\"4\",\"5\"]", "4")
        );
        viewModel.setQuestions(questions);

        Boolean result = viewModel.submitAnswer("3");

        assertNotNull(result);
        assertFalse(result);
        verify(mockResponseRepository, times(1)).saveResponse(any(Response.class));
    }

    @Test
    public void testSubmitAnswerSavesResponseWithCorrectQuestionId() {
        List<Question> questions = Arrays.asList(
                new Question("q-42", "mat-1", "Question?",
                        QuestionType.MULTIPLE_CHOICE, "[\"A\",\"B\"]", "A")
        );
        viewModel.setQuestions(questions);

        viewModel.submitAnswer("A");

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(mockResponseRepository).saveResponse(captor.capture());
        Response saved = captor.getValue();
        assertEquals("q-42", saved.getQuestionId());
        assertEquals("A", saved.getAnswer());
        assertEquals("test-device", saved.getDeviceId());
    }

    @Test
    public void testSubmitAnswerWithNoQuestionLoaded() {
        Boolean result = viewModel.submitAnswer("answer");
        assertNull(result);
        verify(mockResponseRepository, never()).saveResponse(any());
    }

    @Test
    public void testMoveToNextQuestion() {
        List<Question> questions = createTestQuestions(3);
        viewModel.setQuestions(questions);

        boolean moved = viewModel.moveToNextQuestion();

        assertTrue(moved);
        assertEquals(Integer.valueOf(1), viewModel.getCurrentQuestionIndex().getValue());
        UiState<Question> state = viewModel.getCurrentQuestion().getValue();
        assertEquals("q-1", state.getData().getId());
    }

    @Test
    public void testMoveToNextQuestionClearsSelection() {
        List<Question> questions = createTestQuestions(3);
        viewModel.setQuestions(questions);
        viewModel.selectAnswer(2);

        viewModel.moveToNextQuestion();

        assertEquals(Integer.valueOf(-1), viewModel.getSelectedAnswer().getValue());
    }

    @Test
    public void testMoveToNextQuestionAtEnd() {
        List<Question> questions = createTestQuestions(1);
        viewModel.setQuestions(questions);

        boolean moved = viewModel.moveToNextQuestion();

        assertFalse(moved);
        assertEquals(Integer.valueOf(0), viewModel.getCurrentQuestionIndex().getValue());
    }

    @Test
    public void testHasNextQuestion() {
        List<Question> questions = createTestQuestions(2);
        viewModel.setQuestions(questions);

        assertTrue(viewModel.hasNextQuestion());
        viewModel.moveToNextQuestion();
        assertFalse(viewModel.hasNextQuestion());
    }

    @Test
    public void testGetQuestionCount() {
        assertEquals(0, viewModel.getQuestionCount());

        viewModel.setQuestions(createTestQuestions(5));
        assertEquals(5, viewModel.getQuestionCount());
    }

    @Test
    public void testSetLoading() {
        viewModel.setQuestions(createTestQuestions(1));
        viewModel.setLoading();

        UiState<Question> state = viewModel.getCurrentQuestion().getValue();
        assertNotNull(state);
        assertTrue(state.isLoading());
    }

    @Test
    public void testNavigateThroughAllQuestions() {
        List<Question> questions = createTestQuestions(3);
        viewModel.setQuestions(questions);

        assertEquals("q-0", viewModel.getCurrentQuestion().getValue().getData().getId());
        assertTrue(viewModel.moveToNextQuestion());
        assertEquals("q-1", viewModel.getCurrentQuestion().getValue().getData().getId());
        assertTrue(viewModel.moveToNextQuestion());
        assertEquals("q-2", viewModel.getCurrentQuestion().getValue().getData().getId());
        assertFalse(viewModel.moveToNextQuestion());
    }

    @Test
    public void testGetAllQuestions() {
        List<Question> questions = createTestQuestions(3);
        viewModel.setQuestions(questions);

        List<Question> result = viewModel.getAllQuestions().getValue();
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testSetQuestionsResetsIndex() {
        List<Question> questions = createTestQuestions(3);
        viewModel.setQuestions(questions);
        viewModel.moveToNextQuestion();
        viewModel.moveToNextQuestion();

        viewModel.setQuestions(createTestQuestions(2));

        assertEquals(Integer.valueOf(0), viewModel.getCurrentQuestionIndex().getValue());
    }

    private List<Question> createTestQuestions(int count) {
        Question[] questions = new Question[count];
        for (int i = 0; i < count; i++) {
            questions[i] = new Question(
                    "q-" + i, "mat-1", "Question " + i + "?",
                    QuestionType.MULTIPLE_CHOICE,
                    "[\"A\",\"B\",\"C\"]", "A");
        }
        return Arrays.asList(questions);
    }
}
