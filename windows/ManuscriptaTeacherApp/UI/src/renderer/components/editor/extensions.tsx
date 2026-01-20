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

// ============ Attachment Image Extension ============
// Custom image extension with delete button and keyboard deletion prevention per §4(4)(c)

const AttachmentImageComponent: React.FC<NodeViewProps> = ({ node, deleteNode }) => {
    const { src, alt, title } = node.attrs as { src: string; alt: string; title: string };
    // title contains the attachment UUID

    const handleDelete = () => {
        if (window.confirm('Delete this image attachment?')) {
            // Dispatch event for EditorModal to handle entity+file deletion per §4(4)(b)
            window.dispatchEvent(new CustomEvent('attachment-delete', {
                detail: { attachmentId: title, fileExtension: 'image' }
            }));
            deleteNode();
        }
    };

    return (
        <NodeViewWrapper className="attachment-image my-2 inline-block relative group">
            <div className="relative inline-block">
                <img
                    src={src}
                    alt={alt || 'Attachment'}
                    className="max-w-full h-auto rounded border border-gray-300"
                    style={{ maxHeight: '400px' }}
                />
                {/* Delete button - visible on hover */}
                <button
                    onClick={handleDelete}
                    className="absolute top-2 right-2 p-1.5 bg-red-500 text-white rounded-full opacity-0 group-hover:opacity-100 transition-opacity hover:bg-red-600"
                    title="Delete image"
                >
                    🗑️
                </button>
            </div>
        </NodeViewWrapper>
    );
};

export const AttachmentImage = Node.create({
    name: 'image',
    group: 'inline',
    inline: true,
    atom: true,
    // Prevent selection and dragging to block keyboard deletion
    selectable: false,
    draggable: false,

    addAttributes() {
        return {
            src: { default: null },
            alt: { default: null },
            title: { default: null },
        };
    },

    parseHTML() {
        return [
            {
                tag: 'img[src]',
            },
        ];
    },

    renderHTML({ HTMLAttributes }) {
        return ['img', mergeAttributes(HTMLAttributes)];
    },

    addNodeView() {
        return ReactNodeViewRenderer(AttachmentImageComponent);
    },

    // Prevent deletion via keyboard shortcuts
    addKeyboardShortcuts() {
        return {
            Backspace: () => {
                // Check if selection is at/near an image node
                const { state } = this.editor;
                const { selection } = state;
                const node = state.doc.nodeAt(selection.from - 1);
                if (node?.type.name === 'image') {
                    return true; // Prevent default behavior
                }
                return false;
            },
            Delete: () => {
                const { state } = this.editor;
                const { selection } = state;
                const node = state.doc.nodeAt(selection.from);
                if (node?.type.name === 'image') {
                    return true; // Prevent default behavior
                }
                return false;
            },
        };
    },
});

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
    markScheme?: string;
    maxScore?: number;
    materialType?: 'READING' | 'WORKSHEET' | 'POLL';
}

const QuestionRefComponent: React.FC<NodeViewProps> = ({ node, deleteNode }) => {
    const attrs = node.attrs as QuestionRefAttrs;
    const { id, questionText, questionType, options, correctAnswer, markScheme, maxScore, materialType } = attrs;

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

            {/* Mark scheme for AI-marking */}
            {questionType === 'WRITTEN_ANSWER' && markScheme && (
                <div className="mt-2 p-3 bg-purple-50 border border-purple-200 rounded">
                    <p className="text-xs text-purple-600 font-medium mb-1">Mark Scheme (AI-marking):</p>
                    <p className="text-sm text-purple-800 whitespace-pre-wrap">{markScheme}</p>
                </div>
            )}
        </NodeViewWrapper>
    );
};

