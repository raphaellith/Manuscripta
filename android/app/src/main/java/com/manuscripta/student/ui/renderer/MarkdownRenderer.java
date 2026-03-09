package com.manuscripta.student.ui.renderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.manuscripta.student.R;
import com.manuscripta.student.domain.model.Question;
import com.manuscripta.student.utils.FileStorageManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.latex.JLatexMathPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin;

/**
 * Renders markdown content encoded per the Material Encoding
 * Specification into Android Views.
 *
 * <p>Uses Markwon for standard markdown rendering and provides
 * custom handling for admonition markers (pdf, center, question)
 * as specified in Material Encoding §4, plus standalone attachment
 * images per §3.</p>
 *
 * <p>Spec reference: Material Encoding Specification, Student MAT1.</p>
 */
public class MarkdownRenderer {

    /** Tag for logging. */
    private static final String TAG = "MarkdownRenderer";

    /** Default text size for LaTeX rendering (sp), matching body text. */
    private static final float LATEX_TEXT_SIZE = 30f;

    /** Pattern matching admonition marker lines. */
    private static final Pattern ADMONITION_PATTERN =
            Pattern.compile(
                    "^!!! (pdf|center|question)(?: (.+))?$");

    /** Pattern for extracting id attribute values. */
    private static final Pattern ID_ATTRIBUTE_PATTERN =
            Pattern.compile("id=\"([^\"]+)\"");

    /**
     * Pattern matching inline LaTeX delimited by single dollar
     * signs per Material Encoding §2(7)(a). Converts to the
     * double-dollar syntax that Markwon JLatexMathPlugin expects.
     */
    private static final Pattern INLINE_LATEX_PATTERN =
            Pattern.compile("(?<!\\$)\\$(?!\\$)(.+?)(?<!\\$)\\$(?!\\$)");

    /**
     * Pattern matching attachment image lines, including those
     * wrapped in heading or blockquote markers.
     * Captures group 1 = alt text, group 2 = attachment id.
     */
    private static final Pattern IMAGE_PATTERN =
            Pattern.compile(
                    "^[\\s#>]*!\\[([^\\]]*)\\]"
                            + "\\(/attachments/([^)]+)\\)\\s*$");

    /** The Markwon instance for standard markdown rendering. */
    @NonNull
    private Markwon markwon;

    /** Context used to rebuild the Markwon instance on scale changes. */
    @Nullable
    private Context markwonContext;

    /** Renderer for embedded question blocks. */
    @NonNull
    private final QuestionBlockRenderer questionBlockRenderer;

    /** Loader for attachment images (may be null). */
    @Nullable
    private final AttachmentImageLoader attachmentImageLoader;

    /** File storage manager for PDF attachments (may be null). */
    @Nullable
    private final FileStorageManager fileStorageManager;

    /** Background executor for async PDF rendering. */
    @NonNull
    private final ExecutorService executor;

    /** Scale factor applied to text sizes, derived from config. */
    private float textScaleFactor = 1.0f;

