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

// Custom event types for question actions
export interface QuestionEditEvent {
    questionId: string;
}
export interface QuestionDeleteEvent {
    questionId: string;
}

// Declare custom events
declare global {
    interface WindowEventMap {
        'question-edit': CustomEvent<QuestionEditEvent>;
        'question-delete': CustomEvent<QuestionDeleteEvent>;
    }
}

interface QuestionRefAttrs {
    id: string;
    questionText?: string;
    questionType?: 'MULTIPLE_CHOICE' | 'WRITTEN_ANSWER';
    options?: string[];
    correctAnswer?: number | string;
    maxScore?: number;
    materialType?: 'READING' | 'WORKSHEET' | 'POLL';
}

const QuestionRefComponent: React.FC<NodeViewProps> = ({ node, deleteNode }) => {
    const attrs = node.attrs as QuestionRefAttrs;
    const { id, questionText, questionType, options, correctAnswer, maxScore, materialType } = attrs;

    const handleEdit = () => {
        window.dispatchEvent(new CustomEvent('question-edit', {
            detail: { questionId: id }
        }));
    };

    const handleDelete = () => {
        // Confirm before delete
        if (window.confirm('Delete this question?')) {
            window.dispatchEvent(new CustomEvent('question-delete', {
                detail: { questionId: id }
            }));
            deleteNode();
        }
    };

    // If no question data yet, show loading state
    if (!questionText) {
        return (
            <NodeViewWrapper className="question-ref my-4 p-4 border-2 border-brand-orange rounded-lg bg-orange-50">
                <div className="flex items-center gap-2">
                    <span className="text-brand-orange font-semibold">📝 Question</span>
                    <span className="text-xs text-gray-500 font-mono">{id?.slice(0, 8)}...</span>
                </div>
                <p className="text-sm text-gray-500 mt-2 italic">Loading question data...</p>
            </NodeViewWrapper>
        );
    }

    // Hide delete for POLL materials per s4C(3)(c)
    const showDelete = materialType !== 'POLL';

    return (
        <NodeViewWrapper className="question-ref my-4 p-4 border-2 border-brand-orange rounded-lg bg-orange-50">
            {/* Header with edit/delete actions */}
            <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                    <span className="text-brand-orange font-semibold">📝 Question</span>
                    <span className="px-2 py-0.5 text-xs bg-orange-200 text-orange-700 rounded">
                        {questionType === 'MULTIPLE_CHOICE' ? 'Multiple Choice' : 'Written Answer'}
                    </span>
                    {maxScore && (
                        <span className="text-xs text-gray-500">{maxScore} {maxScore === 1 ? 'mark' : 'marks'}</span>
                    )}
                </div>
                <div className="flex items-center gap-1">
                    <button
                        onClick={handleEdit}
                        className="p-1.5 text-gray-500 hover:text-brand-orange hover:bg-orange-100 rounded transition-colors"
                        title="Edit question"
                    >
                        ✏️
                    </button>
                    {showDelete && (
                        <button
                            onClick={handleDelete}
                            className="p-1.5 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded transition-colors"
                            title="Delete question"
                        >
                            🗑️
                        </button>
                    )}
                </div>
            </div>

            {/* Question text */}
            <p className="text-gray-800 font-medium mb-3">{questionText}</p>

            {/* Multiple choice options */}
            {questionType === 'MULTIPLE_CHOICE' && options && options.length > 0 && (
                <div className="space-y-2 ml-2">
                    {options.map((option, index) => (
                        <div
                            key={index}
                            className={`flex items-center gap-2 p-2 rounded ${correctAnswer === index
                                ? 'bg-green-100 border border-green-300'
                                : 'bg-white border border-gray-200'
                                }`}
                        >
                            <span className={`w-6 h-6 flex items-center justify-center rounded-full text-xs font-medium ${correctAnswer === index
                                ? 'bg-green-500 text-white'
                                : 'bg-gray-200 text-gray-600'
                                }`}>
                                {String.fromCharCode(65 + index)}
                            </span>
                            <span className={correctAnswer === index ? 'text-green-800' : 'text-gray-700'}>
                                {option}
                            </span>
                            {correctAnswer === index && (
                                <span className="ml-auto text-green-600 text-xs">✓ Correct</span>
                            )}
                        </div>
                    ))}
                </div>
            )}

            {/* Written answer sample */}
            {questionType === 'WRITTEN_ANSWER' && correctAnswer && typeof correctAnswer === 'string' && (
                <div className="mt-2 p-3 bg-blue-50 border border-blue-200 rounded">
                    <p className="text-xs text-blue-600 font-medium mb-1">Correct Answer:</p>
                    <p className="text-sm text-blue-800">{correctAnswer}</p>
                </div>
            )}
        </NodeViewWrapper>
    );
};

export const QuestionRef = Node.create({
    name: 'questionRef',
    group: 'block',
    atom: true,

    addAttributes() {
        return {
            id: { default: '' },
            questionText: { default: '' },
            questionType: { default: 'MULTIPLE_CHOICE' },
            options: { default: [] },
            correctAnswer: { default: null },
            maxScore: { default: 1 },
            materialType: { default: 'WORKSHEET' },
        };
    },

    parseHTML() {
        return [
            {
                tag: 'div[data-question-id]',
                getAttrs: (node: HTMLElement) => ({
                    id: node.getAttribute('data-question-id'),
                    // Other attrs are loaded dynamically
                }),
            },
        ];
    },

    renderHTML({ HTMLAttributes }) {
        return ['div', mergeAttributes(HTMLAttributes, {
            'data-question-id': HTMLAttributes.id,
            class: 'question-embed'
        })];
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
        })];
    },

    addNodeView() {
        return ReactNodeViewRenderer(PdfEmbedComponent);
    },
});

