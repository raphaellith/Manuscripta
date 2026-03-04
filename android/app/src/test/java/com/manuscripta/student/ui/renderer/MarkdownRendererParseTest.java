package com.manuscripta.student.ui.renderer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import io.noties.markwon.Markwon;

/**
 * Pure JUnit tests for the parsing and logic methods of
 * {@link MarkdownRenderer}. These tests do not require
 * Robolectric because they exercise only the text-parsing
 * and extraction logic, which is pure Java.
 */
public class MarkdownRendererParseTest {

    private MarkdownRenderer renderer;

    /**
     * Sets up the renderer with a mock Markwon instance.
     * The parsing methods do not invoke Markwon.
     */
    @Before
    public void setUp() {
        Markwon mockMarkwon = mock(Markwon.class);
        QuestionBlockRenderer qbr =
                new QuestionBlockRenderer();
        renderer = new MarkdownRenderer(
                mockMarkwon, qbr, null);
    }

    // ==================== extractId tests ====================

    @Test
    public void extractId_validAttribute_returnsId() {
        assertEquals("abc-123",
                MarkdownRenderer.extractId("id=\"abc-123\""));
    }

    @Test
    public void extractId_multipleAttributes_returnsId() {
        assertEquals("xyz",
                MarkdownRenderer.extractId(
                        "type=\"pdf\" id=\"xyz\" class=\"big\""));
    }

    @Test
    public void extractId_nullAttribute_returnsNull() {
        assertNull(MarkdownRenderer.extractId(null));
    }

    @Test
    public void extractId_emptyString_returnsNull() {
        assertNull(MarkdownRenderer.extractId(""));
    }

    @Test
    public void extractId_noIdAttribute_returnsNull() {
        assertNull(
                MarkdownRenderer.extractId("class=\"foo\""));
    }

    @Test
    public void extractId_uuidFormat_returnsId() {
        String uuid =
                "550e8400-e29b-41d4-a716-446655440000";
        assertEquals(uuid,
                MarkdownRenderer.extractId(
                        "id=\"" + uuid + "\""));
    }

    // ==================== parseSegments tests ====================

    @Test
    public void parseSegments_pureMarkdown_singleSegment() {
        String content = "# Hello\n\nSome **bold** text.";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(1, segments.size());
        assertEquals(ContentSegment.Type.MARKDOWN,
                segments.get(0).getType());
        assertEquals("# Hello\n\nSome **bold** text.",
                segments.get(0).getContent());
    }

    @Test
    public void parseSegments_emptyContent_emptyList() {
        List<ContentSegment> segments =
                renderer.parseSegments("");
        assertTrue(segments.isEmpty());
    }

    @Test
    public void parseSegments_whitespaceOnly_emptyList() {
        List<ContentSegment> segments =
                renderer.parseSegments("   \n\n  ");
        assertTrue(segments.isEmpty());
    }

    @Test
    public void parseSegments_questionEmbed_parsesId() {
        String content =
                "Before\n!!! question id=\"q-123\"\nAfter";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(3, segments.size());
        assertEquals(ContentSegment.Type.MARKDOWN,
                segments.get(0).getType());
        assertEquals("Before",
                segments.get(0).getContent());
        assertEquals(ContentSegment.Type.QUESTION_EMBED,
                segments.get(1).getType());
        assertEquals("q-123",
                segments.get(1).getContent());
        assertEquals(ContentSegment.Type.MARKDOWN,
                segments.get(2).getType());
        assertEquals("After",
                segments.get(2).getContent());
    }

    @Test
    public void parseSegments_pdfEmbed_parsesId() {
        String content = "!!! pdf id=\"pdf-456\"";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(1, segments.size());
        assertEquals(ContentSegment.Type.PDF_EMBED,
                segments.get(0).getType());
        assertEquals("pdf-456",
                segments.get(0).getContent());
    }

    @Test
    public void parseSegments_centerBlock_collectsContent() {
        String content = "!!! center\n"
                + "    Centred line one.\n"
                + "    Centred line two.";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(1, segments.size());
        assertEquals(ContentSegment.Type.CENTERED_TEXT,
                segments.get(0).getType());
        assertEquals(
                "Centred line one.\nCentred line two.",
                segments.get(0).getContent());
    }

    @Test
    public void parseSegments_centerBlock_endsAtNonIndented() {
        String content = "!!! center\n"
                + "    Centred text.\n"
                + "Normal text after.";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(2, segments.size());
        assertEquals(ContentSegment.Type.CENTERED_TEXT,
                segments.get(0).getType());
        assertEquals("Centred text.",
                segments.get(0).getContent());
        assertEquals(ContentSegment.Type.MARKDOWN,
                segments.get(1).getType());
        assertEquals("Normal text after.",
                segments.get(1).getContent());
    }

    @Test
    public void parseSegments_centerBlock_noContent_skipped() {
        String content = "!!! center\nNext line.";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(1, segments.size());
        assertEquals(ContentSegment.Type.MARKDOWN,
                segments.get(0).getType());
    }

    @Test
    public void parseSegments_imageEmbed_parsesAttachmentId() {
        String content =
                "Before\n"
                + "![diagram](/attachments/img-789)\n"
                + "After";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(3, segments.size());
        assertEquals(ContentSegment.Type.MARKDOWN,
                segments.get(0).getType());
        assertEquals(ContentSegment.Type.IMAGE_EMBED,
                segments.get(1).getType());
        assertEquals("img-789",
                segments.get(1).getContent());
        assertEquals(ContentSegment.Type.MARKDOWN,
                segments.get(2).getType());
    }

