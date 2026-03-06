/**
 * Modal for creating a new Material.
 * Implements FrontendWorkflowSpecifications §4A(2) and §4B.
 * Two-step process:
 * 1. Enter title and select creation method (AI or Manual)
 * 2a. If Manual: select material type and create
 * 2b. If AI: collect generation parameters and invoke AI generation
 */

import React, { useState } from 'react';
import { ModalOverlay } from './ModalOverlay';
import type { MaterialType, GenerationRequest } from '../../models';

interface CreateMaterialModalProps {
    lessonId: string;
    lessonTitle: string;
    unitCollectionId?: string; // Required for AI generation to access source documents
    onClose: () => void;
    onCreate: (title: string, materialType: MaterialType) => Promise<void>;
    onCreateWithAI?: (
        title: string,
        materialType: MaterialType,
        generationRequest: GenerationRequest,
        readingAge: number,
        actualAge: number
    ) => Promise<void>;

    // Indicates whether the necessary AI runtime dependencies are available.
    // Per BackendRuntimeDependencyManagementSpecification §3(1), frontend
    // assumes dependencies are available until backend reports otherwise.
    aiDependenciesAvailable?: boolean;
}

type CreationMethod = 'AI' | 'MANUAL';
type Step = 'SELECT_METHOD' | 'MANUAL_TYPE' | 'AI_FORM';

const materialTypes: { value: MaterialType; label: string; color: string }[] = [
    { value: 'READING', label: 'Reading', color: 'bg-brand-green text-white' },
    { value: 'WORKSHEET', label: 'Worksheet', color: 'bg-brand-yellow text-gray-900' },
    { value: 'POLL', label: 'Poll', color: 'bg-purple-500 text-white' },
];

