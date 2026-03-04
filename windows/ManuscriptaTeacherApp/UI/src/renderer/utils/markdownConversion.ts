/**
 * Markdown Conversion Utilities.
 * Converts between HTML (TipTap internal format) and Markdown (Material Encoding Spec).
 * Per Material Encoding Specification.
 */

import TurndownService from 'turndown';
import { marked } from 'marked';
import { stripLinksFromHtml } from '../../utils/htmlSanitizer';

// Configure Turndown for HTML → Markdown conversion
const turndownService = new TurndownService({
    headingStyle: 'atx',
    codeBlockStyle: 'fenced',
    bulletListMarker: '-',
    emDelimiter: '*',
    strongDelimiter: '**',
    // Handle empty/blank elements - needed for question refs, pdf embeds, and LaTeX
    // Turndown skips all custom addRule rules for blank nodes (no text content)
    // and goes straight to blankReplacement, so all blank-element conversions must live here.
    blankReplacement: function (content, node) {
        const element = node as HTMLElement;
        if (element.nodeName === 'DIV' && element.hasAttribute('data-question-id')) {
            const questionId = element.getAttribute('data-question-id');
            return `\n!!! question id="${questionId}"\n`;
        }
        if (element.nodeName === 'DIV' && element.hasAttribute('data-pdf-id')) {
            const pdfId = element.getAttribute('data-pdf-id');
            return `\n!!! pdf id="${pdfId}"\n`;
        }
        // Inline LaTeX per Material Encoding Spec §2(7)(a)
        // collapseWhitespace strips spaces from text nodes adjacent to empty inline elements,
        // so we compensate by adding spaces when adjacent text nodes have lost their spacing.
        if (element.nodeName === 'SPAN' && element.hasAttribute('data-latex')) {
            const latex = element.getAttribute('data-latex') || '';
            const prev = element.previousSibling;
            const next = element.nextSibling;
            // Add leading space if previous text doesn't already end with whitespace
            const needsLeadingSpace = prev && prev.nodeType === 3 && !/\s$/.test(prev.nodeValue || '');
            // Add trailing space if next text doesn't already start with whitespace
            const needsTrailingSpace = next && next.nodeType === 3 && !/^\s/.test(next.nodeValue || '');
            return (needsLeadingSpace ? ' ' : '') + `$${latex}$` + (needsTrailingSpace ? ' ' : '');
        }
        // Block LaTeX per Material Encoding Spec §2(7)(b)
        if (element.nodeName === 'DIV' && element.hasAttribute('data-block-latex')) {
            const latex = element.getAttribute('data-block-latex') || '';
            return `\n$$${latex}$$\n`;
        }
        // When a P/DIV is blank because it only contains empty LaTeX spans,
        // Turndown considers the parent blank and never processes the children.
        // Scan children and serialize any LaTeX nodes found.
        if (element.nodeName === 'P' || element.nodeName === 'DIV') {
            const children = element.childNodes;
            let latexParts = '';
            let hasLatex = false;
            for (let i = 0; i < children.length; i++) {
                const child = children[i] as HTMLElement;
                if (child.nodeType === 1) { // Element node
                    if (child.nodeName === 'SPAN' && child.hasAttribute('data-latex')) {
                        latexParts += `$${child.getAttribute('data-latex') || ''}$`;
                        hasLatex = true;
                    } else if (child.nodeName === 'DIV' && child.hasAttribute('data-block-latex')) {
                        latexParts += `$$${child.getAttribute('data-block-latex') || ''}$$`;
                        hasLatex = true;
                    }
                }
            }
            if (hasLatex) {
                return '\n\n' + latexParts + '\n\n';
            }
        }
        // Default behavior for other blank elements
        return node.nodeName === 'DIV' || node.nodeName === 'P' ? '\n\n' : '';
    }
});

// Custom rules for Material Encoding compliance

