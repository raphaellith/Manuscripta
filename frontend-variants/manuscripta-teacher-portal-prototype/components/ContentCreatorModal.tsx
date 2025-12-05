
import React, { useState } from 'react';
import type { ContentType, LessonFolder } from '../types';

const ageGroupLevels = [
    { value: 0, label: "4-6" },
    { value: 1, label: "7-9" },
    { value: 2, label: "10-13" },
    { value: 3, label: "14+" },
];

interface ContentCreatorModalProps {
    unit: string;
    existingLessonFolders: LessonFolder[];
    onClose: () => void;
    onAddContent: (content: { title: string; type: ContentType, lessonNumber: number, lessonTitle: string }) => void;
}

export const ContentCreatorModal: React.FC<ContentCreatorModalProps> = ({ unit, existingLessonFolders, onClose, onAddContent }) => {
    const [contentType, setContentType] = useState<ContentType>('Worksheet');
    const [prompt, setPrompt] = useState('');
    const [ageGroup, setAgeGroup] = useState(2);
    const [readingAge, setReadingAge] = useState(10);
    const [selectedLessonKey, setSelectedLessonKey] = useState<string>(existingLessonFolders.length > 0 ? `${existingLessonFolders[0].number}-${existingLessonFolders[0].title}` : '');

    const handleGenerate = () => {
        if (!prompt.trim()) {
            alert('Please describe what this content should be about.');
            return;
        }

        if (!selectedLessonKey) {
            alert('Please select a lesson folder to add content to.');
            return;
        }

        const [num, ...titleParts] = selectedLessonKey.split('-');
        const lessonNumber = Number(num);
        const lessonTitle = titleParts.join('-');

        onAddContent({ title: prompt, type: contentType, lessonNumber, lessonTitle });
    }

    return (
        <div className="fixed inset-0 bg-text-heading/20 backdrop-blur-sm flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-lg p-8 shadow-2xl w-full max-w-2xl space-y-6 animate-fade-in-up border border-gray-100">
                <h2 className="text-2xl font-serif text-text-heading">Add Content to <span className="italic">"{unit}"</span></h2>
                
                {/* Lesson Folder Selector */}
                <div>
                    <label className="font-sans font-medium text-text-heading text-sm mb-2 block">1. Choose Lesson</label>
                    <select
                        value={selectedLessonKey}
                        onChange={(e) => setSelectedLessonKey(e.target.value)}
                        className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none disabled:opacity-50"
                        disabled={existingLessonFolders.length === 0}
                    >
                        {existingLessonFolders.length > 0 ? (
                            existingLessonFolders.map(folder => (
                                <option key={folder.id} value={`${folder.number}-${folder.title}`}>
                                    {String(folder.number).padStart(2, '0')}. {folder.title}
                                </option>
                            ))
                        ) : (
                            <option value="">-- Create a lesson folder first --</option>
                        )}
                    </select>
                </div>

                {/* Content Type Selector */}
                <div>
                    <label className="font-sans font-medium text-text-heading text-sm mb-2 block">2. Choose Content Type</label>
                    <div className="flex gap-3">
                        {(['Lesson', 'Worksheet', 'Quiz'] as ContentType[]).map(type => (
                            <button 
                                key={type}
                                onClick={() => setContentType(type)}
                                className={`px-5 py-2 rounded-md font-sans font-medium text-sm transition-all shadow-sm ${contentType === type ? 'bg-brand-orange text-white transform -translate-y-0.5' : 'bg-brand-gray text-text-body hover:bg-gray-200'}`}
                            >
                                {type}
                            </button>
                        ))}
                    </div>
                </div>

                {/* Prompt */}
                <div>
                    <label className="font-sans font-medium text-text-heading text-sm mb-2 block">3. What should this {contentType.toLowerCase()} be about?</label>
                    <textarea 
                        value={prompt}
                        onChange={(e) => setPrompt(e.target.value)}
                        className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none resize-none" 
                        placeholder={`e.g., A quiz on the key figures in the Battle of Hastings`}
                        rows={4}
                    />
                </div>

                {/* Age Group */}
                <div>
                    <label className="font-sans font-medium text-text-heading text-sm mb-2 block">4. Age Group</label>
                    <div className="flex items-center gap-6 p-4 bg-brand-cream border border-gray-100 rounded-lg">
                        <input 
                            type="range" 
                            min="0" 
                            max={ageGroupLevels.length - 1}
                            value={ageGroup}
                            onChange={(e) => setAgeGroup(Number(e.target.value))}
                            className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-brand-orange"
                        />
                        <span className="font-sans font-semibold text-brand-orange bg-brand-orange-light py-1 px-3 rounded text-center min-w-[80px]">{ageGroupLevels[ageGroup].label}</span>
                    </div>
                </div>

                {/* Reading Age Level - Separate from age group for precise targeting */}
                <div>
                    <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                        5. Target Reading Age 
                        <span className="text-gray-400 font-normal ml-2">(Progressive Skills Level)</span>
                    </label>
                    <div className="flex items-center gap-6 p-4 bg-brand-cream border border-gray-100 rounded-lg">
                        <input 
                            type="range" 
                            min="5" 
                            max="16"
                            value={readingAge}
                            onChange={(e) => setReadingAge(Number(e.target.value))}
                            className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-brand-green"
                        />
                        <div className="flex items-center gap-2 min-w-[120px]">
                            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-brand-green" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                            </svg>
                            <span className="font-sans font-semibold text-brand-green">{readingAge} years</span>
                        </div>
                    </div>
                    <p className="text-xs text-gray-400 mt-2 ml-1">Adjusts vocabulary complexity and sentence structure for the specified reading level.</p>
                </div>
                
                {/* Actions */}
                <div className="flex flex-wrap gap-4 pt-4 border-t border-gray-100">
                    <button 
                        onClick={handleGenerate} 
                        className="px-6 py-3 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm disabled:opacity-50 disabled:cursor-not-allowed"
                        disabled={existingLessonFolders.length === 0}
                    >
                        Generate Content
                    </button>
                    <button onClick={onClose} className="px-6 py-3 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors">
                        Cancel
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
