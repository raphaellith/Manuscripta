package com.manuscripta.student.ui.renderer;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

/**
 * Pure JUnit tests for the logic methods of
 * {@link QuestionBlockRenderer}. Does not require Robolectric
 * because these methods perform only string operations.
 */
public class QuestionBlockRendererLogicTest {

    private QuestionBlockRenderer renderer;

    /**
     * Creates a new QuestionBlockRenderer before each test.
     */
    @Before
    public void setUp() {
        renderer = new QuestionBlockRenderer();
    }

    // ==================== parseOptions tests ====================

    @Test
    public void parseOptions_validJsonArray_returnsOptions() {
        String[] options =
                renderer.parseOptions("[\"A\",\"B\",\"C\"]");

        assertEquals(3, options.length);
        assertEquals("A", options[0]);
        assertEquals("B", options[1]);
        assertEquals("C", options[2]);
    }

    @Test
    public void parseOptions_emptyString_returnsEmptyArray() {
        String[] options = renderer.parseOptions("");
        assertEquals(0, options.length);
    }

    @Test
    public void parseOptions_invalidJson_returnsEmptyArray() {
        String[] options =
                renderer.parseOptions("{not an array}");
        assertEquals(0, options.length);
    }

    @Test
    public void parseOptions_emptyArray_returnsEmptyArray() {
        String[] options = renderer.parseOptions("[]");
        assertEquals(0, options.length);
    }

    @Test
    public void parseOptions_singleElement_returnsSingle() {
        String[] options =
                renderer.parseOptions("[\"Only option\"]");
        assertEquals(1, options.length);
        assertEquals("Only option", options[0]);
    }

    @Test
    public void parseOptions_manyElements_returnsAll() {
        String[] options = renderer.parseOptions(
                "[\"A\",\"B\",\"C\",\"D\",\"E\"]");
        assertEquals(5, options.length);
        assertEquals("E", options[4]);
    }

    @Test
    public void parseOptions_specialCharacters_preserved() {
        String[] options = renderer.parseOptions(
                "[\"It's a test\",\"with \\\"quotes\\\"\"]");
        assertEquals(2, options.length);
        assertEquals("It's a test", options[0]);
    }

    // ==================== getOptionLabel tests ====================

    @Test
    public void getOptionLabel_zeroReturnsA() {
        assertEquals("A",
                QuestionBlockRenderer.getOptionLabel(0));
    }

    @Test
    public void getOptionLabel_oneReturnsB() {
        assertEquals("B",
                QuestionBlockRenderer.getOptionLabel(1));
    }

    @Test
    public void getOptionLabel_twentyFiveReturnsZ() {
        assertEquals("Z",
                QuestionBlockRenderer.getOptionLabel(25));
    }

    @Test
    public void getOptionLabel_threeReturnsD() {
        assertEquals("D",
                QuestionBlockRenderer.getOptionLabel(3));
    }

    @Test
    public void getOptionLabel_fourReturnsE() {
        assertEquals("E",
                QuestionBlockRenderer.getOptionLabel(4));
    }

    @Test
    public void getOptionLabel_sevenReturnsH() {
        assertEquals("H",
                QuestionBlockRenderer.getOptionLabel(7));
    }
}
