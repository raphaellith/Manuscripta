/**
 * Markdown Conversion Utilities.
 * Converts between HTML (TipTap internal format) and Markdown (Material Encoding Spec).
 * Per Material Encoding Specification.
 */

import TurndownService from 'turndown';
import { marked } from 'marked';

// Configure Turndown for HTML → Markdown conversion
const turndownService = new TurndownService({
    headingStyle: 'atx',
    codeBlockStyle: 'fenced',
    bulletListMarker: '-',
    emDelimiter: '*',
    strongDelimiter: '**',
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

// Handle images - convert to attachment syntax
turndownService.addRule('images', {
    filter: 'img',
    replacement: function (_content, node) {
        const img = node as HTMLImageElement;
        const alt = img.alt || '';
        const src = img.src || '';
        // If it's an attachment URL, use as-is; otherwise wrap
        if (src.includes('/attachments/')) {
            return `![${alt}](${src})`;
        }
        return `![${alt}](${src})`;
    }
});

/**
 * Convert HTML to Markdown per Material Encoding Specification.
 */
export function htmlToMarkdown(html: string): string {
    if (!html || html.trim() === '') return '';
    return turndownService.turndown(html);
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
        /^!!! center\n((?:    .*\n?)*)/gm,
        (_match, content) => {
            const unindented = content.replace(/^    /gm, '');
            centerBlocks.push(unindented.trim());
            return `<!--CENTER_BLOCK_${centerBlocks.length - 1}-->`;
        }
    );

    // Convert !!! pdf markers to placeholder divs
    processed = processed.replace(
        /^!!! pdf id="([^"]+)"$/gm,
        '<div class="pdf-embed" data-pdf-id="$1">[PDF: $1]</div>'
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

    return html;
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
