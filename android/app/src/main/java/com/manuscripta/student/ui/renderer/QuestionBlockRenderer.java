package com.manuscripta.student.ui.renderer;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.manuscripta.student.R;
import com.manuscripta.student.data.model.QuestionType;
import com.manuscripta.student.domain.model.Question;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders embedded question blocks within material content.
 *
 * <p>Handles rendering of MULTIPLE_CHOICE questions as radio button
 * groups and WRITTEN_ANSWER questions as text input fields, as
 * specified in Material Encoding §4(4).</p>
 */
public class QuestionBlockRenderer {

    /** Padding in dp for the question container. */
    private static final int CONTAINER_PADDING_DP = 16;

    /** Padding in dp for individual option items. */
    private static final int OPTION_PADDING_DP = 8;

    /** Minimum height in dp for written answer input. */
    private static final int INPUT_MIN_HEIGHT_DP = 48;

    /** Margin in dp between stacked elements in a question block. */
    private static final int BLOCK_MARGIN_DP = 12;

    /** Prefix used to tag written answer fields with question IDs. */
    private static final String TAG_WRITTEN_ANSWER_PREFIX = "qa_written:";

    /** Prefix used to tag multiple-choice groups with question IDs. */
    private static final String TAG_MULTIPLE_CHOICE_PREFIX = "qa_mc:";

    /** Gson instance for parsing option JSON arrays. */
    private static final Gson GSON = new Gson();

    /** Base body text size in SP used by markdown body text. */
    private static final float BODY_TEXT_SP = 30f;

    /** Smaller text size in SP used for supporting lines such as marks. */
    private static final float SUPPORT_TEXT_SP = 20f;

    /** Text scaling factor shared with markdown rendering. */
    private float textScaleFactor = 1.0f;

    /**
     * Creates a new QuestionBlockRenderer.
     */
    public QuestionBlockRenderer() {
    }

    /**
     * Sets the text scale factor for question blocks so they match body content.
     *
     * @param scaleFactor text scaling factor (1.0 = default)
     */
    public void setTextScaleFactor(float scaleFactor) {
        this.textScaleFactor = scaleFactor;
    }

    /**
     * Renders a question as an Android View.
     *
     * <p>For MULTIPLE_CHOICE questions, creates a radio button group.
     * For WRITTEN_ANSWER questions, creates a text input field.</p>
     *
     * @param context  Android context for View creation
     * @param question the question to render
     * @return a View representing the question
     */
    @NonNull
    public View renderQuestion(
            @NonNull Context context,
            @NonNull Question question) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = MarkdownRenderer.dpToPx(
                context, CONTAINER_PADDING_DP);
        container.setPadding(padding, padding, padding, padding);

