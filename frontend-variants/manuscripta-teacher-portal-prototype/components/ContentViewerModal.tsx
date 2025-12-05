
import React from 'react';
import type { ContentItem } from '../types';

interface ContentViewerModalProps {
    contentItem: ContentItem;
    onClose: () => void;
}

export const ContentViewerModal: React.FC<ContentViewerModalProps> = ({ contentItem, onClose }) => {
    // If it's a PDF type, show only the PDF viewer
    if (contentItem.type === 'PDF' && contentItem.pdfPath) {
        return (
            <div className="fixed inset-0 bg-text-heading/30 backdrop-blur-sm flex items-center justify-center z-50 p-4">
                <div className="bg-white rounded-xl shadow-2xl w-full max-w-5xl h-[90vh] flex flex-col animate-fade-in-up overflow-hidden">
                    <div className="flex justify-between items-center p-6 border-b border-gray-100 bg-white">
                        <div>
                            <h2 className="text-2xl font-serif text-text-heading">{contentItem.title}</h2>
                            <p className="text-sm text-gray-500 mt-1">{contentItem.subject}</p>
                        </div>
                        <button 
                            onClick={onClose}
                            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                            aria-label="Close"
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                            </svg>
                        </button>
                    </div>
                    <div className="flex-1 bg-gray-100">
                        <iframe
                            src={contentItem.pdfPath}
                            className="w-full h-full"
                            title={contentItem.title}
                        />
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
    }

    // For lessons/readings with embedded images or regular content
    return (
        <div className="fixed inset-0 bg-text-heading/30 backdrop-blur-sm flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-xl shadow-2xl w-full max-w-4xl max-h-[90vh] flex flex-col animate-fade-in-up overflow-hidden">
                <div className="flex justify-between items-center p-6 border-b border-gray-100 bg-white">
                    <div>
                        <div className="flex items-center gap-3">
                            <h2 className="text-2xl font-serif text-text-heading">{contentItem.title}</h2>
                            <span className={`text-xs font-sans font-semibold px-2 py-1 rounded-md uppercase tracking-wide ${
                                contentItem.type === 'Lesson' ? 'bg-brand-blue/20 text-blue-900' :
                                contentItem.type === 'Reading' ? 'bg-brand-green/20 text-green-900' :
                                contentItem.type === 'Worksheet' ? 'bg-brand-yellow/30 text-yellow-900' :
                                'bg-brand-orange-light text-brand-orange-dark'
                            }`}>
                                {contentItem.type}
                            </span>
                        </div>
                        <p className="text-sm text-gray-500 mt-1">{contentItem.subject} • {contentItem.created}</p>
                    </div>
                    <button 
                        onClick={onClose}
                        className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                        aria-label="Close"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>
                
                <div className="flex-1 overflow-y-auto p-8">
                    {/* If there's an embedded image, show it prominently */}
                    {contentItem.imageUrl && (
                        <div className="mb-8">
                            <img 
                                src={contentItem.imageUrl} 
                                alt={contentItem.title}
                                className="w-full max-h-[400px] object-contain rounded-lg shadow-md bg-gray-50"
                            />
                        </div>
                    )}
                    
                    {/* Render the HTML content */}
                    {contentItem.content && (
                        <div 
                            className="prose max-w-none"
                            dangerouslySetInnerHTML={{ __html: contentItem.content }}
                        />
                    )}
                </div>
                
                <div className="p-4 border-t border-gray-100 bg-gray-50 flex justify-end gap-3">
                    <button 
                        onClick={onClose}
                        className="px-6 py-2 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm"
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
                .prose { color: #14201E; }
                .prose h1 { font-family: 'Fraunces', serif; font-size: 1.875rem; font-weight: 500; margin-bottom: 1rem; color: #212631; }
                .prose h2 { font-family: 'Fraunces', serif; font-size: 1.5rem; font-weight: 500; margin-bottom: 0.75rem; color: #212631; margin-top: 1.5rem; }
                .prose h3 { font-family: 'Fraunces', serif; font-size: 1.25rem; font-weight: 500; margin-bottom: 0.5rem; color: #212631; margin-top: 1.25rem; }
                .prose p { margin-bottom: 1rem; line-height: 1.7; }
                .prose ul, .prose ol { margin-left: 1.5rem; margin-bottom: 1rem; }
                .prose ul { list-style-type: disc; }
                .prose ol { list-style-type: decimal; }
                .prose li { margin-bottom: 0.25rem; line-height: 1.6; }
                .prose strong, .prose b { font-weight: 600; }
                .prose em, .prose i { font-style: italic; }
            `}</style>
        </div>
    );
};