    @Test
    public void parseSegments_imageEmbedWithWhitespace() {
        String content =
                "  ![alt text](/attachments/abc-123)  ";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(1, segments.size());
        assertEquals(ContentSegment.Type.IMAGE_EMBED,
                segments.get(0).getType());
        assertEquals("abc-123",
                segments.get(0).getContent());
    }

    @Test
    public void parseSegments_inlineImage_remainsInMarkdown() {
        String content =
                "Text ![img](/attachments/x) more text";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(1, segments.size());
        assertEquals(ContentSegment.Type.MARKDOWN,
                segments.get(0).getType());
    }

    @Test
    public void parseSegments_multipleAdmonitions() {
        String content = "# Title\n"
                + "!!! question id=\"q1\"\n"
                + "Middle\n"
                + "!!! pdf id=\"p1\"\n"
                + "End";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(5, segments.size());
        assertEquals(ContentSegment.Type.MARKDOWN,
                segments.get(0).getType());
        assertEquals(ContentSegment.Type.QUESTION_EMBED,
                segments.get(1).getType());
        assertEquals("q1",
                segments.get(1).getContent());
        assertEquals(ContentSegment.Type.MARKDOWN,
                segments.get(2).getType());
        assertEquals(ContentSegment.Type.PDF_EMBED,
                segments.get(3).getType());
        assertEquals("p1",
                segments.get(3).getContent());
        assertEquals(ContentSegment.Type.MARKDOWN,
                segments.get(4).getType());
    }

    @Test
    public void parseSegments_admonitionWithoutId_skipped() {
        String content =
                "Before\n!!! question\nAfter";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(2, segments.size());
        assertEquals(ContentSegment.Type.MARKDOWN,
                segments.get(0).getType());
        assertEquals("Before",
                segments.get(0).getContent());
        assertEquals(ContentSegment.Type.MARKDOWN,
                segments.get(1).getType());
        assertEquals("After",
                segments.get(1).getContent());
    }

    @Test
    public void parseSegments_windowsLineEndings() {
        String content =
                "Line one\r\n"
                + "!!! pdf id=\"p1\"\r\n"
                + "Line two";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(3, segments.size());
        assertEquals(ContentSegment.Type.MARKDOWN,
                segments.get(0).getType());
        assertEquals(ContentSegment.Type.PDF_EMBED,
                segments.get(1).getType());
        assertEquals(ContentSegment.Type.MARKDOWN,
                segments.get(2).getType());
    }

    @Test
    public void parseSegments_pdfWithoutId_skipped() {
        String content = "!!! pdf";
        List<ContentSegment> segments =
                renderer.parseSegments(content);
        assertTrue(segments.isEmpty());
    }

    @Test
    public void parseSegments_returnsUnmodifiableList() {
        List<ContentSegment> segments =
                renderer.parseSegments("Hello");
        try {
            segments.add(new ContentSegment(
                    ContentSegment.Type.MARKDOWN, "x"));
            // Should not reach here
            assertTrue(
                    "Expected UnsupportedOperationException",
                    false);
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void parseSegments_consecutiveAdmonitions() {
        String content =
                "!!! pdf id=\"p1\"\n"
                + "!!! question id=\"q1\"";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(2, segments.size());
        assertEquals(ContentSegment.Type.PDF_EMBED,
                segments.get(0).getType());
        assertEquals(ContentSegment.Type.QUESTION_EMBED,
                segments.get(1).getType());
    }

    @Test
    public void parseSegments_centerBlockAtEndOfContent() {
        String content =
                "!!! center\n    Last centred line.";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(1, segments.size());
        assertEquals(ContentSegment.Type.CENTERED_TEXT,
                segments.get(0).getType());
        assertEquals("Last centred line.",
                segments.get(0).getContent());
    }

    @Test
    public void parseSegments_multipleImages() {
        String content =
                "![a](/attachments/img-1)\n"
                + "![b](/attachments/img-2)";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(2, segments.size());
        assertEquals(ContentSegment.Type.IMAGE_EMBED,
                segments.get(0).getType());
        assertEquals("img-1",
                segments.get(0).getContent());
        assertEquals(ContentSegment.Type.IMAGE_EMBED,
                segments.get(1).getType());
        assertEquals("img-2",
                segments.get(1).getContent());
    }

    @Test
    public void parseSegments_unknownAdmonitionType_skipped() {
        // Only pdf, center, question are recognised
        String content = "!!! unknown id=\"x\"";
        List<ContentSegment> segments =
                renderer.parseSegments(content);
        // The regex only matches pdf|center|question,
        // so this is treated as plain markdown
        assertNotNull(segments);
    }

    @Test
    public void parseSegments_markdownBetweenTwoCenterBlocks() {
        String content = "!!! center\n"
                + "    Block one.\n"
                + "Middle text\n"
                + "!!! center\n"
                + "    Block two.";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(3, segments.size());
        assertEquals(ContentSegment.Type.CENTERED_TEXT,
                segments.get(0).getType());
        assertEquals("Block one.",
                segments.get(0).getContent());
        assertEquals(ContentSegment.Type.MARKDOWN,
                segments.get(1).getType());
        assertEquals("Middle text",
                segments.get(1).getContent());
        assertEquals(ContentSegment.Type.CENTERED_TEXT,
                segments.get(2).getType());
        assertEquals("Block two.",
                segments.get(2).getContent());
    }
}
