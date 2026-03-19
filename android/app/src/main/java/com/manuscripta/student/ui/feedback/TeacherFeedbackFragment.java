package com.manuscripta.student.ui.feedback;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.manuscripta.student.R;
import com.manuscripta.student.databinding.FragmentTeacherFeedbackBinding;
import com.manuscripta.student.domain.model.Feedback;
import com.manuscripta.student.ui.renderer.MarkdownRenderer;
import com.manuscripta.student.ui.renderer.QuestionBlockRenderer;

import java.util.Collections;

/**
 * Fragment displaying teacher feedback using material-style rendering.
 *
 * <p>Feedback is presented as full-width lesson content with optional marks
 * followed by markdown-rendered feedback text.</p>
 */
public class TeacherFeedbackFragment extends Fragment {

    /** Bundle key for feedback text. */
    private static final String ARG_TEXT = "text";

    /** Bundle key for feedback marks. */
    private static final String ARG_MARKS = "marks";

    /** Bundle key indicating if marks were provided. */
    private static final String ARG_HAS_MARKS = "hasMarks";

    /** View binding for this fragment. */
    private FragmentTeacherFeedbackBinding binding;

    /** Renderer for markdown feedback text. */
    @Nullable
    private MarkdownRenderer markdownRenderer;

    /**
     * Creates a new teacher feedback fragment from a feedback domain object.
     *
     * @param feedback The feedback item to display
     * @return A configured fragment instance
     */
    @NonNull
    public static TeacherFeedbackFragment newInstance(@NonNull Feedback feedback) {
        TeacherFeedbackFragment fragment = new TeacherFeedbackFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TEXT, feedback.getText());
        args.putBoolean(ARG_HAS_MARKS, feedback.hasMarks());
        if (feedback.hasMarks() && feedback.getMarks() != null) {
            args.putInt(ARG_MARKS, feedback.getMarks());
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTeacherFeedbackBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        renderFeedback();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Returns text content for TTS.
     *
     * @return Combined marks and feedback text
     */
    @NonNull
    public String getTextContent() {
        if (binding == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        if (binding.feedbackMarks.getVisibility() == View.VISIBLE) {
            text.append(binding.feedbackMarks.getText());
        }
        Bundle args = getArguments();
        if (args != null) {
            String feedbackText = args.getString(ARG_TEXT, "");
            if (!feedbackText.trim().isEmpty()) {
                if (text.length() > 0) {
                    text.append(". ");
                }
                text.append(feedbackText);
            }
        }
        return text.toString();
    }

    /**
     * Renders marks and markdown feedback body.
     */
    private void renderFeedback() {
        if (binding == null) {
            return;
        }

        Bundle args = getArguments();
        if (args == null) {
            binding.feedbackEmpty.setVisibility(View.VISIBLE);
            return;
        }

        boolean hasMarks = args.getBoolean(ARG_HAS_MARKS, false);
        if (hasMarks) {
            int marks = args.getInt(ARG_MARKS, 0);
            binding.feedbackMarks.setText(getString(R.string.teacher_feedback_marks, marks));
            binding.feedbackMarks.setVisibility(View.VISIBLE);
        } else {
            binding.feedbackMarks.setVisibility(View.GONE);
        }

        String feedbackText = args.getString(ARG_TEXT, "");
        if (feedbackText.trim().isEmpty()) {
            binding.feedbackEmpty.setVisibility(View.VISIBLE);
            return;
        }

        binding.feedbackEmpty.setVisibility(View.GONE);
        if (markdownRenderer == null) {
            markdownRenderer = new MarkdownRenderer(
                    requireContext(),
                    new QuestionBlockRenderer(),
                    null,
                    null);
        }

        markdownRenderer.render(
                binding.feedbackContent,
                feedbackText,
                "teacher-feedback",
                Collections.emptyMap());
    }
}
