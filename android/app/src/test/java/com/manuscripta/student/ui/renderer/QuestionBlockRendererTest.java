package com.manuscripta.student.ui.renderer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.manuscripta.student.data.model.QuestionType;
import com.manuscripta.student.domain.model.Question;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link QuestionBlockRenderer}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class QuestionBlockRendererTest {

    private QuestionBlockRenderer renderer;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        renderer = new QuestionBlockRenderer();
    }

    // ==================== renderQuestion tests ====================

    @Test
    public void testRenderQuestion_multipleChoice_createsContainerWithRadioGroup() {
        Question question = new Question(
                "q1", "mat-1",
                "What colour is the sky?",
                QuestionType.MULTIPLE_CHOICE,
                "[\"Red\",\"Blue\",\"Green\"]", "B", null);

        View view = renderer.renderQuestion(context, question);

        assertNotNull(view);
        assertTrue(view instanceof LinearLayout);
        LinearLayout container = (LinearLayout) view;

        // First child: question text
        assertTrue(container.getChildCount() >= 2);
        assertTrue(
                container.getChildAt(0) instanceof TextView);
        assertEquals(
                "What colour is the sky?",
                ((TextView) container.getChildAt(0))
                        .getText().toString());

        // Second child: radio group
        assertTrue(
                container.getChildAt(1) instanceof RadioGroup);
    }

    @Test
    public void testRenderQuestion_writtenAnswer_createsContainerWithEditText() {
        Question question = new Question(
                "q2", "mat-1",
                "Describe the water cycle.",
                QuestionType.WRITTEN_ANSWER, "", "", null);

        View view = renderer.renderQuestion(context, question);

        assertNotNull(view);
        assertTrue(view instanceof LinearLayout);
        LinearLayout container = (LinearLayout) view;

        assertTrue(container.getChildCount() >= 2);
        assertTrue(
                container.getChildAt(0) instanceof TextView);
        assertTrue(
                container.getChildAt(1) instanceof EditText);
    }

    // ==================== renderMultipleChoice tests ====================

    @Test
    public void testRenderMultipleChoice_threeOptions_createsThreeRadioButtons() {
        Question question = new Question(
                "q1", "mat-1", "Pick one:",
                QuestionType.MULTIPLE_CHOICE,
                "[\"Alpha\",\"Beta\",\"Gamma\"]", "A", null);

        View view =
                renderer.renderMultipleChoice(context, question);

        assertTrue(view instanceof RadioGroup);
        RadioGroup group = (RadioGroup) view;
        assertEquals(3, group.getChildCount());
    }

    @Test
    public void testRenderMultipleChoice_optionLabelsCorrect() {
        Question question = new Question(
                "q1", "mat-1", "Pick one:",
                QuestionType.MULTIPLE_CHOICE,
                "[\"First\",\"Second\"]", "A", null);

        RadioGroup group = (RadioGroup)
                renderer.renderMultipleChoice(context, question);

        RadioButton btnA = (RadioButton) group.getChildAt(0);
        RadioButton btnB = (RadioButton) group.getChildAt(1);
        assertEquals("A) First", btnA.getText().toString());
        assertEquals("B) Second", btnB.getText().toString());
    }

    @Test
    public void testRenderMultipleChoice_emptyOptions_emptyRadioGroup() {
        Question question = new Question(
                "q1", "mat-1", "Pick one:",
                QuestionType.MULTIPLE_CHOICE, "", "A", null);

        RadioGroup group = (RadioGroup)
                renderer.renderMultipleChoice(context, question);

        assertEquals(0, group.getChildCount());
    }

    @Test
    public void testRenderMultipleChoice_invalidJson_emptyRadioGroup() {
        Question question = new Question(
                "q1", "mat-1", "Pick one:",
                QuestionType.MULTIPLE_CHOICE,
                "not valid json", "A", null);

        RadioGroup group = (RadioGroup)
                renderer.renderMultipleChoice(context, question);

        assertEquals(0, group.getChildCount());
    }

    // ==================== renderWrittenAnswer tests ====================

    @Test
    public void testRenderWrittenAnswer_createsEditText() {
        View view = renderer.renderWrittenAnswer(context);

        assertNotNull(view);
        assertTrue(view instanceof EditText);
    }

    @Test
    public void testRenderWrittenAnswer_hasHintText() {
        EditText editText =
                (EditText) renderer.renderWrittenAnswer(context);

        assertNotNull(editText.getHint());
        assertEquals(
                "Enter your answer",
                editText.getHint().toString());
    }

    // ==================== parseOptions tests ====================

    @Test
    public void testParseOptions_validJsonArray_returnsOptions() {
        String[] options =
                renderer.parseOptions("[\"A\",\"B\",\"C\"]");

        assertEquals(3, options.length);
        assertEquals("A", options[0]);
        assertEquals("B", options[1]);
        assertEquals("C", options[2]);
    }

    @Test
    public void testParseOptions_emptyString_returnsEmptyArray() {
        String[] options = renderer.parseOptions("");
        assertEquals(0, options.length);
    }

    @Test
    public void testParseOptions_invalidJson_returnsEmptyArray() {
        String[] options =
                renderer.parseOptions("{not an array}");
        assertEquals(0, options.length);
    }

    @Test
    public void testParseOptions_emptyArray_returnsEmptyArray() {
        String[] options = renderer.parseOptions("[]");
        assertEquals(0, options.length);
    }

    @Test
    public void testParseOptions_singleElement_returnsSingleItem() {
        String[] options =
                renderer.parseOptions("[\"Only option\"]");
        assertEquals(1, options.length);
        assertEquals("Only option", options[0]);
    }

    // ==================== getOptionLabel tests ====================

    @Test
    public void testGetOptionLabel_zeroReturnsA() {
        assertEquals("A",
                QuestionBlockRenderer.getOptionLabel(0));
    }

    @Test
    public void testGetOptionLabel_oneReturnsB() {
        assertEquals("B",
                QuestionBlockRenderer.getOptionLabel(1));
    }

    @Test
    public void testGetOptionLabel_twentyFiveReturnsZ() {
        assertEquals("Z",
                QuestionBlockRenderer.getOptionLabel(25));
    }

    @Test
    public void testGetOptionLabel_threeReturnsD() {
        assertEquals("D",
                QuestionBlockRenderer.getOptionLabel(3));
    }
}
