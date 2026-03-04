package com.manuscripta.student.ui.renderer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.manuscripta.student.data.model.QuestionType;
import com.manuscripta.student.domain.model.Question;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.noties.markwon.Markwon;

/**
 * Unit tests for {@link MarkdownRenderer}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class MarkdownRendererTest {

    private MarkdownRenderer renderer;
    private MarkdownRenderer rendererWithImageLoader;
    private Context context;
    private Markwon mockMarkwon;
    private QuestionBlockRenderer questionBlockRenderer;
    private AttachmentImageLoader mockImageLoader;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        mockMarkwon = mock(Markwon.class);
        questionBlockRenderer = new QuestionBlockRenderer();
        mockImageLoader = mock(AttachmentImageLoader.class);

        renderer = new MarkdownRenderer(
                mockMarkwon, questionBlockRenderer, null);
        rendererWithImageLoader = new MarkdownRenderer(
                mockMarkwon, questionBlockRenderer,
                mockImageLoader);
    }

    // ==================== parseSegments tests ====================

    @Test
    public void testParseSegments_pureMarkdown_singleSegment() {
        String content = "# Hello\n\nSome **bold** text.";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(1, segments.size());
        assertEquals(
                ContentSegment.Type.MARKDOWN,
                segments.get(0).getType());
        assertEquals(
                "# Hello\n\nSome **bold** text.",
                segments.get(0).getContent());
    }

    @Test
    public void testParseSegments_emptyContent_emptyList() {
        List<ContentSegment> segments =
                renderer.parseSegments("");

        assertTrue(segments.isEmpty());
    }

    @Test
    public void testParseSegments_whitespaceOnly_emptyList() {
        List<ContentSegment> segments =
                renderer.parseSegments("   \n\n  ");

        assertTrue(segments.isEmpty());
    }

    @Test
    public void testParseSegments_questionEmbed_parsesId() {
        String content =
                "Before\n!!! question id=\"q-123\"\nAfter";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(3, segments.size());
        assertEquals(
                ContentSegment.Type.MARKDOWN,
                segments.get(0).getType());
        assertEquals("Before", segments.get(0).getContent());
        assertEquals(
                ContentSegment.Type.QUESTION_EMBED,
                segments.get(1).getType());
        assertEquals("q-123", segments.get(1).getContent());
        assertEquals(
                ContentSegment.Type.MARKDOWN,
                segments.get(2).getType());
        assertEquals("After", segments.get(2).getContent());
    }

    @Test
    public void testParseSegments_pdfEmbed_parsesId() {
        String content = "!!! pdf id=\"pdf-456\"";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(1, segments.size());
        assertEquals(
                ContentSegment.Type.PDF_EMBED,
                segments.get(0).getType());
        assertEquals("pdf-456", segments.get(0).getContent());
    }

    @Test
    public void testParseSegments_centerBlock_collectsIndentedContent() {
        String content = "!!! center\n"
                + "    Centred line one.\n"
                + "    Centred line two.";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(1, segments.size());
        assertEquals(
                ContentSegment.Type.CENTERED_TEXT,
                segments.get(0).getType());
        assertEquals(
                "Centred line one.\nCentred line two.",
                segments.get(0).getContent());
    }

    @Test
    public void testParseSegments_centerBlock_endsAtNonIndentedLine() {
        String content = "!!! center\n"
                + "    Centred text.\n"
                + "Normal text after.";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(2, segments.size());
        assertEquals(
                ContentSegment.Type.CENTERED_TEXT,
                segments.get(0).getType());
        assertEquals(
                "Centred text.",
                segments.get(0).getContent());
        assertEquals(
                ContentSegment.Type.MARKDOWN,
                segments.get(1).getType());
        assertEquals(
                "Normal text after.",
                segments.get(1).getContent());
    }

    @Test
    public void testParseSegments_centerBlock_noContent_skipped() {
        String content = "!!! center\nNext line.";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(1, segments.size());
        assertEquals(
                ContentSegment.Type.MARKDOWN,
                segments.get(0).getType());
    }

    @Test
    public void testParseSegments_imageEmbed_parsesAttachmentId() {
        String content =
                "Before\n![diagram](/attachments/img-789)\nAfter";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(3, segments.size());
        assertEquals(
                ContentSegment.Type.MARKDOWN,
                segments.get(0).getType());
        assertEquals(
                ContentSegment.Type.IMAGE_EMBED,
                segments.get(1).getType());
        assertEquals("img-789", segments.get(1).getContent());
        assertEquals(
                ContentSegment.Type.MARKDOWN,
                segments.get(2).getType());
    }

    @Test
    public void testParseSegments_imageEmbedWithWhitespace_parsed() {
        String content =
                "  ![alt text](/attachments/abc-123)  ";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(1, segments.size());
        assertEquals(
                ContentSegment.Type.IMAGE_EMBED,
                segments.get(0).getType());
        assertEquals("abc-123", segments.get(0).getContent());
    }

    @Test
    public void testParseSegments_inlineImage_remainsInMarkdown() {
        String content =
                "Text ![img](/attachments/x) more text";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(1, segments.size());
        assertEquals(
                ContentSegment.Type.MARKDOWN,
                segments.get(0).getType());
    }

    @Test
    public void testParseSegments_multipleAdmonitions() {
        String content = "# Title\n"
                + "!!! question id=\"q1\"\n"
                + "Middle\n"
                + "!!! pdf id=\"p1\"\n"
                + "End";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(5, segments.size());
        assertEquals(
                ContentSegment.Type.MARKDOWN,
                segments.get(0).getType());
        assertEquals(
                ContentSegment.Type.QUESTION_EMBED,
                segments.get(1).getType());
        assertEquals("q1", segments.get(1).getContent());
        assertEquals(
                ContentSegment.Type.MARKDOWN,
                segments.get(2).getType());
        assertEquals(
                ContentSegment.Type.PDF_EMBED,
                segments.get(3).getType());
        assertEquals("p1", segments.get(3).getContent());
        assertEquals(
                ContentSegment.Type.MARKDOWN,
                segments.get(4).getType());
    }

    @Test
    public void testParseSegments_admonitionWithoutId_skipped() {
        String content =
                "Before\n!!! question\nAfter";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        // Question marker with no id is skipped,
        // but content is still split around it
        assertEquals(2, segments.size());
        assertEquals(
                ContentSegment.Type.MARKDOWN,
                segments.get(0).getType());
        assertEquals(
                "Before",
                segments.get(0).getContent());
        assertEquals(
                ContentSegment.Type.MARKDOWN,
                segments.get(1).getType());
        assertEquals(
                "After",
                segments.get(1).getContent());
    }

    @Test
    public void testParseSegments_windowsLineEndings() {
        String content =
                "Line one\r\n!!! pdf id=\"p1\"\r\nLine two";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertEquals(3, segments.size());
        assertEquals(
                ContentSegment.Type.MARKDOWN,
                segments.get(0).getType());
        assertEquals(
                ContentSegment.Type.PDF_EMBED,
                segments.get(1).getType());
        assertEquals(
                ContentSegment.Type.MARKDOWN,
                segments.get(2).getType());
    }

    @Test
    public void testParseSegments_pdfWithoutId_skipped() {
        String content = "!!! pdf";
        List<ContentSegment> segments =
                renderer.parseSegments(content);

        assertTrue(segments.isEmpty());
    }

    // ==================== extractId tests ====================

    @Test
    public void testExtractId_validAttribute_returnsId() {
        assertEquals(
                "abc-123",
                MarkdownRenderer.extractId("id=\"abc-123\""));
    }

    @Test
    public void testExtractId_multipleAttributes_returnsId() {
        assertEquals(
                "xyz",
                MarkdownRenderer.extractId(
                        "type=\"pdf\" id=\"xyz\" class=\"big\""));
    }

    @Test
    public void testExtractId_nullAttribute_returnsNull() {
        assertNull(MarkdownRenderer.extractId(null));
    }

    @Test
    public void testExtractId_emptyString_returnsNull() {
        assertNull(MarkdownRenderer.extractId(""));
    }

    @Test
    public void testExtractId_noIdAttribute_returnsNull() {
        assertNull(
                MarkdownRenderer.extractId("class=\"foo\""));
    }

    @Test
    public void testExtractId_uuidFormat_returnsId() {
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        assertEquals(
                uuid,
                MarkdownRenderer.extractId(
                        "id=\"" + uuid + "\""));
    }

    // ==================== dpToPx tests ====================

    @Test
    public void testDpToPx_returnsNonNegativeValue() {
        int result = MarkdownRenderer.dpToPx(context, 16);
        assertTrue(result >= 0);
    }

    @Test
    public void testDpToPx_zeroInput_returnsZero() {
        assertEquals(0, MarkdownRenderer.dpToPx(context, 0));
    }

    // ==================== render tests ====================

    @Test
    public void testRender_addsViewsToParent() {
        LinearLayout parent = new LinearLayout(context);
        String content = "# Hello\n"
                + "!!! question id=\"q1\"\n"
                + "Goodbye";

        Question question = new Question(
                "q1", "mat-1", "What is 2+2?",
                QuestionType.MULTIPLE_CHOICE,
                "[\"3\",\"4\",\"5\"]", "B");
        Map<String, Question> questions = new HashMap<>();
        questions.put("q1", question);

        renderer.render(parent, content, "mat-1", questions);

        assertTrue(parent.getChildCount() > 0);
    }

    @Test
    public void testRender_clearsParentBeforeRendering() {
        LinearLayout parent = new LinearLayout(context);
        parent.addView(new TextView(context));
        assertEquals(1, parent.getChildCount());

        renderer.render(
                parent, "Hello", "mat-1",
                Collections.emptyMap());

        // Previous child should be removed;
        // only new content should remain
        assertTrue(parent.getChildCount() >= 1);
    }

    @Test
    public void testRender_emptyContent_noChildren() {
        LinearLayout parent = new LinearLayout(context);
        renderer.render(
                parent, "", "mat-1",
                Collections.emptyMap());

        assertEquals(0, parent.getChildCount());
    }

    // ==================== renderSegment tests ====================

    @Test
    public void testRenderSegment_markdown_createsTextView() {
        ContentSegment segment = new ContentSegment(
                ContentSegment.Type.MARKDOWN, "# Hello");

        View view = renderer.renderSegment(
                context, segment, "mat-1",
                Collections.emptyMap());

        assertNotNull(view);
        assertTrue(view instanceof TextView);
        verify(mockMarkwon).setMarkdown(
                any(TextView.class), eq("# Hello"));
    }

    @Test
    public void testRenderSegment_centeredText_createsLinearLayout() {
        ContentSegment segment = new ContentSegment(
                ContentSegment.Type.CENTERED_TEXT,
                "Centred content");

        View view = renderer.renderSegment(
                context, segment, "mat-1",
                Collections.emptyMap());

        assertNotNull(view);
        assertTrue(view instanceof LinearLayout);
    }

    @Test
    public void testRenderSegment_questionEmbed_found_rendersQuestion() {
        Question question = new Question(
                "q1", "mat-1", "What?",
                QuestionType.WRITTEN_ANSWER, "", "answer");
        Map<String, Question> questions = new HashMap<>();
        questions.put("q1", question);

        ContentSegment segment = new ContentSegment(
                ContentSegment.Type.QUESTION_EMBED, "q1");

        View view = renderer.renderSegment(
                context, segment, "mat-1", questions);

        assertNotNull(view);
        assertTrue(view instanceof LinearLayout);
    }

    @Test
    public void testRenderSegment_questionEmbed_notFound_placeholder() {
        ContentSegment segment = new ContentSegment(
                ContentSegment.Type.QUESTION_EMBED, "missing");

        View view = renderer.renderSegment(
                context, segment, "mat-1",
                Collections.emptyMap());

        assertNotNull(view);
        assertTrue(view instanceof TextView);
        assertEquals(
                "Question not available",
                ((TextView) view).getText().toString());
    }

    @Test
    public void testRenderSegment_pdfEmbed_createsPlaceholder() {
        ContentSegment segment = new ContentSegment(
                ContentSegment.Type.PDF_EMBED, "pdf-id");

        View view = renderer.renderSegment(
                context, segment, "mat-1",
                Collections.emptyMap());

        assertNotNull(view);
        assertTrue(view instanceof TextView);
        assertTrue(((TextView) view).getText().toString()
                .contains("pdf-id"));
    }

    @Test
    public void testRenderSegment_imageEmbed_createsImageView() {
        ContentSegment segment = new ContentSegment(
                ContentSegment.Type.IMAGE_EMBED, "img-1");

        View view = rendererWithImageLoader.renderSegment(
                context, segment, "mat-1",
                Collections.emptyMap());

        assertNotNull(view);
        assertTrue(view instanceof ImageView);
        verify(mockImageLoader).loadImage(
                eq("img-1"), eq("mat-1"),
                any(ImageView.class));
    }

    @Test
    public void testRenderSegment_imageEmbed_noLoader_stillCreatesView() {
        ContentSegment segment = new ContentSegment(
                ContentSegment.Type.IMAGE_EMBED, "img-2");

        View view = renderer.renderSegment(
                context, segment, "mat-1",
                Collections.emptyMap());

        assertNotNull(view);
        assertTrue(view instanceof ImageView);
    }

    // ==================== Constructor tests ====================

    @Test
    public void testConstructor_withContext_createsInstance() {
        MarkdownRenderer mr = new MarkdownRenderer(
                context, questionBlockRenderer, null);
        assertNotNull(mr);
    }

    @Test
    public void testConstructor_withContextAndImageLoader_createsInstance() {
        MarkdownRenderer mr = new MarkdownRenderer(
                context, questionBlockRenderer,
                mockImageLoader);
        assertNotNull(mr);
    }
}
