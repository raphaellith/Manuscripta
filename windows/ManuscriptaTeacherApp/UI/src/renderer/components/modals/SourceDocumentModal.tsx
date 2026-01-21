/**
 * Modal for managing source documents for a unit collection.
 * Provisional demo with dummy data per s4A(3)(a).
 */

import React, { useState } from 'react';

interface SourceDocumentModalProps {
    collectionId: string;
    collectionTitle: string;
    onClose: () => void;
}

// Dummy source documents for demo
const dummySourceDocuments = [
    { id: 'src-1', name: 'Year 5 English Curriculum.pdf', addedDate: '2024-01-10' },
    { id: 'src-2', name: 'Reading Comprehension Objectives.docx', addedDate: '2024-01-12' },
];

export const SourceDocumentModal: React.FC<SourceDocumentModalProps> = ({
    collectionTitle,
    onClose,
}) => {
    const [documents, setDocuments] = useState(dummySourceDocuments);
    const [isAdding, setIsAdding] = useState(false);

    const handleAddDocument = () => {
        setIsAdding(true);
        // Simulate file selection delay
        setTimeout(() => {
            const newDoc = {
                id: `src-${Date.now()}`,
                name: 'New Document.pdf',
                addedDate: new Date().toISOString().split('T')[0],
            };
            setDocuments(prev => [...prev, newDoc]);
            setIsAdding(false);
        }, 500);
    };

    const handleRemoveDocument = (docId: string) => {
        setDocuments(prev => prev.filter(d => d.id !== docId));
    };

    return (
        <div className="fixed inset-0 bg-text-heading/20 backdrop-blur-sm flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-lg p-6 shadow-2xl w-full max-w-lg space-y-4 animate-fade-in-up border border-gray-100">
                <div className="flex items-center justify-between">
                    <h2 className="text-xl font-serif text-text-heading">
                        Source Documents
                    </h2>
                    <span className="text-sm text-gray-400 italic">{collectionTitle}</span>
                </div>

                <p className="text-sm text-gray-500">
                    Attach reference documents like syllabi, learning objectives, or vocabulary lists.
                    These can be used by AI when generating materials.
                </p>

                {/* Document List */}
                <div className="border border-gray-200 rounded-lg divide-y divide-gray-100 max-h-64 overflow-y-auto">
                    {documents.length === 0 ? (
                        <p className="p-4 text-sm text-gray-400 text-center">No documents attached yet</p>
                    ) : (
                        documents.map(doc => (
                            <div key={doc.id} className="flex items-center gap-3 p-3 hover:bg-gray-50 group">
                                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-brand-blue flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                                </svg>
                                <div className="flex-1 min-w-0">
                                    <p className="text-sm text-text-body truncate">{doc.name}</p>
                                    <p className="text-xs text-gray-400">Added {doc.addedDate}</p>
                                </div>
                                <button
                                    onClick={() => handleRemoveDocument(doc.id)}
                                    className="opacity-0 group-hover:opacity-100 p-1 hover:bg-red-100 rounded transition-all text-red-500"
                                    title="Remove document"
                                >
                                    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                    </svg>
                                </button>
                            </div>
                        ))
                    )}
                </div>

                {/* Actions */}
                <div className="flex gap-3 pt-2">
                    <button
                        onClick={handleAddDocument}
                        disabled={isAdding}
                        className="flex items-center gap-2 px-4 py-2 bg-brand-blue text-white font-sans font-medium text-sm rounded-md hover:bg-brand-blue/90 transition-colors disabled:opacity-50"
                    >
                        {isAdding ? (
                            <>
                                <svg className="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                </svg>
                                Adding...
                            </>
                        ) : (
                            <>
                                <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                                </svg>
                                Add Document
                            </>
                        )}
                    </button>
                    <button
                        onClick={onClose}
                        className="px-4 py-2 bg-white text-gray-600 border border-gray-300 font-sans font-medium text-sm rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors"
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
        </div>
    );
};
