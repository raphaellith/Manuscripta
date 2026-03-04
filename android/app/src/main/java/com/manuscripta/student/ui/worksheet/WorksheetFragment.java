package com.manuscripta.student.ui.worksheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.manuscripta.student.R;
import com.manuscripta.student.databinding.FragmentWorksheetBinding;
import com.manuscripta.student.domain.model.Question;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment displaying worksheet questions with fill-in-the-blank inputs.
 * Matches the WorksheetView from the React prototype.
 */
public class WorksheetFragment extends Fragment {

    /** View binding for the worksheet fragment layout. */
    private FragmentWorksheetBinding binding;

    /** Maps question IDs to their answer EditText fields. */
    private final Map<String, EditText> answerFields = new LinkedHashMap<>();

    /** The list of currently displayed questions. */
    private final List<Question> currentQuestions = new ArrayList<>();

    /** Listener for worksheet submission events. */
    private WorksheetSubmitListener submitListener;

    /** Placeholder used to mark blanks in question text. */
    private static final String BLANK_MARKER = "______";

    /**
     * Callback interface for worksheet submission events.
     */
    public interface WorksheetSubmitListener {
        /**
         * Called when the user submits all worksheet answers.
         *
         * @param answers A map of question ID to the student's answer text.
         */
        void onAnswersSubmitted(@NonNull Map<String, String> answers);
    }

    /**
     * Creates a new instance of WorksheetFragment.
     *
     * @return A new WorksheetFragment instance.
     */
    @NonNull
    public static WorksheetFragment newInstance() {
        return new WorksheetFragment();
    }

    /**
     * Sets the listener for worksheet submission events.
     *
     * @param listener The listener to set.
     */
    public void setSubmitListener(@NonNull WorksheetSubmitListener listener) {
        this.submitListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWorksheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.buttonSubmit.setOnClickListener(v -> handleSubmit());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Displays the given list of worksheet questions.
     *
     * @param questions The questions to display.
     */
    public void displayQuestions(@NonNull List<Question> questions) {
        if (binding == null) {
            return;
        }
        binding.textLoading.setVisibility(View.GONE);
        binding.textTitle.setVisibility(View.VISIBLE);
        binding.scrollItems.setVisibility(View.VISIBLE);
        binding.buttonSubmit.setVisibility(View.VISIBLE);

        currentQuestions.clear();
        currentQuestions.addAll(questions);
        answerFields.clear();
        binding.layoutItems.removeAllViews();

        for (int i = 0; i < questions.size(); i++) {
            addWorksheetItem(questions.get(i), i + 1);
        }
    }

    /**
     * Shows the loading state, hiding worksheet content.
     */
    public void showLoading() {
        if (binding == null) {
            return;
        }
        binding.textLoading.setVisibility(View.VISIBLE);
        binding.textTitle.setVisibility(View.GONE);
        binding.scrollItems.setVisibility(View.GONE);
        binding.buttonSubmit.setVisibility(View.GONE);
    }

    /**
     * Returns the current worksheet text for text-to-speech purposes.
     *
     * @return The concatenated question text, or empty string if none.
     */
    @NonNull
    public String getTextContent() {
        StringBuilder sb = new StringBuilder();
        for (Question q : currentQuestions) {
            if (sb.length() > 0) {
                sb.append(". ");
            }
            sb.append(q.getQuestionText().replace(BLANK_MARKER, "blank"));
        }
        return sb.toString();
    }

    /**
     * Collects all answers from the input fields.
     *
     * @return A map of question ID to the student's answer text.
     */
    @NonNull
    public Map<String, String> collectAnswers() {
        Map<String, String> answers = new LinkedHashMap<>();
        for (Map.Entry<String, EditText> entry : answerFields.entrySet()) {
            String text = entry.getValue().getText().toString().trim();
            answers.put(entry.getKey(), text);
        }
        return answers;
    }

    /**
     * Adds a single worksheet item view to the layout.
     *
     * @param question The question to render.
     * @param number   The 1-based item number.
     */
    private void addWorksheetItem(@NonNull Question question, int number) {
        View itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_worksheet, binding.layoutItems, false);

        TextView textNumber = itemView.findViewById(R.id.textNumber);
        TextView textBefore = itemView.findViewById(R.id.textBefore);
        EditText editAnswer = itemView.findViewById(R.id.editAnswer);
        TextView textAfter = itemView.findViewById(R.id.textAfter);

        textNumber.setText(number + ". ");

        String questionText = question.getQuestionText();
        int blankIndex = questionText.indexOf(BLANK_MARKER);
        if (blankIndex >= 0) {
            textBefore.setText(questionText.substring(0, blankIndex));
            textAfter.setText(questionText.substring(
                    blankIndex + BLANK_MARKER.length()));
            editAnswer.setVisibility(View.VISIBLE);
        } else {
            textBefore.setText(questionText);
            textAfter.setVisibility(View.GONE);
            editAnswer.setVisibility(View.VISIBLE);
            editAnswer.setHint("");
        }

        answerFields.put(question.getId(), editAnswer);
        binding.layoutItems.addView(itemView);
    }

    /**
     * Handles the submit button click by collecting answers and notifying the listener.
     */
    private void handleSubmit() {
        if (submitListener != null) {
            submitListener.onAnswersSubmitted(collectAnswers());
        }
    }
}
