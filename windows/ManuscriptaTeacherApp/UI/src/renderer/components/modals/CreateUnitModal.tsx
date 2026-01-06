/**
 * Modal for creating a new Unit.
 * Styled to match prototype ContentCreatorModal.
 */

import React, { useState } from 'react';

interface CreateUnitModalProps {
    collectionId: string;
    collectionTitle: string;
    onClose: () => void;
    onCreate: (title: string) => Promise<void>;
}

export const CreateUnitModal: React.FC<CreateUnitModalProps> = ({
    collectionTitle,
    onClose,
    onCreate
}) => {
    const [title, setTitle] = useState('');
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
            await onCreate(title.trim());
            onClose();
        } catch (err) {
            setError('Failed to create unit');
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-text-heading/20 backdrop-blur-sm flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-lg p-8 shadow-2xl w-full max-w-md space-y-6 animate-fade-in-up border border-gray-100">
                <h2 className="text-2xl font-serif text-text-heading">
                    Create Unit in <span className="text-brand-orange italic">"{collectionTitle}"</span>
                </h2>

                <div>
                    <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                        Unit Title <span className="text-red-500">*</span>
                    </label>
                    <input
                        type="text"
                        value={title}
                        onChange={(e) => { setTitle(e.target.value); setError(null); }}
                        className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
                        placeholder="e.g., The Norman Conquest"
                        autoFocus
                    />
                    {error && <p className="text-red-500 text-sm mt-2">{error}</p>}
                </div>

                <div className="flex flex-wrap gap-4 pt-4 border-t border-gray-100">
                    <button
                        onClick={handleSubmit}
                        disabled={isSubmitting}
                        className="px-6 py-3 bg-brand-green text-white font-sans font-medium rounded-md hover:bg-green-800 transition-colors shadow-sm disabled:opacity-50"
                    >
                        {isSubmitting ? 'Creating...' : 'Create Unit'}
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
