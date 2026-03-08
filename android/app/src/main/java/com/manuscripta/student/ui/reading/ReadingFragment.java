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

/**
 * Fragment displaying reading material content.
 * Matches the LessonView from the React prototype.
 */
public class ReadingFragment extends Fragment {

    /** View binding for the reading fragment layout. */
    private FragmentReadingBinding binding;

    /**
     * Creates a new instance of ReadingFragment.
     *
     * @return A new ReadingFragment instance.
     */
    @NonNull
    public static ReadingFragment newInstance() {
        return new ReadingFragment();
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
     * Shows the title and renders body content as paragraph TextViews.
     *
     * @param material The material to display.
     */
    public void displayMaterial(@NonNull Material material) {
        if (binding == null) {
            return;
        }
        binding.textLoading.setVisibility(View.GONE);
        binding.scrollContent.setVisibility(View.VISIBLE);
        binding.textTitle.setVisibility(View.VISIBLE);
        binding.textTitle.setText(material.getTitle());

        binding.layoutContent.removeAllViews();
        String content = material.getContent();
        if (!content.isEmpty()) {
            String[] paragraphs = content.split("\n\n");
            for (String paragraph : paragraphs) {
                String trimmed = paragraph.trim();
                if (!trimmed.isEmpty()) {
                    addParagraph(trimmed);
                }
            }
        }
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
        binding.textTitle.setVisibility(View.GONE);
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
        sb.append(binding.textTitle.getText());
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

    /**
     * Adds a paragraph TextView to the content layout.
     *
     * @param text The paragraph text to add.
     */
    private void addParagraph(@NonNull String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextAppearance(
                com.manuscripta.student.R.style.TextAppearance_Manuscripta_Body);
        tv.setTextColor(
                requireContext().getResources().getColor(
                        com.manuscripta.student.R.color.eink_black, null));
        tv.setLineSpacing(0, 1.4f);

        int spacing = getResources().getDimensionPixelSize(
                com.manuscripta.student.R.dimen.spacing_item);
        tv.setPadding(0, 0, 0, spacing);

        binding.layoutContent.addView(tv);
    }
}
