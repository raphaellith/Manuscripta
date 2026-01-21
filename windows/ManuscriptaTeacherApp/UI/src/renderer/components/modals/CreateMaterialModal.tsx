/**
 * Multi-step wizard for creating a new Material (s4A and s4B).
 * 
 * Steps:
 * 1. Title Entry (s4A(1))
 * 2. Method Selection - AI Generation or Manual Creation (s4A(2)(b))
 * 3A. AI Generation Wizard (s4B(1)) - collects description, source docs, type, ages, duration, template
 * 3B. Manual Type Selection (s4A(2)(b)(ii)) - selects type and handles poll auto-initialisation
 * 4. Completion Message (s4A(2)(c))
 */

import React, { useState } from 'react';
import type { MaterialType } from '../../models';

interface CreateMaterialModalProps {
    lessonId: string;
    lessonTitle: string;
    onClose: () => void;
    onCreate: (title: string, materialType: MaterialType, metadata?: MaterialMetadata) => Promise<void>;
}

interface MaterialMetadata {
    readingAge?: number;
    actualAge?: number;
    description?: string;
    template?: string;
    duration?: number;
}

// Wizard steps
type WizardStep = 'title' | 'method' | 'ai-generation' | 'manual-type' | 'complete';

// Material type options
const materialTypes: { value: MaterialType; label: string; color: string; description: string }[] = [
    { value: 'READING', label: 'Reading', color: 'bg-brand-green text-white', description: 'Informational text for students to read' },
    { value: 'WORKSHEET', label: 'Worksheet', color: 'bg-brand-yellow text-gray-900', description: 'Interactive exercises with questions' },
    { value: 'POLL', label: 'Poll', color: 'bg-purple-500 text-white', description: 'Single question for quick responses' },
];

// Template options (placeholder per s4B(1)(f))
const templateOptions = [
    { value: '', label: 'No template' },
    { value: 'vocabulary-focus', label: 'Vocabulary Focus' },
    { value: 'comprehension-practice', label: 'Comprehension Practice' },
    { value: 'critical-thinking', label: 'Critical Thinking' },
    { value: 'summary-writing', label: 'Summary Writing' },
];

// Dummy source documents for demo
const dummySourceDocuments = [
    { id: 'src-1', name: 'Year 5 English Curriculum', type: 'syllabus' },
    { id: 'src-2', name: 'Reading Comprehension Objectives', type: 'objectives' },
    { id: 'src-3', name: 'Vocabulary List Term 1', type: 'reference' },
];

