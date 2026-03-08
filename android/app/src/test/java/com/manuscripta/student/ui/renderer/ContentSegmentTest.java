package com.manuscripta.student.ui.renderer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Unit tests for {@link ContentSegment}. Verifies the segment
 * data class and its Type enum without requiring Android context.
 */
public class ContentSegmentTest {

    @Test
    public void testConstructorAndGetters_markdown() {
        ContentSegment segment = new ContentSegment(
                ContentSegment.Type.MARKDOWN, "# Hello");

        assertEquals(ContentSegment.Type.MARKDOWN,
                segment.getType());
        assertEquals("# Hello", segment.getContent());
    }

    @Test
    public void testConstructorAndGetters_pdfEmbed() {
        ContentSegment segment = new ContentSegment(
                ContentSegment.Type.PDF_EMBED, "pdf-123");

        assertEquals(ContentSegment.Type.PDF_EMBED,
                segment.getType());
        assertEquals("pdf-123", segment.getContent());
    }

    @Test
    public void testConstructorAndGetters_centeredText() {
        ContentSegment segment = new ContentSegment(
                ContentSegment.Type.CENTERED_TEXT,
                "Centred content");

        assertEquals(ContentSegment.Type.CENTERED_TEXT,
                segment.getType());
        assertEquals("Centred content", segment.getContent());
    }

    @Test
    public void testConstructorAndGetters_questionEmbed() {
        ContentSegment segment = new ContentSegment(
                ContentSegment.Type.QUESTION_EMBED, "q-456");

        assertEquals(ContentSegment.Type.QUESTION_EMBED,
                segment.getType());
        assertEquals("q-456", segment.getContent());
    }

    @Test
    public void testConstructorAndGetters_imageEmbed() {
        ContentSegment segment = new ContentSegment(
                ContentSegment.Type.IMAGE_EMBED, "img-789");

        assertEquals(ContentSegment.Type.IMAGE_EMBED,
                segment.getType());
        assertEquals("img-789", segment.getContent());
    }

    @Test
    public void testTypeEnumValues() {
        ContentSegment.Type[] values =
                ContentSegment.Type.values();
        assertEquals(5, values.length);
    }

    @Test
    public void testTypeEnumValueOf_markdown() {
        assertEquals(ContentSegment.Type.MARKDOWN,
                ContentSegment.Type.valueOf("MARKDOWN"));
    }

    @Test
    public void testTypeEnumValueOf_pdfEmbed() {
        assertEquals(ContentSegment.Type.PDF_EMBED,
                ContentSegment.Type.valueOf("PDF_EMBED"));
    }

    @Test
    public void testTypeEnumValueOf_centeredText() {
        assertEquals(ContentSegment.Type.CENTERED_TEXT,
                ContentSegment.Type.valueOf("CENTERED_TEXT"));
    }

    @Test
    public void testTypeEnumValueOf_questionEmbed() {
        assertEquals(ContentSegment.Type.QUESTION_EMBED,
                ContentSegment.Type.valueOf("QUESTION_EMBED"));
    }

    @Test
    public void testTypeEnumValueOf_imageEmbed() {
        assertEquals(ContentSegment.Type.IMAGE_EMBED,
                ContentSegment.Type.valueOf("IMAGE_EMBED"));
    }

    @Test
    public void testContentPreservesWhitespace() {
        ContentSegment segment = new ContentSegment(
                ContentSegment.Type.MARKDOWN,
                "  spaces and\n\nnewlines  ");

        assertEquals("  spaces and\n\nnewlines  ",
                segment.getContent());
    }

    @Test
    public void testAllEnumValuesNotNull() {
        for (ContentSegment.Type type
                : ContentSegment.Type.values()) {
            assertNotNull(type);
        }
    }
}