// Handle tables
turndownService.addRule('table', {
    filter: 'table',
    replacement: function (_content, node) {
        const table = node as HTMLTableElement;
        const rows: { cells: string[]; isHeader: boolean }[] = [];

        table.querySelectorAll('tr').forEach((tr) => {
            const cells: string[] = [];
            tr.querySelectorAll('th, td').forEach((cell) => {
                cells.push(cell.textContent?.trim() || '');
            });
            const hasHeaderCells = tr.querySelectorAll('th').length > 0;
            rows.push({ cells, isHeader: hasHeaderCells });
        });

        if (rows.length === 0) return '';

        const headerIndex = rows.findIndex(row => row.isHeader);
        const headerRow = (headerIndex !== -1 ? rows[headerIndex] : rows[0]).cells;
        const dataRows = rows
            .filter((_, index) => index !== (headerIndex !== -1 ? headerIndex : 0))
            .map(row => row.cells);

        const separator = headerRow.map(() => '---');

        let markdown = '| ' + headerRow.join(' | ') + ' |\n';
        markdown += '| ' + separator.join(' | ') + ' |\n';
        dataRows.forEach(row => {
            markdown += '| ' + row.join(' | ') + ' |\n';
        });

        return '\n' + markdown + '\n';
    }
});

// Handle horizontal rules
turndownService.addRule('horizontalRule', {
    filter: 'hr',
    replacement: () => '\n---\n'
});

// Handle centered text (div, p, or heading with text-align: center)
turndownService.addRule('centeredText', {
    filter: function (node) {
        const element = node as HTMLElement;
        const nodeName = element.nodeName;
        const textAlign = element.style.textAlign;

        // Match DIV, P, or heading elements with center alignment
        return (nodeName === 'DIV' || nodeName === 'P' ||
            nodeName === 'H1' || nodeName === 'H2' || nodeName === 'H3' ||
            nodeName === 'H4' || nodeName === 'H5' || nodeName === 'H6') &&
            textAlign === 'center';
    },
    replacement: function (content) {
        // Indent each line by 4 spaces as per Material Encoding Spec §4(3)
        const lines = content.trim().split('\n');
        const indentedContent = lines.map(line => '    ' + line).join('\n');
        return '\n!!! center\n' + indentedContent + '\n';
    }
});

// Handle images - convert to standard markdown image syntax
// If the image has an attachment ID in the title attribute (set during insert),
// use /attachments/{id} instead of the data URL for proper storage
turndownService.addRule('images', {
    filter: 'img',
    replacement: function (_content, node) {
        const img = node as HTMLImageElement;
        // Sanitize alt text: remove newlines and excess whitespace (Word often adds these)
        const alt = (img.alt || '').replace(/[\r\n]+/g, ' ').replace(/\s+/g, ' ').trim();
        const title = img.title || '';
        let src = img.src || '';

        // If title contains an attachment ID and src is a data URL, use attachment path
        if (title && src.startsWith('data:')) {
            src = `/attachments/${title}`;
        }

        return `![${alt}](${src})`;
    }
});

// Handle question references - convert back to !!! question marker per Material Encoding Spec §4(4)
turndownService.addRule('questionRef', {
    filter: function (node) {
        const element = node as HTMLElement;
        const isDiv = element.nodeName === 'DIV';
        const hasAttr = element.hasAttribute('data-question-id');
        return isDiv && hasAttr;
    },
    replacement: function (_content, node) {
        const questionId = (node as HTMLElement).getAttribute('data-question-id');
        return `\n!!! question id="${questionId}"\n`;
    }
});

// Handle PDF embeds - convert back to !!! pdf marker per Material Encoding Spec §4(2)
turndownService.addRule('pdfEmbed', {
    filter: function (node) {
        const element = node as HTMLElement;
        return element.nodeName === 'DIV' && element.hasAttribute('data-pdf-id');
    },
    replacement: function (_content, node) {
        const pdfId = (node as HTMLElement).getAttribute('data-pdf-id');
        return `\n!!! pdf id="${pdfId}"\n`;
    }
});


// Prevent Turndown from stripping these custom divs (they have no visible content)
turndownService.keep(function (node) {
    const element = node as HTMLElement;
    return element.nodeName === 'DIV' &&
        (element.hasAttribute('data-question-id') || element.hasAttribute('data-pdf-id'));
});

/**
 * Convert HTML to Markdown per Material Encoding Specification.
 */
export function htmlToMarkdown(html: string): string {
    if (!html || html.trim() === '') return '';
    return turndownService.turndown(stripLinksFromHtml(html));
}

/**
 * Convert Markdown to HTML for TipTap editor.
 */
