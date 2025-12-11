
import React, { useRef, useState } from 'react';
import type { ContentItem, VocabularyTerm } from '../types';

interface ContentEditorModalProps {
    contentItem: ContentItem;
    onClose: () => void;
    onSave: (updatedItem: ContentItem) => void;
}

const EditorToolbar: React.FC = () => {
    const applyStyle = (command: string) => (e: React.MouseEvent<HTMLButtonElement>) => {
        e.preventDefault(); // Prevent editor from losing focus
        document.execCommand(command, false);
    };

    return (
        <div className="editor-toolbar flex items-center gap-1 p-2 border-b border-gray-200 bg-brand-cream rounded-t-lg">
            <button title="Bold" onClick={applyStyle('bold')} className="font-bold w-10 h-10 rounded hover:bg-gray-200 text-text-heading transition-colors font-serif">B</button>
            <button title="Italic" onClick={applyStyle('italic')} className="italic w-10 h-10 rounded hover:bg-gray-200 text-text-heading transition-colors font-serif">I</button>
            <button title="Underline" onClick={applyStyle('underline')} className="underline w-10 h-10 rounded hover:bg-gray-200 text-text-heading transition-colors font-serif">U</button>
            <div className="w-px h-6 bg-gray-300 mx-2" />
            <button title="Bulleted List" onClick={applyStyle('insertUnorderedList')} className="w-10 h-10 rounded hover:bg-gray-200 text-text-heading transition-colors">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mx-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h7" />
                </svg>
            </button>
            <button title="Numbered List" onClick={applyStyle('insertOrderedList')} className="w-10 h-10 rounded hover:bg-gray-200 text-text-heading transition-colors">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mx-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h7M5 6V4M5 12v-2m0 8v-2m0-4h.01M5 20v-2" />
                </svg>
            </button>
        </div>
    );
};

interface VocabularyEditorProps {
    terms: VocabularyTerm[];
    onTermsChange: (terms: VocabularyTerm[]) => void;
}

const VocabularyEditor: React.FC<VocabularyEditorProps> = ({ terms, onTermsChange }) => {
    const [newTerm, setNewTerm] = useState('');
    const [newDefinition, setNewDefinition] = useState('');

    const handleAddTerm = () => {
        if (!newTerm.trim() || !newDefinition.trim()) return;
        onTermsChange([...terms, { term: newTerm.trim(), definition: newDefinition.trim() }]);
        setNewTerm('');
        setNewDefinition('');
    };

    const handleRemoveTerm = (index: number) => {
        onTermsChange(terms.filter((_, i) => i !== index));
    };

    return (
        <div className="flex flex-col h-full">
            <h3 className="font-sans font-semibold text-text-heading text-sm mb-3 flex items-center gap-2">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-brand-green" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                </svg>
                Vocabulary
            </h3>
            
            {/* Existing terms */}
            <div className="flex-1 overflow-y-auto space-y-2 mb-4">
                {terms.length === 0 ? (
                    <p className="text-xs text-gray-400 italic">No vocabulary terms yet. Add keywords below.</p>
                ) : (
                    terms.map((term, index) => (
                        <div key={index} className="bg-white rounded-md p-3 border border-gray-100 shadow-sm group relative">
                            <button 
                                onClick={() => handleRemoveTerm(index)}
                                className="absolute top-1 right-1 w-5 h-5 text-gray-400 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity"
                                title="Remove"
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                            <p className="font-sans font-semibold text-brand-green text-sm">{term.term}</p>
                            <p className="font-sans text-xs text-gray-600 mt-1">{term.definition}</p>
                        </div>
                    ))
                )}
            </div>
            
            {/* Add new term form */}
            <div className="border-t border-gray-200 pt-3 space-y-2">
                <input
                    type="text"
                    value={newTerm}
                    onChange={(e) => setNewTerm(e.target.value)}
                    placeholder="Keyword..."
                    className="w-full p-2 text-sm bg-white text-text-body font-sans rounded border border-gray-200 focus:border-brand-green focus:outline-none"
                />
                <textarea
                    value={newDefinition}
                    onChange={(e) => setNewDefinition(e.target.value)}
                    placeholder="Definition..."
                    rows={2}
                    className="w-full p-2 text-sm bg-white text-text-body font-sans rounded border border-gray-200 focus:border-brand-green focus:outline-none resize-none"
                />
                <button
                    onClick={handleAddTerm}
                    disabled={!newTerm.trim() || !newDefinition.trim()}
                    className="w-full py-2 bg-brand-green text-white font-sans font-medium text-sm rounded hover:bg-green-800 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                    + Add Term
                </button>
            </div>
        </div>
    );
};

