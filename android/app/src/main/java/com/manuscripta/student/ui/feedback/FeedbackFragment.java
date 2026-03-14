package com.manuscripta.student.ui.feedback;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.manuscripta.student.R;
import com.manuscripta.student.databinding.FragmentFeedbackBinding;

/**
 * Fragment displaying feedback after a quiz answer submission.
 * Shows a correct (checkmark) or incorrect (cross) variant
 * matching the FeedbackView from the React prototype.
 */
public class FeedbackFragment extends Fragment {

    /** Bundle key for the correctness boolean. */
    private static final String ARG_IS_CORRECT = "isCorrect";

    /** Bundle key for the explanation text. */
    private static final String ARG_EXPLANATION = "explanation";

    /** Bundle key for the correct answer text (incorrect variant). */
    private static final String ARG_CORRECT_ANSWER = "correctAnswer";

    /** View binding for the feedback fragment layout. */
    private FragmentFeedbackBinding binding;

    /** Listener for navigation events from this fragment. */
    private FeedbackNavigationListener navigationListener;

    /**
     * Callback interface for feedback navigation events.
     */
    public interface FeedbackNavigationListener {
        /**
         * Called when the user taps "Next Question" (correct variant).
         */
        void onNextQuestion();

        /**
         * Called when the user taps "Try Again" (incorrect variant).
         */
        void onTryAgain();
    }

    /**
     * Creates a new FeedbackFragment for a correct answer.
     *
     * @param explanation The explanation text to display.
     * @return A new FeedbackFragment instance configured for the correct variant.
     */
    @NonNull
    public static FeedbackFragment newCorrectInstance(@NonNull String explanation) {
        FeedbackFragment fragment = new FeedbackFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_CORRECT, true);
        args.putString(ARG_EXPLANATION, explanation);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Creates a new FeedbackFragment for an incorrect answer.
     *
     * @param correctAnswer The correct answer text to display.
     * @return A new FeedbackFragment instance configured for the incorrect variant.
     */
    @NonNull
    public static FeedbackFragment newIncorrectInstance(@NonNull String correctAnswer) {
        FeedbackFragment fragment = new FeedbackFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_CORRECT, false);
        args.putString(ARG_CORRECT_ANSWER, correctAnswer);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Sets the navigation listener for feedback events.
     *
     * @param listener The listener to set.
     */
    public void setNavigationListener(@NonNull FeedbackNavigationListener listener) {
        this.navigationListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFeedbackBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupFromArguments();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Returns the feedback text content for text-to-speech purposes.
     *
     * @return The feedback title and explanation text.
     */
    @NonNull
    public String getTextContent() {
        if (binding == null) {
            return "";
        }
        return binding.textTitle.getText() + ". "
                + binding.textExplanation.getText();
    }

    /**
     * Configures the fragment UI from its arguments bundle.
     */
    private void setupFromArguments() {
        Bundle args = getArguments();
        if (args == null) {
            return;
        }

        boolean isCorrect = args.getBoolean(ARG_IS_CORRECT, true);

        if (isCorrect) {
            setupCorrectVariant(args);
        } else {
            setupIncorrectVariant(args);
        }
    }

    /**
     * Configures the UI for the correct answer variant.
     *
     * @param args The arguments bundle containing explanation text.
     */
    private void setupCorrectVariant(@NonNull Bundle args) {
        binding.textIcon.setText(getString(R.string.feedback_icon_correct));
        binding.textTitle.setText(R.string.feedback_correct);

        String explanation = args.getString(ARG_EXPLANATION, "");
        binding.textExplanation.setText(explanation);
        binding.textCorrectLabel.setVisibility(View.GONE);

        binding.buttonAction.setText(R.string.next_question);
        binding.buttonAction.setOnClickListener(v -> {
            if (navigationListener != null) {
                navigationListener.onNextQuestion();
            }
        });
    }

    /**
     * Configures the UI for the incorrect answer variant.
     *
     * @param args The arguments bundle containing the correct answer text.
     */
    private void setupIncorrectVariant(@NonNull Bundle args) {
        binding.textIcon.setText(getString(R.string.feedback_icon_incorrect));
        binding.textTitle.setText(R.string.feedback_incorrect);

        String correctAnswer = args.getString(ARG_CORRECT_ANSWER, "");
        binding.textExplanation.setText(correctAnswer);
        binding.textCorrectLabel.setVisibility(View.VISIBLE);

        binding.buttonAction.setText(R.string.try_again);
        binding.buttonAction.setOnClickListener(v -> {
            if (navigationListener != null) {
                navigationListener.onTryAgain();
            }
        });
    }
}
