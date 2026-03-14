package com.manuscripta.student.ui.view;

import static org.junit.Assert.assertNotNull;

import android.widget.ImageButton;

import androidx.test.core.app.ApplicationProvider;

import com.manuscripta.student.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link AudioButtonView}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class AudioButtonViewTest {

    /** The audio button view under test. */
    private AudioButtonView audioButtonView;

    /**
     * Creates a fresh AudioButtonView before each test.
     */
    @Before
    public void setUp() {
        audioButtonView = new AudioButtonView(
            ApplicationProvider.getApplicationContext()
        );
    }

    /**
     * Verifies the view inflates and contains the internal ImageButton.
     */
    @Test
    public void init_inflatesLayoutWithImageButton() {
        ImageButton button = audioButtonView.getAudioImageButton();
        assertNotNull("AudioImageButton should be inflated", button);
    }

    /**
     * Verifies the inner button has the correct content description.
     */
    @Test
    public void init_setsContentDescription() {
        ImageButton button = audioButtonView.getAudioImageButton();
        String expected = audioButtonView.getContext()
            .getString(R.string.cd_audio_button);
        assertNotNull(
            "Content description should be set",
            button.getContentDescription()
        );
        org.junit.Assert.assertEquals(
            expected, button.getContentDescription().toString()
        );
    }

    /**
     * Verifies the audio click listener is invoked on button click.
     */
    @Test
    public void setOnAudioClickListener_clickInvokesListener() {
        boolean[] clicked = {false};
        audioButtonView.setOnAudioClickListener(() -> clicked[0] = true);

        audioButtonView.getAudioImageButton().performClick();

        org.junit.Assert.assertTrue(
            "Listener should have been invoked", clicked[0]
        );
    }

    /**
     * Verifies that setting a null listener clears the click handler.
     */
    @Test
    public void setOnAudioClickListener_nullClearsListener() {
        boolean[] clicked = {false};
        audioButtonView.setOnAudioClickListener(() -> clicked[0] = true);
        audioButtonView.setOnAudioClickListener(null);

        audioButtonView.getAudioImageButton().performClick();

        org.junit.Assert.assertFalse(
            "Listener should not be invoked after clearing", clicked[0]
        );
    }

    /**
     * Verifies the button returns the correct ImageButton reference.
     */
    @Test
    public void getAudioImageButton_returnsNonNullButton() {
        ImageButton button = audioButtonView.getAudioImageButton();
        assertNotNull(button);
    }
}
