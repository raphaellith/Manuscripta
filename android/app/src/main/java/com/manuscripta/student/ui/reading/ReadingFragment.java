package com.manuscripta.student.ui.reading;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.manuscripta.student.databinding.FragmentReadingBinding;
import com.manuscripta.student.domain.model.Material;
import com.manuscripta.student.domain.model.Question;
import com.manuscripta.student.ui.renderer.AttachmentImageLoader;
import com.manuscripta.student.ui.renderer.MarkdownRenderer;
import com.manuscripta.student.ui.renderer.QuestionBlockRenderer;
import com.manuscripta.student.utils.FileStorageManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment displaying reading material content.
 * Matches the LessonView from the React prototype.
 */
public class ReadingFragment extends Fragment {

    /** View binding for the reading fragment layout. */
    private FragmentReadingBinding binding;

    /** Renderer for markdown content with admonition support. */
    private MarkdownRenderer markdownRenderer;

    /** Loader for attachment images. */
    @Nullable
    private AttachmentImageLoader attachmentImageLoader;

    /** File storage manager for PDF attachments. */
    @Nullable
    private FileStorageManager fileStorageManager;

    /** Text scale factor from configuration (1.0 = default). */
    private float textScaleFactor = 1.0f;

    /**
     * Creates a new instance of ReadingFragment.
     *
     * @return A new ReadingFragment instance.
     */
    @NonNull
    public static ReadingFragment newInstance() {
        return new ReadingFragment();
    }

    /**
     * Sets the attachment image loader for rendering embedded images.
     *
     * @param loader the attachment image loader
     */
    public void setAttachmentImageLoader(
            @NonNull AttachmentImageLoader loader) {
        this.attachmentImageLoader = loader;
    }

    /**
     * Sets the file storage manager for PDF rendering.
     *
     * @param manager the file storage manager
     */
    public void setFileStorageManager(
            @NonNull FileStorageManager manager) {
        this.fileStorageManager = manager;
    }

    /**
     * Sets the text scale factor applied to rendered content.
     * Resets the cached renderer so the next render uses the
     * updated factor.
     *
     * @param scaleFactor the scale factor (1.0 = default)
     */
    public void setTextScaleFactor(float scaleFactor) {
        this.textScaleFactor = scaleFactor;
        if (markdownRenderer != null) {
            markdownRenderer.setTextScaleFactor(scaleFactor);
        }
    }

    /**
     * Resets the cached MarkdownRenderer so the next call to
     * {@link #displayMaterial} creates a fresh instance with
     * the current dependency references. Called after
     * re-injecting dependencies following a configuration change.
     */
    public void resetRenderer() {
        this.markdownRenderer = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentReadingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Displays the given material content in the fragment.
     * Renders body content using MarkdownRenderer.
     *
     * @param material  The material to display.
     * @param questions The questions associated with this material.
     */
    public void displayMaterial(@NonNull Material material,
                                @NonNull List<Question> questions) {
        if (binding == null) {
            return;
        }
        binding.textLoading.setVisibility(View.GONE);
        binding.scrollContent.setVisibility(View.VISIBLE);

        if (markdownRenderer == null) {
            markdownRenderer = new MarkdownRenderer(
                    requireContext(),
                    new QuestionBlockRenderer(),
                    attachmentImageLoader,
                    fileStorageManager);
            markdownRenderer.setTextScaleFactor(textScaleFactor);
        }

        Map<String, Question> questionMap;
        if (questions.isEmpty()) {
            questionMap = Collections.emptyMap();
        } else {
            questionMap = new HashMap<>();
            for (Question q : questions) {
                questionMap.put(q.getId(), q);
            }
        }

        markdownRenderer.render(
                binding.layoutContent,
                material.getContent(),
                material.getId(),
                questionMap);
    }

    /**
     * Shows the loading state, hiding content.
     */
    public void showLoading() {
        if (binding == null) {
            return;
        }
        binding.textLoading.setVisibility(View.VISIBLE);
        binding.scrollContent.setVisibility(View.GONE);
    }

    /**
     * Returns the current text content of for text-to-speech purposes.
     *
     * @return The concatenated text content currently displayed, or empty string if none.
     */
    @NonNull
    public String getTextContent() {
        if (binding == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int childCount = binding.layoutContent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = binding.layoutContent.getChildAt(i);
            if (child instanceof TextView) {
                sb.append(". ");
                sb.append(((TextView) child).getText());
            }
        }
        return sb.toString();
    }
}
