package com.manuscripta.student.ui.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link AiResponsePanelView}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class AiResponsePanelViewTest {

    /** The AI response panel view under test. */
    private AiResponsePanelView panelView;

    /**
     * Creates a fresh AiResponsePanelView before each test.
     */
    @Before
    public void setUp() {
        panelView = new AiResponsePanelView(
            ApplicationProvider.getApplicationContext()
        );
    }

    /**
     * Verifies the panel inflates all internal child views.
     */
    @Test
    public void init_inflatesAllChildViews() {
        assertNotNull("Task name text should exist",
            panelView.getTaskNameText());
        assertNotNull("Close button should exist",
            panelView.getCloseButton());
        assertNotNull("Loading text should exist",
            panelView.getLoadingText());
        assertNotNull("Response text should exist",
            panelView.getResponseText());
    }

    /**
     * Verifies the panel is hidden by default.
     */
    @Test
    public void init_panelIsHiddenByDefault() {
        assertFalse("Panel should not be showing initially",
            panelView.isShowing());
        assertEquals(View.GONE, panelView.getVisibility());
    }

    /**
     * Verifies showLoading makes the panel visible with loading state.
     */
    @Test
    public void showLoading_makesPanelVisibleWithLoadingState() {
        panelView.showLoading("Simplify");

        assertTrue("Panel should be showing", panelView.isShowing());
        assertEquals(View.VISIBLE, panelView.getVisibility());
        assertEquals("Simplify",
            panelView.getTaskNameText().getText().toString());
        assertEquals(View.VISIBLE,
            panelView.getLoadingText().getVisibility());
        assertEquals(View.GONE,
            panelView.getResponseText().getVisibility());
    }

    /**
     * Verifies showContent displays the response text.
     */
    @Test
    public void showContent_displaysResponseText() {
        panelView.showContent("Summarise", "This is a summary.");

        assertTrue("Panel should be showing", panelView.isShowing());
        assertEquals("Summarise",
            panelView.getTaskNameText().getText().toString());
        assertEquals("This is a summary.",
            panelView.getResponseText().getText().toString());
        assertEquals(View.VISIBLE,
            panelView.getResponseText().getVisibility());
        assertEquals(View.GONE,
            panelView.getLoadingText().getVisibility());
    }

    /**
     * Verifies transitioning from loading to content state.
     */
    @Test
    public void showLoading_thenShowContent_transitionsCorrectly() {
        panelView.showLoading("Simplify");
        assertEquals(View.VISIBLE,
            panelView.getLoadingText().getVisibility());

        panelView.showContent("Simplify", "Simplified content.");

        assertEquals(View.GONE,
            panelView.getLoadingText().getVisibility());
        assertEquals(View.VISIBLE,
            panelView.getResponseText().getVisibility());
        assertEquals("Simplified content.",
            panelView.getResponseText().getText().toString());
    }

    /**
     * Verifies hide makes the panel invisible.
     */
    @Test
    public void hide_makesPanelInvisible() {
        panelView.showContent("Simplify", "Content");
        assertTrue(panelView.isShowing());

        panelView.hide();

        assertFalse("Panel should not be showing after hide",
            panelView.isShowing());
        assertEquals(View.GONE, panelView.getVisibility());
    }

    /**
     * Verifies the close button triggers hide.
     */
    @Test
    public void closeButton_hidesPanelOnClick() {
        panelView.showContent("Summarise", "Content");
        assertTrue(panelView.isShowing());

        panelView.getCloseButton().performClick();

        assertFalse("Panel should be hidden after close click",
            panelView.isShowing());
    }

    /**
     * Verifies the close listener is invoked when the panel is hidden.
     */
    @Test
    public void hide_invokesOnCloseListener() {
        boolean[] closed = {false};
        panelView.setOnCloseListener(() -> closed[0] = true);
        panelView.showContent("Task", "Content");

        panelView.hide();

        assertTrue("Close listener should have been invoked", closed[0]);
    }

    /**
     * Verifies the close listener is invoked via the close button.
     */
    @Test
    public void closeButton_invokesOnCloseListener() {
        boolean[] closed = {false};
        panelView.setOnCloseListener(() -> closed[0] = true);
        panelView.showContent("Task", "Content");

        panelView.getCloseButton().performClick();

        assertTrue("Close listener should have been invoked via button",
            closed[0]);
    }

    /**
     * Verifies showContent updates the task name if changed.
     */
    @Test
    public void showContent_updatesTaskName() {
        panelView.showContent("Simplify", "First content");
        assertEquals("Simplify",
            panelView.getTaskNameText().getText().toString());

        panelView.showContent("Summarise", "Second content");
        assertEquals("Summarise",
            panelView.getTaskNameText().getText().toString());
    }

    /**
     * Verifies that isShowing returns false when the panel is GONE.
     */
    @Test
    public void isShowing_returnsFalseWhenGone() {
        panelView.setVisibility(View.GONE);
        assertFalse(panelView.isShowing());
    }

    /**
     * Verifies that isShowing returns true when the panel is VISIBLE.
     */
    @Test
    public void isShowing_returnsTrueWhenVisible() {
        panelView.showLoading("Test");
        assertTrue(panelView.isShowing());
    }
}