export const CreateMaterialModal: React.FC<CreateMaterialModalProps> = ({
    lessonTitle,
    unitCollectionId,
    onClose,
    onCreate,
    onCreateWithAI,
    aiDependenciesAvailable: aiDepsProp
}) => {
    const aiAvailable = aiDepsProp ?? true;

    // Step 1: Title and method selection
    const [step, setStep] = useState<Step>('SELECT_METHOD');
    const [title, setTitle] = useState('');
    // Step 2a: Manual creation
    const [materialType, setMaterialType] = useState<MaterialType>('READING');
    
    // Step 2b: AI generation parameters (per FrontendWorkflowSpec §4B(1))
    const [aiDescription, setAiDescription] = useState('');
    const [readingAge, setReadingAge] = useState<number>(10);
    const [actualAge, setActualAge] = useState<number>(10);
    const [durationInMinutes, setDurationInMinutes] = useState<number>(30);
    const [aiMaterialType, setAiMaterialType] = useState<'READING' | 'WORKSHEET'>('READING');
    
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleMethodSelect = (method: CreationMethod) => {
        if (method === 'AI' && !aiAvailable) {
            setError('AI generation is not available right now. Please install required dependencies.');
            return;
        }

        if (method === 'MANUAL') {
            setStep('MANUAL_TYPE');
        } else {
            setStep('AI_FORM');
        }
    };

    const handleManualCreate = async () => {
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

    const handleAIGenerate = async () => {
        if (!title.trim()) {
            setError('Title is required');
            return;
        }
        if (!aiDescription.trim()) {
            setError('Description is required for AI generation');
            return;
        }
        if (!unitCollectionId) {
            setError('Cannot use AI generation: No unit collection context available');
            return;
        }
        if (!onCreateWithAI) {
            setError('AI generation is not supported in this context');
            return;
        }
        if (!aiAvailable) {
            setError('AI runtime dependencies are not available');
            return;
        }

        setIsSubmitting(true);
        setError(null);
        try {
            // Per FrontendWorkflowSpec §4B(1): Collect generation parameters
            const request: GenerationRequest = {
                description: aiDescription,
                readingAge,
                actualAge,
                durationInMinutes,
                unitCollectionId,
                // sourceDocumentIds is optional - if not provided, all indexed documents are searched
            };

            // Per FrontendWorkflowSpec §4B(2): Pass request to parent handler
            // The parent will:
            // 1. Create the material with empty content
            // 2. Call generateReading or generateWorksheet
            // 3. Display result in editor modal
            // 4. Persist reading age and actual age metadata
            await onCreateWithAI(title.trim(), aiMaterialType, request, readingAge, actualAge);
            // Note: Parent manages modal transition to editor, so don't call onClose() here
        } catch (err) {
            setError('Failed to generate material with AI');
        } finally {
            setIsSubmitting(false);
        }
    };

    const renderSelectMethodStep = () => (
        <>
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
                    How would you like to create this material?
                </label>
                <div className="flex flex-col gap-3">
                    <button
                        onClick={() => handleMethodSelect('AI')}
                        disabled={!title.trim() || !unitCollectionId || !aiAvailable}
                        className="w-full p-4 border-2 border-gray-200 rounded-lg hover:border-brand-orange hover:bg-orange-50 transition-all text-left disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        <div className="font-sans font-semibold text-text-heading">
                            ✨ AI Generation
                        </div>
                        <div className="text-sm text-gray-600 mt-1">
                            Generate content based on your description and source documents
                        </div>
                        {!unitCollectionId && (
                            <div className="text-xs text-red-500 mt-1">
                                Not available: No unit collection context
                            </div>
                        )}
                        {!aiAvailable && (
                            <div className="text-xs text-red-500 mt-1">
                                AI generation unavailable: install the missing runtime dependency and retry.
                            </div>
                        )}
                        <div className="text-xs text-gray-500 italic mt-2">
                            Note: Polls cannot be created via AI generation
                        </div>
                        <div className="text-xs text-amber-700 bg-amber-50 border border-amber-200 rounded px-2 py-1.5 mt-2">
                            ⚠️ AI-generated materials may contain mistakes and should be reviewed before being deployed to students.
                        </div>
                    </button>
                    <button
                        onClick={() => handleMethodSelect('MANUAL')}
                        disabled={!title.trim()}
                        className="w-full p-4 border-2 border-gray-200 rounded-lg hover:border-brand-orange hover:bg-orange-50 transition-all text-left disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        <div className="font-sans font-semibold text-text-heading">
                            ✏️ Manual Creation
                        </div>
                        <div className="text-sm text-gray-600 mt-1">
                            Start with a blank material and add content yourself
                        </div>
                    </button>
                </div>
            </div>

            <p className="text-sm text-gray-500 italic">
                You'll be able to make manual edits or invoke the AI assistant after creation.
            </p>

            {error && <p className="text-red-500 text-sm">{error}</p>}

            <div className="flex gap-4 pt-4 border-t border-gray-100">
                <button
                    onClick={onClose}
                    className="px-6 py-3 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors"
                >
                    Cancel
                </button>
            </div>
        </>
    );

    const renderManualTypeStep = () => (
        <>
            <button
                onClick={() => setStep('SELECT_METHOD')}
                className="text-brand-orange hover:text-brand-orange-dark text-sm font-sans flex items-center gap-1"
            >
                ← Back
            </button>

            <h2 className="text-2xl font-serif text-text-heading">
                Select Material Type
            </h2>

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

            {error && <p className="text-red-500 text-sm">{error}</p>}

            <div className="flex gap-4 pt-4 border-t border-gray-100">
                <button
                    onClick={handleManualCreate}
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
        </>
    );

    const renderAIFormStep = () => (
        <>
            <button
                onClick={() => setStep('SELECT_METHOD')}
                className="text-brand-orange hover:text-brand-orange-dark text-sm font-sans flex items-center gap-1"
            >
                ← Back
            </button>

            <h2 className="text-2xl font-serif text-text-heading">
                AI Material Generation
            </h2>

            <div>
                <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                    Material Type <span className="text-red-500">*</span>
                </label>
                <div className="flex gap-3">
                    <button
                        onClick={() => setAiMaterialType('READING')}
                        className={`px-5 py-2 rounded-md font-sans font-medium text-sm transition-all shadow-sm ${aiMaterialType === 'READING'
                            ? 'bg-brand-green text-white transform -translate-y-0.5'
                            : 'bg-gray-100 text-text-body hover:bg-gray-200'
                            }`}
                    >
                        Reading
                    </button>
                    <button
                        onClick={() => setAiMaterialType('WORKSHEET')}
                        className={`px-5 py-2 rounded-md font-sans font-medium text-sm transition-all shadow-sm ${aiMaterialType === 'WORKSHEET'
                            ? 'bg-brand-yellow text-gray-900 transform -translate-y-0.5'
                            : 'bg-gray-100 text-text-body hover:bg-gray-200'
                            }`}
                    >
                        Worksheet
                    </button>
                </div>
            </div>

            <div>
                <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                    Description <span className="text-red-500">*</span>
                </label>
                <textarea
                    value={aiDescription}
                    onChange={(e) => { setAiDescription(e.target.value); setError(null); }}
                    className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
                    placeholder="Describe the content you want to generate, including any specific requirements..."
                    rows={4}
                />
            </div>

            <div className="grid grid-cols-2 gap-4">
                <div>
                    <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                        Reading Age <span className="text-red-500">*</span>
                    </label>
                    <input
                        type="number"
                        value={readingAge}
                        onChange={(e) => setReadingAge(parseInt(e.target.value) || 10)}
                        className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
                        min={5}
                        max={18}
                    />
                </div>
                <div>
                    <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                        Actual Age <span className="text-red-500">*</span>
                    </label>
                    <input
                        type="number"
                        value={actualAge}
                        onChange={(e) => setActualAge(parseInt(e.target.value) || 10)}
                        className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
                        min={5}
                        max={18}
                    />
                </div>
            </div>

            <div>
                <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                    Estimated Duration (minutes) <span className="text-red-500">*</span>
                </label>
                <input
                    type="number"
                    value={durationInMinutes}
                    onChange={(e) => setDurationInMinutes(parseInt(e.target.value) || 30)}
                    className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
                    min={5}
                    max={120}
                    step={5}
                />
            </div>

            {error && <p className="text-red-500 text-sm">{error}</p>}

            <div className="flex gap-4 pt-4 border-t border-gray-100">
                <button
                    onClick={handleAIGenerate}
                    disabled={isSubmitting || !aiDescription.trim()}
                    className="px-6 py-3 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm disabled:opacity-50"
                >
                    {isSubmitting ? 'Generating...' : 'Generate with AI'}
                </button>
                <button
                    onClick={onClose}
                    className="px-6 py-3 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors"
                >
                    Cancel
                </button>
            </div>
        </>
    );

    return (
        <ModalOverlay priority="low">
            <div className="bg-white rounded-lg p-8 shadow-2xl w-full max-w-lg space-y-6 animate-fade-in-up border border-gray-100 max-h-[90vh] overflow-y-auto">
                {step === 'SELECT_METHOD' && renderSelectMethodStep()}
                {step === 'MANUAL_TYPE' && renderManualTypeStep()}
                {step === 'AI_FORM' && renderAIFormStep()}
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