        TextView questionText = new TextView(context);
        questionText.setText(question.getQuestionText());
        questionText.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                BODY_TEXT_SP * textScaleFactor);
        applyBodyFont(context, questionText);
        container.addView(questionText);

        if (question.getMaxScore() != null) {
            TextView marksText = new TextView(context);
            marksText.setText(context.getString(
                R.string.worksheet_marks_available,
                question.getMaxScore()));
            marksText.setTextSize(
                    TypedValue.COMPLEX_UNIT_SP,
                    SUPPORT_TEXT_SP * textScaleFactor);
            applyBodyFont(context, marksText);
            marksText.setGravity(android.view.Gravity.END);
            LinearLayout.LayoutParams marksParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            marksParams.topMargin = MarkdownRenderer.dpToPx(context, BLOCK_MARGIN_DP / 2);
            marksText.setLayoutParams(marksParams);
            container.addView(marksText);
        }

        if (question.getQuestionType()
                == QuestionType.MULTIPLE_CHOICE) {
            View multipleChoice = renderMultipleChoice(context, question);
            LinearLayout.LayoutParams mcParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            mcParams.topMargin = MarkdownRenderer.dpToPx(context, BLOCK_MARGIN_DP);
            multipleChoice.setLayoutParams(mcParams);
            multipleChoice.setTag(TAG_MULTIPLE_CHOICE_PREFIX + question.getId());
            container.addView(multipleChoice);
        } else {
            View writtenAnswer = renderWrittenAnswer(context);
            LinearLayout.LayoutParams answerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            answerParams.topMargin = MarkdownRenderer.dpToPx(context, BLOCK_MARGIN_DP);
            writtenAnswer.setLayoutParams(answerParams);
            writtenAnswer.setTag(TAG_WRITTEN_ANSWER_PREFIX + question.getId());
            container.addView(writtenAnswer);
        }

        return container;
    }

    /**
     * Renders multiple choice options as a RadioGroup.
     *
     * @param context  Android context
     * @param question the multiple choice question
     * @return a RadioGroup with option buttons
     */
    @NonNull
    public View renderMultipleChoice(
            @NonNull Context context,
            @NonNull Question question) {
        RadioGroup radioGroup = new RadioGroup(context);
        radioGroup.setOrientation(RadioGroup.VERTICAL);

        String[] options = parseOptions(question.getOptions());
        for (int i = 0; i < options.length; i++) {
            RadioButton radioButton = new RadioButton(context);
            String label =
                    getOptionLabel(i) + ") " + options[i];
            radioButton.setText(label);
            radioButton.setTextSize(
                    TypedValue.COMPLEX_UNIT_SP,
                    BODY_TEXT_SP * textScaleFactor);
            applyBodyFont(context, radioButton);
            radioButton.setId(View.generateViewId());
            int optPadding = MarkdownRenderer.dpToPx(
                    context, OPTION_PADDING_DP);
            radioButton.setPadding(
                    optPadding, optPadding,
                    optPadding, optPadding);
            radioGroup.addView(radioButton);
        }

        return radioGroup;
    }

    /**
     * Renders a written answer input field.
     *
     * @param context Android context
     * @return an EditText for free-form answers
     */
    @NonNull
    View renderWrittenAnswer(@NonNull Context context) {
        EditText editText = new EditText(context);
        editText.setHint("Enter your answer");
        editText.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                BODY_TEXT_SP * textScaleFactor);
        applyBodyFont(context, editText);
        editText.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setMinHeight(MarkdownRenderer.dpToPx(
                context, INPUT_MIN_HEIGHT_DP));
        editText.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return editText;
    }

    /**
     * Parses the options JSON string into an array.
     * Returns an empty array if parsing fails.
     *
     * @param optionsJson the JSON array string of options
     * @return an array of option strings
     */
    @NonNull
    String[] parseOptions(@NonNull String optionsJson) {
        if (optionsJson.isEmpty()) {
            return new String[0];
        }
        String trimmed = optionsJson.trim();

        if (!trimmed.startsWith("[")) {
            return hasDelimitedOptions(trimmed)
                    ? parseDelimitedOptions(trimmed)
                    : new String[0];
        }

        try {
            String[] options = GSON.fromJson(trimmed, String[].class);
            return options != null ? options : new String[0];
        } catch (JsonSyntaxException e) {
            return hasDelimitedOptions(trimmed)
                    ? parseDelimitedOptions(trimmed)
                    : new String[0];
        }
    }

    /**
     * Checks whether raw option text appears to be a delimited list.
     *
     * @param optionsRaw raw options text
     * @return true when a newline, pipe, or semicolon delimiter is present
     */
    private boolean hasDelimitedOptions(@NonNull String optionsRaw) {
        return optionsRaw.contains("\n")
                || optionsRaw.contains("|")
                || optionsRaw.contains(";");
    }

    /**
     * Parses non-JSON options from newline, pipe, or semicolon-delimited text.
     *
     * @param optionsRaw Raw options string
     * @return parsed options, or an empty array if no options are found
     */
    @NonNull
    private String[] parseDelimitedOptions(@NonNull String optionsRaw) {
        String[] parts = optionsRaw.split("\\n|\\||;");
        List<String> options = new ArrayList<>();
        for (String part : parts) {
            String trimmedPart = part.trim();
            if (!trimmedPart.isEmpty()) {
                options.add(trimmedPart);
            }
        }
        return options.toArray(new String[0]);
    }

    /**
     * Extracts the question ID from a view tag, if the tag matches a known field prefix.
     *
     * @param tag The view tag to inspect
     * @return The extracted question ID, or null if unavailable
     */
    @Nullable
    public String extractQuestionIdFromTag(@Nullable Object tag) {
        if (!(tag instanceof String)) {
            return null;
        }
        String tagString = (String) tag;
        if (tagString.startsWith(TAG_WRITTEN_ANSWER_PREFIX)) {
            return tagString.substring(TAG_WRITTEN_ANSWER_PREFIX.length());
        }
        if (tagString.startsWith(TAG_MULTIPLE_CHOICE_PREFIX)) {
            return tagString.substring(TAG_MULTIPLE_CHOICE_PREFIX.length());
        }
        return null;
    }

    /**
     * Checks whether a view tag identifies a written answer input field.
     *
     * @param tag The view tag to inspect
     * @return true if this is a written answer field tag
     */
    public boolean isWrittenAnswerTag(@Nullable Object tag) {
        return tag instanceof String
                && ((String) tag).startsWith(TAG_WRITTEN_ANSWER_PREFIX);
    }

    /**
     * Checks whether a view tag identifies a multiple-choice radio group.
     *
     * @param tag The view tag to inspect
     * @return true if this is a multiple-choice field tag
     */
    public boolean isMultipleChoiceTag(@Nullable Object tag) {
        return tag instanceof String
                && ((String) tag).startsWith(TAG_MULTIPLE_CHOICE_PREFIX);
    }

    /**
     * Returns the letter label for an option index (A, B, C, …).
     *
     * @param index zero-based option index
     * @return the letter label
     */
    @NonNull
    static String getOptionLabel(int index) {
        return String.valueOf((char) ('A' + index));
    }

    /**
     * Applies the standard worksheet body font where available.
     *
     * @param context Android context
     * @param textView Target text view
     */
    private void applyBodyFont(
            @NonNull Context context,
            @NonNull TextView textView) {
        try {
            android.graphics.Typeface bodyFont = ResourcesCompat.getFont(
                    context, R.font.ibm_plex_sans);
            if (bodyFont != null) {
                textView.setTypeface(bodyFont);
            }
        } catch (Exception ignored) {
            // Keep default font when resource cannot be loaded.
        }
    }
}
