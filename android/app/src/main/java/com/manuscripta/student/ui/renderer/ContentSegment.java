package com.manuscripta.student.ui.renderer;

import androidx.annotation.NonNull;

/**
 * Represents a segment of material content, either standard markdown
 * or a custom admonition marker.
 *
 * <p>Used internally by {@link MarkdownRenderer} during content parsing
 * per the Material Encoding Specification.</p>
 */
class ContentSegment {

    /**
     * The types of content segment recognised by the renderer.
     */
    enum Type {
        /** Standard markdown text. */
        MARKDOWN,

        /** PDF embed marker per Material Encoding §4(2). */
        PDF_EMBED,

        /** Centred text block per Material Encoding §4(3). */
        CENTERED_TEXT,

        /** Embedded question per Material Encoding §4(4). */
        QUESTION_EMBED,

        /** Standalone image attachment per Material Encoding §3. */
        IMAGE_EMBED
    }

    /** The segment type. */
    @NonNull
    private final Type type;

    /** The segment content: text for MARKDOWN/CENTERED_TEXT, ID for others. */
    @NonNull
    private final String content;

    /**
     * Creates a new content segment.
     *
     * @param type    the segment type
     * @param content the segment content
     */
    ContentSegment(@NonNull Type type, @NonNull String content) {
        this.type = type;
        this.content = content;
    }

    /**
     * Returns the segment type.
     *
     * @return the type of this segment
     */
    @NonNull
    Type getType() {
        return type;
    }

    /**
     * Returns the segment content.
     * For MARKDOWN and CENTERED_TEXT this is the text content.
     * For PDF_EMBED, QUESTION_EMBED, and IMAGE_EMBED this is the ID.
     *
     * @return the content string
     */
    @NonNull
    String getContent() {
        return content;
    }
}
