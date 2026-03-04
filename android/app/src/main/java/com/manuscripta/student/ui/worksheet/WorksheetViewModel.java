package com.manuscripta.student.ui.worksheet;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.manuscripta.student.data.repository.ResponseRepository;
import com.manuscripta.student.domain.model.Question;
import com.manuscripta.student.domain.model.Response;
import com.manuscripta.student.utils.UiState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for the WorksheetFragment. Manages worksheet questions,
 * student answers, and batch submission of responses.
 */
@HiltViewModel
public class WorksheetViewModel extends ViewModel {

    /** Repository for persisting responses. */
    private final ResponseRepository responseRepository;

    /** The UI state for the worksheet questions. */
    private final MutableLiveData<UiState<List<Question>>> worksheetQuestions =
            new MutableLiveData<>(UiState.loading());

    /** Maps question IDs to student answers. */
    private final Map<String, String> answers = new LinkedHashMap<>();

    /** The device ID for submitting responses. */
    private String deviceId = "";

    /**
     * Constructor for WorksheetViewModel with Hilt injection.
     *
     * @param responseRepository The response repository for persisting answers
     */
    @Inject
    public WorksheetViewModel(@NonNull ResponseRepository responseRepository) {
        this.responseRepository = responseRepository;
    }

    /**
     * Gets the observable worksheet questions UI state.
     *
     * @return LiveData containing the UI state of the worksheet questions
     */
    @NonNull
    public LiveData<UiState<List<Question>>> getWorksheetQuestions() {
        return worksheetQuestions;
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
     * Sets the worksheet questions and clears any existing answers.
     *
     * @param questions The list of worksheet questions
     */
    public void setQuestions(@NonNull List<Question> questions) {
        answers.clear();
        if (!questions.isEmpty()) {
            worksheetQuestions.setValue(UiState.success(new ArrayList<>(questions)));
        } else {
            worksheetQuestions.setValue(UiState.error("No worksheet questions available"));
        }
    }

    /**
     * Records an answer for a specific question.
     *
     * @param questionId The ID of the question being answered
     * @param answer     The student's answer text
     */
    public void setAnswer(@NonNull String questionId, @NonNull String answer) {
        answers.put(questionId, answer);
    }

    /**
     * Gets the current answer for a specific question.
     *
     * @param questionId The ID of the question
     * @return The student's answer, or an empty string if not yet answered
     */
    @NonNull
    public String getAnswer(@NonNull String questionId) {
        String answer = answers.get(questionId);
        return answer != null ? answer : "";
    }

    /**
     * Gets an unmodifiable view of all current answers.
     *
     * @return Map of question ID to answer text
     */
    @NonNull
    public Map<String, String> getAllAnswers() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(answers));
    }

    /**
     * Submits all answers, creating Response objects and saving them.
     * Sets all answers from the provided map before submission.
     *
     * @param submittedAnswers A map of question ID to answer text
     * @return The number of responses successfully saved
     */
    public int submitAllAnswers(@NonNull Map<String, String> submittedAnswers) {
        answers.putAll(submittedAnswers);
        int count = 0;
        for (Map.Entry<String, String> entry : answers.entrySet()) {
            String questionId = entry.getKey();
            String answer = entry.getValue();
            if (answer != null && !answer.trim().isEmpty()) {
                Response response = Response.create(questionId, answer, deviceId);
                responseRepository.saveResponse(response);
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the number of questions that have been answered.
     *
     * @return The count of non-empty answers
     */
    public int getAnsweredCount() {
        int count = 0;
        for (String answer : answers.values()) {
            if (answer != null && !answer.trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the total number of worksheet questions.
     *
     * @return The total question count, or 0 if not loaded
     */
    public int getQuestionCount() {
        UiState<List<Question>> state = worksheetQuestions.getValue();
        if (state != null && state.isSuccess() && state.getData() != null) {
            return state.getData().size();
        }
        return 0;
    }

    /**
     * Sets the state to loading.
     */
    public void setLoading() {
        worksheetQuestions.setValue(UiState.loading());
    }
}
