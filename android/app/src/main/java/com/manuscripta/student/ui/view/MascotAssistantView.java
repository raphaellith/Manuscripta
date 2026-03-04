package com.manuscripta.student.ui.view;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.manuscripta.student.R;

import java.util.function.Consumer;

/**
 * Custom compound view for the AI mascot assistant character.
 *
 * <p>Matches the prototype CharacterHelper: shows mascot image (96×96dp) with
 * click listener. On click, toggles a dialogue popup with "How can I help?"
 * and Simplify/Summarise action buttons. Includes hover/press animation
 * (scale 1.1x, translate-Y -4dp).</p>
 */
public class MascotAssistantView extends FrameLayout {

    /** Scale factor applied on press/hover. */
    private static final float PRESS_SCALE = 1.1f;

    /** Vertical translation on press/hover in pixels (converted from 4dp). */
    private static final float PRESS_TRANSLATE_DP = -4f;

    /** Animation duration in milliseconds. */
    private static final long ANIMATION_DURATION_MS = 200L;

    /** The mascot character image button. */
    private ImageButton mascotButton;

    /** The dialogue popup container. */
    private LinearLayout dialoguePopup;

    /** The simplify action button. */
    private Button simplifyButton;

    /** The summarise action button. */
    private Button summariseButton;

    /** Whether the dialogue popup is currently showing. */
    private boolean isDialogueOpen;

    /** Callback for when a task is selected (Simplify or Summarise). */
    @Nullable
    private Consumer<String> onTaskSelectedListener;

    /**
     * Constructs a MascotAssistantView from code.
     *
     * @param context the context
     */
    public MascotAssistantView(@NonNull Context context) {
        super(context);
        init(context);
    }

    /**
     * Constructs a MascotAssistantView from XML.
     *
     * @param context the context
     * @param attrs   the attribute set from XML
     */
    public MascotAssistantView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * Constructs a MascotAssistantView from XML with a default style.
     *
     * @param context      the context
     * @param attrs        the attribute set from XML
     * @param defStyleAttr the default style attribute
     */
    public MascotAssistantView(@NonNull Context context, @Nullable AttributeSet attrs,
                               int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * Inflates the layout and sets up click listeners and animations.
     *
     * @param context the context
     */
    @SuppressWarnings("ClickableViewAccessibility")
    private void init(@NonNull Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_mascot_assistant, this, true);

        mascotButton = findViewById(R.id.mascotButton);
        dialoguePopup = findViewById(R.id.dialoguePopup);
        simplifyButton = findViewById(R.id.simplifyButton);
        summariseButton = findViewById(R.id.summariseButton);

        isDialogueOpen = false;

        mascotButton.setOnClickListener(v -> toggleDialogue());

        mascotButton.setOnTouchListener((v, event) -> {
            handleTouchAnimation(event);
            return false;
        });

        simplifyButton.setOnClickListener(v -> onTaskClicked(
            context.getString(R.string.ai_simplify)
        ));

        summariseButton.setOnClickListener(v -> onTaskClicked(
            context.getString(R.string.ai_summarise)
        ));
    }

    /**
     * Handles touch events on the mascot for press animation.
     *
     * @param event the motion event
     */
    private void handleTouchAnimation(@NonNull MotionEvent event) {
        float translatePx = PRESS_TRANSLATE_DP * getResources().getDisplayMetrics().density;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                animateMascot(PRESS_SCALE, translatePx);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                animateMascot(1f, 0f);
                break;
            default:
                break;
        }
    }

    /**
     * Animates the mascot button to the given scale and translation.
     *
     * @param scale      target scale
     * @param translateY target vertical translation in pixels
     */
    private void animateMascot(float scale, float translateY) {
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
            ObjectAnimator.ofFloat(mascotButton, View.SCALE_X, scale),
            ObjectAnimator.ofFloat(mascotButton, View.SCALE_Y, scale),
            ObjectAnimator.ofFloat(mascotButton, View.TRANSLATION_Y, translateY)
        );
        animatorSet.setDuration(ANIMATION_DURATION_MS);
        animatorSet.start();
    }

    /**
     * Toggles the dialogue popup visibility.
     */
    private void toggleDialogue() {
        isDialogueOpen = !isDialogueOpen;
        dialoguePopup.setVisibility(isDialogueOpen ? View.VISIBLE : View.GONE);
    }

    /**
     * Handles a task button click (Simplify or Summarise).
     *
     * @param task the task name
     */
    private void onTaskClicked(@NonNull String task) {
        hideDialogue();
        if (onTaskSelectedListener != null) {
            onTaskSelectedListener.accept(task);
        }
    }

    /**
     * Hides the dialogue popup.
     */
    public void hideDialogue() {
        isDialogueOpen = false;
        dialoguePopup.setVisibility(View.GONE);
    }

    /**
     * Returns whether the dialogue popup is currently open.
     *
     * @return true if the dialogue is visible
     */
    public boolean isDialogueOpen() {
        return isDialogueOpen;
    }

    /**
     * Sets the callback invoked when a task (Simplify/Summarise) is selected.
     *
     * @param listener the consumer receiving the task name string
     */
    public void setOnTaskSelected(@Nullable Consumer<String> listener) {
        this.onTaskSelectedListener = listener;
    }

    /**
     * Returns the mascot image button for testing.
     *
     * @return the mascot ImageButton
     */
    @NonNull
    public ImageButton getMascotButton() {
        return mascotButton;
    }

    /**
     * Returns the dialogue popup container for testing.
     *
     * @return the dialogue LinearLayout
     */
    @NonNull
    public LinearLayout getDialoguePopup() {
        return dialoguePopup;
    }

    /**
     * Returns the simplify button for testing.
     *
     * @return the simplify Button
     */
    @NonNull
    public Button getSimplifyButton() {
        return simplifyButton;
    }

    /**
     * Returns the summarise button for testing.
     *
     * @return the summarise Button
     */
    @NonNull
    public Button getSummariseButton() {
        return summariseButton;
    }
}
