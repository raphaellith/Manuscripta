/**
 * Modal for creating a new Material.
 * Only requires Title and MaterialType at creation.
 * Content is optional - edited later with markdown editor.
 */

import React, { useState } from 'react';
import type { MaterialType } from '../../models';

interface CreateMaterialModalProps {
    lessonId: string;
    lessonTitle: string;
    onClose: () => void;
    onCreate: (title: string, materialType: MaterialType) => Promise<void>;
}

const materialTypes: { value: MaterialType; label: string; color: string }[] = [
    { value: 'READING', label: 'Reading', color: 'bg-brand-green text-white' },
    { value: 'WORKSHEET', label: 'Worksheet', color: 'bg-brand-yellow text-gray-900' },
    { value: 'QUIZ', label: 'Quiz', color: 'bg-brand-orange text-white' },
    { value: 'POLL', label: 'Poll', color: 'bg-purple-500 text-white' },
];

export const CreateMaterialModal: React.FC<CreateMaterialModalProps> = ({
    lessonTitle,
    onClose,
    onCreate
}) => {
    const [title, setTitle] = useState('');
    const [materialType, setMaterialType] = useState<MaterialType>('READING');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async () => {
        if (!title.trim()) {
            setError('Title is required');
            return;
        }
        setIsSubmitting(true);
        setError(null);
        try {
            await onCreate(title.trim(), materialType);
            onClose();
        } catch (err) {
            setError('Failed to create material');
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-text-heading/20 backdrop-blur-sm flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-lg p-8 shadow-2xl w-full max-w-lg space-y-6 animate-fade-in-up border border-gray-100">
                <h2 className="text-2xl font-serif text-text-heading">
                    Add Material to <span className="text-brand-blue italic">"{lessonTitle}"</span>
                </h2>

                <div>
                    <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                        Material Title <span className="text-red-500">*</span>
                    </label>
                    <input
                        type="text"
                        value={title}
                        onChange={(e) => { setTitle(e.target.value); setError(null); }}
                        className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
                        placeholder="e.g., Key Vocabulary"
                        autoFocus
                    />
                </div>

                <div>
                    <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                        Material Type <span className="text-red-500">*</span>
                    </label>
                    <div className="flex gap-3 flex-wrap">
                        {materialTypes.map(type => (
                            <button
                                key={type.value}
                                onClick={() => setMaterialType(type.value)}
                                className={`px-5 py-2 rounded-md font-sans font-medium text-sm transition-all shadow-sm ${materialType === type.value
                                        ? `${type.color} transform -translate-y-0.5`
                                        : 'bg-gray-100 text-text-body hover:bg-gray-200'
                                    }`}
                            >
                                {type.label}
                            </button>
                        ))}
                    </div>
                </div>

                <p className="text-sm text-gray-500 italic">
                    Content can be added after creation using the editor.
                </p>

                {error && <p className="text-red-500 text-sm">{error}</p>}

                <div className="flex flex-wrap gap-4 pt-4 border-t border-gray-100">
                    <button
                        onClick={handleSubmit}
                        disabled={isSubmitting}
                        className="px-6 py-3 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm disabled:opacity-50"
                    >
                        {isSubmitting ? 'Creating...' : 'Create Material'}
                    </button>
                    <button
                        onClick={onClose}
                        className="px-6 py-3 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors"
                    >
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
