/**
 * Editor Modal - True WYSIWYG editor for material content.
 * Uses TipTap for rich text editing with custom extensions.
 * Stores content as Markdown per Material Encoding Specification.
 * Per Frontend Workflow Spec §4C.
 */

import React, { useState, useEffect, useCallback, useRef } from 'react';
import ReactDOM from 'react-dom';
import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Placeholder from '@tiptap/extension-placeholder';
import Gapcursor from '@tiptap/extension-gapcursor';
import TableExt from '@tiptap/extension-table';
import TableRowExt from '@tiptap/extension-table-row';
import TableCellExt from '@tiptap/extension-table-cell';
import TableHeaderExt from '@tiptap/extension-table-header';
import Image from '@tiptap/extension-image';
import Link from '@tiptap/extension-link';
import TextAlign from '@tiptap/extension-text-align';
import Underline from '@tiptap/extension-underline';
import { InlineLatex, BlockLatex, QuestionRef, PdfEmbed } from './extensions';
import { htmlToMarkdown, markdownToHtml } from '../../utils/markdownConversion';
import type { MaterialEntity } from '../../models';
import { useAppContext } from '../../state/AppContext';
import 'katex/dist/katex.min.css';

interface EditorModalProps {
    material: MaterialEntity;
    onClose: () => void;
}

