package com.manuscripta.student.ui.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import com.manuscripta.student.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link MascotAssistantView}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class MascotAssistantViewTest {

    /** The mascot view under test. */
    private MascotAssistantView mascotView;

    /**
     * Creates a fresh MascotAssistantView before each test.
     */
    @Before
    public void setUp() {
        mascotView = new MascotAssistantView(
            ApplicationProvider.getApplicationContext()
        );
    }

    /**
     * Verifies the view inflates all internal child views.
     */
    @Test
    public void init_inflatesAllChildViews() {
        assertNotNull("Mascot button should exist",
            mascotView.getMascotButton());
        assertNotNull("Dialogue popup should exist",
            mascotView.getDialoguePopup());
        assertNotNull("Simplify button should exist",
            mascotView.getSimplifyButton());
        assertNotNull("Summarise button should exist",
            mascotView.getSummariseButton());
    }

    /**
     * Verifies the dialogue is initially hidden.
     */
    @Test
    public void init_dialogueIsHiddenByDefault() {
        assertFalse("Dialogue should be closed initially",
            mascotView.isDialogueOpen());
        assertEquals(View.GONE,
            mascotView.getDialoguePopup().getVisibility());
    }

    /**
     * Verifies clicking the mascot opens the dialogue popup.
     */
    @Test
    public void clickMascot_opensDialogue() {
        mascotView.getMascotButton().performClick();

        assertTrue("Dialogue should be open after click",
            mascotView.isDialogueOpen());
        assertEquals(View.VISIBLE,
            mascotView.getDialoguePopup().getVisibility());
    }

    /**
     * Verifies clicking the mascot a second time closes the dialogue.
     */
    @Test
    public void clickMascotTwice_closesDialogue() {
        mascotView.getMascotButton().performClick();
        mascotView.getMascotButton().performClick();

        assertFalse("Dialogue should be closed after second click",
            mascotView.isDialogueOpen());
        assertEquals(View.GONE,
            mascotView.getDialoguePopup().getVisibility());
    }

    /**
     * Verifies the Simplify button invokes the task listener.
     */
    @Test
    public void simplifyButton_invokesTaskListener() {
        String[] result = {null};
        mascotView.setOnTaskSelected(task -> result[0] = task);

        mascotView.getMascotButton().performClick();
        mascotView.getSimplifyButton().performClick();

        String expected = mascotView.getContext()
            .getString(R.string.ai_simplify);
        assertEquals("Should receive Simplify task", expected, result[0]);
    }

    /**
     * Verifies the Summarise button invokes the task listener.
     */
    @Test
    public void summariseButton_invokesTaskListener() {
        String[] result = {null};
        mascotView.setOnTaskSelected(task -> result[0] = task);

        mascotView.getMascotButton().performClick();
        mascotView.getSummariseButton().performClick();

        String expected = mascotView.getContext()
            .getString(R.string.ai_summarise);
        assertEquals("Should receive Summarise task", expected, result[0]);
    }

    /**
     * Verifies clicking a task button hides the dialogue popup.
     */
    @Test
    public void taskButtonClick_hidesDialogue() {
        mascotView.setOnTaskSelected(task -> { });
        mascotView.getMascotButton().performClick();

        assertTrue("Dialogue should be open", mascotView.isDialogueOpen());

        mascotView.getSimplifyButton().performClick();

        assertFalse("Dialogue should be hidden after task click",
            mascotView.isDialogueOpen());
        assertEquals(View.GONE,
            mascotView.getDialoguePopup().getVisibility());
    }

    /**
     * Verifies hideDialogue programmatically closes the popup.
     */
    @Test
    public void hideDialogue_closesOpenPopup() {
        mascotView.getMascotButton().performClick();
        assertTrue(mascotView.isDialogueOpen());

        mascotView.hideDialogue();

        assertFalse("Dialogue should be hidden after hideDialogue",
            mascotView.isDialogueOpen());
        assertEquals(View.GONE,
            mascotView.getDialoguePopup().getVisibility());
    }

    /**
     * Verifies the mascot button has the correct content description.
     */
    @Test
    public void mascotButton_hasContentDescription() {
        String expected = mascotView.getContext()
            .getString(R.string.cd_mascot);
        assertEquals(expected,
            mascotView.getMascotButton()
                .getContentDescription().toString());
    }
}
