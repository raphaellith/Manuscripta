/**
 * Question Editor Dialog for creating and editing embedded questions.
 * Per Frontend Workflow Spec §4C(3).
 */

import React, { useState, useEffect, useCallback } from 'react';
import type { QuestionEntity, QuestionType } from '../../models';

interface QuestionEditorDialogProps {
    isOpen: boolean;
    materialId: string;
    materialType: 'READING' | 'WORKSHEET' | 'POLL';
    question?: QuestionEntity; // If provided, editing existing question
    onSave: (question: QuestionEntity) => Promise<string>; // Returns question ID
    onDelete?: (questionId: string) => Promise<void>; // Only for existing questions
    onCancel: () => void;
}

const generateTempId = () => `temp-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

export const QuestionEditorDialog: React.FC<QuestionEditorDialogProps> = ({
    isOpen,
    materialId,
    materialType,
    question,
    onSave,
    onDelete,
    onCancel,
}) => {
    const isEditing = question && question.id && !question.id.startsWith('temp-');

    // Form state
    const [questionType, setQuestionType] = useState<QuestionType>(
        question?.questionType || (materialType === 'POLL' ? 'MULTIPLE_CHOICE' : 'MULTIPLE_CHOICE')
    );
    const [questionText, setQuestionText] = useState(question?.questionText || '');
    const [options, setOptions] = useState<string[]>(
        question?.options || ['', '']
    );
    // -1 means no correct answer selected (for auto-marking disabled)
    const [correctAnswerIndex, setCorrectAnswerIndex] = useState<number>(
        typeof question?.correctAnswer === 'number' ? question.correctAnswer : -1
    );
    const [correctAnswer, setCorrectAnswer] = useState<string>(
        typeof question?.correctAnswer === 'string' ? question.correctAnswer : ''
    );
    const [maxScore, setMaxScore] = useState<number>(question?.maxScore || 1);
    // Auto-marking toggle for written answer questions
    const [autoMarking, setAutoMarking] = useState<boolean>(
        typeof question?.correctAnswer === 'string' && question.correctAnswer.length > 0
    );

    const [isSaving, setIsSaving] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [hasChanges, setHasChanges] = useState(false);

    // Reset form when question changes
    useEffect(() => {
        if (isOpen) {
            setQuestionType(question?.questionType || (materialType === 'POLL' ? 'MULTIPLE_CHOICE' : 'MULTIPLE_CHOICE'));
            setQuestionText(question?.questionText || '');
            setOptions(question?.options || ['', '']);
            setCorrectAnswerIndex(typeof question?.correctAnswer === 'number' ? question.correctAnswer : -1);
            setCorrectAnswer(typeof question?.correctAnswer === 'string' ? question.correctAnswer : '');
            setMaxScore(question?.maxScore || 1);
            setAutoMarking(typeof question?.correctAnswer === 'string' && question.correctAnswer.length > 0);
            setHasChanges(false);
            setError(null);
        }
    }, [isOpen, question, materialType]);

    // Track changes
    useEffect(() => {
        if (isEditing) {
            // Normalize the original correct answer for comparison
            const originalMcCorrectAnswer = typeof question?.correctAnswer === 'number'
                ? question.correctAnswer
                : -1;
            const originalWrittenCorrectAnswer = typeof question?.correctAnswer === 'string'
                ? question.correctAnswer
                : '';

            const changed =
                questionText !== question?.questionText ||
                JSON.stringify(options) !== JSON.stringify(question?.options) ||
                (questionType === 'MULTIPLE_CHOICE' && correctAnswerIndex !== originalMcCorrectAnswer) ||
                (questionType === 'WRITTEN_ANSWER' && correctAnswer !== originalWrittenCorrectAnswer) ||
                maxScore !== question?.maxScore;
            setHasChanges(changed);
        } else {
            setHasChanges(true); // New question always has "changes"
        }
    }, [questionText, options, correctAnswerIndex, correctAnswer, maxScore, questionType, question, isEditing]);

    // Validation per s4C(3)(b1)(ii)
    const isValid = useCallback(() => {
        if (!questionText.trim()) return false;

        if (questionType === 'MULTIPLE_CHOICE') {
            // Must have at least 2 non-empty options
            const nonEmptyOptions = options.filter(o => o.trim());
            if (nonEmptyOptions.length < 2) return false;
            // If a correct answer is selected, it must be valid
            if (correctAnswerIndex >= 0) {
                if (correctAnswerIndex >= options.length) return false;
                // The selected option must be non-empty
                if (!options[correctAnswerIndex]?.trim()) return false;
            }
            // correctAnswerIndex === -1 (no selection) is valid - auto-marking disabled
        }

        // WRITTEN_ANSWER: correctAnswer is optional (auto-marking toggle controls it)
        return true;
    }, [questionText, questionType, options, correctAnswerIndex]);

    // Can save per s4C(3)(b1)
    const canSave = isValid() && (isEditing ? hasChanges : true);

    const handleAddOption = () => {
        setOptions([...options, '']);
    };

    const handleRemoveOption = (index: number) => {
        if (options.length <= 2) return; // Must have at least 2 options
        const newOptions = options.filter((_, i) => i !== index);
        setOptions(newOptions);
        // Adjust correct answer if needed
        if (correctAnswerIndex >= newOptions.length) {
            setCorrectAnswerIndex(newOptions.length - 1);
        } else if (correctAnswerIndex > index) {
            // Shift down if we removed an option before the selected one
            setCorrectAnswerIndex(correctAnswerIndex - 1);
        }
    };

    const handleOptionChange = (index: number, value: string) => {
        const newOptions = [...options];
        newOptions[index] = value;
        setOptions(newOptions);
    };

    const handleSave = async () => {
        if (!canSave) return;

        setIsSaving(true);
        setError(null);

        try {
            // Keep track of original indices when filtering out empty options
            const indexedOptions = options.map((text, index) => ({ text, index }));
            const nonEmptyOptionEntries = indexedOptions.filter(o => o.text.trim());
            const nonEmptyOptions = nonEmptyOptionEntries.map(o => o.text);

            // Remap correctAnswerIndex to the filtered array (or undefined if -1)
            let mappedCorrectIndex: number | undefined = undefined;
            if (correctAnswerIndex >= 0) {
                const entryIndex = nonEmptyOptionEntries.findIndex(
                    entry => entry.index === correctAnswerIndex,
                );
                mappedCorrectIndex = entryIndex >= 0 ? entryIndex : undefined;
            }

            // For written answer, only include correctAnswer if autoMarking is enabled
            const writtenCorrectAnswer = autoMarking && correctAnswer.trim()
                ? correctAnswer.trim()
                : undefined;

            const questionEntity: QuestionEntity = {
                id: question?.id || generateTempId(),
                materialId,
                questionType,
                questionText: questionText.trim(),
                options: questionType === 'MULTIPLE_CHOICE' ? nonEmptyOptions : undefined,
                correctAnswer: questionType === 'MULTIPLE_CHOICE'
                    ? mappedCorrectIndex
                    : writtenCorrectAnswer,
                maxScore: maxScore > 0 ? maxScore : 1,
            };

            await onSave(questionEntity);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to save question');
        } finally {
            setIsSaving(false);
        }
    };

    const handleDelete = async () => {
        if (!isEditing || !onDelete || !question?.id) return;

        setIsDeleting(true);
        setError(null);

        try {
            await onDelete(question.id);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to delete question');
        } finally {
            setIsDeleting(false);
        }
    };

    if (!isOpen) return null;

    // Poll materials can only have MULTIPLE_CHOICE per Validation Rules §2B(3)(b)
    const canChangeType = materialType !== 'POLL';

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-[120]">
            <div className="bg-white rounded-lg shadow-xl w-[600px] max-h-[80vh] flex flex-col">
                {/* Header */}
                <div className="flex items-center justify-between p-4 border-b">
                    <h3 className="text-lg font-semibold">
                        {isEditing ? 'Edit Question' : 'Create Question'}
                    </h3>
                    <button
                        onClick={onCancel}
                        className="text-gray-400 hover:text-gray-600 text-2xl leading-none"
                    >
                        ×
                    </button>
                </div>

                {/* Form */}
                <div className="flex-1 overflow-y-auto p-4 space-y-4">
                    {error && (
                        <div className="bg-red-50 border border-red-200 text-red-700 px-3 py-2 rounded text-sm">
                            {error}
                        </div>
                    )}

                    {/* Question Type */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            Question Type
                        </label>
                        <select
                            value={questionType}
                            onChange={(e) => setQuestionType(e.target.value as QuestionType)}
                            disabled={!canChangeType}
                            className="w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-brand-orange disabled:bg-gray-100"
                        >
                            <option value="MULTIPLE_CHOICE">Multiple Choice</option>
                            <option value="WRITTEN_ANSWER">Written Answer</option>
                        </select>
                        {!canChangeType && (
                            <p className="text-xs text-gray-500 mt-1">
                                Poll materials can only contain multiple choice questions.
                            </p>
                        )}
                    </div>

                    {/* Question Text */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            Question Text <span className="text-red-500">*</span>
                        </label>
                        <textarea
                            value={questionText}
                            onChange={(e) => setQuestionText(e.target.value)}
                            placeholder="Enter your question..."
                            rows={3}
                            className="w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-brand-orange"
                        />
                    </div>

                    {/* Multiple Choice Options */}
                    {questionType === 'MULTIPLE_CHOICE' && (
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">
                                Options <span className="text-red-500">*</span>
                            </label>
                            <div className="space-y-2">
                                {options.map((option, index) => (
                                    <div key={index} className="flex items-center gap-2">
                                        <input
                                            type="radio"
                                            name="correctAnswer"
                                            checked={correctAnswerIndex === index}
                                            onClick={() => {
                                                // Click again to deselect
                                                if (correctAnswerIndex === index) {
                                                    setCorrectAnswerIndex(-1);
                                                } else {
                                                    setCorrectAnswerIndex(index);
                                                }
                                            }}
                                            onChange={() => { /* Handled by onClick for toggle behavior */ }}
                                            className="text-brand-orange cursor-pointer"
                                            title="Mark as correct answer (click again to clear)"
                                        />
                                        <input
                                            type="text"
                                            value={option}
                                            onChange={(e) => handleOptionChange(index, e.target.value)}
                                            placeholder={`Option ${index + 1}`}
                                            className={`flex-1 px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-brand-orange ${correctAnswerIndex === index ? 'border-green-500 bg-green-50' : ''
                                                }`}
                                        />
                                        {options.length > 2 && (
                                            <button
                                                onClick={() => handleRemoveOption(index)}
                                                className="text-red-500 hover:text-red-700 px-2"
                                                title="Remove option"
                                            >
                                                ×
                                            </button>
                                        )}
                                    </div>
                                ))}
                            </div>
                            <button
                                onClick={handleAddOption}
                                className="mt-2 text-sm text-brand-orange hover:text-orange-600"
                            >
                                + Add Option
                            </button>
                            <p className="text-xs text-gray-500 mt-1">
                                {correctAnswerIndex >= 0
                                    ? 'Click the selected option again to disable auto-marking.'
                                    : 'Select the correct answer to enable auto-marking (optional).'}
                            </p>
                        </div>
                    )}

                    {/* Written Answer */}
                    {questionType === 'WRITTEN_ANSWER' && (
                        <div className="space-y-3">
                            {/* Auto-marking toggle */}
                            <div className="flex items-center gap-3">
                                <label className="relative inline-flex items-center cursor-pointer">
                                    <input
                                        type="checkbox"
                                        checked={autoMarking}
                                        onChange={(e) => setAutoMarking(e.target.checked)}
                                        className="sr-only peer"
                                    />
                                    <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-orange-300 rounded-full peer peer-checked:after:translate-x-full rtl:peer-checked:after:-translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:start-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-brand-orange"></div>
                                </label>
                                <span className="text-sm font-medium text-gray-700">
                                    Enable Auto-marking
                                </span>
                            </div>
                            <p className="text-xs text-gray-500">
                                {autoMarking
                                    ? 'Responses will be automatically evaluated against the correct answer.'
                                    : 'Responses will require manual grading.'}
                            </p>

                            {/* Correct Answer field - only shown when auto-marking is enabled */}
                            {autoMarking && (
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Correct Answer
                                    </label>
                                    <textarea
                                        value={correctAnswer}
                                        onChange={(e) => setCorrectAnswer(e.target.value)}
                                        placeholder="Enter the correct answer for auto-marking..."
                                        rows={3}
                                        className="w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-brand-orange"
                                    />
                                </div>
                            )}
                        </div>
                    )}

                    {/* Max Score */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            Max Score
                        </label>
                        <input
                            type="number"
                            value={maxScore}
                            onChange={(e) => setMaxScore(parseInt(e.target.value) || 1)}
                            min={1}
                            className="w-24 px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-brand-orange"
                        />
                    </div>
                </div>

                {/* Footer */}
                <div className="flex items-center justify-between p-4 border-t bg-gray-50">
                    <div>
                        {/* Delete button - hidden for polls per s4C(3)(c) */}
                        {isEditing && onDelete && materialType !== 'POLL' && (
                            <button
                                onClick={handleDelete}
                                disabled={isDeleting}
                                className="px-4 py-2 text-red-600 hover:bg-red-50 rounded-md disabled:opacity-50"
                            >
                                {isDeleting ? 'Deleting...' : 'Delete Question'}
                            </button>
                        )}
                    </div>
                    <div className="flex items-center gap-2">
                        <button
                            onClick={onCancel}
                            className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-md"
                        >
                            Cancel
                        </button>
                        {/* Save button per s4C(3)(b) */}
                        <button
                            onClick={handleSave}
                            disabled={!canSave || isSaving}
                            className="px-4 py-2 bg-brand-orange text-white rounded-md hover:bg-orange-600 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {isSaving ? 'Saving...' : 'Save Question'}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default QuestionEditorDialog;
