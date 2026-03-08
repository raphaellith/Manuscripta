/**
 * Modal for managing source documents within a unit collection.
 * Per FrontendWorkflowSpecifications §4AA.
 *
 * Provides:
 * - Listing source documents for a unit collection §4AA(1)
 * - Uploading via file (PDF, DOCX) or plain text §4AA(2)(a-c)
 * - Editing transcript §4AA(3)(a)
 * - Deleting source documents §4AA(4)(a)
 * - Error indicator and retry for FAILED embedding status §4AA(5)
 * - Polling embedding status §4AA(2)(e)
 */

import React, { useState, useEffect, useRef, useCallback } from 'react';
import { ModalOverlay } from './ModalOverlay';
import { useAppContext } from '../../state/AppContext';
import type { SourceDocumentEntity, EmbeddingStatus } from '../../models';

interface SourceDocumentModalProps {
    unitCollectionId: string;
    unitCollectionTitle: string;
    onClose: () => void;
}

type UploadMode = 'none' | 'file' | 'text';

/**
 * Extracts text from a PDF file buffer using pdfjs-dist.
 * Per §4AA(2)(b): Create textual transcript from uploaded file.
 */
async function extractPdfText(buffer: ArrayBuffer): Promise<string> {
    // Dynamic import to avoid loading pdfjs-dist unless needed
    const pdfjsLib = await import('pdfjs-dist');
    // Set the worker source for pdfjs - use CDN-hosted worker matching installed version
    const pdfjsVersion = pdfjsLib.version;
    pdfjsLib.GlobalWorkerOptions.workerSrc = `https://unpkg.com/pdfjs-dist@${pdfjsVersion}/build/pdf.worker.min.mjs`;
    const doc = await pdfjsLib.getDocument({ data: new Uint8Array(buffer) }).promise;
    const pages: string[] = [];
    for (let i = 1; i <= doc.numPages; i++) {
        const page = await doc.getPage(i);
        const content = await page.getTextContent();
        const text = content.items
            .filter((item): item is typeof item & { str: string } => 'str' in item)
            .map(item => item.str)
            .join(' ');
        pages.push(text);
    }
    return pages.join('\n\n');
}

/**
 * Extracts text from a DOCX file buffer using mammoth.
 * Per §4AA(2)(b): Create textual transcript from uploaded file.
 * NOTE: mammoth is loaded via script tag in index.html and available as window.mammoth
 */
async function extractDocxText(buffer: ArrayBuffer): Promise<string> {
    // mammoth is loaded via script tag as a UMD global
    const mammoth = (window as unknown as { mammoth: { extractRawText: (opts: { arrayBuffer: ArrayBuffer }) => Promise<{ value: string }> } }).mammoth;
    if (!mammoth) {
        throw new Error('mammoth.js library not loaded');
    }
    const result = await mammoth.extractRawText({ arrayBuffer: buffer });
    return result.value;
}

/**
 * Returns a human-readable label for an EmbeddingStatus value.
 */
function getStatusLabel(status?: EmbeddingStatus): string {
    switch (status) {
        case 'PENDING': return 'Indexing...';
        case 'INDEXED': return 'Indexed';
        case 'FAILED': return 'Indexing Failed';
        default: return 'Not indexed';
    }
}

/**
 * Returns CSS classes for embedding status badge.
 */
function getStatusClasses(status?: EmbeddingStatus): string {
    switch (status) {
        case 'PENDING': return 'bg-yellow-100 text-yellow-700';
        case 'INDEXED': return 'bg-green-100 text-green-700';
        case 'FAILED': return 'bg-red-100 text-red-700';
        default: return 'bg-gray-100 text-gray-500';
    }
}

