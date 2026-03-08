package com.manuscripta.student.ui.worksheet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.manuscripta.student.data.model.QuestionType;
import com.manuscripta.student.data.repository.ResponseRepository;
import com.manuscripta.student.domain.model.Question;
import com.manuscripta.student.domain.model.Response;
import com.manuscripta.student.network.tcp.PairingManager;
import com.manuscripta.student.utils.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link WorksheetViewModel}.
 */
public class WorksheetViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private ResponseRepository mockResponseRepository;

    @Mock
    private PairingManager mockPairingManager;

    private WorksheetViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockPairingManager.getDeviceId()).thenReturn("test-device");
        viewModel = new WorksheetViewModel(mockResponseRepository, mockPairingManager);
    }

    @Test
    public void testInitialStateIsLoading() {
        UiState<List<Question>> state = viewModel.getWorksheetQuestions().getValue();
        assertNotNull(state);
        assertTrue(state.isLoading());
    }

    @Test
    public void testSetQuestions() {
        List<Question> questions = createTestQuestions(3);

        viewModel.setQuestions(questions);

        UiState<List<Question>> state = viewModel.getWorksheetQuestions().getValue();
        assertNotNull(state);
        assertTrue(state.isSuccess());
        assertEquals(3, state.getData().size());
    }

    @Test
    public void testSetQuestionsEmptyList() {
        viewModel.setQuestions(Collections.emptyList());

        UiState<List<Question>> state = viewModel.getWorksheetQuestions().getValue();
        assertNotNull(state);
        assertTrue(state.isError());
    }

    @Test
    public void testSetAndGetAnswer() {
        viewModel.setAnswer("q-1", "My answer");

        assertEquals("My answer", viewModel.getAnswer("q-1"));
    }

    @Test
    public void testGetAnswerReturnsEmptyStringForUnset() {
        assertEquals("", viewModel.getAnswer("nonexistent"));
    }

    @Test
    public void testOverwriteAnswer() {
        viewModel.setAnswer("q-1", "First answer");
        viewModel.setAnswer("q-1", "Updated answer");

        assertEquals("Updated answer", viewModel.getAnswer("q-1"));
    }

    @Test
    public void testGetAllAnswers() {
        viewModel.setAnswer("q-1", "Answer 1");
        viewModel.setAnswer("q-2", "Answer 2");

        Map<String, String> answers = viewModel.getAllAnswers();

        assertEquals(2, answers.size());
        assertEquals("Answer 1", answers.get("q-1"));
        assertEquals("Answer 2", answers.get("q-2"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetAllAnswersReturnsUnmodifiableMap() {
        viewModel.setAnswer("q-1", "Answer 1");

        Map<String, String> result = viewModel.getAllAnswers();
        result.put("q-extra", "Injected");
    }

    @Test
    public void testSubmitAllAnswers() {
        Map<String, String> answers = new HashMap<>();
        answers.put("q-1", "Answer 1");
        answers.put("q-2", "Answer 2");
        answers.put("q-3", "Answer 3");

        int count = viewModel.submitAllAnswers(answers);

        assertEquals(3, count);
        verify(mockResponseRepository, times(3)).saveResponse(any(Response.class));
    }

    @Test
    public void testSubmitAllAnswersWithEmptyMap() {
        int count = viewModel.submitAllAnswers(Collections.emptyMap());

        assertEquals(0, count);
    }

    @Test
    public void testSubmitAllAnswersSavesCorrectData() {
        Map<String, String> answers = new HashMap<>();
        answers.put("q-42", "Test answer");

        viewModel.submitAllAnswers(answers);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(mockResponseRepository).saveResponse(captor.capture());
        Response saved = captor.getValue();
        assertEquals("q-42", saved.getQuestionId());
        assertEquals("Test answer", saved.getAnswer());
        assertEquals("test-device", saved.getDeviceId());
    }

    @Test
    public void testGetAnsweredCount() {
        assertEquals(0, viewModel.getAnsweredCount());

        viewModel.setAnswer("q-1", "A");
        assertEquals(1, viewModel.getAnsweredCount());

        viewModel.setAnswer("q-2", "B");
        assertEquals(2, viewModel.getAnsweredCount());
    }

    @Test
    public void testGetQuestionCount() {
        assertEquals(0, viewModel.getQuestionCount());

        viewModel.setQuestions(createTestQuestions(4));
        assertEquals(4, viewModel.getQuestionCount());
    }

    @Test
    public void testSetLoading() {
        viewModel.setQuestions(createTestQuestions(2));
        viewModel.setLoading();

        UiState<List<Question>> state = viewModel.getWorksheetQuestions().getValue();
        assertNotNull(state);
        assertTrue(state.isLoading());
    }

    @Test
    public void testSetQuestionsClearsAnswers() {
        viewModel.setAnswer("q-0", "Saved answer");
        viewModel.setQuestions(createTestQuestions(3));

        assertEquals("", viewModel.getAnswer("q-0"));
    }

    private List<Question> createTestQuestions(int count) {
        Question[] questions = new Question[count];
        for (int i = 0; i < count; i++) {
            questions[i] = new Question(
                    "q-" + i, "mat-1", "Worksheet question " + i + "?",
                    QuestionType.WRITTEN_ANSWER,
                    "", "");
        }
        return Arrays.asList(questions);
    }
}
