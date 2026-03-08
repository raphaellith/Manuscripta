package com.manuscripta.student.ui.quiz;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.manuscripta.student.data.repository.ResponseRepository;
import com.manuscripta.student.domain.model.Question;
import com.manuscripta.student.domain.model.Response;
import com.manuscripta.student.utils.UiState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for the QuizFragment. Manages the quiz flow including
 * current question display, answer selection, and submission.
 */
@HiltViewModel
public class QuizViewModel extends ViewModel {

    /** Repository for persisting responses. */
    private final ResponseRepository responseRepository;

    /** The UI state for the current question. */
    private final MutableLiveData<UiState<Question>> currentQuestion =
            new MutableLiveData<>(UiState.loading());

    /** All quiz questions for the current material. */
    private final MutableLiveData<List<Question>> allQuestions =
            new MutableLiveData<>(Collections.emptyList());

    /** The index of the currently displayed question. */
    private final MutableLiveData<Integer> currentQuestionIndex = new MutableLiveData<>(0);

    /** The currently selected answer option index, or -1 if none selected. */
    private final MutableLiveData<Integer> selectedAnswer = new MutableLiveData<>(-1);

    /** The device ID for submitting responses. */
    private String deviceId = "";

    /**
     * Constructor for QuizViewModel with Hilt injection.
     *
     * @param responseRepository The response repository for persisting answers
     */
    @Inject
    public QuizViewModel(@NonNull ResponseRepository responseRepository) {
        this.responseRepository = responseRepository;
    }

    /**
     * Gets the observable UI state for the current question.
     *
     * @return LiveData containing the current question UI state
     */
    @NonNull
    public LiveData<UiState<Question>> getCurrentQuestion() {
        return currentQuestion;
    }

    /**
     * Gets the observable list of all quiz questions.
     *
     * @return LiveData containing all quiz questions
     */
    @NonNull
    public LiveData<List<Question>> getAllQuestions() {
        return allQuestions;
    }

    /**
     * Gets the observable current question index.
     *
     * @return LiveData containing the index of the current question
     */
    @NonNull
    public LiveData<Integer> getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    /**
     * Gets the observable selected answer index.
     *
     * @return LiveData containing the selected answer index, or -1 if none
     */
    @NonNull
    public LiveData<Integer> getSelectedAnswer() {
        return selectedAnswer;
    }

    /**
     * Sets the device ID for response submission.
     *
     * @param deviceId The device identifier
     */
    public void setDeviceId(@NonNull String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Sets the list of quiz questions and resets to the first question.
     *
     * @param questions The list of quiz questions
     */
    public void setQuestions(@NonNull List<Question> questions) {
        allQuestions.setValue(new ArrayList<>(questions));
        currentQuestionIndex.setValue(0);
        selectedAnswer.setValue(-1);
        if (!questions.isEmpty()) {
            currentQuestion.setValue(UiState.success(questions.get(0)));
        } else {
            currentQuestion.setValue(UiState.error("No quiz questions available"));
        }
    }

    /**
     * Selects an answer option by index.
     *
     * @param index The index of the selected option
     */
    public void selectAnswer(int index) {
        selectedAnswer.setValue(index);
    }

    /**
     * Clears the current answer selection.
     */
    public void clearSelection() {
        selectedAnswer.setValue(-1);
    }

    /**
     * Submits the currently selected answer for the current question.
     * Creates a Response domain object and saves it via the repository.
     *
     * @param answer The answer text to submit
     * @return true if the answer is correct, false otherwise;
     *         null if no question is loaded
     */
    @Nullable
    public Boolean submitAnswer(@NonNull String answer) {
        UiState<Question> state = currentQuestion.getValue();
        if (state == null || !state.isSuccess() || state.getData() == null) {
            return null;
        }
        Question question = state.getData();
        boolean isCorrect = answer.equals(question.getCorrectAnswer());

        Response response = Response.create(question.getId(), answer, deviceId);
        responseRepository.saveResponse(response);

        return isCorrect;
    }

    /**
     * Advances to the next question in the list.
     *
     * @return true if there is a next question and navigation occurred, false otherwise
     */
    public boolean moveToNextQuestion() {
        List<Question> questions = allQuestions.getValue();
        Integer index = currentQuestionIndex.getValue();
        if (questions == null || index == null) {
            return false;
        }
        int nextIndex = index + 1;
        if (nextIndex < questions.size()) {
            currentQuestionIndex.setValue(nextIndex);
            selectedAnswer.setValue(-1);
            currentQuestion.setValue(UiState.success(questions.get(nextIndex)));
            return true;
        }
        return false;
    }

    /**
     * Checks whether there are more questions after the current one.
     *
     * @return true if there are more questions remaining
     */
    public boolean hasNextQuestion() {
        List<Question> questions = allQuestions.getValue();
        Integer index = currentQuestionIndex.getValue();
        if (questions == null || index == null) {
            return false;
        }
        return (index + 1) < questions.size();
    }

    /**
     * Gets the total number of quiz questions.
     *
     * @return The total question count
     */
    public int getQuestionCount() {
        List<Question> questions = allQuestions.getValue();
        return questions != null ? questions.size() : 0;
    }

    /**
     * Sets the state to loading.
     */
    public void setLoading() {
        currentQuestion.setValue(UiState.loading());
    }
}