export function markdownToHtml(markdown: string): string {
    if (!markdown || markdown.trim() === '') return '';

    // Pre-process custom markers before marked conversion
    let processed = markdown;

    // Convert !!! center markers - mark content for post-processing
    // Use a unique placeholder that won't be affected by marked
    const centerBlocks: string[] = [];
    processed = processed.replace(
        /^!!! center\n((?:[ ]{4}.*\n?)*)/gm,
        (_match, content) => {
            const unindented = content.replace(/^[ ]{4}/gm, '');
            centerBlocks.push(unindented.trim());
            // Add newline after placeholder to ensure following content is at line start
            return `<!--CENTER_BLOCK_${centerBlocks.length - 1}-->\n`;
        }
    );

    // Convert !!! pdf markers to placeholder divs
    processed = processed.replace(
        /^!!! pdf id="([^"]+)"$/gm,
        '<div class="pdf-embed" data-pdf-id="$1">[PDF: $1]</div>'
    );

    // Convert block LaTeX $$...$$ to TipTap block latex nodes per §2(7)(b)
    // Must be processed before inline LaTeX to prevent $$ being matched by single $
    processed = processed.replace(
        /\$\$([\s\S]*?)\$\$/g,
        (_match, latex) => `<div data-block-latex="${latex.trim()}"></div>`
    );

    // Convert inline LaTeX $...$ to TipTap inline latex nodes per §2(7)(a)
    // Negative lookbehind/lookahead for $ to avoid matching $$ remnants
    processed = processed.replace(
        /(?<!\$)\$(?!\$)([^$]+?)\$(?!\$)/g,
        (_match, latex) => `<span data-latex="${latex.trim()}"></span>`
    );

    // Convert !!! question markers to placeholder divs
    processed = processed.replace(
        /^!!! question id="([^"]+)"$/gm,
        '<div class="question-embed" data-question-id="$1">[Question: $1]</div>'
    );

    // Convert using marked
    let html = marked.parse(processed, { async: false }) as string;

    // Post-process center blocks: convert each placeholder to centered content
    // Parse each center block through marked separately and apply text-align to block elements
    html = html.replace(
        /<!--CENTER_BLOCK_(\d+)-->/g,
        (_match, index) => {
            const blockContent = centerBlocks[parseInt(index)];
            // Parse the center block content
            let blockHtml = marked.parse(blockContent, { async: false }) as string;
            // Apply text-align: center to all block-level elements (p, h1-h6)
            blockHtml = blockHtml.replace(
                /<(p|h[1-6])([^>]*)>/g,
                (_tagMatch, tagName, attrs) => {
                    let newAttrs = attrs || '';
                    const styleMatch = newAttrs.match(/\sstyle\s*=\s*"([^"]*)"/i);
                    if (styleMatch) {
                        const existingStyle = styleMatch[1];
                        const updatedStyle = (existingStyle.trim().endsWith(';') || existingStyle.trim() === '')
                            ? existingStyle + ' text-align: center;'
                            : existingStyle + '; text-align: center;';
                        newAttrs = newAttrs.replace(styleMatch[1], updatedStyle);
                    } else {
                        newAttrs = (newAttrs || '') + ' style="text-align: center;"';
                    }
                    return `<${tagName}${newAttrs}>`;
                }
            );
            return blockHtml;
        }
    );

    return stripLinksFromHtml(html);
}

/**
 * Convert Markdown to HTML for streaming preview display.
 * Per FrontendWorkflowSpecifications §4B(2)(a1)(ii): renders content tokens
 * "in a manner consistent with the editor's rendering capabilities".
 *
 * Unlike `markdownToHtml` (which emits TipTap data-attributes for LaTeX/questions),
 * this function produces self-contained HTML with:
 *  - KaTeX-rendered LaTeX
 *  - question-draft preview cards matching editor styling
 *  - standard Markdown formatting (headers, bold, tables, etc.)
 *
 * Tolerant of incomplete Markdown mid-stream: `marked` renders what it can and
 * passes through unparsed syntax as text.
 */
