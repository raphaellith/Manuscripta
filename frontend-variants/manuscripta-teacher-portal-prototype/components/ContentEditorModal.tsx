
import React, { useRef } from 'react';
import type { ContentItem } from '../types';

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

export const ContentEditorModal: React.FC<ContentEditorModalProps> = ({ contentItem, onClose, onSave }) => {
    const editorRef = useRef<HTMLDivElement>(null);
    
    const handleSave = () => {
        onSave({
            ...contentItem,
            content: editorRef.current?.innerHTML || '',
        });
    };

    return (
        <div className="fixed inset-0 bg-text-heading/20 backdrop-blur-sm flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-lg p-8 shadow-2xl w-full max-w-4xl space-y-6 animate-fade-in-up">
                <h2 className="text-2xl font-serif text-text-heading">
                    Editing: <span className="text-brand-orange">{contentItem.title}</span>
                </h2>
                
                <div className="border-2 border-brand-gray rounded-lg focus-within:border-brand-orange transition-all bg-white overflow-hidden">
                    <EditorToolbar />
                    <div
                        ref={editorRef}
                        contentEditable={true}
                        suppressContentEditableWarning={true}
                        className="prose max-w-none p-6 h-96 overflow-y-auto focus:outline-none font-serif text-text-body"
                        dangerouslySetInnerHTML={{ __html: contentItem.content || '' }}
                    />
                </div>
                
                <div className="flex flex-wrap gap-4 pt-4 border-t border-gray-100">
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
