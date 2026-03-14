package com.manuscripta.student.ui.view;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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

/**
 * Custom compound view for the AI response overlay panel.
 *
 * <p>Positions above the footer, full-width. Contains a header with
 * task name and close button (with 4dp bottom border), and a scrollable
 * body that shows either a "Thinking…" loading state (with pulsing
 * animation) or the response text. Max height is 70% of parent.</p>
 */
public class AiResponsePanelView extends LinearLayout {

    /** Maximum height as a fraction of parent height. */
    private static final float MAX_HEIGHT_FRACTION = 0.7f;

    /** Pulse animation duration in milliseconds. */
    private static final long PULSE_DURATION_MS = 1000L;

    /** Minimum alpha during pulse animation. */
    private static final float PULSE_ALPHA_MIN = 0.3f;

    /** Maximum alpha during pulse animation. */
    private static final float PULSE_ALPHA_MAX = 1.0f;

    /** The task name heading text view. */
    private TextView taskNameText;

    /** The close button. */
    private Button closeButton;

    /** The loading text view ("Thinking…"). */
    private TextView loadingText;

    /** The response content text view. */
    private TextView responseText;

    /** The scrollable body container. */
    private ScrollView bodyScrollView;

    /** The pulsing animation for the loading state. */
    @Nullable
    private ObjectAnimator pulseAnimator;

    /** Callback for the close button. */
    @Nullable
    private Runnable onCloseListener;

    /**
     * Constructs an AiResponsePanelView from code.
     *
     * @param context the context
     */
    public AiResponsePanelView(@NonNull Context context) {
        super(context);
        init(context);
    }

    /**
     * Constructs an AiResponsePanelView from XML.
     *
     * @param context the context
     * @param attrs   the attribute set from XML
     */
    public AiResponsePanelView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * Constructs an AiResponsePanelView from XML with a default style.
     *
     * @param context      the context
     * @param attrs        the attribute set from XML
     * @param defStyleAttr the default style attribute
     */
    public AiResponsePanelView(@NonNull Context context, @Nullable AttributeSet attrs,
                               int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * Inflates the layout and configures internal views.
     *
     * @param context the context
     */
    private void init(@NonNull Context context) {
        setOrientation(VERTICAL);
        setBackgroundResource(R.drawable.bg_dialogue);
        int padding = context.getResources()
            .getDimensionPixelSize(R.dimen.padding_content);
        setPadding(padding, padding, padding, padding);
        setElevation(8f * context.getResources().getDisplayMetrics().density);
        setVisibility(View.GONE);

        LayoutInflater.from(context).inflate(
            R.layout.view_ai_response_panel, this, true
        );

        taskNameText = findViewById(R.id.taskNameText);
        closeButton = findViewById(R.id.closeButton);
        loadingText = findViewById(R.id.loadingText);
        responseText = findViewById(R.id.responseText);
        bodyScrollView = findViewById(R.id.bodyScrollView);

        closeButton.setOnClickListener(v -> hide());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        View parent = (View) getParent();
        if (parent != null) {
            int maxHeight = (int) (parent.getHeight() * MAX_HEIGHT_FRACTION);
            int heightSpec = MeasureSpec.makeMeasureSpec(
                maxHeight, MeasureSpec.AT_MOST
            );
            super.onMeasure(widthMeasureSpec, heightSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    /**
     * Shows the loading state with a pulsing "Thinking…" message.
     *
     * @param taskName the name of the AI task (e.g. "Simplify")
     */
    public void showLoading(@NonNull String taskName) {
        taskNameText.setText(taskName);
        loadingText.setVisibility(View.VISIBLE);
        responseText.setVisibility(View.GONE);
        setVisibility(View.VISIBLE);
        startPulseAnimation();
    }

    /**
     * Shows the AI response content.
     *
     * @param taskName the name of the AI task (e.g. "Simplify")
     * @param content  the response text to display
     */
    public void showContent(@NonNull String taskName, @NonNull String content) {
        stopPulseAnimation();
        taskNameText.setText(taskName);
        responseText.setText(content);
        responseText.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.GONE);
        setVisibility(View.VISIBLE);
        bodyScrollView.scrollTo(0, 0);
    }

    /**
     * Hides the panel and stops any running animations.
     */
    public void hide() {
        stopPulseAnimation();
        setVisibility(View.GONE);
        if (onCloseListener != null) {
            onCloseListener.run();
        }
    }

    /**
     * Returns whether the panel is currently visible.
     *
     * @return true if the panel is showing
     */
    public boolean isShowing() {
        return getVisibility() == View.VISIBLE;
    }

    /**
     * Sets the callback for the close button.
     *
     * @param listener the runnable to invoke when the panel is closed
     */
    public void setOnCloseListener(@Nullable Runnable listener) {
        this.onCloseListener = listener;
    }

    /**
     * Starts the pulsing alpha animation on the loading text.
     */
    private void startPulseAnimation() {
        stopPulseAnimation();
        pulseAnimator = ObjectAnimator.ofFloat(
            loadingText, View.ALPHA, PULSE_ALPHA_MAX, PULSE_ALPHA_MIN
        );
        pulseAnimator.setDuration(PULSE_DURATION_MS);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.start();
    }

    /**
     * Stops the pulsing animation if running.
     */
    private void stopPulseAnimation() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
            loadingText.setAlpha(PULSE_ALPHA_MAX);
        }
    }

    /**
     * Returns the task name text view for testing.
     *
     * @return the task name TextView
     */
    @NonNull
    public TextView getTaskNameText() {
        return taskNameText;
    }

    /**
     * Returns the loading text view for testing.
     *
     * @return the loading TextView
     */
    @NonNull
    public TextView getLoadingText() {
        return loadingText;
    }

    /**
     * Returns the response text view for testing.
     *
     * @return the response content TextView
     */
    @NonNull
    public TextView getResponseText() {
        return responseText;
    }

    /**
     * Returns the close button for testing.
     *
     * @return the close Button
     */
    @NonNull
    public Button getCloseButton() {
        return closeButton;
    }
}
