/**
 * Custom TipTap Extensions for Material Encoding Specification.
 * Implements LaTeX, PDF embed, question reference, and centered text nodes.
 */

import { Node, mergeAttributes } from '@tiptap/core';
import { ReactNodeViewRenderer, NodeViewWrapper, NodeViewProps } from '@tiptap/react';
import React from 'react';
import katex from 'katex';
import CodeBlockExt from '@tiptap/extension-code-block';
import 'katex/dist/katex.min.css';

// ============ Code Block with Language Support ============

export const CodeBlockWithLanguage = CodeBlockExt.extend({
    addAttributes() {
        return {
            ...this.parent?.(),
            language: {
                default: null,
                parseHTML: element => {
                    // Parse from data-language attribute or class="language-xxx"
                    const dataLang = element.getAttribute('data-language');
                    if (dataLang) return dataLang;
                    
                    const classMatch = element.className?.match(/language-(\w+)/);
                    return classMatch?.[1] || null;
                },
                renderHTML: attributes => {
                    if (!attributes.language) return {};
                    return {
                        'data-language': attributes.language,
                        class: `language-${attributes.language}`,
                    };
                },
            },
        };
    },
});

// ============ Inline LaTeX Extension ============

const InlineLatexComponent: React.FC<NodeViewProps> = ({ node }) => {
    const latex = node.attrs.latex as string;
    let html = '';

    try {
        html = katex.renderToString(latex, {
            throwOnError: false,
            displayMode: false,
        });
    } catch (e) {
        html = `<span class="latex-error">${latex}</span>`;
    }

    return (
        <NodeViewWrapper as="span" className="inline-latex">
            <span dangerouslySetInnerHTML={{ __html: html }} />
        </NodeViewWrapper>
    );
};

export const InlineLatex = Node.create({
    name: 'inlineLatex',
    group: 'inline',
    inline: true,
    atom: true,

    addAttributes() {
        return {
            latex: {
                default: '',
            },
        };
    },

    parseHTML() {
        return [
            {
                tag: 'span[data-latex]',
                getAttrs: (node: HTMLElement) => ({
                    latex: node.getAttribute('data-latex'),
                }),
            },
        ];
    },

    renderHTML({ HTMLAttributes }) {
        return ['span', mergeAttributes(HTMLAttributes, { 'data-latex': HTMLAttributes.latex }), 0];
    },

    addNodeView() {
        return ReactNodeViewRenderer(InlineLatexComponent);
    },
});

// ============ Block LaTeX Extension ============

const BlockLatexComponent: React.FC<NodeViewProps> = ({ node }) => {
    const latex = node.attrs.latex as string;
    let html = '';

    try {
        html = katex.renderToString(latex, {
            throwOnError: false,
            displayMode: true,
        });
    } catch (e) {
        html = `<div class="latex-error">${latex}</div>`;
    }

    return (
        <NodeViewWrapper className="block-latex my-4 text-center">
            <div dangerouslySetInnerHTML={{ __html: html }} />
        </NodeViewWrapper>
    );
};

export const BlockLatex = Node.create({
    name: 'blockLatex',
    group: 'block',
    atom: true,

    addAttributes() {
        return {
            latex: {
                default: '',
            },
        };
    },

    parseHTML() {
        return [
            {
                tag: 'div[data-block-latex]',
                getAttrs: (node: HTMLElement) => ({
                    latex: node.getAttribute('data-block-latex'),
                }),
            },
        ];
    },

    renderHTML({ HTMLAttributes }) {
        return ['div', mergeAttributes(HTMLAttributes, { 'data-block-latex': HTMLAttributes.latex }), 0];
    },

    addNodeView() {
        return ReactNodeViewRenderer(BlockLatexComponent);
    },
});

// ============ Question Reference Extension ============

const QuestionRefComponent: React.FC<NodeViewProps> = ({ node }) => {
    const questionId = node.attrs.id as string;

    return (
        <NodeViewWrapper className="question-ref my-4 p-4 border-2 border-brand-orange rounded-lg bg-orange-50">
            <div className="flex items-center gap-2">
                <span className="text-brand-orange font-semibold">📝 Question</span>
                <span className="text-xs text-gray-500 font-mono">{questionId.slice(0, 8)}...</span>
            </div>
            <p className="text-sm text-gray-600 mt-2">
                [Question will be rendered here based on QuestionEntity]
            </p>
        </NodeViewWrapper>
    );
};

export const QuestionRef = Node.create({
    name: 'questionRef',
    group: 'block',
    atom: true,

    addAttributes() {
        return {
            id: {
                default: '',
            },
        };
    },

    parseHTML() {
        return [
            {
                tag: 'div[data-question-id]',
                getAttrs: (node: HTMLElement) => ({
                    id: node.getAttribute('data-question-id'),
                }),
            },
        ];
    },

    renderHTML({ HTMLAttributes }) {
        return ['div', mergeAttributes(HTMLAttributes, {
            'data-question-id': HTMLAttributes.id,
            class: 'question-embed'
        }), 0];
    },

    addNodeView() {
        return ReactNodeViewRenderer(QuestionRefComponent);
    },
});

// ============ PDF Embed Extension ============

const PdfEmbedComponent: React.FC<NodeViewProps> = ({ node }) => {
    const pdfId = node.attrs.id as string;

    return (
        <NodeViewWrapper className="pdf-embed my-4 p-4 border-2 border-blue-400 rounded-lg bg-blue-50">
            <div className="flex items-center gap-2">
                <span className="text-blue-600 font-semibold">📄 PDF Document</span>
                <span className="text-xs text-gray-500 font-mono">{pdfId.slice(0, 8)}...</span>
            </div>
            <p className="text-sm text-gray-600 mt-2">
                [PDF viewer will load from /attachments/{pdfId}]
            </p>
        </NodeViewWrapper>
    );
};

export const PdfEmbed = Node.create({
    name: 'pdfEmbed',
    group: 'block',
    atom: true,

    addAttributes() {
        return {
            id: {
                default: '',
            },
        };
    },

    parseHTML() {
        return [
            {
                tag: 'div[data-pdf-id]',
                getAttrs: (node: HTMLElement) => ({
                    id: node.getAttribute('data-pdf-id'),
                }),
            },
        ];
    },

    renderHTML({ HTMLAttributes }) {
        return ['div', mergeAttributes(HTMLAttributes, {
            'data-pdf-id': HTMLAttributes.id,
            class: 'pdf-embed'
        }), 0];
    },

    addNodeView() {
        return ReactNodeViewRenderer(PdfEmbedComponent);
    },
});

// ============ Centered Text Extension ============

const CenteredTextComponent: React.FC<NodeViewProps> = ({ node, children }) => {
    return (
        <NodeViewWrapper className="centered-text text-center my-4">
            {children}
        </NodeViewWrapper>
    );
};

export const CenteredText = Node.create({
    name: 'centeredText',
    group: 'block',
    content: 'block+',

    parseHTML() {
        return [
            {
                tag: 'div[data-centered]',
            },
            {
                tag: 'div',
                getAttrs: (node: HTMLElement) => {
                    return node.style.textAlign === 'center' ? {} : false;
                },
            },
        ];
    },

    renderHTML({ HTMLAttributes }) {
        return ['div', mergeAttributes(HTMLAttributes, {
            'data-centered': 'true',
            style: 'text-align: center;'
        }), 0];
    },

    addNodeView() {
        return ReactNodeViewRenderer(CenteredTextComponent);
    },
});
