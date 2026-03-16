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
import TextAlign from '@tiptap/extension-text-align';
import Underline from '@tiptap/extension-underline';
import { InlineLatex, BlockLatex, LatexFormattingGuard, QuestionRef, PdfEmbed, AttachmentImage } from './extensions';
import { QuestionEditorDialog } from './QuestionEditorDialog';
import { htmlToMarkdown, markdownToHtml } from '../../utils/markdownConversion';
import type { MaterialEntity, QuestionEntity, InternalCreateAttachmentDto, PdfExportSettingsEntity, LinePatternType, LineSpacingPreset, FontSizePreset, ValidationWarning } from '../../models';
import { useAppContext } from '../../state/AppContext';
import signalRService from '../../services/signalr/SignalRService';
import { StreamingGenerationView } from './StreamingGenerationView';
import { BubbleMenu } from '@tiptap/react/menus';
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
        aria-label={title}
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
    const [linePatternType, setLinePatternType] = useState<LinePatternType | null | undefined>(material.linePatternType);
    const [lineSpacingPreset, setLineSpacingPreset] = useState<LineSpacingPreset | null | undefined>(material.lineSpacingPreset);
    const [fontSizePreset, setFontSizePreset] = useState<FontSizePreset | null | undefined>(material.fontSizePreset);
    const [pdfDefaults, setPdfDefaults] = useState<PdfExportSettingsEntity | null>(null);
    const [showPdfSettingsModal, setShowPdfSettingsModal] = useState(false);
    const [isDirty, setIsDirty] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const [isExportingPdf, setIsExportingPdf] = useState(false);
    const [lastSaved, setLastSaved] = useState<Date | null>(null);
    // Force re-render counter to update toolbar button active states
    const [, setForceUpdate] = useState(0);

    // Dialog state
    const [inputDialog, setInputDialog] = useState<{
        isOpen: boolean;
        title: string;
        placeholder?: string;
        onSubmit: (value: string) => void;
    }>({ isOpen: false, title: '', onSubmit: () => { /* no-op */ } });
    const [confirmDialog, setConfirmDialog] = useState<{
        isOpen: boolean;
        onConfirm: () => void;
    }>({ isOpen: false, onConfirm: () => { /* no-op */ } });

    // Question editor dialog state - per s4C(3)
    const [questionDialog, setQuestionDialog] = useState<{
        isOpen: boolean;
        question?: QuestionEntity;
    }>({ isOpen: false });

    const autoSaveTimeoutRef = useRef<NodeJS.Timeout | null>(null);
    // Guard to prevent concurrent orphan removal executions
    const isRemovingOrphansRef = useRef(false);

    // AI Assistant state
    const [isAiGenerating, setIsAiGenerating] = useState(false);
    const [aiGenerationId, setAiGenerationId] = useState<string | null>(null);
    const [aiInstructionRaw, setAiInstructionRaw] = useState('');
    const [showAiInput, setShowAiInput] = useState(false);
    const [aiContentTokens, setAiContentTokens] = useState('');
    const [aiThinkingTokens, setAiThinkingTokens] = useState('');
    const [aiWarnings, setAiWarnings] = useState<ValidationWarning[]>([]);
    // Ref to cancel the active modification (set inside handleAiInstructionSubmit)
    const aiCancelRef = useRef<(() => void) | null>(null);
    // Ref to track current generation ID synchronously (avoids stale closures in event handlers)
    const aiGenerationIdRef = useRef<string | null>(null);



    // Convert stored markdown to HTML for editor
    const initialContent = markdownToHtml(material.content || '');

    // Initialize TipTap editor with all extensions
    const editor = useEditor({
        extensions: [
            StarterKit.configure({
                heading: {
                    levels: [1, 2, 3],
                },
                link: false, // Prevent link creation per Material Encoding Spec §1(4)
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
            TextAlign.configure({
                types: ['heading', 'paragraph'],
            }),
            Underline,
            // Custom extensions for Material Encoding
            InlineLatex,
            BlockLatex,
            LatexFormattingGuard,
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

    const handleAiInstructionSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        const instruction = aiInstructionRaw.trim();
        if (!instruction || !editor || isAiGenerating) return;

        setShowAiInput(false);
        setAiInstructionRaw('');

        // Capture selection range at submit time so the replacement is deterministic
        // even if the user focus changes during generation (§4C(2)(a)(iii))
        const { from, to } = editor.state.selection;

        const originalText = editor.state.doc.textBetween(from, to, '\n\n');

        // Per §4B(2)(a2) / §4C(2)(a)(iv): Disable editing during AI generation
        editor.setEditable(false);
        setIsAiGenerating(true);
        setAiGenerationId(null);
        aiGenerationIdRef.current = null;
        setAiWarnings([]);

        // Per §4B(2)(a1): Buffer streaming tokens to avoid setState per token (performance)
        let thinkingBuffer = '';
        let contentBuffer = '';
        let flushScheduled = false;

        const flushBufferedTokens = () => {
            if (!thinkingBuffer && !contentBuffer) {
                flushScheduled = false;
                return;
            }
            if (thinkingBuffer) {
                const chunk = thinkingBuffer;
                thinkingBuffer = '';
                setAiThinkingTokens(prev => prev + chunk);
            }
            if (contentBuffer) {
                const chunk = contentBuffer;
                contentBuffer = '';
                setAiContentTokens(prev => prev + chunk);
            }
            flushScheduled = false;
        };

        // Per NetworkingAPISpec §2(1)(h)(ii): Subscribe before invoking to capture ID
        const offStarted = signalRService.onGenerationStarted((id: string) => {
            aiGenerationIdRef.current = id;
            setAiGenerationId(id);
        });

        const offProgress = signalRService.onGenerationProgress((token: string, isThinking: boolean, done: boolean) => {
            if (isThinking) {
                thinkingBuffer += token;
            } else {
                contentBuffer += token;
            }
            if (!flushScheduled) {
                flushScheduled = true;
                window.requestAnimationFrame(flushBufferedTokens);
            }
            if (done) {
                flushBufferedTokens();
            }
        });

        const offCancelled = signalRService.onGenerationCancelled((cancelledId: string) => {
            if (cancelledId === aiGenerationIdRef.current) {
                setIsAiGenerating(false);
                setAiGenerationId(null);
                aiGenerationIdRef.current = null;
                setAiContentTokens('');
                setAiThinkingTokens('');
                editor.setEditable(true);
            }
        });

        // Store cancel accessor for handleCancelModification
        aiCancelRef.current = () => {
            offStarted();
            offProgress();
            offCancelled();
        };

        try {
            const result = await signalRService.modifyContent(
                originalText,
                instruction,
                material.materialType.toLowerCase(),
                material.title,
                material.readingAge,
                material.actualAge,
                material.id
            );

            offStarted();
            offProgress();
            offCancelled();
            aiCancelRef.current = null;

            // Per §4C(2)(a)(iii): Replace the originally selected range deterministically
            editor
                .chain()
                .focus()
                .insertContentAt({ from, to }, markdownToHtml(result.content))
                .run();

            // If the modification created new questions, populate their data in the editor nodes
            if (result.createdQuestionIds && result.createdQuestionIds.length > 0) {
                await loadQuestionData();
            }

            setIsAiGenerating(false);
            setAiGenerationId(null);
            aiGenerationIdRef.current = null;
            setAiContentTokens('');
            setAiThinkingTokens('');

            // Per §4C(7): Surface validation warnings if present
            if (result.warnings && result.warnings.length > 0) {
                setAiWarnings(result.warnings);
            }
        } catch (error) {
            offStarted();
            offProgress();
            offCancelled();
            aiCancelRef.current = null;

            console.error('Failed to invoke AI assistant:', error);
            setIsAiGenerating(false);
            aiGenerationIdRef.current = null;
            setAiGenerationId(null);
            setAiContentTokens('');
            setAiThinkingTokens('');
            // User cancellation shouldn't popup an alert.
        } finally {
            editor.setEditable(true);
        }
    };

    const handleCancelModification = async () => {
        const idToCancel = aiGenerationIdRef.current ?? aiGenerationId;
        if (!idToCancel) {
            return;
        }

        try {
            await signalRService.cancelGeneration(idToCancel);
            // Unsubscribe streaming handlers
            if (aiCancelRef.current) {
                aiCancelRef.current();
                aiCancelRef.current = null;
            }
            setIsAiGenerating(false);
            setAiGenerationId(null);
            aiGenerationIdRef.current = null;
            setAiContentTokens('');
            setAiThinkingTokens('');
            editor?.setEditable(true);
        } catch (err) {
            console.error('Failed to cancel generation:', err);
        }
    };

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
                linePatternType: linePatternType ?? null,
                lineSpacingPreset: lineSpacingPreset ?? null,
                fontSizePreset: fontSizePreset ?? null,
                timestamp: new Date().toISOString(),
            });
            setIsDirty(false);
            setLastSaved(new Date());
        } catch (err) {
            console.error('Failed to save:', err);
        } finally {
            setIsSaving(false);
        }
    }, [isDirty, editor, material, title, readingAge, actualAge, linePatternType, lineSpacingPreset, fontSizePreset, updateMaterial, getMarkdownContent, convertExternalImagesToAttachments]);

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

    // Load PDF export defaults for "Default (…)" labels per §4C(2)(b1)
    useEffect(() => {
        signalRService.getPdfExportSettings().then(setPdfDefaults).catch(console.error);
    }, []);

    // Cleanup AI streaming subscriptions on unmount to prevent setState on an unmounted component
    useEffect(() => {
        return () => {
            if (aiCancelRef.current) {
                aiCancelRef.current();
                aiCancelRef.current = null;
            }
            if (aiGenerationIdRef.current) {
                signalRService.cancelGeneration(aiGenerationIdRef.current).catch(() => {});
                aiGenerationIdRef.current = null;
            }
        };
    }, []);

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
                        // Match /attachments/{uuid} pattern - handles both relative paths and
                        // full URLs (browser may resolve relative paths to http://localhost:*/attachments/{uuid})
                        const match = src.match(/\/attachments\/([a-f0-9-]+)$/i);
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

    // Load question data for questionRef nodes that are missing it.
    // Extracted as a callback so it can be called on mount and after AI modification.
    const loadQuestionData = useCallback(async () => {
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
    }, [editor, material.id, material.materialType]);

    // Load question data for existing question references on mount
    useEffect(() => {
        const timeoutId = setTimeout(loadQuestionData, 100);
        return () => clearTimeout(timeoutId);
    }, [loadQuestionData]);

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
        setInputDialog({ isOpen: false, title: '', onSubmit: () => { /* no-op */ } });
    };

    // Export material to PDF - per FrontendWorkflowSpecifications §4D
    const handleExportPdf = async () => {
        if (isExportingPdf) return;
        setIsExportingPdf(true);

        try {
            // First save any unsaved changes
            if (isDirty) {
                await saveContent();
            }

            // Generate PDF via SignalR hub
            const pdfBytes = await signalRService.generateMaterialPdf(material.id);

            // Prompt user to save the file
            const defaultFilename = `${title.replace(/[^a-zA-Z0-9 ]/g, '')}.pdf`;
            const saved = await window.electronAPI.savePdfFile(pdfBytes, defaultFilename);

            if (saved) {
                console.log('PDF exported successfully');
            }
        } catch (error) {
            console.error('Failed to export PDF:', error);
            if (typeof window !== 'undefined' && window.alert) {
                window.alert('Failed to export PDF. Please try again.');
            }
        } finally {
            setIsExportingPdf(false);
        }
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

                        {/* Per §4C(2)(b1): Per-material PDF export overrides */}
                        <button
                            onClick={() => setShowPdfSettingsModal(true)}
                            className="px-3 py-1.5 text-sm border border-gray-300 rounded-md hover:bg-gray-100 text-gray-700 flex items-center gap-1.5"
                            title="PDF Conversion Settings"
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                                <path strokeLinecap="round" strokeLinejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                                <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                            </svg>
                            PDF Settings
                        </button>

                        {/* Save status */}
                        <div className="text-xs text-gray-400 min-w-[100px]">
                            {isSaving ? 'Saving...' : lastSaved ? `Saved ${lastSaved.toLocaleTimeString()}` : isDirty ? 'Unsaved changes' : 'No changes'}
                        </div>

                        {/* Export to PDF button */}
                        <button
                            onClick={handleExportPdf}
                            disabled={isExportingPdf}
                            className="px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                            title="Export to PDF"
                        >
                            {isExportingPdf ? (
                                <>
                                    <svg className="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                    Exporting...
                                </>
                            ) : (
                                'Export PDF'
                            )}
                        </button>

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
                        <span className="material-symbols-outlined text-base">format_align_left</span>
                    </ToolbarButton>
                    <ToolbarButton
                        onClick={() => editor.chain().focus().setTextAlign('center').run()}
                        isActive={editor.isActive({ textAlign: 'center' })}
                        title="Align Center"
                    >
                        <span className="material-symbols-outlined text-base">format_align_center</span>
                    </ToolbarButton>

                    <ToolbarDivider />

                    {/* Blocks */}
                    <ToolbarButton
                        onClick={() => editor.chain().focus().toggleBlockquote().run()}
                        isActive={editor.isActive('blockquote')}
                        title="Blockquote"
                    >
                        <span className="material-symbols-outlined text-base">format_indent_increase</span>
                    </ToolbarButton>
                    <ToolbarButton
                        onClick={() => editor.chain().focus().toggleCodeBlock().run()}
                        isActive={editor.isActive('codeBlock')}
                        title="Code Block"
                    >
                        <span className="material-symbols-outlined text-base">code</span>
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
                        <span className="material-symbols-outlined text-base">table_chart</span>
                    </ToolbarButton>

                    {/* Table Controls - only show when cursor is in a table */}
                    {isInTable && (
                        <>
                            <ToolbarDivider />
                            <span className="text-xs text-gray-500 px-1 self-center">Table:</span>
                            <ToolbarButton onClick={addColumnBefore} title="Add Column Before">
                                <span className="material-symbols-outlined text-base">add_column_left</span>
                            </ToolbarButton>
                            <ToolbarButton onClick={addColumnAfter} title="Add Column After">
                                <span className="material-symbols-outlined text-base">add_column_right</span>
                            </ToolbarButton>
                            <ToolbarButton onClick={addRowBefore} title="Add Row Above">
                                <span className="material-symbols-outlined text-base">add_row_above</span>
                            </ToolbarButton>
                            <ToolbarButton onClick={addRowAfter} title="Add Row Below">
                                <span className="material-symbols-outlined text-base">add_row_below</span>
                            </ToolbarButton>
                            <ToolbarButton onClick={deleteRow} title="Delete Row">
                                <span className="material-symbols-outlined text-base">delete</span>
                                <span className="material-symbols-outlined text-base">
                                arrows_left_right_circle
                                </span>
                            </ToolbarButton>
                            <ToolbarButton onClick={deleteColumn} title="Delete Column">
                                <span className="material-symbols-outlined text-base">delete</span>
                                <span className="material-symbols-outlined text-base">
                                arrows_up_down_circle
                                </span>
                            </ToolbarButton>
                            <ToolbarButton onClick={deleteTable} title="Delete Table">
                                <span className="material-symbols-outlined text-base">remove_selection</span>
                            </ToolbarButton>
                        </>
                    )}

                    <ToolbarDivider />

                    {/* LaTeX */}
                    <ToolbarButton onClick={insertInlineLatex} title="Inline LaTeX ($...$)">
                        Inline Maths
                    </ToolbarButton>
                    <ToolbarButton onClick={insertBlockLatex} title="Block LaTeX ($$...$$)">
                        Display Maths
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
                        <span className="material-symbols-outlined text-base">quiz</span>
                    </ToolbarButton>
                    <ToolbarButton onClick={insertAttachment} title="Attach File (PNG, JPEG, PDF)">
                        <span className="material-symbols-outlined text-base">attach_file</span>
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

            {/* AI Assistant Bubble Menu */}
            {editor && (
                <BubbleMenu
                    editor={editor}
                    options={{}}
                    shouldShow={({ editor, from, to }) => {
                        return from !== to && !editor.isActive('image') && !editor.isActive('pdfEmbed') && material.materialType !== 'POLL' && !isAiGenerating;
                    }}
                >
                    <div className="bg-white rounded-lg shadow-xl border border-gray-200 p-1 flex items-center gap-1">
                        {!showAiInput ? (
                            <button
                                onClick={() => setShowAiInput(true)}
                                className="flex items-center gap-2 px-3 py-1.5 text-sm font-medium text-brand-orange hover:bg-orange-50 rounded"
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path><polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline><line x1="12" y1="22.08" x2="12" y2="12"></line></svg>
                                AI Modify
                            </button>
                        ) : (
                            <form onSubmit={handleAiInstructionSubmit} className="flex items-center gap-2 px-2 py-1">
                                <input
                                    type="text"
                                    autoFocus
                                    value={aiInstructionRaw}
                                    onChange={(e) => setAiInstructionRaw(e.target.value)}
                                    placeholder="e.g. Make it simpler..."
                                    className="text-sm px-2 py-1 border border-brand-orange rounded outline-none focus:ring-1 focus:ring-brand-orange w-48"
                                />
                                <button type="submit" className="text-white bg-brand-orange hover:bg-orange-600 px-2 py-1 rounded text-sm font-medium">
                                    Go
                                </button>
                                <button type="button" onClick={() => setShowAiInput(false)} className="text-gray-500 hover:text-gray-700 font-bold px-1">
                                    ×
                                </button>
                            </form>
                        )}
                    </div>
                </BubbleMenu>
            )}

            {/* Per §4C(2)(a)(iv) / §4B(2)(a1): Full-screen streaming overlay during generation */}
            <StreamingGenerationView
                isVisible={isAiGenerating}
                thinkingTokens={aiThinkingTokens}
                contentTokens={aiContentTokens}
                isComplete={false}
                onCancel={handleCancelModification}
            />

            {/* Per §4C(7): Validation warnings banner after modification completes */}
            {aiWarnings.length > 0 && (
                <div className="absolute inset-x-8 bottom-8 z-50 bg-yellow-50 border border-yellow-400 rounded-lg shadow-lg p-4">
                    <div className="flex items-start justify-between gap-2">
                        <div className="flex items-start gap-2">
                            <svg className="h-5 w-5 text-yellow-500 mt-0.5 shrink-0" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                                <path fillRule="evenodd" d="M8.485 2.495c.673-1.167 2.357-1.167 3.03 0l6.28 10.875c.673 1.167-.17 2.625-1.516 2.625H3.72c-1.347 0-2.189-1.458-1.515-2.625L8.485 2.495zM10 5a.75.75 0 01.75.75v3.5a.75.75 0 01-1.5 0v-3.5A.75.75 0 0110 5zm0 9a1 1 0 100-2 1 1 0 000 2z" clipRule="evenodd" />
                            </svg>
                            <div>
                                <p className="text-sm font-semibold text-yellow-800">
                                    AI modification completed with {aiWarnings.length} validation warning{aiWarnings.length > 1 ? 's' : ''} — please review the content.
                                </p>
                                <ul className="mt-2 space-y-1">
                                    {aiWarnings.map((w, i) => (
                                        <li key={i} className="text-sm text-yellow-700">
                                            <span className="font-medium">[{w.errorType}]</span>
                                            {w.lineNumber !== undefined && (
                                                <span className="ml-1 text-yellow-600">(line {w.lineNumber})</span>
                                            )}
                                            <span className="ml-1">{w.description}</span>
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        </div>
                        <button
                            onClick={() => setAiWarnings([])}
                            className="text-yellow-600 hover:text-yellow-800 text-lg leading-none shrink-0"
                            aria-label="Dismiss warnings"
                        >
                            ×
                        </button>
                    </div>
                </div>
            )}

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
                    setConfirmDialog({ isOpen: false, onConfirm: () => { /* no-op */ } });
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

            {/* PDF Conversion Settings Modal - per §4C(2)(b1) */}
            {showPdfSettingsModal && (
                <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-[200]">
                    <div className="bg-white rounded-lg shadow-xl w-full max-w-md p-6">
                        <h2 className="text-lg font-semibold mb-4">PDF Conversion Settings</h2>
                        <p className="text-xs text-gray-500 mb-4">
                            Override the global defaults for this material. Per-device settings on external devices may further override these values. Select &quot;Default&quot; to use the global setting.
                        </p>

                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Line Pattern</label>
                                <select
                                    value={linePatternType ?? ''}
                                    onChange={(e) => { setLinePatternType(e.target.value ? e.target.value as LinePatternType : null); setIsDirty(true); }}
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-orange"
                                >
                                    <option value="">Default ({pdfDefaults ? { RULED: 'Ruled', SQUARE: 'Square', ISOMETRIC: 'Isometric', NONE: 'None' }[pdfDefaults.linePatternType] : '...'})</option>
                                    <option value="RULED">Ruled</option>
                                    <option value="SQUARE">Square</option>
                                    <option value="ISOMETRIC">Isometric</option>
                                    <option value="NONE">None</option>
                                </select>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Line Spacing</label>
                                <select
                                    value={lineSpacingPreset ?? ''}
                                    onChange={(e) => { setLineSpacingPreset(e.target.value ? e.target.value as LineSpacingPreset : null); setIsDirty(true); }}
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-orange"
                                >
                                    <option value="">Default ({pdfDefaults ? { SMALL: '6mm', MEDIUM: '8mm', LARGE: '10mm', EXTRA_LARGE: '14mm' }[pdfDefaults.lineSpacingPreset] : '...'})</option>
                                    <option value="SMALL">Small (6mm)</option>
                                    <option value="MEDIUM">Medium (8mm)</option>
                                    <option value="LARGE">Large (10mm)</option>
                                    <option value="EXTRA_LARGE">Extra Large (14mm)</option>
                                </select>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Font Size</label>
                                <select
                                    value={fontSizePreset ?? ''}
                                    onChange={(e) => { setFontSizePreset(e.target.value ? e.target.value as FontSizePreset : null); setIsDirty(true); }}
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-orange"
                                >
                                    <option value="">Default ({pdfDefaults ? { SMALL: '10pt', MEDIUM: '12pt', LARGE: '14pt', EXTRA_LARGE: '16pt' }[pdfDefaults.fontSizePreset] : '...'})</option>
                                    <option value="SMALL">Small (10pt)</option>
                                    <option value="MEDIUM">Medium (12pt)</option>
                                    <option value="LARGE">Large (14pt)</option>
                                    <option value="EXTRA_LARGE">Extra Large (16pt)</option>
                                </select>
                            </div>
                        </div>

                        <div className="flex justify-end mt-6">
                            <button
                                onClick={() => setShowPdfSettingsModal(false)}
                                className="px-4 py-2 bg-brand-orange text-white rounded-md hover:bg-orange-600 text-sm font-medium"
                            >
                                Done
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>,
        document.body
    );
};