export const SourceDocumentModal: React.FC<SourceDocumentModalProps> = ({
    unitCollectionId,
    unitCollectionTitle,
    onClose,
}) => {
    const {
        getSourceDocumentsForCollection,
        createSourceDocument,
        updateSourceDocument,
        deleteSourceDocument,
        setSourceDocumentEmbeddingStatus,
        getEmbeddingStatus,
        retryEmbedding,
    } = useAppContext();

    const sourceDocuments = getSourceDocumentsForCollection(unitCollectionId);

    // Upload state
    const [uploadMode, setUploadMode] = useState<UploadMode>('none');
    const [plainTextInput, setPlainTextInput] = useState('');
    const [isUploading, setIsUploading] = useState(false);
    const [uploadError, setUploadError] = useState<string | null>(null);

    // Edit state
    const [editingDocId, setEditingDocId] = useState<string | null>(null);
    const [editTranscript, setEditTranscript] = useState('');
    const [isSaving, setIsSaving] = useState(false);
    const [saveError, setSaveError] = useState<string | null>(null);

    // General error
    const [actionError, setActionError] = useState<string | null>(null);

    // Per §4AA(2)(e): Poll embedding status for PENDING documents
    const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null);

    const pollEmbeddingStatuses = useCallback(async () => {
        const pendingDocs = sourceDocuments.filter(sd => sd.embeddingStatus === 'PENDING');
        if (pendingDocs.length === 0) return;

        for (const doc of pendingDocs) {
            try {
                const status = await getEmbeddingStatus(doc.id);
                if (status !== doc.embeddingStatus) {
                    // Update local state only; do not call updateSourceDocument
                    // to avoid triggering backend re-indexing (§4AA(3))
                    setSourceDocumentEmbeddingStatus(doc.id, status);
                }
            } catch {
                // Silently ignore polling errors
            }
        }
    }, [sourceDocuments, getEmbeddingStatus, setSourceDocumentEmbeddingStatus]);

    useEffect(() => {
        const hasPending = sourceDocuments.some(sd => sd.embeddingStatus === 'PENDING');
        if (hasPending) {
            pollingRef.current = setInterval(pollEmbeddingStatuses, 3000);
        }
        return () => {
            if (pollingRef.current) {
                clearInterval(pollingRef.current);
                pollingRef.current = null;
            }
        };
    }, [sourceDocuments, pollEmbeddingStatuses]);

    /**
     * Per §4AA(2)(a-c): Handle file upload - select file, extract transcript, create source document.
     */
    const handleFileUpload = async () => {
        setUploadError(null);
        try {
            const result = await window.electronAPI.showOpenDialog({
                title: 'Select Source Document',
                filters: [
                    { name: 'Documents', extensions: ['pdf', 'docx', 'txt'] },
                ],
                properties: ['openFile'],
            });

            if (result.canceled || result.filePaths.length === 0) return;

            setIsUploading(true);
            const filePath = result.filePaths[0];
            const fileToken = result.fileTokens[0];
            const extension = filePath.split('.').pop()?.toLowerCase() || '';

            // Per §4AA(2)(b): Create textual transcript of the document
            let transcript: string;
            const buffer = await window.electronAPI.readFileBuffer(fileToken);

            if (extension === 'pdf') {
                transcript = await extractPdfText(buffer);
            } else if (extension === 'docx') {
                transcript = await extractDocxText(buffer);
            } else {
                // Plain text files - decode as UTF-8
                const decoder = new TextDecoder('utf-8');
                transcript = decoder.decode(new Uint8Array(buffer));
            }

            if (!transcript.trim()) {
                setUploadError('The selected file appears to be empty or could not be read.');
                setIsUploading(false);
                return;
            }

            // Per §4AA(2)(c): Invoke CreateSourceDocument
            await createSourceDocument({
                unitCollectionId,
                transcript: transcript.trim(),
            });

            setUploadMode('none');
        } catch (err) {
            console.error('File upload failed:', err);
            setUploadError('Failed to process the selected file. Please try again.');
        } finally {
            setIsUploading(false);
        }
    };

    /**
     * Per §4AA(2)(a,c): Handle plain text submission.
     */
    const handlePlainTextSubmit = async () => {
        if (!plainTextInput.trim()) {
            setUploadError('Please enter some text.');
            return;
        }

        setIsUploading(true);
        setUploadError(null);
        try {
            // Per §4AA(2)(c): Invoke CreateSourceDocument
            await createSourceDocument({
                unitCollectionId,
                transcript: plainTextInput.trim(),
            });
            setPlainTextInput('');
            setUploadMode('none');
        } catch (err) {
            console.error('Plain text upload failed:', err);
            setUploadError('Failed to create source document. Please try again.');
        } finally {
            setIsUploading(false);
        }
    };

    /**
     * Per §4AA(3)(a): Begin editing a source document transcript.
     */
    const handleStartEdit = (doc: SourceDocumentEntity) => {
        setEditingDocId(doc.id);
        setEditTranscript(doc.transcript);
        setSaveError(null);
    };

    /**
     * Per §4AA(3)(a): Save edited transcript via UpdateSourceDocument.
     */
    const handleSaveEdit = async (doc: SourceDocumentEntity) => {
        if (!editTranscript.trim()) {
            setSaveError('Transcript cannot be empty.');
            return;
        }

        setIsSaving(true);
        setSaveError(null);
        try {
            await updateSourceDocument({
                ...doc,
                transcript: editTranscript.trim(),
            });
            setEditingDocId(null);
            setEditTranscript('');
        } catch (err) {
            console.error('Failed to update source document:', err);
            setSaveError('Failed to save changes. Please try again.');
        } finally {
            setIsSaving(false);
        }
    };

    /**
     * Per §4AA(4)(a): Delete a source document.
     */
    const handleDelete = async (docId: string) => {
        setActionError(null);
        try {
            await deleteSourceDocument(docId);
            if (editingDocId === docId) {
                setEditingDocId(null);
                setEditTranscript('');
            }
        } catch (err) {
            console.error('Failed to delete source document:', err);
            setActionError('Failed to delete source document. Please try again.');
        }
    };

    /**
     * Per §4AA(5)(b): Retry indexing for a FAILED source document.
     */
    const handleRetryEmbedding = async (doc: SourceDocumentEntity) => {
        setActionError(null);
        try {
            await retryEmbedding(doc.id);
        } catch (err) {
            console.error('Failed to retry embedding:', err);
            setActionError('Failed to retry indexing. Please try again.');
        }
    };

    const cancelEdit = () => {
        setEditingDocId(null);
        setEditTranscript('');
        setSaveError(null);
    };

    return (
        <ModalOverlay priority="low" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
            <div
                className="bg-white rounded-lg shadow-2xl w-full max-w-2xl max-h-[80vh] flex flex-col animate-fade-in-up border border-gray-100"
                onClick={(e) => e.stopPropagation()}
            >
                {/* Header */}
                <div className="p-6 border-b border-gray-100 flex-shrink-0">
                    <h2 className="text-xl font-serif text-text-heading">Source Documents</h2>
                    <p className="text-sm text-gray-500 mt-1">{unitCollectionTitle}</p>
                </div>

                {/* Content */}
                <div className="flex-1 overflow-y-auto p-6 space-y-4">
                    {actionError && (
                        <div className="p-3 bg-red-50 text-red-700 text-sm rounded-md border border-red-200">
                            {actionError}
                        </div>
                    )}

                    {/* Source document list */}
                    {sourceDocuments.length === 0 && uploadMode === 'none' && (
                        <div className="text-center py-8">
                            <p className="text-sm text-gray-400 mb-2">No source documents yet</p>
                            <p className="text-xs text-gray-400">Upload a file or enter text to add source documents for AI generation.</p>
                        </div>
                    )}

                    {sourceDocuments.map(doc => (
                        <div
                            key={doc.id}
                            className={`border rounded-lg p-4 ${
                                doc.embeddingStatus === 'FAILED'
                                    ? 'border-red-300 bg-red-50/50'
                                    : 'border-gray-200'
                            }`}
                        >
                            <div className="flex items-center justify-between mb-2">
                                <div className="flex items-center gap-2">
                                    {/* Document icon */}
                                    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-gray-400 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                                    </svg>
                                    {/* Per §4AA(5)(a): Status indicator */}
                                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${getStatusClasses(doc.embeddingStatus)}`}>
                                        {getStatusLabel(doc.embeddingStatus)}
                                    </span>
                                    {/* Per §4AA(5)(a): Error indicator for FAILED */}
                                    {doc.embeddingStatus === 'FAILED' && (
                                        <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
                                        </svg>
                                    )}
                                </div>
                                <div className="flex items-center gap-1">
                                    {/* Per §4AA(5)(b): Retry Indexing button for FAILED status */}
                                    {doc.embeddingStatus === 'FAILED' && (
                                        <button
                                            onClick={() => handleRetryEmbedding(doc)}
                                            className="text-xs px-2 py-1 text-yellow-700 bg-yellow-100 hover:bg-yellow-200 rounded transition-colors"
                                            title="Retry Indexing"
                                        >
                                            Retry Indexing
                                        </button>
                                    )}
                                    {/* Edit button */}
                                    {editingDocId !== doc.id && (
                                        <button
                                            onClick={() => handleStartEdit(doc)}
                                            className="p-1 text-gray-400 hover:text-brand-orange rounded transition-colors"
                                            aria-label="Edit transcript"
                                            title="Edit transcript"
                                        >
                                            <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                            </svg>
                                        </button>
                                    )}
                                    {/* Per §4AA(4)(a) and §4AA(5)(c): Delete button */}
                                    <button
                                        onClick={() => handleDelete(doc.id)}
                                        className="p-1 text-gray-400 hover:text-red-500 rounded transition-colors"
                                        aria-label="Delete source document"
                                        title="Delete"
                                    >
                                        <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                        </svg>
                                    </button>
                                </div>
                            </div>

                            {/* Edit mode */}
                            {editingDocId === doc.id ? (
                                <div className="space-y-2">
                                    <textarea
                                        value={editTranscript}
                                        onChange={(e) => { setEditTranscript(e.target.value); setSaveError(null); }}
                                        className="w-full h-32 p-3 text-sm text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none resize-y"
                                    />
                                    {saveError && <p className="text-red-500 text-xs">{saveError}</p>}
                                    <div className="flex gap-2">
                                        <button
                                            onClick={() => handleSaveEdit(doc)}
                                            disabled={isSaving}
                                            className="px-3 py-1.5 text-sm bg-brand-orange text-white rounded-md hover:bg-brand-orange-dark transition-colors disabled:opacity-50"
                                        >
                                            {isSaving ? 'Saving...' : 'Save'}
                                        </button>
                                        <button
                                            onClick={cancelEdit}
                                            className="px-3 py-1.5 text-sm text-gray-600 border border-gray-300 rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors"
                                        >
                                            Cancel
                                        </button>
                                    </div>
                                </div>
                            ) : (
                                /* Display mode - show truncated transcript */
                                <p className="text-sm text-gray-600 line-clamp-3 whitespace-pre-wrap">
                                    {doc.transcript}
                                </p>
                            )}
                        </div>
                    ))}

                    {/* Upload area */}
                    {uploadMode === 'none' && (
                        <div className="flex gap-3 pt-2">
                            {/* Per §4AA(2)(a): Prompt to select a file */}
                            <button
                                onClick={() => { setUploadMode('file'); setUploadError(null); }}
                                className="flex-1 p-4 border-2 border-dashed border-gray-300 rounded-lg hover:border-brand-orange hover:bg-brand-orange/5 transition-colors text-center"
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 mx-auto text-gray-400 mb-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                                </svg>
                                <span className="text-sm text-gray-600 font-medium">Upload File</span>
                                <span className="block text-xs text-gray-400 mt-0.5">PDF, DOCX, or TXT</span>
                            </button>
                            {/* Per §4AA(2)(a): Prompt to enter plain text */}
                            <button
                                onClick={() => { setUploadMode('text'); setUploadError(null); }}
                                className="flex-1 p-4 border-2 border-dashed border-gray-300 rounded-lg hover:border-brand-orange hover:bg-brand-orange/5 transition-colors text-center"
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 mx-auto text-gray-400 mb-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                </svg>
                                <span className="text-sm text-gray-600 font-medium">Enter Text</span>
                                <span className="block text-xs text-gray-400 mt-0.5">Paste or type content</span>
                            </button>
                        </div>
                    )}

                    {/* File upload mode */}
                    {uploadMode === 'file' && (
                        <div className="border border-gray-200 rounded-lg p-4 space-y-3">
                            <p className="text-sm text-text-heading font-medium">Upload a source document</p>
                            {uploadError && <p className="text-red-500 text-sm">{uploadError}</p>}
                            <div className="flex gap-2">
                                <button
                                    onClick={handleFileUpload}
                                    disabled={isUploading}
                                    className="px-4 py-2 text-sm bg-brand-orange text-white rounded-md hover:bg-brand-orange-dark transition-colors disabled:opacity-50"
                                >
                                    {isUploading ? 'Processing...' : 'Select File'}
                                </button>
                                <button
                                    onClick={() => { setUploadMode('none'); setUploadError(null); }}
                                    disabled={isUploading}
                                    className="px-4 py-2 text-sm text-gray-600 border border-gray-300 rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors disabled:opacity-50"
                                >
                                    Cancel
                                </button>
                            </div>
                        </div>
                    )}

                    {/* Plain text input mode */}
                    {uploadMode === 'text' && (
                        <div className="border border-gray-200 rounded-lg p-4 space-y-3">
                            <p className="text-sm text-text-heading font-medium">Enter source document text</p>
                            <textarea
                                value={plainTextInput}
                                onChange={(e) => { setPlainTextInput(e.target.value); setUploadError(null); }}
                                className="w-full h-40 p-3 text-sm text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none resize-y"
                                placeholder="Paste or type the source document content here..."
                                autoFocus
                            />
                            {uploadError && <p className="text-red-500 text-sm">{uploadError}</p>}
                            <div className="flex gap-2">
                                <button
                                    onClick={handlePlainTextSubmit}
                                    disabled={isUploading}
                                    className="px-4 py-2 text-sm bg-brand-orange text-white rounded-md hover:bg-brand-orange-dark transition-colors disabled:opacity-50"
                                >
                                    {isUploading ? 'Saving...' : 'Add Document'}
                                </button>
                                <button
                                    onClick={() => { setUploadMode('none'); setPlainTextInput(''); setUploadError(null); }}
                                    disabled={isUploading}
                                    className="px-4 py-2 text-sm text-gray-600 border border-gray-300 rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors disabled:opacity-50"
                                >
                                    Cancel
                                </button>
                            </div>
                        </div>
                    )}
                </div>

                {/* Footer */}
                <div className="p-6 border-t border-gray-100 flex-shrink-0">
                    <button
                        onClick={onClose}
                        className="px-6 py-2.5 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors"
                    >
                        Close
                    </button>
                </div>
            </div>
            <style>{`
                @keyframes fade-in-up {
                    from { opacity: 0; transform: translateY(20px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .animate-fade-in-up { animation: fade-in-up 0.3s ease-out forwards; }
            `}</style>
        </ModalOverlay>
    );
};