    /**
     * Creates a new MarkdownRenderer with the given context.
     *
     * @param context               Android context for View creation
     * @param questionBlockRenderer renderer for embedded questions
     * @param attachmentImageLoader loader for attachment images,
     *                              or null if image loading is
     *                              not required
     * @param fileStorageManager    file storage manager for PDF
     *                              attachments, or null if not
     *                              required
     */
    public MarkdownRenderer(
            @NonNull Context context,
            @NonNull QuestionBlockRenderer questionBlockRenderer,
            @Nullable AttachmentImageLoader attachmentImageLoader,
            @Nullable FileStorageManager fileStorageManager
    ) {
        this.markwonContext = context;
        this.markwon = buildMarkwon(
                context, LATEX_TEXT_SIZE);
        this.questionBlockRenderer = questionBlockRenderer;
        this.attachmentImageLoader = attachmentImageLoader;
        this.fileStorageManager = fileStorageManager;
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Builds a Markwon instance with the given LaTeX text size.
     *
     * @param context      Android context
     * @param latexTextSize LaTeX rendering text size in sp
     * @return the configured Markwon instance
     */
    @NonNull
    private static Markwon buildMarkwon(
            @NonNull Context context,
            float latexTextSize) {
        return Markwon.builder(context)
                .usePlugin(TablePlugin.create(context))
                .usePlugin(HtmlPlugin.create())
                .usePlugin(MarkwonInlineParserPlugin.create())
                .usePlugin(JLatexMathPlugin.create(
                        latexTextSize,
                        builder -> builder.inlinesEnabled(true)))
                .build();
    }

    /**
     * Creates a MarkdownRenderer with a pre-built Markwon instance.
     * Primarily for testing.
     *
     * @param markwon               pre-configured Markwon instance
     * @param questionBlockRenderer renderer for embedded questions
     * @param attachmentImageLoader loader for attachment images,
     *                              or null if not required
     * @param fileStorageManager    file storage manager for PDF
     *                              attachments, or null if not
     *                              required
     */
    MarkdownRenderer(
            @NonNull Markwon markwon,
            @NonNull QuestionBlockRenderer questionBlockRenderer,
            @Nullable AttachmentImageLoader attachmentImageLoader,
            @Nullable FileStorageManager fileStorageManager
    ) {
        this.markwon = markwon;
        this.questionBlockRenderer = questionBlockRenderer;
        this.attachmentImageLoader = attachmentImageLoader;
        this.fileStorageManager = fileStorageManager;
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Sets the text scale factor derived from the configuration
     * text size. A value of 1.0 uses the default sizes. The body
     * text size is computed as {@code BASE_BODY_SP * scaleFactor}.
     *
     * @param scaleFactor the scale factor (e.g. 1.5 for 50% larger)
     */
    public void setTextScaleFactor(float scaleFactor) {
        this.textScaleFactor = scaleFactor;
        if (markwonContext != null) {
            this.markwon = buildMarkwon(
                    markwonContext,
                    LATEX_TEXT_SIZE * scaleFactor);
        }
    }

    /**
     * Renders markdown content into the specified parent ViewGroup.
     *
     * <p>The content is parsed into segments of standard markdown and
     * custom markers. Standard markdown is rendered using Markwon.
     * Custom markers are rendered as appropriate Views.</p>
     *
     * @param parent     the parent ViewGroup to populate
     * @param content    the markdown content string
     * @param materialId the material ID for resolving attachments
     * @param questions  map of question ID to Question for
     *                   resolving question markers (may be empty)
     */
    public void render(
            @NonNull ViewGroup parent,
            @NonNull String content,
            @NonNull String materialId,
            @NonNull Map<String, Question> questions) {
        parent.removeAllViews();

        Log.d(TAG, "render: materialId=" + materialId
                + " contentLen=" + content.length()
                + " imageLoader="
                + (attachmentImageLoader != null)
                + " fsm=" + (fileStorageManager != null));

        List<ContentSegment> segments = parseSegments(content);
        Log.d(TAG, "render: parsed " + segments.size()
                + " segments");
        Context context = parent.getContext();

        for (ContentSegment segment : segments) {
            View view = renderSegment(
                    context, segment, materialId, questions);
            if (view != null) {
                parent.addView(view);
            }
        }
    }

    /**
     * Renders a single content segment into a View.
     *
     * @param context    Android context
     * @param segment    the content segment to render
     * @param materialId the material ID for attachment resolution
     * @param questions  map of question ID to Question
     * @return the rendered View, or null if unrenderable
     */
    @Nullable
    View renderSegment(
            @NonNull Context context,
            @NonNull ContentSegment segment,
            @NonNull String materialId,
            @NonNull Map<String, Question> questions) {
        Log.d(TAG, "renderSegment: type="
                + segment.getType()
                + " content=" + segment.getContent());
        switch (segment.getType()) {
            case MARKDOWN:
                return renderMarkdown(
                        context, segment.getContent());
            case CENTERED_TEXT:
                return renderCenteredText(
                        context, segment.getContent());
            case QUESTION_EMBED:
                return renderQuestionEmbed(
                        context, segment.getContent(),
                        questions);
            case PDF_EMBED:
                return renderPdfEmbed(
                        context, segment.getContent(),
                        materialId);
            case IMAGE_EMBED:
                return renderImageEmbed(
                        context, segment.getContent(),
                        materialId);
            default:
                return null;
        }
    }

    /**
     * Base body text size in SP used as the reference for scaling.
     */
    private static final float BASE_BODY_SP = 30f;

    /**
     * Renders standard markdown content into a TextView.
     *
     * @param context  Android context
     * @param markdown the markdown text to render
     * @return a TextView with rendered markdown content
     */
    @NonNull
    private TextView renderMarkdown(
            @NonNull Context context,
            @NonNull String markdown) {
        TextView textView = new TextView(context);
        textView.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                BASE_BODY_SP * textScaleFactor);
        applyBodyFont(context, textView);
        String processed = convertInlineLatex(markdown.trim());
        markwon.setMarkdown(textView, processed);
        return textView;
    }

    /**
     * Converts single-dollar inline LaTeX ({@code $...$}) to
     * double-dollar syntax ({@code $$...$$}) that the Markwon
     * JLatexMathPlugin requires, per Material Encoding §2(7)(a).
     *
     * @param text the markdown text to process
     * @return the text with inline LaTeX delimiters converted
     */
    @NonNull
    String convertInlineLatex(@NonNull String text) {
        return INLINE_LATEX_PATTERN.matcher(text)
                .replaceAll("\\$\\$$1\\$\\$");
    }

    /**
     * Renders centred text content per Material Encoding §4(3).
     *
     * @param context Android context
     * @param content the text content to centre
     * @return a centred layout containing the rendered content
     */
    @NonNull
    private View renderCenteredText(
            @NonNull Context context,
            @NonNull String content) {
        LinearLayout wrapper = new LinearLayout(context);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView textView = new TextView(context);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                BASE_BODY_SP * textScaleFactor);
        applyBodyFont(context, textView);
        String processed = convertInlineLatex(content.trim());
        markwon.setMarkdown(textView, processed);
        wrapper.addView(textView);

        return wrapper;
    }

