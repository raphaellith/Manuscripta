package com.manuscripta.student.ui.renderer;

import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.manuscripta.student.data.model.QuestionType;
import com.manuscripta.student.domain.model.Question;

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

    /** Gson instance for parsing option JSON arrays. */
    private static final Gson GSON = new Gson();

    /**
     * Creates a new QuestionBlockRenderer.
     */
    public QuestionBlockRenderer() {
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
        questionText.setTextSize(18f);
        container.addView(questionText);

        if (question.getQuestionType()
                == QuestionType.MULTIPLE_CHOICE) {
            container.addView(
                    renderMultipleChoice(context, question));
        } else {
            container.addView(renderWrittenAnswer(context));
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
        try {
            String[] options =
                    GSON.fromJson(optionsJson, String[].class);
            return options != null ? options : new String[0];
        } catch (JsonSyntaxException e) {
            return new String[0];
        }
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
}
