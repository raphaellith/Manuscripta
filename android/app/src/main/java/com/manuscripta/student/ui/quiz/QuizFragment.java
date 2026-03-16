package com.manuscripta.student.ui.quiz;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.manuscripta.student.R;
import com.manuscripta.student.data.model.QuestionType;
import com.manuscripta.student.databinding.FragmentQuizBinding;
import com.manuscripta.student.domain.model.Question;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment displaying a quiz question with multiple-choice options.
 * Matches the QuizView from the React prototype.
 */
public class QuizFragment extends Fragment {

    /** View binding for the quiz fragment layout. */
    private FragmentQuizBinding binding;

    /** Adapter for the options RecyclerView. */
    private QuizOptionAdapter adapter;

    /** The currently displayed question. */
    private Question currentQuestion;

    /** Listener for navigation events from this fragment. */
    private QuizNavigationListener navigationListener;

    /**
     * Callback interface for quiz navigation events.
     */
    public interface QuizNavigationListener {
        /**
         * Called when the user taps the back-to-lesson button.
         */
        void onBackToLesson();

        /**
         * Called when the user submits an answer.
         *
         * @param question  The question that was answered.
         * @param answer    The selected answer text.
         * @param isCorrect Whether the answer is correct.
         */
        void onAnswerSubmitted(@NonNull Question question,
                               @NonNull String answer,
                               boolean isCorrect);
    }

    /**
     * Creates a new instance of QuizFragment.
     *
     * @return A new QuizFragment instance.
     */
    @NonNull
    public static QuizFragment newInstance() {
        return new QuizFragment();
    }

    /**
     * Sets the navigation listener for quiz events.
     *
     * @param listener The listener to set.
     */
    public void setNavigationListener(@NonNull QuizNavigationListener listener) {
        this.navigationListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentQuizBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        setupButtons();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Displays the given question in the fragment.
     *
     * @param question The question to display.
     */
    public void displayQuestion(@NonNull Question question) {
        if (binding == null) {
            return;
        }
        this.currentQuestion = question;
        binding.textLoading.setVisibility(View.GONE);
        binding.cardQuestion.setVisibility(View.VISIBLE);
        binding.recyclerOptions.setVisibility(View.VISIBLE);
        binding.buttonSubmit.setVisibility(View.VISIBLE);

        binding.textQuestion.setText(question.getQuestionText());

        if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
            List<String> options = parseOptions(question.getOptions());
            adapter.setOptions(options);
        }
    }

    /**
     * Shows the loading state, hiding quiz content.
     */
    public void showLoading() {
        if (binding == null) {
            return;
        }
        binding.textLoading.setVisibility(View.VISIBLE);
        binding.cardQuestion.setVisibility(View.GONE);
        binding.recyclerOptions.setVisibility(View.GONE);
        binding.buttonSubmit.setVisibility(View.GONE);
    }

    /**
     * Returns the current question text for text-to-speech purposes.
     *
     * @return The question text currently displayed, or empty string if none.
     */
    @NonNull
    public String getTextContent() {
        if (currentQuestion == null) {
            return "";
        }
        return currentQuestion.getQuestionText();
    }

    /**
     * Configures the options RecyclerView with a LinearLayoutManager and adapter.
     */
    private void setupRecyclerView() {
        adapter = new QuizOptionAdapter();
        binding.recyclerOptions.setLayoutManager(
                new LinearLayoutManager(requireContext()));

        int spacing = getResources().getDimensionPixelSize(R.dimen.spacing_options);
        binding.recyclerOptions.addItemDecoration(
                new OptionSpacingDecoration(spacing));
        binding.recyclerOptions.setAdapter(adapter);
    }

    /**
     * Sets up button click listeners for submit actions.
     */
    private void setupButtons() {
        binding.buttonSubmit.setOnClickListener(v -> handleSubmit());
    }

    /**
     * Handles the submit button click.
     * Shows a toast if no option is selected; otherwise checks the answer
     * and notifies the navigation listener.
     */
    private void handleSubmit() {
        if (currentQuestion == null) {
            return;
        }
        int selected = adapter.getSelectedIndex();
        if (selected < 0) {
            Toast.makeText(requireContext(),
                    R.string.select_answer_prompt, Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> options = parseOptions(currentQuestion.getOptions());
        String answer = String.valueOf(selected);
        String correctText = resolveCorrectAnswer(currentQuestion.getCorrectAnswer(), options);
        String selectedText = selected < options.size() ? options.get(selected) : "";
        boolean isCorrect = selectedText.equals(correctText);

        if (navigationListener != null) {
            navigationListener.onAnswerSubmitted(currentQuestion, answer, isCorrect);
        }
    }

    /**
     * Resolves the correct answer to its option text.
     * If the value is a numeric string that is a valid index into the options list,
     * returns the option text at that index. Otherwise returns the value as-is.
     *
     * @param correctAnswer The raw correct answer value from the server.
     * @param options       The list of option text values for this question.
     * @return The resolved option text.
     */
    @NonNull
    public static String resolveCorrectAnswer(@NonNull String correctAnswer,
            @NonNull List<String> options) {
        try {
            int index = Integer.parseInt(correctAnswer);
            if (index >= 0 && index < options.size()) {
                return options.get(index);
            }
        } catch (NumberFormatException e) {
            // Not a numeric index — use as-is
        }
        return correctAnswer;
    }

    /**
     * Parses a JSON array string into a list of option strings.
     *
     * @param json The JSON array string to parse.
     * @return A list of option strings, or an empty list if parsing fails.
     */
    @NonNull
    public static List<String> parseOptions(@NonNull String json) {
        List<String> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                result.add(array.getString(i));
            }
        } catch (JSONException e) {
            // Return empty list for unparseable options
        }
        return result;
    }
}