    /**
     * Attempts to apply the IBM Plex Sans font to a TextView.
     * Falls back silently if the font is not available (e.g. on
     * devices without Google Play Services).
     *
     * @param context  Android context
     * @param textView the TextView to style
     */
    private void applyBodyFont(
            @NonNull Context context,
            @NonNull TextView textView) {
        try {
            Typeface bodyFont = ResourcesCompat.getFont(
                    context, R.font.ibm_plex_sans);
            if (bodyFont != null) {
                textView.setTypeface(bodyFont);
            }
        } catch (Exception e) {
            Log.d(TAG, "Body font not available, using default");
        }
    }

    /**
     * Renders an embedded question per Material Encoding §4(4).
     *
     * @param context    Android context
     * @param questionId the question UUID to render
     * @param questions  map of available questions
     * @return a View for the question, or a placeholder
     */
    @NonNull
    private View renderQuestionEmbed(
            @NonNull Context context,
            @NonNull String questionId,
            @NonNull Map<String, Question> questions) {
        Question question = questions.get(questionId);
        if (question != null) {
            return questionBlockRenderer.renderQuestion(
                    context, question);
        }
        return createPlaceholder(
                context, "Question not available");
    }

    /**
     * Renders a PDF embed marker per Material Encoding §4(2).
     *
     * <p>Loads the PDF file from local storage and renders each
     * page as a Bitmap in an ImageView. If the file is not
     * available, shows a placeholder.</p>
     *
     * @param context      Android context
     * @param attachmentId the PDF attachment UUID
     * @param materialId   the parent material UUID
     * @return a View containing the rendered PDF pages
     */
    @NonNull
    private View renderPdfEmbed(
            @NonNull Context context,
            @NonNull String attachmentId,
            @NonNull String materialId) {
        Log.d(TAG, "renderPdfEmbed: attachment="
                + attachmentId + " material=" + materialId
                + " fsm=" + (fileStorageManager != null));
        if (fileStorageManager == null) {
            return createPlaceholder(
                    context,
                    "PDF document: " + attachmentId);
        }

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        TextView loading = new TextView(context);
        loading.setText("Loading PDF\u2026");
        loading.setTypeface(null, Typeface.ITALIC);
        int padding = dpToPx(context, 16);
        loading.setPadding(padding, padding, padding, padding);
        container.addView(loading);

        executor.execute(() -> {
            File pdfFile = fileStorageManager.getAttachmentFile(
                    materialId, attachmentId);
            Log.d(TAG, "renderPdfEmbed: file="
                    + (pdfFile != null
                            ? pdfFile.getAbsolutePath()
                            : "null")
                    + " exists="
                    + (pdfFile != null && pdfFile.exists()));
            if (pdfFile == null || !pdfFile.exists()) {
                container.post(() -> {
                    container.removeAllViews();
                    container.addView(createPlaceholder(
                            context,
                            "PDF not available: "
                                    + attachmentId));
                });
                return;
            }

            List<Bitmap> pages = renderPdfPages(pdfFile);
            container.post(() -> {
                container.removeAllViews();
                if (pages.isEmpty()) {
                    container.addView(createPlaceholder(
                            context,
                            "Unable to render PDF"));
                    return;
                }
                for (Bitmap page : pages) {
                    ImageView iv = new ImageView(context);
                    iv.setAdjustViewBounds(true);
                    iv.setLayoutParams(
                            new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams
                                            .MATCH_PARENT,
                                    ViewGroup.LayoutParams
                                            .WRAP_CONTENT));
                    iv.setImageBitmap(page);
                    container.addView(iv);
                }
            });
        });