export function markdownToStreamingHtml(markdown: string): string {
    if (!markdown || markdown.trim() === '') return '';

    let processed = markdown;

    // --- Question-draft preview cards (GenAISpec Appendix C) ---
    // Must run before marked to avoid interference with indented content.
    // Matches complete question-draft blocks (header + indented body + blank line or EOF).
    processed = processed.replace(
        /^!!! question-draft type="(MULTIPLE_CHOICE|WRITTEN_ANSWER)"\n((?:[ ]{4}.*\n?)*)/gm,
        (_match, type: string, body: string) => {
            return buildQuestionDraftHtml(type, body);
        }
    );

    // --- Incomplete question-draft (streaming: header arrived but body still incoming) ---
    processed = processed.replace(
        /^!!! question-draft type="(MULTIPLE_CHOICE|WRITTEN_ANSWER)"$/gm,
        (_match, type: string) => {
            const label = type === 'MULTIPLE_CHOICE' ? 'Multiple Choice' : 'Written Answer';
            return `<div class="question-draft-preview question-draft-partial">` +
                `<div class="question-draft-header">` +
                `<span class="question-draft-badge">📝 Draft</span>` +
                `<span class="question-draft-type">${label}</span>` +
                `</div>` +
                `<p class="question-draft-loading">Generating question…</p>` +
                `</div>`;
        }
    );

    // --- Center blocks ---
    const centerBlocks: string[] = [];
    processed = processed.replace(
        /^!!! center\n((?:[ ]{4}.*\n?)*)/gm,
        (_match, content) => {
            const unindented = content.replace(/^[ ]{4}/gm, '');
            centerBlocks.push(unindented.trim());
            return `<!--CENTER_BLOCK_${centerBlocks.length - 1}-->\n`;
        }
    );

    // --- PDF embeds ---
    processed = processed.replace(
        /^!!! pdf id="([^"]+)"$/gm,
        '<div class="streaming-pdf-placeholder">[PDF Attachment]</div>'
    );

    // --- Question embeds (already-persisted, unlikely during stream but handle gracefully) ---
    processed = processed.replace(
        /^!!! question id="([^"]+)"$/gm,
        '<div class="streaming-question-placeholder">[Embedded Question]</div>'
    );

    // --- Block LaTeX $$...$$ → KaTeX HTML ---
    processed = processed.replace(
        /\$\$([\s\S]*?)\$\$/g,
        (_match, latex: string) => {
            return renderKatexSafe(latex.trim(), true);
        }
    );

    // --- Inline LaTeX $...$ → KaTeX HTML ---
    processed = processed.replace(
        /(?<!\$)\$(?!\$)([^$]+?)\$(?!\$)/g,
        (_match, latex: string) => {
            return renderKatexSafe(latex.trim(), false);
        }
    );

    // --- Run marked for standard Markdown ---
    let html = marked.parse(processed, { async: false }) as string;

    // --- Post-process center blocks ---
    html = html.replace(
        /<!--CENTER_BLOCK_(\d+)-->/g,
        (_match, index) => {
            const blockContent = centerBlocks[parseInt(index)];
            let blockHtml = marked.parse(blockContent, { async: false }) as string;
            blockHtml = blockHtml.replace(
                /<(p|h[1-6])([^>]*)>/g,
                (_tagMatch, tagName, attrs) => {
                    let newAttrs = attrs || '';
                    const styleMatch = newAttrs.match(/\sstyle\s*=\s*"([^"]*)"/i);
                    if (styleMatch) {
                        const updatedStyle = styleMatch[1].trim().endsWith(';')
                            ? styleMatch[1] + ' text-align: center;'
                            : styleMatch[1] + '; text-align: center;';
                        newAttrs = newAttrs.replace(styleMatch[1], updatedStyle);
                    } else {
                        newAttrs = (newAttrs || '') + ' style="text-align: center;"';
                    }
                    return `<${tagName}${newAttrs}>`;
                }
            );
            return blockHtml;
        }
    );

    return html;
}

/**
 * Render a LaTeX string via KaTeX, returning HTML.
 * Falls back to escaped source text on parse errors.
 */
function renderKatexSafe(latex: string, displayMode: boolean): string {
    try {
        // Dynamic import not possible synchronously; katex is available via the
        // same bundle that loads extensions.tsx. Use a deferred require.
        // eslint-disable-next-line @typescript-eslint/no-var-requires
        const katex = require('katex');
        return katex.renderToString(latex, { throwOnError: false, displayMode });
    } catch {
        // If KaTeX isn't available (shouldn't happen), return raw text
        const escaped = latex.replace(/</g, '&lt;').replace(/>/g, '&gt;');
        return displayMode ? `<div class="katex-fallback">$$${escaped}$$</div>`
            : `<span class="katex-fallback">$${escaped}$</span>`;
    }
}