// Toolbar button component
const ToolbarButton: React.FC<{
    onClick: () => void;
    isActive?: boolean;
    disabled?: boolean;
    title: string;
    children: React.ReactNode;
}> = ({ onClick, isActive, disabled, title, children }) => (
    <button
        onClick={onClick}
        disabled={disabled}
        title={title}
        className={`p-2 rounded transition-colors ${isActive
            ? 'bg-brand-orange text-white'
            : 'hover:bg-gray-100 text-gray-700'
            } ${disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
    >
        {children}
    </button>
);

const ToolbarDivider = () => <div className="w-px h-6 bg-gray-300 mx-1" />;

// Input dialog for Electron-compatible prompts
interface InputDialogProps {
    isOpen: boolean;
    title: string;
    placeholder?: string;
    onSubmit: (value: string) => void;
    onCancel: () => void;
}

const InputDialog: React.FC<InputDialogProps> = ({ isOpen, title, placeholder, onSubmit, onCancel }) => {
    const [value, setValue] = React.useState('');
    const inputRef = React.useRef<HTMLInputElement>(null);

    React.useEffect(() => {
        if (isOpen) {
            setValue('');
            setTimeout(() => inputRef.current?.focus(), 100);
        }
    }, [isOpen]);

    if (!isOpen) return null;

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (value.trim()) {
            onSubmit(value.trim());
        }
    };

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-[110]">
            <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow-xl p-6 w-96">
                <h3 className="text-lg font-semibold mb-4">{title}</h3>
                <input
                    ref={inputRef}
                    type="text"
                    value={value}
                    onChange={(e) => setValue(e.target.value)}
                    placeholder={placeholder}
                    className="w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-brand-orange mb-4"
                />
                <div className="flex justify-end gap-2">
                    <button type="button" onClick={onCancel} className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded">
                        Cancel
                    </button>
                    <button type="submit" className="px-4 py-2 bg-brand-orange text-white rounded hover:bg-orange-600">
                        Insert
                    </button>
                </div>
            </form>
        </div>
    );
};

// Confirm dialog for Electron-compatible confirms
interface ConfirmDialogProps {
    isOpen: boolean;
    title: string;
    message: string;
    onConfirm: () => void;
    onCancel: () => void;
}

const ConfirmDialog: React.FC<ConfirmDialogProps> = ({ isOpen, title, message, onConfirm, onCancel }) => {
    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-[110]">
            <div className="bg-white rounded-lg shadow-xl p-6 w-96">
                <h3 className="text-lg font-semibold mb-2">{title}</h3>
                <p className="text-gray-600 mb-4">{message}</p>
                <div className="flex justify-end gap-2">
                    <button onClick={onCancel} className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded">
                        Don't Save
                    </button>
                    <button onClick={onConfirm} className="px-4 py-2 bg-brand-green text-white rounded hover:bg-green-700">
                        Save
                    </button>
                </div>
            </div>
        </div>
    );
};

export const EditorModal: React.FC<EditorModalProps> = ({ material, onClose }) => {
    const { updateMaterial } = useAppContext();

    const [title, setTitle] = useState(material.title);
    const [readingAge, setReadingAge] = useState<number | undefined>(material.readingAge);
    const [actualAge, setActualAge] = useState<number | undefined>(material.actualAge);
    const [isDirty, setIsDirty] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const [lastSaved, setLastSaved] = useState<Date | null>(null);
    // Force re-render counter to update toolbar button active states
    const [, setForceUpdate] = useState(0);

    // Dialog state
    const [inputDialog, setInputDialog] = useState<{
        isOpen: boolean;
        title: string;
        placeholder?: string;
        onSubmit: (value: string) => void;
    }>({ isOpen: false, title: '', onSubmit: () => { } });
    const [confirmDialog, setConfirmDialog] = useState<{
        isOpen: boolean;
        onConfirm: () => void;
    }>({ isOpen: false, onConfirm: () => { } });

    const autoSaveTimeoutRef = useRef<NodeJS.Timeout | null>(null);

    // Convert stored markdown to HTML for editor
    const initialContent = markdownToHtml(material.content || '');

    // Initialize TipTap editor with all extensions
    const editor = useEditor({
        extensions: [
            StarterKit.configure({
                heading: {
                    levels: [1, 2, 3],
                },
            }),
            Placeholder.configure({
                placeholder: 'Start writing your material content...',
            }),
            // Gap cursor for positioning before/after block elements like tables
            Gapcursor,
            // Table extensions
            TableExt.Table.configure({
                resizable: false,
                allowTableNodeSelection: true,
            }),
            TableRowExt.TableRow,
            TableCellExt.TableCell,
            TableHeaderExt.TableHeader,
            // Standard extensions
            Image.configure({
                inline: true,
            }),
            Link.configure({
                openOnClick: false,
            }),
            TextAlign.configure({
                types: ['heading', 'paragraph'],
            }),
            Underline,
            // Custom extensions for Material Encoding
            InlineLatex,
            BlockLatex,
            QuestionRef,
            PdfEmbed,
        ],
        content: initialContent,
        onUpdate: () => {
            setIsDirty(true);
        },
        // Force toolbar re-render on selection/formatting changes
        onSelectionUpdate: () => {
            setForceUpdate(n => n + 1);
        },
        onTransaction: () => {
            setForceUpdate(n => n + 1);
        },
    });

    // Get content as Markdown for storage
    const getMarkdownContent = useCallback(() => {
        if (!editor) return '';
        const html = editor.getHTML();
        return htmlToMarkdown(html);
    }, [editor]);

    // Auto-save with 5-second debounce
    const saveContent = useCallback(async () => {
        if (!isDirty || !editor) return;

        setIsSaving(true);
        try {
            await updateMaterial({
                ...material,
                title,
                content: getMarkdownContent(),
                readingAge,
                actualAge,
                timestamp: new Date().toISOString(),
            });
            setIsDirty(false);
            setLastSaved(new Date());
        } catch (err) {
            console.error('Failed to save:', err);
        } finally {
            setIsSaving(false);
        }
    }, [isDirty, editor, material, title, readingAge, actualAge, updateMaterial, getMarkdownContent]);

    // Setup auto-save
    useEffect(() => {
        if (isDirty) {
            if (autoSaveTimeoutRef.current) {
                clearTimeout(autoSaveTimeoutRef.current);
            }
            autoSaveTimeoutRef.current = setTimeout(saveContent, 1000);
        }
        return () => {
            if (autoSaveTimeoutRef.current) {
                clearTimeout(autoSaveTimeoutRef.current);
            }
        };
    }, [isDirty, saveContent]);

    // Mark dirty on metadata change
    const handleTitleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setTitle(e.target.value);
        setIsDirty(true);
    };

    // Close and save any pending changes
    const handleClose = async () => {
        if (isDirty) {
            await saveContent();
        }
        onClose();
    };

    const closeInputDialog = () => {
        setInputDialog({ isOpen: false, title: '', onSubmit: () => { } });
    };

    // Insert image URL
    const insertImage = () => {
        setInputDialog({
            isOpen: true,
            title: 'Insert Image',
            placeholder: 'Enter image URL or attachment ID',
            onSubmit: (url) => {
                closeInputDialog();
                if (editor) {
                    const src = url.includes('/') ? url : `/attachments/${url}`;
                    editor.chain().focus().setImage({ src }).run();
                }
            },
        });
    };

    // Insert link
    const insertLink = () => {
        setInputDialog({
            isOpen: true,
            title: 'Insert Link',
            placeholder: 'Enter link URL',
            onSubmit: (url) => {
                closeInputDialog();
                if (editor) {
                    editor.chain().focus().setLink({ href: url }).run();
                }
            },
        });
    };

    // Insert table
    const insertTable = () => {
        if (editor) {
            editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run();
        }
    };

    // Table controls
    const addColumnBefore = () => editor?.chain().focus().addColumnBefore().run();
    const addColumnAfter = () => editor?.chain().focus().addColumnAfter().run();
    const deleteColumn = () => editor?.chain().focus().deleteColumn().run();
    const addRowBefore = () => editor?.chain().focus().addRowBefore().run();
    const addRowAfter = () => editor?.chain().focus().addRowAfter().run();
    const deleteRow = () => editor?.chain().focus().deleteRow().run();
    const deleteTable = () => editor?.chain().focus().deleteTable().run();
    const isInTable = editor?.isActive('table') ?? false;

    // Insert inline LaTeX
    const insertInlineLatex = () => {
        setInputDialog({
            isOpen: true,
            title: 'Insert Inline LaTeX',
            placeholder: 'e.g., x^2 + y^2 = z^2',
            onSubmit: (latex) => {
                closeInputDialog();
                if (editor) {
                    editor.chain().focus().insertContent({
                        type: 'inlineLatex',
                        attrs: { latex },
                    }).run();
                }
            },
        });
    };

    // Insert block LaTeX
    const insertBlockLatex = () => {
        setInputDialog({
            isOpen: true,
            title: 'Insert Block LaTeX',
            placeholder: 'e.g., \\int_0^1 f(x) dx',
            onSubmit: (latex) => {
                closeInputDialog();
                if (editor) {
                    editor.chain().focus().insertContent({
                        type: 'blockLatex',
                        attrs: { latex },
                    }).run();
                }
            },
        });
    };

    // Insert question reference
    const insertQuestion = () => {
        setInputDialog({
            isOpen: true,
            title: 'Insert Question Reference',
            placeholder: 'Enter Question UUID',
            onSubmit: (id) => {
                closeInputDialog();
                if (editor) {
                    editor.chain().focus().insertContent({
                        type: 'questionRef',
                        attrs: { id },
                    }).run();
                }
            },
        });
    };

    // Check if adding a question is allowed
    const canAddQuestion = () => {
        // READING materials cannot contain questions
        if (material.materialType === 'READING') {
            return false;
        }
        
        // POLL materials can only contain one question
        if (material.materialType === 'POLL') {
            // Count existing question references in the content
            const content = editor?.getHTML() || '';
            const questionCount = (content.match(/class="question-embed"/g) || []).length;
            return questionCount === 0;
        }
        
        // WORKSHEET can have multiple questions
        return true;
    };

    // Insert PDF embed
    const insertPdf = () => {
        setInputDialog({
            isOpen: true,
            title: 'Insert PDF Embed',
            placeholder: 'Enter PDF Attachment UUID',
            onSubmit: (id) => {
                closeInputDialog();
                if (editor) {
                    editor.chain().focus().insertContent({
                        type: 'pdfEmbed',
                        attrs: { id },
                    }).run();
                }
            },
        });
    };

    if (!editor) {
        return null;
    }

    return ReactDOM.createPortal(
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-[100] p-0">
            <div className="bg-white w-full h-full flex flex-col overflow-hidden">
                {/* Header */}
                <div className="flex items-center justify-between p-4 border-b border-gray-200 bg-gray-50">
                    <div className="flex items-center gap-4 flex-1">
                        <input
                            type="text"
                            value={title}
                            onChange={handleTitleChange}
                            className="text-xl font-semibold text-text-heading bg-transparent border-b-2 border-transparent hover:border-gray-300 focus:border-brand-orange focus:outline-none px-1 py-0.5 min-w-[200px]"
                            placeholder="Material Title"
                        />
                        <span className={`text-xs px-2 py-1 rounded ${material.materialType === 'READING' ? 'bg-brand-green text-white' :
                            material.materialType === 'WORKSHEET' ? 'bg-brand-yellow text-gray-900' :
                                'bg-purple-500 text-white'
                            }`}>
                            {material.materialType}
                        </span>
                    </div>

                    <div className="flex items-center gap-3">
                        {/* Age inputs */}
                        <div className="flex items-center gap-2 text-sm">
                            <label className="text-gray-500">Reading Age:</label>
                            <input
                                type="number"
                                value={readingAge ?? ''}
                                onChange={(e) => { setReadingAge(e.target.value ? parseInt(e.target.value) : undefined); setIsDirty(true); }}
                                className="w-12 px-2 py-1 border rounded text-center"
                                min="4"
                                max="18"
                            />
                        </div>
                        <div className="flex items-center gap-2 text-sm">
                            <label className="text-gray-500">Actual Age:</label>
                            <input
                                type="number"
                                value={actualAge ?? ''}
                                onChange={(e) => { setActualAge(e.target.value ? parseInt(e.target.value) : undefined); setIsDirty(true); }}
                                className="w-12 px-2 py-1 border rounded text-center"
                                min="4"
                                max="18"
                            />
                        </div>

                        {/* Save status */}
                        <div className="text-xs text-gray-400 min-w-[100px]">
                            {isSaving ? 'Saving...' : lastSaved ? `Saved ${lastSaved.toLocaleTimeString()}` : isDirty ? 'Unsaved changes' : 'No changes'}
                        </div>

                        <button
                            onClick={handleClose}
                            className="px-4 py-2 bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300 text-sm font-medium"
                        >
                            Close
                        </button>
                    </div>
                </div>

                {/* Toolbar */}
                <div className="flex items-center gap-1 p-2 border-b border-gray-200 bg-white flex-wrap">
                    {/* Headings */}
                    <ToolbarButton
                        onClick={() => editor.chain().focus().toggleHeading({ level: 1 }).run()}
                        isActive={editor.isActive('heading', { level: 1 })}
                        title="Heading 1"
                    >
                        H1
                    </ToolbarButton>
                    <ToolbarButton
                        onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}
                        isActive={editor.isActive('heading', { level: 2 })}
                        title="Heading 2"
                    >
                        H2
                    </ToolbarButton>
                    <ToolbarButton
                        onClick={() => editor.chain().focus().toggleHeading({ level: 3 }).run()}
                        isActive={editor.isActive('heading', { level: 3 })}
                        title="Heading 3"
                    >
                        H3
                    </ToolbarButton>

                    <ToolbarDivider />

                    {/* Text formatting */}
                    <ToolbarButton
                        onClick={() => editor.chain().focus().toggleBold().run()}
                        isActive={editor.isActive('bold')}
                        title="Bold (Ctrl+B)"
                    >
                        <strong>B</strong>
                    </ToolbarButton>
                    <ToolbarButton
                        onClick={() => editor.chain().focus().toggleItalic().run()}
                        isActive={editor.isActive('italic')}
                        title="Italic (Ctrl+I)"
                    >
                        <em>I</em>
                    </ToolbarButton>
                    <ToolbarButton
                        onClick={() => editor.chain().focus().toggleUnderline().run()}
                        isActive={editor.isActive('underline')}
                        title="Underline (Ctrl+U)"
                    >
                        <span className="underline">U</span>
                    </ToolbarButton>
                    <ToolbarButton
                        onClick={() => editor.chain().focus().toggleStrike().run()}
                        isActive={editor.isActive('strike')}
                        title="Strikethrough"
                    >
                        <span className="line-through">S</span>
                    </ToolbarButton>

                    <ToolbarDivider />

                    {/* Lists */}
                    <ToolbarButton
                        onClick={() => editor.chain().focus().toggleBulletList().run()}
                        isActive={editor.isActive('bulletList')}
                        title="Bullet List"
                    >
                        • List
                    </ToolbarButton>
                    <ToolbarButton
                        onClick={() => editor.chain().focus().toggleOrderedList().run()}
                        isActive={editor.isActive('orderedList')}
                        title="Numbered List"
                    >
                        1. List
                    </ToolbarButton>

                    <ToolbarDivider />

                    {/* Alignment */}
                    <ToolbarButton
                        onClick={() => editor.chain().focus().setTextAlign('left').run()}
                        isActive={editor.isActive({ textAlign: 'left' })}
                        title="Align Left"
                    >
                        ⬅
                    </ToolbarButton>
                    <ToolbarButton
                        onClick={() => editor.chain().focus().setTextAlign('center').run()}
                        isActive={editor.isActive({ textAlign: 'center' })}
                        title="Align Center"
                    >
                        ⬌
                    </ToolbarButton>

                    <ToolbarDivider />

                    {/* Blocks */}
                    <ToolbarButton
                        onClick={() => editor.chain().focus().toggleBlockquote().run()}
                        isActive={editor.isActive('blockquote')}
                        title="Blockquote"
                    >
                        ❝
                    </ToolbarButton>
                    <ToolbarButton
                        onClick={() => editor.chain().focus().toggleCodeBlock().run()}
                        isActive={editor.isActive('codeBlock')}
                        title="Code Block"
                    >
                        {'</>'}
                    </ToolbarButton>
                    <ToolbarButton
                        onClick={() => editor.chain().focus().setHorizontalRule().run()}
                        title="Horizontal Rule"
                    >
                        ─
                    </ToolbarButton>

                    <ToolbarDivider />

                    {/* Insert */}
                    <ToolbarButton onClick={insertTable} title="Insert Table">
                        📊
                    </ToolbarButton>

                    {/* Table Controls - only show when cursor is in a table */}
                    {isInTable && (
                        <>
                            <ToolbarDivider />
                            <span className="text-xs text-gray-500 px-1 self-center">Table:</span>
                            <ToolbarButton onClick={addColumnBefore} title="Add Column Before">
                                ⬅+
                            </ToolbarButton>
                            <ToolbarButton onClick={addColumnAfter} title="Add Column After">
                                +➡
                            </ToolbarButton>
                            <ToolbarButton onClick={deleteColumn} title="Delete Column">
                                🗑️↔
                            </ToolbarButton>
                            <ToolbarButton onClick={addRowBefore} title="Add Row Above">
                                ⬆+
                            </ToolbarButton>
                            <ToolbarButton onClick={addRowAfter} title="Add Row Below">
                                +⬇
                            </ToolbarButton>
                            <ToolbarButton onClick={deleteRow} title="Delete Row">
                                🗑️↕
                            </ToolbarButton>
                            <ToolbarButton onClick={deleteTable} title="Delete Table">
                                🗑️📊
                            </ToolbarButton>
                        </>
                    )}

                    <ToolbarButton onClick={insertImage} title="Insert Image">
                        🖼
                    </ToolbarButton>
                    <ToolbarButton
                        onClick={insertLink}
                        isActive={editor.isActive('link')}
                        title="Insert Link"
                    >
                        🔗
                    </ToolbarButton>

                    <ToolbarDivider />

                    {/* LaTeX */}
                    <ToolbarButton onClick={insertInlineLatex} title="Inline LaTeX ($...$)">
                        ∑
                    </ToolbarButton>
                    <ToolbarButton onClick={insertBlockLatex} title="Block LaTeX ($$...$$)">
                        ∫
                    </ToolbarButton>

                    <ToolbarDivider />

                    {/* Custom markers */}
                    <ToolbarButton 
                        onClick={insertQuestion} 
                        disabled={!canAddQuestion()}
                        title={material.materialType === 'READING' 
                            ? 'Questions not allowed in Reading materials'
                            : material.materialType === 'POLL' && !canAddQuestion()
                            ? 'Poll materials can only contain one question'
                            : 'Insert Question Reference'}
                    >
                        📝
                    </ToolbarButton>
                    <ToolbarButton onClick={insertPdf} title="Insert PDF Embed">
                        📄
                    </ToolbarButton>

                    <ToolbarDivider />

                    {/* Undo/Redo */}
                    <ToolbarButton
                        onClick={() => editor.chain().focus().undo().run()}
                        disabled={!editor.can().undo()}
                        title="Undo (Ctrl+Z)"
                    >
                        ↩
                    </ToolbarButton>
                    <ToolbarButton
                        onClick={() => editor.chain().focus().redo().run()}
                        disabled={!editor.can().redo()}
                        title="Redo (Ctrl+Y)"
                    >
                        ↪
                    </ToolbarButton>
                </div>

                {/* Editor Area */}
                <div className="flex-1 overflow-y-auto">
                    <EditorContent
                        editor={editor}
                        className="h-full p-6 focus:outline-none prose prose-sm max-w-none"
                    />
                </div>
            </div>

            {/* Editor styles */}
            <style>{`
                .ProseMirror {
                    min-height: 100%;
                    outline: none;
                }
                .ProseMirror p.is-editor-empty:first-child::before {
                    color: #adb5bd;
                    content: attr(data-placeholder);
                    float: left;
                    height: 0;
                    pointer-events: none;
                }
                .ProseMirror h1 {
                    font-size: 2em;
                    font-weight: bold;
                    margin: 0.67em 0;
                    line-height: 1.2;
                }
                .ProseMirror h2 {
                    font-size: 1.5em;
                    font-weight: bold;
                    margin: 0.83em 0;
                    line-height: 1.3;
                }
                .ProseMirror h3 {
                    font-size: 1.17em;
                    font-weight: bold;
                    margin: 1em 0;
                    line-height: 1.4;
                }
                .ProseMirror ul {
                    list-style-type: disc;
                    padding-left: 1.5em;
                    margin: 0.5em 0;
                }
                .ProseMirror ol {
                    list-style-type: decimal;
                    padding-left: 1.5em;
                    margin: 0.5em 0;
                }
                .ProseMirror li {
                    margin: 0.25em 0;
                }
                .ProseMirror ul ul {
                    list-style-type: circle;
                }
                .ProseMirror ul ul ul {
                    list-style-type: square;
                }
                .ProseMirror table {
                    border-collapse: collapse;
                    margin: 1em 0;
                    width: 100%;
                }
                /* Gap cursor - allows clicking before tables at document start */
                .ProseMirror .ProseMirror-gapcursor {
                    display: block;
                    position: relative;
                    height: 1em;
                    margin: 0.25em 0;
                }
                .ProseMirror .ProseMirror-gapcursor:after {
                    content: '';
                    display: block;
                    position: absolute;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 2px;
                    background-color: #F97316;
                    animation: gapcursor-blink 1s infinite;
                }
                @keyframes gapcursor-blink {
                    0%, 100% { opacity: 1; }
                    50% { opacity: 0; }
                }
                /* Clickable area before first block element */
                .ProseMirror > *:first-child {
                    margin-top: 0.5em;
                }
                .ProseMirror:before {
                    content: '';
                    display: block;
                    height: 0.5em;
                    cursor: text;
                }
                .ProseMirror table td,
                .ProseMirror table th {
                    border: 1px solid #ddd;
                    padding: 8px;
                    min-width: 100px;
                }
                .ProseMirror table th {
                    background-color: #f5f5f5;
                    font-weight: bold;
                }
                .ProseMirror blockquote {
                    border-left: 4px solid #e5e7eb;
                    padding-left: 1em;
                    margin-left: 0;
                    color: #6b7280;
                }
                .ProseMirror pre {
                    background: #1e1e1e;
                    color: #d4d4d4;
                    padding: 1em;
                    border-radius: 4px;
                    overflow-x: auto;
                }
                .ProseMirror code {
                    background: #f3f4f6;
                    padding: 0.2em 0.4em;
                    border-radius: 3px;
                    font-size: 0.9em;
                }
                .ProseMirror pre code {
                    background: none;
                    padding: 0;
                }
                .ProseMirror img {
                    max-width: 100%;
                    height: auto;
                }
                .ProseMirror hr {
                    border: none;
                    border-top: 2px solid #e5e7eb;
                    margin: 2em 0;
                }
                .inline-latex {
                    display: inline;
                }
                .block-latex {
                    display: block;
                    margin: 1em 0;
                }
                .latex-error {
                    color: red;
                    font-family: monospace;
                }
            `}</style>

            {/* Input Dialog */}
            <InputDialog
                isOpen={inputDialog.isOpen}
                title={inputDialog.title}
                placeholder={inputDialog.placeholder}
                onSubmit={inputDialog.onSubmit}
                onCancel={closeInputDialog}
            />

            {/* Confirm Dialog */}
            <ConfirmDialog
                isOpen={confirmDialog.isOpen}
                title="Unsaved Changes"
                message="You have unsaved changes. Save before closing?"
                onConfirm={confirmDialog.onConfirm}
                onCancel={() => {
                    setConfirmDialog({ isOpen: false, onConfirm: () => { } });
                    onClose();
                }}
            />
        </div>,
        document.body
    );
};