export const ContentEditorModal: React.FC<ContentEditorModalProps> = ({ contentItem, onClose, onSave }) => {
    const editorRef = useRef<HTMLDivElement>(null);
    const [vocabularyTerms, setVocabularyTerms] = useState<VocabularyTerm[]>(contentItem.vocabularyTerms || []);
    
    const handleSave = () => {
        onSave({
            ...contentItem,
            content: editorRef.current?.innerHTML || '',
            vocabularyTerms: vocabularyTerms.length > 0 ? vocabularyTerms : undefined,
        });
    };

    return (
        <div className="fixed inset-0 bg-text-heading/20 backdrop-blur-sm flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-lg shadow-2xl w-full max-w-6xl animate-fade-in-up flex flex-col" style={{ maxHeight: '90vh' }}>
                {/* Header */}
                <div className="p-6 border-b border-gray-100 flex-shrink-0">
                    <h2 className="text-2xl font-serif text-text-heading">
                        Editing: <span className="text-brand-orange">{contentItem.title}</span>
                    </h2>
                </div>
                
                {/* Content area with sidebar */}
                <div className="flex flex-1 overflow-hidden">
                    {/* Main editor */}
                    <div className="flex-1 p-6 flex flex-col min-w-0">
                        <div className="border-2 border-brand-gray rounded-lg focus-within:border-brand-orange transition-all bg-white overflow-hidden flex-1 flex flex-col">
                            <EditorToolbar />
                            <div
                                ref={editorRef}
                                contentEditable={true}
                                suppressContentEditableWarning={true}
                                className="prose max-w-none p-6 flex-1 overflow-y-auto focus:outline-none font-serif text-text-body"
                                dangerouslySetInnerHTML={{ __html: contentItem.content || '' }}
                            />
                        </div>
                    </div>
                    
                    {/* Vocabulary sidebar */}
                    <div className="w-72 bg-brand-cream border-l border-gray-200 p-4 flex flex-col overflow-hidden flex-shrink-0">
                        <VocabularyEditor terms={vocabularyTerms} onTermsChange={setVocabularyTerms} />
                    </div>
                </div>
                
                {/* Footer */}
                <div className="flex flex-wrap gap-4 p-6 border-t border-gray-100 flex-shrink-0">
                    <button onClick={handleSave} className="px-6 py-3 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm">
                        Save Changes
                    </button>
                    <button onClick={onClose} className="px-6 py-3 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors">
                        Cancel
                    </button>
                </div>
            </div>
            {/* Minimal styles for prose since tailwind/typography isn't available */}
            <style>{`
                .editor-toolbar button { color: #2c3e50; }
                .prose { color: #14201E; }
                .prose h1 { font-family: 'Fraunces', serif; font-size: 1.875rem; font-weight: 500; margin-bottom: 1rem; color: #212631; }
                .prose h3 { font-family: 'Fraunces', serif; font-size: 1.25rem; font-weight: 500; margin-bottom: 0.5rem; color: #212631; }
                .prose p { margin-bottom: 1rem; line-height: 1.6; }
                .prose ul, .prose ol { margin-left: 1.5rem; margin-bottom: 1rem; }
                .prose ul { list-style-type: disc; }
                .prose ol { list-style-type: decimal; }
                @keyframes fade-in-up {
                    from { opacity: 0; transform: translateY(20px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .animate-fade-in-up { animation: fade-in-up 0.3s ease-out forwards; }
            `}</style>
        </div>
    );
};
