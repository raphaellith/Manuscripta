package com.manuscripta.student.ui.feedback;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.manuscripta.student.domain.model.Question;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for the FeedbackFragment. Manages the feedback state
 * including correctness, explanation, and the associated question.
 */
@HiltViewModel
public class FeedbackViewModel extends ViewModel {

    /** Whether the answer was correct. */
    private final MutableLiveData<Boolean> isCorrect = new MutableLiveData<>();

    /** The explanation or correct answer text. */
    private final MutableLiveData<String> explanation = new MutableLiveData<>();

    /** The question that was answered. */
    private final MutableLiveData<Question> question = new MutableLiveData<>();

    /**
     * Constructor for FeedbackViewModel with Hilt injection.
     */
    @Inject
    public FeedbackViewModel() {
        // No dependencies needed
    }

    /**
     * Gets the observable correctness state.
     *
     * @return LiveData indicating whether the answer was correct
     */
    @NonNull
    public LiveData<Boolean> getIsCorrect() {
        return isCorrect;
    }

    /**
     * Gets the observable explanation text.
     *
     * @return LiveData containing the explanation or correct answer text
     */
    @NonNull
    public LiveData<String> getExplanation() {
        return explanation;
    }

    /**
     * Gets the observable question.
     *
     * @return LiveData containing the question that was answered
     */
    @NonNull
    public LiveData<Question> getQuestion() {
        return question;
    }

    /**
     * Sets the feedback data for a correct answer.
     *
     * @param answeredQuestion The question that was answered
     * @param explanationText  The explanation text to display
     */
    public void setCorrectFeedback(@NonNull Question answeredQuestion,
                                   @NonNull String explanationText) {
        isCorrect.setValue(true);
        question.setValue(answeredQuestion);
        explanation.setValue(explanationText);
    }

    /**
     * Sets the feedback data for an incorrect answer.
     *
     * @param answeredQuestion The question that was answered
     * @param correctAnswer    The correct answer text to display
     */
    public void setIncorrectFeedback(@NonNull Question answeredQuestion,
                                     @NonNull String correctAnswer) {
        isCorrect.setValue(false);
        question.setValue(answeredQuestion);
        explanation.setValue(correctAnswer);
    }

    /**
     * Sets up the feedback state directly with all parameters.
     *
     * @param correct         Whether the answer was correct
     * @param answeredQuestion The question that was answered
     * @param explanationText  The explanation or correct answer text
     */
    public void setFeedback(boolean correct,
                            @NonNull Question answeredQuestion,
                            @NonNull String explanationText) {
        isCorrect.setValue(correct);
        question.setValue(answeredQuestion);
        explanation.setValue(explanationText);
    }

    /**
     * Checks if feedback data has been set.
     *
     * @return true if feedback data is available
     */
    public boolean hasFeedback() {
        return isCorrect.getValue() != null && question.getValue() != null;
    }
}
