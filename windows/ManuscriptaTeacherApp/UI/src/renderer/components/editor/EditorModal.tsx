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
import { Table } from '@tiptap/extension-table';
import { TableRow } from '@tiptap/extension-table-row';
import { TableCell } from '@tiptap/extension-table-cell';
import { TableHeader } from '@tiptap/extension-table-header';
import Link from '@tiptap/extension-link';
import TextAlign from '@tiptap/extension-text-align';
import Underline from '@tiptap/extension-underline';
import { InlineLatex, BlockLatex, QuestionRef, PdfEmbed, AttachmentImage } from './extensions';
import { QuestionEditorDialog } from './QuestionEditorDialog';
import { htmlToMarkdown, markdownToHtml } from '../../utils/markdownConversion';
import type { MaterialEntity, QuestionEntity, InternalCreateAttachmentDto } from '../../models';
import { useAppContext } from '../../state/AppContext';
import signalRService from '../../services/signalr/SignalRService';
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

    // Question editor dialog state - per s4C(3)
    const [questionDialog, setQuestionDialog] = useState<{
        isOpen: boolean;
        question?: QuestionEntity;
    }>({ isOpen: false });

    const autoSaveTimeoutRef = useRef<NodeJS.Timeout | null>(null);
    // Guard to prevent concurrent orphan removal executions
    const isRemovingOrphansRef = useRef(false);

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
            Table.configure({
                resizable: false,
                allowTableNodeSelection: true,
            }),
            TableRow,
            TableCell,
            TableHeader,
            // Standard extensions
            AttachmentImage.configure({
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
        // Per §4C(4)(d): Support drag-and-drop of attachments and copy-paste of images
        editorProps: {
            handleDrop: (view, event, _slice, moved) => {
                // Ignore if this is a moved node within the editor
                if (moved) return false;

                const files = event.dataTransfer?.files;
                if (!files || files.length === 0) return false;

                const file = files[0];
                const fileName = file.name;
                const lastDotIndex = fileName.lastIndexOf('.');
                let fileExtension = lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1).toLowerCase() : '';

                // Normalize jpg to jpeg
                if (fileExtension === 'jpg') fileExtension = 'jpeg';

                // Only handle supported file types
                if (!['png', 'jpeg', 'pdf'].includes(fileExtension)) {
                    return false;
                }

                // Prevent default handling
                event.preventDefault();

                // Get drop position coordinates and resolve to editor position
                const coordinates = view.posAtCoords({ left: event.clientX, top: event.clientY });
                const insertPos = coordinates?.pos ?? view.state.selection.from;

                // Process the file asynchronously
                (async () => {
                    try {
                        const fileBaseName = lastDotIndex > 0 ? fileName.substring(0, lastDotIndex) : fileName;

                        // Read file as base64 (file.path is not available in sandboxed Electron)
                        const arrayBuffer = await file.arrayBuffer();
                        const base64 = btoa(
                            new Uint8Array(arrayBuffer).reduce((data, byte) => data + String.fromCharCode(byte), '')
                        );

                        // Create attachment entity via SignalR per §4C(4)(a)(ii)
                        const dto: InternalCreateAttachmentDto = {
                            materialId: material.id,
                            fileBaseName,
                            fileExtension
                        };
                        const attachmentId = await signalRService.createAttachment(dto);

                        // Save file from base64 data per §4C(4)(a)(iii)
                        // Per §4C(4)(a1): if file save fails, rollback by deleting entity
                        try {
                            await window.electronAPI.saveAttachmentFromBase64(base64, attachmentId, fileExtension);
                        } catch (fileSaveError) {
                            console.error('File save failed, rolling back entity:', fileSaveError);
                            await signalRService.deleteAttachment(attachmentId);
                            throw fileSaveError;
                        }

                        // Get data URL for WYSIWYG display
                        const dataUrl = await window.electronAPI.getAttachmentDataUrl(attachmentId, fileExtension);
                        if (!dataUrl) return;

                        // Insert at drop position per §4C(4)(a)(iv)
                        const { tr } = view.state;
                        if (fileExtension === 'pdf') {
                            const pdfNode = view.state.schema.nodes.pdfEmbed.create({ id: attachmentId });
                            tr.insert(insertPos, pdfNode);
                        } else {
                            const imageNode = view.state.schema.nodes.image.create({
                                src: dataUrl,
                                alt: fileBaseName,
                                title: attachmentId,
                            });
                            tr.insert(insertPos, imageNode);
                        }
                        view.dispatch(tr);
                        setIsDirty(true);
                    } catch (error) {
                        console.error('Error processing dropped attachment:', error);
                    }
                })();

                return true;
            },
            handlePaste: (view, event, _slice) => {
                const clipboardData = event.clipboardData;
                if (!clipboardData) return false;

                const items = clipboardData.items;

                // Check if HTML content is available - if so, prefer HTML over standalone image
                // This handles paste from Microsoft Word, Apple Notes, etc. that provide both
                const hasHtmlContent = clipboardData.getData('text/html')?.length > 0;

                // CASE 1: Standalone image in clipboard (e.g., screenshot, copied image)
                // Only process if no HTML content is available (to prefer HTML from Word, Notes, etc.)
                let imageItem: DataTransferItem | null = null;
                if (!hasHtmlContent) {
                    for (const item of items) {
                        if (item.type.startsWith('image/') && item.kind === 'file') {
                            imageItem = item;
                            break;
                        }
                    }
                }

                if (imageItem) {
                    // Determine extension from MIME type
                    const mimeType = imageItem.type;
                    let fileExtension = 'png';
                    if (mimeType === 'image/jpeg') {
                        fileExtension = 'jpeg';
                    } else if (mimeType === 'image/png') {
                        fileExtension = 'png';
                    } else {
                        return false;
                    }

                    const blob = imageItem.getAsFile();
                    if (!blob) return false;

                    event.preventDefault();
                    const insertPos = view.state.selection.from;

                    (async () => {
                        try {
                            const arrayBuffer = await blob.arrayBuffer();
                            const base64 = btoa(
                                new Uint8Array(arrayBuffer).reduce((data, byte) => data + String.fromCharCode(byte), '')
                            );

                            const dto: InternalCreateAttachmentDto = {
                                materialId: material.id,
                                fileBaseName: 'pasted-image',
                                fileExtension
                            };
                            const attachmentId = await signalRService.createAttachment(dto);

                            try {
                                await window.electronAPI.saveAttachmentFromBase64(base64, attachmentId, fileExtension);
                            } catch (fileSaveError) {
                                console.error('File save failed, rolling back entity:', fileSaveError);
                                await signalRService.deleteAttachment(attachmentId);
                                throw fileSaveError;
                            }

                            const dataUrl = await window.electronAPI.getAttachmentDataUrl(attachmentId, fileExtension);
                            if (!dataUrl) return;

                            const { tr } = view.state;
                            const imageNode = view.state.schema.nodes.image.create({
                                src: dataUrl,
                                alt: 'pasted-image',
                                title: attachmentId,
                            });
                            tr.insert(insertPos, imageNode);
                            view.dispatch(tr);
                            setIsDirty(true);
                        } catch (error) {
                            console.error('Error processing pasted image:', error);
                        }
                    })();

                    return true;
                }

                // CASE 2: HTML content with embedded images - let TipTap handle paste normally.
                // External images (blob:/http:/data:) will be converted to attachments on save
                // per §4C(4)(d). This approach is more reliable than intercepting paste events.

                // CASE 3: No special handling needed - let TipTap handle normally
                return false;
            },
        },
    });

    // Get content as Markdown for storage
    const getMarkdownContent = useCallback(() => {
        if (!editor) return '';
        const html = editor.getHTML();
        return htmlToMarkdown(html);
    }, [editor]);

    // Convert external images (blob:/http:/https:/data:) to attachments per §4C(4)(d)
    // Called before save to ensure all pasted/dropped images are properly stored
    const convertExternalImagesToAttachments = useCallback(async (): Promise<void> => {
        if (!editor) return;

        const { state } = editor;
        let tr = state.tr;
        let hasChanges = false;
        const nodesToProcess: { pos: number; node: typeof state.doc.firstChild; }[] = [];

        // First pass: collect all image nodes with external sources
        state.doc.descendants((node, pos) => {
            if (node.type.name === 'image') {
                const src = node.attrs.src as string;
                const title = node.attrs.title as string;

                // Skip non-image data URL attachments (have title/UUID and non-image data: URL)
                if (title && src?.startsWith('data:') && !src.startsWith('data:image/')) {
                    return true;
                }

                // Check if this is an external image that needs conversion
                if (src && (
                    src.startsWith('blob:') ||
                    src.startsWith('http://') ||
                    src.startsWith('https://') ||
                    (src.startsWith('data:image/') && !title) // data URL without attachment ID
                )) {
                    nodesToProcess.push({ pos, node });
                }
            }
            return true;
        });

        if (nodesToProcess.length === 0) return;

        console.log(`Converting ${nodesToProcess.length} external images to attachments...`);

        // Second pass: process each image (in reverse order to maintain positions)
        for (const { pos, node } of nodesToProcess.reverse()) {
            const src = node.attrs.src as string;
            const alt = node.attrs.alt as string || 'pasted-image';

            try {
                let base64Data: string | null = null;
                let fileExtension = 'png';

                if (src.startsWith('data:image/')) {
                    // Extract data URL
                    const match = src.match(/^data:image\/(png|jpeg|jpg|gif|webp);base64,(.+)$/i);
                    if (match) {
                        const ext = match[1].toLowerCase();
                        fileExtension = (ext === 'jpg') ? 'jpeg' : (ext === 'gif' || ext === 'webp') ? 'png' : ext;
                        base64Data = match[2];
                    }
                } else if (src.startsWith('blob:') || src.startsWith('http://') || src.startsWith('https://')) {
                    // Fetch the image
                    try {
                        const response = await fetch(src);
                        if (response.ok) {
                            const blob = await response.blob();
                            const contentType = blob.type || 'image/png';
                            if (contentType.includes('jpeg') || contentType.includes('jpg')) {
                                fileExtension = 'jpeg';
                            } else if (contentType.includes('png')) {
                                fileExtension = 'png';
                            }
                            const arrayBuffer = await blob.arrayBuffer();
                            base64Data = btoa(
                                new Uint8Array(arrayBuffer).reduce((data, byte) => data + String.fromCharCode(byte), '')
                            );
                        }
                    } catch (fetchError) {
                        console.warn('Failed to fetch image for conversion:', src.substring(0, 50), fetchError);
                    }
                }

                if (!base64Data) {
                    console.warn('Could not convert image, removing:', src.substring(0, 50));
                    // Remove the image node since we can't process it
                    tr = tr.delete(pos, pos + node.nodeSize);
                    hasChanges = true;
                    continue;
                }

                // Create attachment entity
                const dto: InternalCreateAttachmentDto = {
                    materialId: material.id,
                    fileBaseName: alt || 'pasted-image',
                    fileExtension
                };
                const attachmentId = await signalRService.createAttachment(dto);

                // Save the file
                try {
                    await window.electronAPI.saveAttachmentFromBase64(base64Data, attachmentId, fileExtension);
                } catch (fileSaveError) {
                    console.error('File save failed, rolling back entity:', fileSaveError);
                    await signalRService.deleteAttachment(attachmentId);
                    tr = tr.delete(pos, pos + node.nodeSize);
                    hasChanges = true;
                    continue;
                }

                // Get data URL for display
                const dataUrl = await window.electronAPI.getAttachmentDataUrl(attachmentId, fileExtension);
                if (dataUrl) {
                    // Update the node with new attributes
                    tr = tr.setNodeMarkup(pos, undefined, {
                        ...node.attrs,
                        src: dataUrl,
                        title: attachmentId, // Store attachment ID for markdown conversion
                    });
                    hasChanges = true;
                    console.log('Converted image to attachment:', attachmentId);
                }
            } catch (error) {
                console.error('Error converting image to attachment:', error);
                tr = tr.delete(pos, pos + node.nodeSize);
                hasChanges = true;
            }
        }

        // Apply all changes at once
        if (hasChanges) {
            editor.view.dispatch(tr);
        }
    }, [editor, material.id, signalRService]);

    // Auto-save with 1-second debounce
    const saveContent = useCallback(async () => {
        if (!isDirty || !editor) return;

        setIsSaving(true);
        try {
            // Convert any external images to attachments before saving per §4C(4)(d)
            await convertExternalImagesToAttachments();

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
    }, [isDirty, editor, material, title, readingAge, actualAge, updateMaterial, getMarkdownContent, convertExternalImagesToAttachments]);

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

    // Resolve attachment image paths to data URLs for WYSIWYG display
    // Also handles orphaned attachment entities per §4C(6)
    useEffect(() => {
        const resolveAttachmentImages = async () => {
            if (!editor) return;

            try {
                // Find all image nodes with /attachments/ paths
                const { doc, tr } = editor.state;
                let modified = false;

                // Track positions to delete (orphaned attachments with missing files per §4C(6))
                const positionsToDelete: number[] = [];
                const entitiesToDelete: string[] = [];

                const promises: Promise<void>[] = [];

                doc.descendants((node, pos) => {
                    if (node.type.name === 'image') {
                        const src = node.attrs.src || '';
                        // Match /attachments/{uuid} pattern
                        const match = src.match(/^\/attachments\/([a-f0-9-]+)$/i);
                        if (match) {
                            const attachmentId = match[1];
                            // Get attachment from backend to get file extension
                            promises.push(
                                signalRService.getAttachmentsUnderMaterial(material.id)
                                    .then(async (attachments) => {
                                        const attachment = attachments.find(a => a.id === attachmentId);
                                        if (attachment) {
                                            const dataUrl = await window.electronAPI.getAttachmentDataUrl(
                                                attachmentId,
                                                attachment.fileExtension
                                            );
                                            if (dataUrl) {
                                                tr.setNodeMarkup(pos, undefined, {
                                                    ...node.attrs,
                                                    src: dataUrl,
                                                    title: attachmentId, // Store ID for markdown conversion
                                                });
                                                modified = true;
                                            } else {
                                                // §4C(6): File doesn't exist - track for removal
                                                console.log('Orphaned attachment entity (missing file):', attachmentId);
                                                positionsToDelete.push(pos);
                                                entitiesToDelete.push(attachmentId);
                                            }
                                        }
                                    })
                                    .catch(err => console.error('Failed to resolve attachment:', err))
                            );
                        }
                    }
                });

                await Promise.all(promises);

                // §4C(6)(a): Remove attachment references for missing files
                // Delete from end to avoid position shifts
                positionsToDelete.sort((a, b) => b - a);
                for (const pos of positionsToDelete) {
                    // Get node size at this position
                    const node = doc.nodeAt(pos);
                    if (node) {
                        tr.delete(pos, pos + node.nodeSize);
                        modified = true;
                    }
                }

                if (modified) {
                    tr.setMeta('addToHistory', false);
                    editor.view.dispatch(tr);
                }

                // §4C(6)(b): Delete orphaned attachment entities
                for (const id of entitiesToDelete) {
                    try {
                        await signalRService.deleteAttachment(id);
                        console.log('Deleted orphaned attachment entity:', id);
                    } catch (err) {
                        console.error('Failed to delete orphaned attachment entity:', err);
                    }
                }
            } catch (err) {
                console.error('Failed to resolve attachment images:', err);
            }
        };

        // Resolve after a short delay to ensure editor is ready
        const timeoutId = setTimeout(resolveAttachmentImages, 150);
        return () => clearTimeout(timeoutId);
    }, [editor, material.id]);

    // Load question data for existing question references on mount
    useEffect(() => {
        const loadQuestionData = async () => {
            if (!editor) return;

            try {
                // Fetch all questions for this material
                const questions = await signalRService.getQuestionsUnderMaterial(material.id);
                if (questions.length === 0) return;

                // Create a map for quick lookup
                const questionMap = new Map(questions.map(q => [q.id, q]));

                // Find all questionRef nodes and update them with full data
                const { doc, tr } = editor.state;
                let modified = false;

                doc.descendants((node, pos) => {
                    if (node.type.name === 'questionRef') {
                        const questionId = node.attrs.id;
                        const questionData = questionMap.get(questionId);

                        if (questionData && !node.attrs.questionText) {
                            // Update node attrs with full question data
                            const mcq = questionData as { options?: string[]; correctAnswerIndex?: number };
                            const waq = questionData as { correctAnswer?: string; markScheme?: string };

                            tr.setNodeMarkup(pos, undefined, {
                                ...node.attrs,
                                questionText: questionData.questionText,
                                questionType: questionData.questionType,
                                options: mcq.options || [],
                                correctAnswer: mcq.correctAnswerIndex ?? waq.correctAnswer,
                                markScheme: waq.markScheme,
                                maxScore: questionData.maxScore || 1,
                                materialType: material.materialType,
                            });
                            modified = true;
                        }
                    }
                });

                if (modified) {
                    tr.setMeta('addToHistory', false);
                    editor.view.dispatch(tr);
                }
            } catch (err) {
                console.error('Failed to load question data:', err);
            }
        };

        // Load after a short delay to ensure editor is ready
        const timeoutId = setTimeout(loadQuestionData, 100);
        return () => clearTimeout(timeoutId);
    }, [editor, material.id, material.materialType]);

    // Orphan removal per §4C(5) - runs on editor enter and exit
    // Delete attachments and questions not referenced in the material content
    const removeOrphans = useCallback(async () => {
        // Guard against concurrent executions (race condition prevention)
        if (isRemovingOrphansRef.current) {
            console.log('Orphan removal already in progress, skipping');
            return;
        }
        if (!editor) return;

        isRemovingOrphansRef.current = true;

        try {
            // Fetch all attachments and questions for this material
            const [attachments, questions] = await Promise.all([
                signalRService.getAttachmentsUnderMaterial(material.id),
                signalRService.getQuestionsUnderMaterial(material.id),
            ]);

            if (attachments.length === 0 && questions.length === 0) return;

            // Parse current editor content (not stored content) for referenced IDs
            const content = getMarkdownContent();

            // Find referenced attachment IDs from markdown: ![...](/attachments/{uuid})
            const attachmentRefs = new Set<string>();
            const attachmentMatches = content.matchAll(/!\[.*?\]\(\/attachments\/([a-f0-9-]+)\)/gi);
            for (const match of attachmentMatches) {
                attachmentRefs.add(match[1]);
            }
            // Also check for PDF embeds: !!! pdf id="{uuid}"
            const pdfMatches = content.matchAll(/!!! pdf id="([a-f0-9-]+)"/gi);
            for (const match of pdfMatches) {
                attachmentRefs.add(match[1]);
            }

            // Find referenced question IDs: !!! question id="{uuid}"
            const questionRefs = new Set<string>();
            const questionMatches = content.matchAll(/!!! question id="([a-f0-9-]+)"/gi);
            for (const match of questionMatches) {
                questionRefs.add(match[1]);
            }

            // Delete orphan attachments (entity + file per §4C(5)(b)(i))
            for (const att of attachments) {
                if (!attachmentRefs.has(att.id)) {
                    console.log('Deleting orphan attachment:', att.id);
                    await signalRService.deleteAttachment(att.id);
                    await window.electronAPI.deleteAttachmentFile(att.id, att.fileExtension);
                }
            }

            // Delete orphan questions per §4C(5)(b)(ii)
            for (const q of questions) {
                if (!questionRefs.has(q.id)) {
                    console.log('Deleting orphan question:', q.id);
                    await signalRService.deleteQuestion(q.id);
                }
            }
        } catch (err) {
            console.error('Failed to remove orphans:', err);
        } finally {
            isRemovingOrphansRef.current = false;
        }
    }, [editor, material.id, getMarkdownContent]);

    // Run orphan removal on editor mount per §4C(5)
    useEffect(() => {
        // Run after a short delay to ensure content is loaded
        const timeoutId = setTimeout(removeOrphans, 200);
        return () => clearTimeout(timeoutId);
    }, [removeOrphans]);

    // Listen for question edit/delete events from QuestionRefComponent
    useEffect(() => {
        const handleEditEvent = async (e: CustomEvent<{ questionId: string }>) => {
            const questionId = e.detail.questionId;
            // Fetch question data from backend
            try {
                const questions = await signalRService.getQuestionsUnderMaterial(material.id);
                const question = questions.find(q => q.id === questionId);
                if (question) {
                    // Convert backend entity to dialog format
                    const dialogQuestion: QuestionEntity = {
                        id: question.id,
                        materialId: question.materialId,
                        questionType: question.questionType,
                        questionText: question.questionText,
                        options: (question as { options?: string[] }).options,
                        correctAnswer: (question as { correctAnswerIndex?: number }).correctAnswerIndex ??
                            (question as { correctAnswer?: string }).correctAnswer,
                        maxScore: question.maxScore,
                        markScheme: (question as { markScheme?: string }).markScheme,
                    };
                    setQuestionDialog({ isOpen: true, question: dialogQuestion });
                }
            } catch (err) {
                console.error('Failed to load question for editing:', err);
            }
        };

        // Question deletion deferred per §4C(3)(c)(i) [DELETED]
        // The question reference is removed from editor content; actual deletion happens on orphan removal
        const handleDeleteEvent = (_e: CustomEvent<{ questionId: string }>) => {
            // Deletion is deferred to orphan removal on exit per §4C(5)
            setIsDirty(true);
        };

        // Attachment deletion deferred per §4C(4)(b)(i) [DELETED]
        // The attachment reference is removed from editor content; actual deletion happens on orphan removal
        const handleAttachmentDeleteEvent = (_e: CustomEvent<{ attachmentId: string; fileExtension: string }>) => {
            // Deletion is deferred to orphan removal on exit per §4C(5)
            setIsDirty(true);
        };

        window.addEventListener('question-edit', handleEditEvent as EventListener);
        window.addEventListener('question-delete', handleDeleteEvent as EventListener);
        window.addEventListener('attachment-delete', handleAttachmentDeleteEvent as EventListener);

        return () => {
            window.removeEventListener('question-edit', handleEditEvent as EventListener);
            window.removeEventListener('question-delete', handleDeleteEvent as EventListener);
            window.removeEventListener('attachment-delete', handleAttachmentDeleteEvent as EventListener);
        };
    }, [material.id]);

    // Mark dirty on metadata change
    const handleTitleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setTitle(e.target.value);
        setIsDirty(true);
    };

    // Close and save any pending changes, run orphan removal per §4C(5)
    const handleClose = async () => {
        if (isDirty) {
            await saveContent();
        }
        // Run orphan removal on exit per §4C(5)
        // Wrapped in try-catch to ensure modal closes even if orphan removal fails
        try {
            await removeOrphans();
        } catch (err) {
            console.error('Orphan removal failed on close:', err);
        }
        onClose();
    };

    const closeInputDialog = () => {
        setInputDialog({ isOpen: false, title: '', onSubmit: () => { } });
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

    // Insert question reference - opens QuestionEditorDialog per s4C(3)
    const insertQuestion = () => {
        setQuestionDialog({ isOpen: true, question: undefined });
    };

    // Handle question save - creates or updates via SignalR per s4C(3)(b)
    const handleQuestionSave = async (question: QuestionEntity): Promise<string> => {
        const isNew = !question.id || question.id.startsWith('temp-');

        if (isNew) {
            // Create new question - per s4C(3)(b)(ii)
            // Convert QuestionEntity to InternalCreateQuestionDto
            const createDto = {
                materialId: question.materialId,
                questionType: question.questionType,
                questionText: question.questionText,
                options: question.options,
                correctAnswerIndex: typeof question.correctAnswer === 'number' ? question.correctAnswer : undefined,
                correctAnswer: typeof question.correctAnswer === 'string' ? question.correctAnswer : undefined,
                markScheme: question.markScheme,
                maxScore: question.maxScore,
            };

            const newId = await signalRService.createQuestion(createDto);

            // Insert question reference into editor with full data for WYSIWYG
            if (editor) {
                editor.chain().focus().insertContent({
                    type: 'questionRef',
                    attrs: {
                        id: newId,
                        questionText: question.questionText,
                        questionType: question.questionType,
                        options: question.options || [],
                        correctAnswer: question.correctAnswer,
                        markScheme: question.markScheme,
                        maxScore: question.maxScore || 1,
                        materialType: material.materialType,
                    },
                }).run();
            }

            setQuestionDialog({ isOpen: false });
            return newId;
        } else {
            // Update existing question - per s4C(3)(b)(i)
            // Convert QuestionEntity to InternalUpdateQuestionDto
            const updateDto = {
                id: question.id,
                materialId: question.materialId,
                questionType: question.questionType,
                questionText: question.questionText,
                options: question.options,
                correctAnswerIndex: typeof question.correctAnswer === 'number' ? question.correctAnswer : undefined,
                correctAnswer: typeof question.correctAnswer === 'string' ? question.correctAnswer : undefined,
                markScheme: question.markScheme,
                maxScore: question.maxScore,
            };

            await signalRService.updateQuestion(updateDto);

            // Update the node in the editor with new data
            if (editor) {
                const { doc, tr } = editor.state;
                doc.descendants((node, pos) => {
                    if (node.type.name === 'questionRef' && node.attrs.id === question.id) {
                        tr.setNodeMarkup(pos, undefined, {
                            ...node.attrs,
                            questionText: question.questionText,
                            questionType: question.questionType,
                            options: question.options || [],
                            correctAnswer: question.correctAnswer,
                            markScheme: question.markScheme,
                            maxScore: question.maxScore || 1,
                        });
                    }
                });
                editor.view.dispatch(tr);
            }

            setQuestionDialog({ isOpen: false });
            return question.id;
        }
    };

    // Handle question delete - per §4C(3)(c)
    // Deletion is deferred per §4C(3)(c)(i) [DELETED] - actual deletion happens in orphan removal on exit
    const handleQuestionDelete = async (questionId: string): Promise<void> => {
        // Remove question reference from content - per §4C(3)(c)(ii)
        if (editor) {
            const { doc, tr } = editor.state;

            // Collect ranges of all matching questionRef nodes
            const ranges: { from: number; to: number }[] = [];
            doc.descendants((node, pos) => {
                if (node.type.name === 'questionRef' && node.attrs.id === questionId) {
                    ranges.push({ from: pos, to: pos + node.nodeSize });
                }
            });

            if (ranges.length > 0) {
                // Delete from the end to avoid position shifts
                for (let i = ranges.length - 1; i >= 0; i--) {
                    const { from, to } = ranges[i];
                    tr.delete(from, to);
                }

                editor.view.dispatch(tr);
                setIsDirty(true);
            }
        }

        setQuestionDialog({ isOpen: false });
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

    // Insert attachment via file picker - per FrontendWorkflowSpecifications §4C(4)
    const insertAttachment = async () => {
        try {
            // Show file picker for allowed types
            const result = await window.electronAPI.showOpenDialog({
                properties: ['openFile'],
                filters: [
                    { name: 'Attachments', extensions: ['png', 'jpeg', 'jpg', 'pdf'] }
                ]
            });

            if (result.canceled || result.filePaths.length === 0) return;

            const filePath = result.filePaths[0];
            const fileName = filePath.split(/[/\\]/).pop() || '';
            const lastDotIndex = fileName.lastIndexOf('.');
            const fileBaseName = lastDotIndex > 0 ? fileName.substring(0, lastDotIndex) : fileName;
            let fileExtension = lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1).toLowerCase() : '';

            // Normalize jpg to jpeg
            if (fileExtension === 'jpg') fileExtension = 'jpeg';

            // Validate extension
            if (!['png', 'jpeg', 'pdf'].includes(fileExtension)) {
                console.error('Invalid file extension:', fileExtension);
                return;
            }

            // Create attachment entity via SignalR
            const dto: InternalCreateAttachmentDto = {
                materialId: material.id,
                fileBaseName,
                fileExtension
            };
            const attachmentId = await signalRService.createAttachment(dto);

            // Copy file to AppData with UUID filename
            // Per §4(4)(a1): if file save fails, rollback by deleting entity
            try {
                await window.electronAPI.saveAttachmentFile(filePath, attachmentId, fileExtension);
            } catch (fileSaveError) {
                console.error('File save failed, rolling back entity:', fileSaveError);
                await signalRService.deleteAttachment(attachmentId);
                throw fileSaveError;
            }

            // Get data URL for WYSIWYG display
            const dataUrl = await window.electronAPI.getAttachmentDataUrl(attachmentId, fileExtension);

            // Insert into editor based on type
            if (editor && dataUrl) {
                if (fileExtension === 'pdf') {
                    // Insert PDF embed
                    editor.chain().focus().insertContent({
                        type: 'pdfEmbed',
                        attrs: { id: attachmentId },
                    }).run();
                } else {
                    // Insert image with data URL for display
                    // The src will be converted to /attachments/{id} when saved to markdown
                    editor.chain().focus().insertContent({
                        type: 'image',
                        attrs: {
                            src: dataUrl,
                            alt: fileBaseName,
                            title: attachmentId, // Store ID for markdown conversion
                        },
                    }).run();
                }
            }
        } catch (error) {
            console.error('Error attaching file:', error);
        }
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
                    <ToolbarButton onClick={insertAttachment} title="Attach File (PNG, JPEG, PDF)">
                        📎
                    </ToolbarButton>

                    <ToolbarDivider />

                    {/* Undo/Redo - per §4C(2)(c) */}
                    <ToolbarButton
                        onClick={() => editor.chain().focus().undo().run()}
                        disabled={!editor.can().undo()}
                        title="Undo (Ctrl+Z)"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M3 7v6h6" />
                            <path d="M21 17a9 9 0 0 0-9-9 9 9 0 0 0-6 2.3L3 13" />
                        </svg>
                    </ToolbarButton>
                    <ToolbarButton
                        onClick={() => editor.chain().focus().redo().run()}
                        disabled={!editor.can().redo()}
                        title="Redo (Ctrl+Y)"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M21 7v6h-6" />
                            <path d="M3 17a9 9 0 0 1 9-9 9 9 0 0 1 6 2.3L21 13" />
                        </svg>
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

            {/* Question Editor Dialog - per s4C(3) */}
            <QuestionEditorDialog
                isOpen={questionDialog.isOpen}
                materialId={material.id}
                materialType={material.materialType}
                question={questionDialog.question}
                onSave={handleQuestionSave}
                onDelete={questionDialog.question?.id ? handleQuestionDelete : undefined}
                onCancel={() => setQuestionDialog({ isOpen: false })}
            />
        </div>,
        document.body
    );
};