        return container;
    }

    /**
     * Renders all pages of a PDF file to bitmaps using
     * Android's built-in PdfRenderer.
     *
     * @param pdfFile the PDF file to render
     * @return a list of bitmaps, one per page
     */
    @NonNull
    private List<Bitmap> renderPdfPages(@NonNull File pdfFile) {
        List<Bitmap> pages = new ArrayList<>();
        try (ParcelFileDescriptor fd =
                     ParcelFileDescriptor.open(
                             pdfFile,
                             ParcelFileDescriptor
                                     .MODE_READ_ONLY);
             PdfRenderer renderer = new PdfRenderer(fd)) {

            for (int i = 0; i < renderer.getPageCount(); i++) {
                try (PdfRenderer.Page page =
                             renderer.openPage(i)) {
                    Bitmap bitmap = Bitmap.createBitmap(
                            page.getWidth() * 2,
                            page.getHeight() * 2,
                            Bitmap.Config.ARGB_8888);
                    bitmap.eraseColor(
                            android.graphics.Color.WHITE);
                    page.render(bitmap, null, null,
                            PdfRenderer.Page
                                    .RENDER_MODE_FOR_DISPLAY);
                    pages.add(bitmap);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to render PDF: "
                    + pdfFile.getName(), e);
        }
        return pages;
    }

    /**
     * Renders a standalone attachment image per Material Encoding §3.
     *
     * @param context      Android context
     * @param attachmentId the image attachment UUID
     * @param materialId   the parent material UUID
     * @return an ImageView for the attachment
     */
    @NonNull
    private View renderImageEmbed(
            @NonNull Context context,
            @NonNull String attachmentId,
            @NonNull String materialId) {
        Log.d(TAG, "renderImageEmbed: attachment="
                + attachmentId + " material=" + materialId
                + " loader="
                + (attachmentImageLoader != null));
        ImageView imageView = new ImageView(context);
        imageView.setAdjustViewBounds(true);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        if (attachmentImageLoader != null) {
            attachmentImageLoader.loadImage(
                    attachmentId, materialId, imageView);
        } else {
            Log.w(TAG, "No image loader — image will not"
                    + " render for " + attachmentId);
        }

        return imageView;
    }

    /**
     * Creates a placeholder TextView for unavailable content.
     *
     * @param context Android context
     * @param message the placeholder message
     * @return a styled placeholder TextView
     */
    @NonNull
    private TextView createPlaceholder(
            @NonNull Context context,
            @NonNull String message) {
        TextView placeholder = new TextView(context);
        placeholder.setText(message);
        placeholder.setTypeface(null, Typeface.ITALIC);
        int padding = dpToPx(context, 16);
        placeholder.setPadding(
                padding, padding, padding, padding);
        return placeholder;
    }

    /**
     * Parses markdown content into a list of content segments,
     * splitting on custom admonition markers and standalone images.
     *
     * <p>Standard markdown is collected into MARKDOWN segments.
     * Admonition markers (pdf, center, question) are parsed into
     * their respective segment types per Material Encoding §4.
     * Standalone attachment images are parsed per §3.</p>
     *
     * @param content the raw markdown content string
     * @return an unmodifiable list of content segments
     */
    @NonNull
    List<ContentSegment> parseSegments(@NonNull String content) {
        List<ContentSegment> segments = new ArrayList<>();
        String[] lines = content.split("\\r?\\n", -1);
        StringBuilder markdownBuffer = new StringBuilder();
        int i = 0;

        while (i < lines.length) {
            Matcher admonitionMatcher =
                    ADMONITION_PATTERN.matcher(lines[i]);
            Matcher imageMatcher =
                    IMAGE_PATTERN.matcher(lines[i]);

            if (admonitionMatcher.matches()) {
                flushMarkdown(markdownBuffer, segments);
                String markerType = admonitionMatcher.group(1);
                String attributes = admonitionMatcher.group(2);
                i = processAdmonition(
                        markerType, attributes,
                        lines, i, segments);
            } else if (imageMatcher.matches()) {
                flushMarkdown(markdownBuffer, segments);
                String attachmentId = imageMatcher.group(2);
                segments.add(new ContentSegment(
                        ContentSegment.Type.IMAGE_EMBED,
                        attachmentId));
                i++;
            } else {
                markdownBuffer.append(lines[i]);
                if (i < lines.length - 1) {
                    markdownBuffer.append("\n");
                }
                i++;
            }
        }

        flushMarkdown(markdownBuffer, segments);
        return Collections.unmodifiableList(segments);
    }

    /**
     * Processes an admonition marker and its content.
     *
     * @param markerType the marker type (pdf, center, question)
     * @param attributes the attribute string (may be null)
     * @param lines      all content lines
     * @param lineIndex  the index of the marker line
     * @param segments   the segment list to append to
     * @return the next line index to process
     */
    private int processAdmonition(
            @NonNull String markerType,
            @Nullable String attributes,
            @NonNull String[] lines,
            int lineIndex,
            @NonNull List<ContentSegment> segments) {
        switch (markerType) {
            case "center":
                return processCenterBlock(
                        lines, lineIndex + 1, segments);
            case "pdf":
                String pdfId = extractId(attributes);
                if (pdfId != null) {
                    segments.add(new ContentSegment(
                            ContentSegment.Type.PDF_EMBED,
                            pdfId));
                }
                return lineIndex + 1;
            case "question":
                String questionId = extractId(attributes);
                if (questionId != null) {
                    segments.add(new ContentSegment(
                            ContentSegment.Type.QUESTION_EMBED,
                            questionId));
                }
                return lineIndex + 1;
            default:
                return lineIndex + 1;
        }
    }

    /**
     * Processes a center block by collecting indented content
     * lines per Material Encoding §4(3).
     *
     * @param lines    all content lines
     * @param start    the first line after the marker
     * @param segments the segment list to append to
     * @return the next line index to process
     */
    private int processCenterBlock(
            @NonNull String[] lines,
            int start,
            @NonNull List<ContentSegment> segments) {
        StringBuilder centered = new StringBuilder();
        int i = start;

        while (i < lines.length
                && lines[i].startsWith("    ")) {
            if (centered.length() > 0) {
                centered.append("\n");
            }
            centered.append(lines[i].substring(4));
            i++;
        }

        if (centered.length() > 0) {
            segments.add(new ContentSegment(
                    ContentSegment.Type.CENTERED_TEXT,
                    centered.toString()));
        }

        return i;
    }

    /**
     * Flushes accumulated markdown text into a MARKDOWN segment
     * if the buffer is non-empty after trimming.
     *
     * @param buffer   the markdown text buffer
     * @param segments the segment list to append to
     */
    private void flushMarkdown(
            @NonNull StringBuilder buffer,
            @NonNull List<ContentSegment> segments) {
        String text = buffer.toString().trim();
        if (!text.isEmpty()) {
            segments.add(new ContentSegment(
                    ContentSegment.Type.MARKDOWN, text));
        }
        buffer.setLength(0);
    }

    /**
     * Extracts the {@code id} attribute value from an admonition
     * attribute string.
     *
     * @param attributes the attribute string
     *                   (e.g., {@code id="abc-123"})
     * @return the extracted ID, or null if not found
     */
    @Nullable
    static String extractId(@Nullable String attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return null;
        }
        Matcher matcher =
                ID_ATTRIBUTE_PATTERN.matcher(attributes);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Converts density-independent pixels to actual pixels.
     *
     * @param context Android context
     * @param dp      the dp value to convert
     * @return the equivalent pixel value
     */
    static int dpToPx(@NonNull Context context, int dp) {
        float density = context.getResources()
                .getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