export const QuestionRef = Node.create({
    name: 'questionRef',
    group: 'block',
    atom: true,
    // Prevent selection to block keyboard deletion
    selectable: false,
    draggable: false,

    addAttributes() {
        return {
            id: { default: '' },
            questionText: { default: '' },
            questionType: { default: 'MULTIPLE_CHOICE' },
            options: { default: [] },
            correctAnswer: { default: null },
            markScheme: { default: null },
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

    // Prevent deletion via keyboard shortcuts per §4(3)(d)
    addKeyboardShortcuts() {
        return {
            Backspace: () => {
                const { state } = this.editor;
                const { selection } = state;
                // Check positions around selection for questionRef nodes
                for (let i = -2; i <= 1; i++) {
                    const pos = selection.from + i;
                    if (pos >= 0 && pos < state.doc.content.size) {
                        const node = state.doc.nodeAt(pos);
                        if (node?.type.name === 'questionRef') {
                            return true; // Prevent default behavior
                        }
                    }
                }
                // Also check if we're inside or right after a questionRef
                const $from = state.doc.resolve(selection.from);
                for (let d = $from.depth; d >= 0; d--) {
                    if ($from.node(d).type.name === 'questionRef') {
                        return true;
                    }
                }
                return false;
            },
            Delete: () => {
                const { state } = this.editor;
                const { selection } = state;
                // Check positions around selection for questionRef nodes
                for (let i = 0; i <= 2; i++) {
                    const pos = selection.from + i;
                    if (pos >= 0 && pos < state.doc.content.size) {
                        const node = state.doc.nodeAt(pos);
                        if (node?.type.name === 'questionRef') {
                            return true; // Prevent default behavior
                        }
                    }
                }
                // Also check if we're inside a questionRef
                const $from = state.doc.resolve(selection.from);
                for (let d = $from.depth; d >= 0; d--) {
                    if ($from.node(d).type.name === 'questionRef') {
                        return true;
                    }
                }
                return false;
            },
        };
    },
});

// ============ PDF Embed Extension ============

const PdfEmbedComponent: React.FC<NodeViewProps> = ({ node, deleteNode }) => {
    const pdfId = node.attrs.id as string;
    const [pdfDataUrl, setPdfDataUrl] = React.useState<string | null>(null);
    const [loading, setLoading] = React.useState(true);
    const [error, setError] = React.useState<string | null>(null);
    const [numPages, setNumPages] = React.useState<number>(0);
    const [pageNumber, setPageNumber] = React.useState<number>(1);

    // Dynamically import react-pdf to avoid SSR issues
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const [PdfComponents, setPdfComponents] = React.useState<{
        Document: React.ComponentType<React.PropsWithChildren<{ file: string; onLoadSuccess: (data: { numPages: number }) => void; onLoadError: (error: Error) => void; loading: React.ReactNode }>>;
        Page: React.ComponentType<{ pageNumber: number; width: number; renderTextLayer: boolean; renderAnnotationLayer: boolean }>;
    } | null>(null);

    React.useEffect(() => {
        // Import react-pdf dynamically
        import('react-pdf').then((pdfjs) => {
            // Configure worker from CDN with https protocol
            pdfjs.pdfjs.GlobalWorkerOptions.workerSrc = `https://unpkg.com/pdfjs-dist@${pdfjs.pdfjs.version}/build/pdf.worker.min.mjs`;
            setPdfComponents({
                Document: pdfjs.Document,
                Page: pdfjs.Page,
            });
        }).catch(err => {
            console.error('Failed to load react-pdf:', err);
            setError('Failed to load PDF viewer');
        });
    }, []);

    // Load PDF data URL on mount
    React.useEffect(() => {
        const loadPdf = async () => {
            try {
                setLoading(true);
                const dataUrl = await window.electronAPI.getAttachmentDataUrl(pdfId, 'pdf');
                if (dataUrl) {
                    setPdfDataUrl(dataUrl);
                } else {
                    setError('PDF file not found');
                }
            } catch (err) {
                console.error('Failed to load PDF:', err);
                setError('Failed to load PDF');
            } finally {
                setLoading(false);
            }
        };

        if (pdfId) {
            loadPdf();
        }
    }, [pdfId]);

    const handleDelete = () => {
        if (window.confirm('Delete this PDF attachment?')) {
            // Dispatch event for EditorModal to handle entity+file deletion per §4(4)(b)
            window.dispatchEvent(new CustomEvent('attachment-delete', {
                detail: { attachmentId: pdfId, fileExtension: 'pdf' }
            }));
            deleteNode();
        }
    };

    const onDocumentLoadSuccess = ({ numPages }: { numPages: number }) => {
        setNumPages(numPages);
        setPageNumber(1);
    };

    const onDocumentLoadError = (err: Error) => {
        console.error('PDF load error:', err);
        setError('Failed to render PDF');
    };

    const goToPrevPage = () => setPageNumber(prev => Math.max(prev - 1, 1));
    const goToNextPage = () => setPageNumber(prev => Math.min(prev + 1, numPages));

    return (
        <NodeViewWrapper className="pdf-embed my-4 border-2 border-blue-400 rounded-lg bg-blue-50 overflow-hidden">
            {/* Header */}
            <div className="flex items-center justify-between p-3 bg-blue-100 border-b border-blue-300">
                <div className="flex items-center gap-2">
                    <span className="text-blue-600 font-semibold">📄 PDF Document</span>
                    {numPages > 0 && (
                        <span className="text-xs text-gray-600">
                            Page {pageNumber} of {numPages}
                        </span>
                    )}
                </div>
                <div className="flex items-center gap-2">
                    {numPages > 1 && (
                        <>
                            <button
                                onClick={goToPrevPage}
                                disabled={pageNumber <= 1}
                                className="px-2 py-1 text-sm bg-white border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                ←
                            </button>
                            <button
                                onClick={goToNextPage}
                                disabled={pageNumber >= numPages}
                                className="px-2 py-1 text-sm bg-white border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                →
                            </button>
                        </>
                    )}
                    <button
                        onClick={handleDelete}
                        className="p-1.5 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded transition-colors"
                        title="Delete PDF"
                    >
                        🗑️
                    </button>
                </div>
            </div>

            {/* PDF Viewer */}
            <div className="p-2 flex justify-center bg-gray-100">
                {loading && (
                    <div className="h-96 flex items-center justify-center text-gray-500">
                        <span className="animate-pulse">Loading PDF...</span>
                    </div>
                )}
                {error && (
                    <div className="h-32 flex items-center justify-center text-red-500">
                        {error}
                    </div>
                )}
                {!loading && !error && pdfDataUrl && PdfComponents && (
                    <PdfComponents.Document
                        file={pdfDataUrl}
                        onLoadSuccess={onDocumentLoadSuccess}
                        onLoadError={onDocumentLoadError}
                        loading={<div className="h-96 flex items-center justify-center"><span className="animate-pulse">Rendering PDF...</span></div>}
                    >
                        <PdfComponents.Page
                            pageNumber={pageNumber}
                            width={600}
                            renderTextLayer={false}
                            renderAnnotationLayer={false}
                        />
                    </PdfComponents.Document>
                )}
            </div>
        </NodeViewWrapper>
    );
};

export const PdfEmbed = Node.create({
    name: 'pdfEmbed',
    group: 'block',
    atom: true,
    // Prevent selection to block keyboard deletion
    selectable: false,
    draggable: false,

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

    // Prevent deletion via keyboard shortcuts per §4(4)(c)
    addKeyboardShortcuts() {
        return {
            Backspace: () => {
                const { state } = this.editor;
                const { selection } = state;
                const node = state.doc.nodeAt(selection.from - 1);
                if (node?.type.name === 'pdfEmbed') {
                    return true; // Prevent default behavior
                }
                return false;
            },
            Delete: () => {
                const { state } = this.editor;
                const { selection } = state;
                const node = state.doc.nodeAt(selection.from);
                if (node?.type.name === 'pdfEmbed') {
                    return true; // Prevent default behavior
                }
                return false;
            },
        };
    },
});

