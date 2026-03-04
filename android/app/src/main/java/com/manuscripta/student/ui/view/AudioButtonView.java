package com.manuscripta.student.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.manuscripta.student.R;

/**
 * Custom compound view wrapping an audio playback button.
 *
 * <p>Matches the prototype AudioButton: 80×80dp, eink_light background,
 * 4dp eink_black border, rounded corners, speaker icon centred.</p>
 */
public class AudioButtonView extends FrameLayout {

    /** The inner image button that displays the speaker icon. */
    private ImageButton audioImageButton;

    /**
     * Constructs an AudioButtonView from code.
     *
     * @param context the context
     */
    public AudioButtonView(@NonNull Context context) {
        super(context);
        init(context);
    }

    /**
     * Constructs an AudioButtonView from XML.
     *
     * @param context the context
     * @param attrs   the attribute set from XML
     */
    public AudioButtonView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * Constructs an AudioButtonView from XML with a default style.
     *
     * @param context      the context
     * @param attrs        the attribute set from XML
     * @param defStyleAttr the default style attribute
     */
    public AudioButtonView(@NonNull Context context, @Nullable AttributeSet attrs,
                           int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * Inflates the layout and initialises internal views.
     *
     * @param context the context
     */
    private void init(@NonNull Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_audio_button, this, true);
        audioImageButton = findViewById(R.id.audioImageButton);
    }

    /**
     * Sets the click listener for the audio button.
     *
     * @param listener the runnable to invoke when the button is clicked
     */
    public void setOnAudioClickListener(@Nullable Runnable listener) {
        if (listener != null) {
            audioImageButton.setOnClickListener(v -> listener.run());
        } else {
            audioImageButton.setOnClickListener(null);
        }
    }

    /**
     * Returns the inner image button for testing or further configuration.
     *
     * @return the inner ImageButton
     */
    @NonNull
    public ImageButton getAudioImageButton() {
        return audioImageButton;
    }
}