export const CreateMaterialModal: React.FC<CreateMaterialModalProps> = ({
    lessonTitle,
    onClose,
    onCreate
}) => {
    // Wizard state
    const [step, setStep] = useState<WizardStep>('title');

    // Common form state
    const [title, setTitle] = useState('');
    const [materialType, setMaterialType] = useState<MaterialType>('READING');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // AI Generation form state (s4B(1))
    const [description, setDescription] = useState('');
    const [selectedSourceDocs, setSelectedSourceDocs] = useState<string[]>([]);
    const [readingAge, setReadingAge] = useState<number>(10);
    const [actualAge, setActualAge] = useState<number>(10);
    const [duration, setDuration] = useState<number>(15);
    const [template, setTemplate] = useState('');

    const handleTitleSubmit = () => {
        if (!title.trim()) {
            setError('Title is required');
            return;
        }
        setError(null);
        setStep('method');
    };

    const handleMethodSelect = (method: 'ai' | 'manual') => {
        if (method === 'ai') {
            setStep('ai-generation');
        } else {
            setStep('manual-type');
        }
    };

    const handleAIGenerate = async () => {
        // Validate AI generation form
        if (!description.trim()) {
            setError('Please provide a description of the material');
            return;
        }

        setIsSubmitting(true);
        setError(null);

        try {
            // For demo: simulate AI generation delay
            await new Promise(resolve => setTimeout(resolve, 1500));

            // Create material with metadata
            await onCreate(title.trim(), materialType, {
                readingAge,
                actualAge,
                description: description.trim(),
                template: template || undefined,
                duration
            });

            setStep('complete');
        } catch (err) {
            setError('Failed to generate material');
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleManualCreate = async () => {
        setIsSubmitting(true);
        setError(null);

        try {
            await onCreate(title.trim(), materialType);
            setStep('complete');
        } catch (err) {
            setError('Failed to create material');
        } finally {
            setIsSubmitting(false);
        }
    };

    const toggleSourceDoc = (docId: string) => {
        setSelectedSourceDocs(prev =>
            prev.includes(docId)
                ? prev.filter(id => id !== docId)
                : [...prev, docId]
        );
    };

    // Step indicator
    const getStepNumber = () => {
        switch (step) {
            case 'title': return 1;
            case 'method': return 2;
            case 'ai-generation':
            case 'manual-type': return 3;
            case 'complete': return 4;
        }
    };

    return (
        <div className="fixed inset-0 bg-text-heading/20 backdrop-blur-sm flex items-start justify-center z-50 p-4 pt-28">
            <div className="bg-white rounded-lg p-8 shadow-2xl w-full max-w-2xl space-y-6 animate-fade-in-up border border-gray-100 max-h-[80vh] overflow-y-auto">
                {/* Header */}
                <div className="flex items-center justify-between">
                    <h2 className="text-2xl font-serif text-text-heading">
                        {step === 'complete' ? 'Material Created!' : (
                            <>Add Material to <span className="text-brand-blue italic">"{lessonTitle}"</span></>
                        )}
                    </h2>
                    {step !== 'complete' && (
                        <span className="text-sm text-gray-400">Step {getStepNumber()} of 4</span>
                    )}
                </div>

                {/* Step: Title Entry */}
                {step === 'title' && (
                    <>
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
                                onKeyDown={(e) => e.key === 'Enter' && handleTitleSubmit()}
                            />
                        </div>
                        {error && <p className="text-red-500 text-sm">{error}</p>}
                        <div className="flex gap-4 pt-4 border-t border-gray-100">
                            <button
                                onClick={handleTitleSubmit}
                                className="px-6 py-3 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm"
                            >
                                Continue
                            </button>
                            <button
                                onClick={onClose}
                                className="px-6 py-3 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors"
                            >
                                Cancel
                            </button>
                        </div>
                    </>
                )}

                {/* Step: Method Selection */}
                {step === 'method' && (
                    <>
                        <p className="text-text-body">How would you like to create this material?</p>

                        <div className="grid grid-cols-2 gap-4">
                            {/* AI Generation Option */}
                            <button
                                onClick={() => handleMethodSelect('ai')}
                                className="group p-6 rounded-xl border-2 border-gray-200 hover:border-brand-orange hover:bg-brand-orange/5 transition-all text-left"
                            >
                                <div className="flex items-center gap-3 mb-3">
                                    <div className="w-10 h-10 rounded-full bg-gradient-to-br from-purple-500 to-brand-orange flex items-center justify-center">
                                        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                                        </svg>
                                    </div>
                                    <span className="font-semibold text-text-heading">Generate with AI</span>
                                </div>
                                <p className="text-sm text-gray-500">
                                    Describe what you need and let AI create a draft for you.
                                </p>
                                <p className="text-xs text-gray-400 mt-2 italic">
                                    Note: Polls cannot be generated with AI.
                                </p>
                            </button>

                            {/* Manual Creation Option */}
                            <button
                                onClick={() => handleMethodSelect('manual')}
                                className="group p-6 rounded-xl border-2 border-gray-200 hover:border-brand-green hover:bg-brand-green/5 transition-all text-left"
                            >
                                <div className="flex items-center gap-3 mb-3">
                                    <div className="w-10 h-10 rounded-full bg-brand-green flex items-center justify-center">
                                        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                        </svg>
                                    </div>
                                    <span className="font-semibold text-text-heading">Create Manually</span>
                                </div>
                                <p className="text-sm text-gray-500">
                                    Start with a blank material and write your own content.
                                </p>
                            </button>
                        </div>

                        <div className="flex gap-4 pt-4 border-t border-gray-100">
                            <button
                                onClick={() => setStep('title')}
                                className="px-6 py-3 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:border-gray-400 transition-colors"
                            >
                                Back
                            </button>
                            <button
                                onClick={onClose}
                                className="px-6 py-3 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors"
                            >
                                Cancel
                            </button>
                        </div>
                    </>
                )}

                {/* Step: AI Generation Wizard (s4B(1)) */}
                {step === 'ai-generation' && (
                    <>
                        <div className="space-y-5">
                            {/* Material Type Selection (s4B(1)(c)) */}
                            <div>
                                <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                                    Material Type <span className="text-red-500">*</span>
                                </label>
                                <div className="flex gap-3">
                                    {materialTypes
                                        .filter(t => t.value !== 'POLL') // AI cannot generate polls
                                        .map(type => (
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

                            {/* Description (s4B(1)(a)) */}
                            <div>
                                <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                                    Description & Requirements <span className="text-red-500">*</span>
                                </label>
                                <textarea
                                    value={description}
                                    onChange={(e) => setDescription(e.target.value)}
                                    className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none resize-none"
                                    placeholder="Describe the material you want to create. Include topic, key concepts, learning objectives..."
                                    rows={4}
                                />
                            </div>

                            {/* Source Documents (s4B(1)(b)) */}
                            <div>
                                <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                                    Reference Source Documents
                                </label>
                                <div className="border border-gray-200 rounded-lg p-3 space-y-2 max-h-32 overflow-y-auto">
                                    {dummySourceDocuments.map(doc => (
                                        <label
                                            key={doc.id}
                                            className="flex items-center gap-3 p-2 rounded-md hover:bg-gray-50 cursor-pointer"
                                        >
                                            <input
                                                type="checkbox"
                                                checked={selectedSourceDocs.includes(doc.id)}
                                                onChange={() => toggleSourceDoc(doc.id)}
                                                className="w-4 h-4 rounded text-brand-orange focus:ring-brand-orange"
                                            />
                                            <span className="text-sm text-text-body">{doc.name}</span>
                                            <span className="text-xs text-gray-400 ml-auto">{doc.type}</span>
                                        </label>
                                    ))}
                                </div>
                            </div>

                            {/* Age Settings (s4B(1)(d)) */}
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                                        Reading Age
                                    </label>
                                    <input
                                        type="number"
                                        value={readingAge}
                                        onChange={(e) => setReadingAge(parseInt(e.target.value) || 0)}
                                        min={5}
                                        max={18}
                                        className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
                                    />
                                </div>
                                <div>
                                    <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                                        Actual Age Group
                                    </label>
                                    <input
                                        type="number"
                                        value={actualAge}
                                        onChange={(e) => setActualAge(parseInt(e.target.value) || 0)}
                                        min={5}
                                        max={18}
                                        className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
                                    />
                                </div>
                            </div>

                            {/* Duration (s4B(1)(e)) */}
                            <div>
                                <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                                    Estimated Completion Time (minutes)
                                </label>
                                <input
                                    type="number"
                                    value={duration}
                                    onChange={(e) => setDuration(parseInt(e.target.value) || 0)}
                                    min={5}
                                    max={120}
                                    step={5}
                                    className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
                                />
                            </div>

                            {/* Template (s4B(1)(f)) */}
                            <div>
                                <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                                    Template
                                </label>
                                <select
                                    value={template}
                                    onChange={(e) => setTemplate(e.target.value)}
                                    className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
                                >
                                    {templateOptions.map(opt => (
                                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                                    ))}
                                </select>
                            </div>
                        </div>

                        {error && <p className="text-red-500 text-sm">{error}</p>}

                        <div className="flex gap-4 pt-4 border-t border-gray-100">
                            <button
                                onClick={handleAIGenerate}
                                disabled={isSubmitting}
                                className="px-6 py-3 bg-gradient-to-r from-purple-500 to-brand-orange text-white font-sans font-medium rounded-md hover:opacity-90 transition-opacity shadow-sm disabled:opacity-50 flex items-center gap-2"
                            >
                                {isSubmitting ? (
                                    <>
                                        <svg className="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                        </svg>
                                        Generating...
                                    </>
                                ) : 'Generate Material'}
                            </button>
                            <button
                                onClick={() => setStep('method')}
                                disabled={isSubmitting}
                                className="px-6 py-3 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:border-gray-400 transition-colors disabled:opacity-50"
                            >
                                Back
                            </button>
                            <button
                                onClick={onClose}
                                disabled={isSubmitting}
                                className="px-6 py-3 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors disabled:opacity-50"
                            >
                                Cancel
                            </button>
                        </div>
                    </>
                )}

                {/* Step: Manual Type Selection */}
                {step === 'manual-type' && (
                    <>
                        <div>
                            <label className="font-sans font-medium text-text-heading text-sm mb-3 block">
                                Select Material Type <span className="text-red-500">*</span>
                            </label>
                            <div className="space-y-3">
                                {materialTypes.map(type => (
                                    <button
                                        key={type.value}
                                        onClick={() => setMaterialType(type.value)}
                                        className={`w-full p-4 rounded-lg border-2 transition-all text-left flex items-center gap-4 ${materialType === type.value
                                            ? 'border-brand-orange bg-brand-orange/5'
                                            : 'border-gray-200 hover:border-gray-300'
                                            }`}
                                    >
                                        <span className={`px-3 py-1 rounded-md text-sm font-medium ${type.color}`}>
                                            {type.label}
                                        </span>
                                        <span className="text-sm text-gray-500">{type.description}</span>
                                        {type.value === 'POLL' && (
                                            <span className="text-xs text-purple-500 ml-auto">
                                                Auto-creates 1 question
                                            </span>
                                        )}
                                    </button>
                                ))}
                            </div>
                        </div>

                        {error && <p className="text-red-500 text-sm">{error}</p>}

                        <div className="flex gap-4 pt-4 border-t border-gray-100">
                            <button
                                onClick={handleManualCreate}
                                disabled={isSubmitting}
                                className="px-6 py-3 bg-brand-green text-white font-sans font-medium rounded-md hover:bg-brand-green/90 transition-colors shadow-sm disabled:opacity-50"
                            >
                                {isSubmitting ? 'Creating...' : 'Create Material'}
                            </button>
                            <button
                                onClick={() => setStep('method')}
                                disabled={isSubmitting}
                                className="px-6 py-3 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:border-gray-400 transition-colors disabled:opacity-50"
                            >
                                Back
                            </button>
                            <button
                                onClick={onClose}
                                disabled={isSubmitting}
                                className="px-6 py-3 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors disabled:opacity-50"
                            >
                                Cancel
                            </button>
                        </div>
                    </>
                )}

                {/* Step: Completion (s4A(2)(c)) */}
                {step === 'complete' && (
                    <>
                        <div className="text-center py-6">
                            <div className="w-16 h-16 rounded-full bg-brand-green/10 flex items-center justify-center mx-auto mb-4">
                                <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-brand-green" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                </svg>
                            </div>
                            <h3 className="text-lg font-semibold text-text-heading mb-2">
                                "{title}" has been created!
                            </h3>
                            <p className="text-gray-500">
                                You can now make manual edits or invoke the AI assistant from the editor.
                            </p>
                        </div>

                        <div className="flex justify-center pt-4 border-t border-gray-100">
                            <button
                                onClick={onClose}
                                className="px-8 py-3 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm"
                            >
                                Open in Editor
                            </button>
                        </div>
                    </>
                )}
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
