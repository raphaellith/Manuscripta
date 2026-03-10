package com.manuscripta.student.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.manuscripta.student.R;
import com.manuscripta.student.domain.model.Feedback;

import java.util.List;

/**
 * Custom compound view for displaying teacher feedback.
 *
 * <p>Shows above the footer, full-width. Contains a header with
 * title and close button, and a scrollable body showing the
 * teacher's marks and/or text comment. Hidden by default.</p>
 */
public class TeacherFeedbackPanelView extends LinearLayout {

    /** Maximum height as a fraction of parent height. */
    private static final float MAX_HEIGHT_FRACTION = 0.5f;

    private TextView marksText;
    private TextView feedbackText;
    private ScrollView scrollView;

    public TeacherFeedbackPanelView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public TeacherFeedbackPanelView(@NonNull Context context,
            @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TeacherFeedbackPanelView(@NonNull Context context,
            @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(@NonNull Context context) {
        setOrientation(VERTICAL);
        setBackgroundResource(R.drawable.bg_dialogue);
        int padding = context.getResources()
                .getDimensionPixelSize(R.dimen.padding_content);
        setPadding(padding, padding, padding, padding);
        setElevation(8f * context.getResources().getDisplayMetrics().density);
        setVisibility(View.GONE);

        LayoutInflater.from(context).inflate(
                R.layout.view_teacher_feedback_panel, this, true);

        marksText = findViewById(R.id.feedbackMarks);
        feedbackText = findViewById(R.id.feedbackText);
        scrollView = findViewById(R.id.feedbackScrollView);
        Button closeButton = findViewById(R.id.feedbackCloseButton);

        closeButton.setOnClickListener(v -> hide());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        View parent = (View) getParent();
        if (parent != null) {
            int maxHeight = (int) (parent.getHeight() * MAX_HEIGHT_FRACTION);
            int heightSpec = MeasureSpec.makeMeasureSpec(
                    maxHeight, MeasureSpec.AT_MOST);
            super.onMeasure(widthMeasureSpec, heightSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    /**
     * Displays the most recent feedback item from the list.
     *
     * @param feedbackList The list of feedback to display.
     */
    public void showFeedback(@NonNull List<Feedback> feedbackList) {
        if (feedbackList.isEmpty()) {
            hide();
            return;
        }
        Feedback latest = feedbackList.get(feedbackList.size() - 1);

        if (latest.hasMarks()) {
            marksText.setText(getContext().getString(
                    R.string.teacher_feedback_marks, latest.getMarks()));
            marksText.setVisibility(View.VISIBLE);
        } else {
            marksText.setVisibility(View.GONE);
        }

        if (latest.hasText()) {
            feedbackText.setText(latest.getText());
            feedbackText.setVisibility(View.VISIBLE);
        } else {
            feedbackText.setVisibility(View.GONE);
        }

        setVisibility(View.VISIBLE);
        scrollView.scrollTo(0, 0);
    }

    /**
     * Hides the panel.
     */
    public void hide() {
        setVisibility(View.GONE);
    }
}