/**
 * Build an HTML preview card for a `!!! question-draft` block.
 * Styling mirrors the editor's QuestionRef component (extensions.tsx).
 */
function buildQuestionDraftHtml(type: string, body: string): string {
    const lines = body.replace(/^[ ]{4}/gm, '').split('\n');
    const label = type === 'MULTIPLE_CHOICE' ? 'Multiple Choice' : 'Written Answer';

    let text = '';
    let maxScore = '';
    let correctAnswer = '';
    let markScheme = '';
    let correctIndex = -1;
    const options: string[] = [];
    let inOptions = false;

    for (const line of lines) {
        const trimmed = line.trim();
        if (trimmed === '') { inOptions = false; continue; }

        const textMatch = trimmed.match(/^text:\s*"(.+)"$/);
        if (textMatch) { text = textMatch[1]; inOptions = false; continue; }

        const scoreMatch = trimmed.match(/^max_score:\s*(\d+)$/);
        if (scoreMatch) { maxScore = scoreMatch[1]; inOptions = false; continue; }

        const correctMatch = trimmed.match(/^correct:\s*(\d+)$/);
        if (correctMatch) { correctIndex = parseInt(correctMatch[1]); inOptions = false; continue; }

        const answerMatch = trimmed.match(/^correct_answer:\s*"(.+)"$/);
        if (answerMatch) { correctAnswer = answerMatch[1]; inOptions = false; continue; }

        const schemeMatch = trimmed.match(/^mark_scheme:\s*"(.+)"$/);
        if (schemeMatch) { markScheme = schemeMatch[1]; inOptions = false; continue; }

        if (trimmed === 'options:') { inOptions = true; continue; }

        if (inOptions) {
            const optMatch = trimmed.match(/^-\s*"(.+)"$/);
            if (optMatch) { options.push(optMatch[1]); }
        }
    }

    // --- Build HTML ---
    let html = `<div class="question-draft-preview">`;

    // Header
    html += `<div class="question-draft-header">`;
    html += `<span class="question-draft-badge">📝 Draft</span>`;
    html += `<span class="question-draft-type">${label}</span>`;
    if (maxScore) {
        html += `<span class="question-draft-score">${maxScore} ${maxScore === '1' ? 'mark' : 'marks'}</span>`;
    }
    html += `</div>`;

    // Question text
    if (text) {
        html += `<p class="question-draft-text">${escapeHtml(text)}</p>`;
    }

    // MC options
    if (type === 'MULTIPLE_CHOICE' && options.length > 0) {
        html += `<div class="question-draft-options">`;
        options.forEach((opt, i) => {
            const isCorrect = i === correctIndex;
            const cls = isCorrect ? 'question-draft-option question-draft-option-correct' : 'question-draft-option';
            const letter = String.fromCharCode(65 + i);
            html += `<div class="${cls}">`;
            html += `<span class="question-draft-option-letter ${isCorrect ? 'correct' : ''}">${letter}</span>`;
            html += `<span>${escapeHtml(opt)}</span>`;
            if (isCorrect) html += `<span class="question-draft-correct-mark">✓ Correct</span>`;
            html += `</div>`;
        });
        html += `</div>`;
    }

    // Written answer - correct answer
    if (type === 'WRITTEN_ANSWER' && correctAnswer) {
        html += `<div class="question-draft-answer">`;
        html += `<p class="question-draft-answer-label">Correct Answer:</p>`;
        html += `<p class="question-draft-answer-value">${escapeHtml(correctAnswer)}</p>`;
        html += `</div>`;
    }

    // Written answer - mark scheme
    if (type === 'WRITTEN_ANSWER' && markScheme) {
        html += `<div class="question-draft-markscheme">`;
        html += `<p class="question-draft-markscheme-label">Mark Scheme (AI-marking):</p>`;
        html += `<p class="question-draft-markscheme-value">${escapeHtml(markScheme)}</p>`;
        html += `</div>`;
    }

    html += `</div>`;
    return html;
}

/** Escape HTML special characters. */
function escapeHtml(s: string): string {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

/**
 * Process custom markers in content for display.
 * Used to render PDF embeds and question references.
 */
export function processCustomMarkers(content: string): string {
    // This function can be extended to handle dynamic rendering
    // of PDF embeds and question references
    return content;
}
