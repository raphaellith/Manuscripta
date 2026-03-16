package com.manuscripta.student.ui.worksheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.manuscripta.student.databinding.FragmentWorksheetBinding;
import com.manuscripta.student.domain.model.Material;
import com.manuscripta.student.domain.model.Question;
import com.manuscripta.student.ui.renderer.AttachmentImageLoader;
import com.manuscripta.student.ui.renderer.MarkdownRenderer;
import com.manuscripta.student.ui.renderer.QuestionBlockRenderer;
import com.manuscripta.student.utils.FileStorageManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

    /** Maps question IDs to their written-answer EditText fields. */
    private final Map<String, EditText> answerFields = new LinkedHashMap<>();

    /** Maps question IDs to their multiple-choice RadioGroup fields. */
    private final Map<String, RadioGroup> mcFields = new LinkedHashMap<>();

    /** Renderer for question blocks (multiple choice, written answer). */
    private final QuestionBlockRenderer questionBlockRenderer = new QuestionBlockRenderer();

    /** Renderer for worksheet markdown content and embedded question blocks. */
    @Nullable
    private MarkdownRenderer markdownRenderer;

    /** Loader for attachment images inside worksheet content. */
    @Nullable
    private AttachmentImageLoader attachmentImageLoader;

    /** File storage manager for worksheet PDF embeds. */
    @Nullable
    private FileStorageManager fileStorageManager;

    /** Text scale factor from configuration (1.0 = default). */
    private float textScaleFactor = 1.0f;

    /** The list of currently displayed questions. */
    private final List<Question> currentQuestions = new ArrayList<>();

    /** Listener for worksheet submission events. */
    private WorksheetSubmitListener submitListener;

    /** Last rendered material content for accessibility/text-to-speech support. */
    @NonNull
    private String currentMaterialContent = "";

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

    /**
     * Sets the attachment image loader used by the markdown renderer.
     *
     * @param loader loader for worksheet attachment images
     */
    public void setAttachmentImageLoader(@NonNull AttachmentImageLoader loader) {
        this.attachmentImageLoader = loader;
    }

    /**
     * Sets the file storage manager used for worksheet PDF embeds.
     *
     * @param manager storage manager used by the markdown renderer
     */
    public void setFileStorageManager(@NonNull FileStorageManager manager) {
        this.fileStorageManager = manager;
    }

    /**
     * Sets the text scale factor for rendered worksheet content.
     *
     * @param scaleFactor text scaling factor (1.0 = default)
     */
    public void setTextScaleFactor(float scaleFactor) {
        this.textScaleFactor = scaleFactor;
        if (markdownRenderer != null) {
            markdownRenderer.setTextScaleFactor(scaleFactor);
        }
    }

    /**
     * Clears the cached markdown renderer so dependencies can be re-injected.
     */
    public void resetRenderer() {
        this.markdownRenderer = null;
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
     * Displays worksheet material content with embedded questions.
     *
     * @param material The worksheet material to display.
     * @param questions The questions associated with this worksheet.
     */
    public void displayMaterial(@NonNull Material material,
                                @NonNull List<Question> questions) {
        if (binding == null) {
            return;
        }

        binding.textLoading.setVisibility(View.GONE);
        binding.scrollItems.setVisibility(View.VISIBLE);
        binding.buttonSubmit.setVisibility(View.VISIBLE);

        currentMaterialContent = material.getContent();
        currentQuestions.clear();
        currentQuestions.addAll(questions);

        if (markdownRenderer == null) {
            markdownRenderer = new MarkdownRenderer(
                    requireContext(),
                    questionBlockRenderer,
                    attachmentImageLoader,
                    fileStorageManager);
            markdownRenderer.setTextScaleFactor(textScaleFactor);
        }

        Map<String, Question> questionMap;
        if (questions.isEmpty()) {
            questionMap = Collections.emptyMap();
        } else {
            questionMap = new HashMap<>();
            for (Question question : questions) {
                questionMap.put(question.getId(), question);
            }
        }

        markdownRenderer.render(
                binding.layoutItems,
                material.getContent(),
                material.getId(),
                questionMap);
        rebindInteractiveFields();
    }

    /**
     * Backwards-compatible entry point that renders questions only.
     *
     * @param questions The questions to display.
     */
    public void displayQuestions(@NonNull List<Question> questions) {
        if (questions.isEmpty()) {
            showLoading();
            return;
        }
        StringBuilder syntheticContent = new StringBuilder();
        for (Question question : questions) {
            if (syntheticContent.length() > 0) {
                syntheticContent.append("\n\n");
            }
            syntheticContent.append("!!! question id=\"")
                    .append(question.getId())
                    .append("\"");
        }
        Material syntheticMaterial = new Material(
                questions.get(0).getMaterialId(),
                com.manuscripta.student.data.model.MaterialType.WORKSHEET,
                "Worksheet",
                syntheticContent.toString(),
                "{}",
                "[]",
                System.currentTimeMillis());
        displayMaterial(syntheticMaterial, questions);
    }

    /**
     * Shows the loading state, hiding worksheet content.
     */
    public void showLoading() {
        if (binding == null) {
            return;
        }
        binding.textLoading.setVisibility(View.VISIBLE);
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
        String materialText = currentMaterialContent.replace("______", "blank");
        StringBuilder sb = new StringBuilder();
        if (!materialText.trim().isEmpty()) {
            sb.append(materialText);
        }
        for (Question q : currentQuestions) {
            if (sb.length() > 0) {
                sb.append(". ");
            }
            sb.append(q.getQuestionText().replace("______", "blank"));
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
        for (Map.Entry<String, RadioGroup> entry : mcFields.entrySet()) {
            RadioGroup radioGroup = entry.getValue();
            int checkedId = radioGroup.getCheckedRadioButtonId();
            if (checkedId != -1) {
                RadioButton selected = radioGroup.findViewById(checkedId);
                answers.put(entry.getKey(), selected.getText().toString());
            } else {
                answers.put(entry.getKey(), "");
            }
        }
        return answers;
    }

    /**
     * Rebuilds the answer field index by scanning rendered worksheet views.
     */
    private void rebindInteractiveFields() {
        answerFields.clear();
        mcFields.clear();
        if (binding == null) {
            return;
        }
        collectFieldsRecursive(binding.layoutItems);
    }

    /**
     * Traverses rendered views and registers question input controls by tagged question ID.
     *
     * @param view Current view in the traversal
     */
    private void collectFieldsRecursive(@NonNull View view) {
        Object tag = view.getTag();

        if (view instanceof EditText
                && questionBlockRenderer.isWrittenAnswerTag(tag)) {
            String questionId = questionBlockRenderer.extractQuestionIdFromTag(tag);
            if (questionId != null) {
                answerFields.put(questionId, (EditText) view);
            }
        }

        if (view instanceof RadioGroup
                && questionBlockRenderer.isMultipleChoiceTag(tag)) {
            String questionId = questionBlockRenderer.extractQuestionIdFromTag(tag);
            if (questionId != null) {
                mcFields.put(questionId, (RadioGroup) view);
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectFieldsRecursive(group.getChildAt(i));
            }
        }
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
