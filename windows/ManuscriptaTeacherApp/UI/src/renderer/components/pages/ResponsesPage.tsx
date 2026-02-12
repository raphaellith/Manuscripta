/**
 * Responses Page component.
 * Per WindowsAppStructureSpec §2B(1)(d)(i) (placed in pages/).
 * Implements FrontendWorkflowSpec §6 — class-level and device-level views.
 */

import React, { useState, useMemo, useEffect, useCallback, useRef } from 'react';
import { Card } from '../common/Card';
import { useAppContext } from '../../state/AppContext';
import { useAlertContext } from '../../state/AlertContext';
import signalRService from '../../services/signalr/SignalRService';
import type {
    MaterialEntity,
    QuestionEntity,
    ResponseEntity,
    FeedbackEntity,
    PairedDeviceEntity,
    FeedbackStatus,
    InternalCreateFeedbackDto
} from '../../models';

type ViewMode = 'class' | 'device';

// Debounce hook for auto-save
function useDebounce<T>(value: T, delay: number): T {
    const [debouncedValue, setDebouncedValue] = useState(value);
    useEffect(() => {
        const timer = setTimeout(() => setDebouncedValue(value), delay);
        return () => clearTimeout(timer);
    }, [value, delay]);
    return debouncedValue;
}

// Status badge component with failure indicator per §6A(6)
const FeedbackStatusBadge: React.FC<{ status: FeedbackStatus; hasFailed?: boolean; onRetry?: () => void }> = ({ status, hasFailed, onRetry }) => {
    const styles: Record<FeedbackStatus, string> = {
        PROVISIONAL: 'bg-gray-100 text-gray-600',
        READY: hasFailed ? 'bg-red-100 text-red-700' : 'bg-yellow-100 text-yellow-700',
        DELIVERED: 'bg-green-100 text-green-700'
    };
    const labels: Record<FeedbackStatus, string> = {
        PROVISIONAL: 'Not Sent',
        READY: hasFailed ? 'Delivery Failed' : 'Pending',
        DELIVERED: 'Delivered'
    };
    return (
        <div className="flex items-center gap-2">
            <span className={`text-xs font-medium px-2 py-1 rounded-full ${styles[status]}`}>
                {labels[status]}
            </span>
            {/* Per §6A(6)(a): Failure indicator for specific response */}
            {hasFailed && status === 'READY' && (
                <span className="text-xs text-red-600" title="Delivery to device failed">
                    ⚠
                </span>
            )}
            {/* Per §6A(6)(b): Retry option near the indicator */}
            {status === 'READY' && onRetry && (
                <button
                    onClick={onRetry}
                    className="text-xs text-brand-orange hover:text-brand-orange-dark font-medium"
                >
                    Retry
                </button>
            )}
        </div>
    );
};

// Feedback input component with debounced auto-save
const FeedbackInput: React.FC<{
    responseId: string;
    feedback: FeedbackEntity | undefined;
    maxScore?: number;
    hasFailed?: boolean;
    onSave: (responseId: string, marks: number | null, text: string | null, existingId?: string) => void;
    onApprove: (feedbackId: string) => void;
    onRetry: (feedbackId: string) => void;
}> = ({ responseId, feedback, maxScore, hasFailed, onSave, onApprove, onRetry }) => {
    const [marks, setMarks] = useState<string>(feedback?.marks?.toString() ?? '');
    const [text, setText] = useState<string>(feedback?.text ?? '');

    // Per §6A(7)(b)(i): Only PROVISIONAL feedback can be edited
    const isEditable = !feedback || feedback.status === 'PROVISIONAL';

    const debouncedMarks = useDebounce(marks, 1000);
    const debouncedText = useDebounce(text, 1000);
    const lastSavedRef = useRef({ marks: feedback?.marks?.toString() ?? '', text: feedback?.text ?? '' });

    // Auto-save on debounced change per §6(5)(d)(iv) / §6(6)(e)(iv)
    useEffect(() => {
        const currentMarks = debouncedMarks;
        const currentText = debouncedText;

        if (currentMarks !== lastSavedRef.current.marks || currentText !== lastSavedRef.current.text) {
            lastSavedRef.current = { marks: currentMarks, text: currentText };
            const marksNum = currentMarks === '' ? null : parseInt(currentMarks, 10);
            onSave(responseId, isNaN(marksNum!) ? null : marksNum, currentText || null, feedback?.id);
        }
    }, [debouncedMarks, debouncedText, responseId, feedback?.id, onSave]);

    return (
        <div className="mt-3 p-3 bg-gray-50 rounded-lg border border-gray-200 space-y-3">
            <div className="flex items-center gap-4">
                <div className="flex items-center gap-2">
                    <label className="text-sm font-medium text-gray-600">Mark:</label>
                    <input
                        type="number"
                        min={0}
                        max={maxScore}
                        value={marks}
                        disabled={!isEditable}
                        onChange={(e) => {
                            const val = e.target.value;
                            if (maxScore && val !== '' && parseInt(val) > maxScore) return; // Prevent exceeding maxScore
                            setMarks(val);
                        }}
                        className={`w-20 p-2 text-sm border rounded focus:ring-1 outline-none disabled:bg-gray-100 disabled:cursor-not-allowed ${maxScore && marks && parseInt(marks) > maxScore
                            ? 'border-red-500 focus:border-red-500 focus:ring-red-500 text-red-600'
                            : 'border-gray-300 focus:border-brand-orange focus:ring-brand-orange'
                            }`}
                        placeholder={maxScore ? `/ ${maxScore}` : '—'}
                    />
                    {maxScore && <span className="text-sm text-gray-500">/ {maxScore}</span>}
                </div>
                {feedback && (
                    <FeedbackStatusBadge
                        status={feedback.status}
                        hasFailed={hasFailed}
                        onRetry={feedback.status === 'READY' ? () => onRetry(feedback.id) : undefined}
                    />
                )}
            </div>
            <div>
                <label className="text-sm font-medium text-gray-600 block mb-1">Feedback:</label>
                <textarea
                    value={text}
                    disabled={!isEditable}
                    onChange={(e) => setText(e.target.value)}
                    rows={2}
                    className="w-full p-2 text-sm border border-gray-300 rounded focus:border-brand-orange focus:ring-1 focus:ring-brand-orange outline-none resize-none disabled:bg-gray-100 disabled:cursor-not-allowed"
                    placeholder="Add feedback comment..."
                />
            </div>
            {feedback && feedback.status === 'PROVISIONAL' && (
                <button
                    onClick={() => onApprove(feedback.id)}
                    className="bg-brand-orange hover:bg-brand-orange-dark text-white text-sm font-medium py-2 px-4 rounded transition-colors"
                >
                    Send Feedback
                </button>
            )}
        </div>
    );
};

export const ResponsesPage: React.FC = () => {
    const { unitCollections, getUnitsForCollection, getLessonsForUnit, getMaterialsForLesson, getQuestionsForMaterial } = useAppContext();
    const { addAlert } = useAlertContext();

    // View mode per §6(4)-(5)
    const [viewMode, setViewMode] = useState<ViewMode>('class');

    // Selection state
    const [selectedMaterialId, setSelectedMaterialId] = useState<string>('');
    const [selectedQuestionId, setSelectedQuestionId] = useState<string>('');
    const [selectedDeviceId, setSelectedDeviceId] = useState<string>('');

    // Data state
    const [responses, setResponses] = useState<ResponseEntity[]>([]);
    const [feedbacks, setFeedbacks] = useState<FeedbackEntity[]>([]);
    const [devices, setDevices] = useState<PairedDeviceEntity[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    // Track failed feedback IDs per §6A(6) - for showing indicator near specific responses
    const [failedFeedbackIds, setFailedFeedbackIds] = useState<Set<string>>(new Set());

    // Popup state per §6(4)(c)(iv)
    const [devicePopup, setDevicePopup] = useState<{ isOpen: boolean; title: string; deviceNames: string[] }>({
        isOpen: false,
        title: '',
        deviceNames: []
    });

    // Nested navigation state
    const [selectedCollectionId, setSelectedCollectionId] = useState<string>('');
    const [selectedUnitId, setSelectedUnitId] = useState<string>('');
    const [selectedLessonId, setSelectedLessonId] = useState<string>('');

    // Load data
    const loadData = useCallback(async () => {
        try {
            const [responsesData, feedbacksData, devicesData] = await Promise.all([
                signalRService.getAllResponses(),
                signalRService.getAllFeedbacks(),
                signalRService.getAllPairedDevices()
            ]);
            setResponses(responsesData);
            setFeedbacks(feedbacksData);
            setDevices(devicesData);
        } catch (error) {
            console.error('Failed to load responses data:', error);
            addAlert('control_failed', undefined, 'Failed to load responses');
        } finally {
            setIsLoading(false);
        }
    }, [addAlert]);

    // Initial load and RefreshResponses handler per §6(3)(b)
    useEffect(() => {
        loadData();
        const unsubRefresh = signalRService.onRefreshResponses(() => loadData());
        // Per §6A(6): Track failed feedbacks for per-response indicator
        // Payload includes feedbackId and deviceId per API Contract §3.6.2
        const unsubDeliveryFailed = signalRService.onFeedbackDeliveryFailed((payload: { deviceId: string; feedbackId: string }) => {
            // Track failed feedback ID for indicator near specific response
            setFailedFeedbackIds(prev => new Set(prev).add(payload.feedbackId));
            // Show alert with device name (not entity IDs - those are internal)
            const device = devices.find(d => d.deviceId === payload.deviceId);
            addAlert('feedback_failed', undefined, `Feedback delivery failed${device ? ` to ${device.name}` : ''}`);
        });
        return () => {
            unsubRefresh();
            unsubDeliveryFailed();
        };
    }, [loadData, addAlert, devices]);

    // Derived data - materials with responses per §6(4)(a)
    const materialsWithResponses = useMemo(() => {
        // Build a set of question IDs that have at least one response
        const responseQuestionIds = new Set(responses.map(r => r.questionId));

        const materialsWithAnyResponse: MaterialEntity[] = [];

        // Traverse the content hierarchy once and collect materials
        // that contain at least one question with a response.
        unitCollections.forEach(uc => {
            getUnitsForCollection(uc.id).forEach(u => {
                getLessonsForUnit(u.id).forEach(l => {
                    getMaterialsForLesson(l.id).forEach(m => {
                        const questions = getQuestionsForMaterial(m.id);
                        if (questions.some(q => responseQuestionIds.has(q.id))) {
                            materialsWithAnyResponse.push(m);
                        }
                    });
                });
            });
        });

        return materialsWithAnyResponse;
    }, [responses, unitCollections, getUnitsForCollection, getLessonsForUnit, getMaterialsForLesson, getQuestionsForMaterial]);

    // Devices with responses per §6(6)(a)
    const devicesWithResponses = useMemo(() => {
        const deviceIds = new Set(responses.map(r => r.deviceId));
        return devices.filter(d => deviceIds.has(d.deviceId));
    }, [responses, devices]);

    // Questions for selected material
    const questionsForMaterial = useMemo(() => {
        if (!selectedMaterialId) return [];
        return getQuestionsForMaterial(selectedMaterialId);
    }, [selectedMaterialId, getQuestionsForMaterial]);

    // Responses for selected question
    const responsesForQuestion = useMemo(() => {
        if (!selectedQuestionId) return [];
        return responses.filter(r => r.questionId === selectedQuestionId);
    }, [selectedQuestionId, responses]);

    // Get device name
    const getDeviceName = useCallback((deviceId: string) => {
        return devices.find(d => d.deviceId === deviceId)?.name ?? 'Unknown Device';
    }, [devices]);

    // Get feedback for response
    const getFeedbackForResponse = useCallback((responseId: string) => {
        return feedbacks.find(f => f.responseId === responseId);
    }, [feedbacks]);

    // Feedback handlers
    // Per FrontendWorkflowSpecifications §6A(7):
    // - Only PROVISIONAL feedback can be edited
    // - Clearing both fields (marks and text) triggers deletion
    const handleSaveFeedback = useCallback(async (responseId: string, marks: number | null, text: string | null, existingId?: string) => {
        try {
            const bothEmpty = marks === null && (!text || text.trim() === '');

            if (existingId) {
                const existing = feedbacks.find(f => f.id === existingId);
                if (existing) {
                    // Per §6A(7)(b): Only PROVISIONAL feedback can be edited
                    if (existing.status !== 'PROVISIONAL') {
                        console.warn('Cannot edit non-PROVISIONAL feedback');
                        return;
                    }

                    // Per §6A(7)(a)(ii): Clearing both fields = delete
                    if (bothEmpty) {
                        await signalRService.deleteFeedback(existingId);
                        setFeedbacks(prev => prev.filter(f => f.id !== existingId));
                    } else {
                        await signalRService.updateFeedback({ ...existing, marks, text });
                        setFeedbacks(prev => prev.map(f => f.id === existingId ? { ...f, marks, text } : f));
                    }
                }
            } else {
                // Don't create empty feedback
                if (bothEmpty) return;

                const newFeedback: InternalCreateFeedbackDto = {
                    responseId,
                    marks,
                    text
                };
                const created = await signalRService.createFeedback(newFeedback);
                setFeedbacks(prev => [...prev, created]);
            }
        } catch (error) {
            console.error('Failed to save feedback:', error);
            addAlert('control_failed', undefined, 'Failed to save feedback');
        }
    }, [feedbacks, addAlert]);

    const handleApproveFeedback = useCallback(async (feedbackId: string) => {
        try {
            await signalRService.approveFeedback(feedbackId);
            setFeedbacks(prev => prev.map(f => f.id === feedbackId ? { ...f, status: 'READY' as FeedbackStatus } : f));
        } catch (error) {
            console.error('Failed to approve feedback:', error);
            addAlert('control_failed', undefined, 'Failed to send feedback');
        }
    }, [addAlert]);

    const handleRetryFeedback = useCallback(async (feedbackId: string) => {
        try {
            // Clear failed status when retry is initiated (will be re-added if it fails again)
            setFailedFeedbackIds(prev => {
                const next = new Set(prev);
                next.delete(feedbackId);
                return next;
            });
            await signalRService.retryFeedbackDispatch(feedbackId);
        } catch (error) {
            console.error('Failed to retry feedback dispatch:', error);
            addAlert('control_failed', undefined, 'Failed to retry feedback');
        }
    }, [addAlert]);

    // Derived navigation
    const unitsForCollection = useMemo(() => selectedCollectionId ? getUnitsForCollection(selectedCollectionId) : [], [selectedCollectionId, getUnitsForCollection]);
    const lessonsForUnit = useMemo(() => selectedUnitId ? getLessonsForUnit(selectedUnitId) : [], [selectedUnitId, getLessonsForUnit]);
    const materialsForLesson = useMemo(() => selectedLessonId ? getMaterialsForLesson(selectedLessonId).filter(m => materialsWithResponses.some(mwr => mwr.id === m.id)) : [], [selectedLessonId, getMaterialsForLesson, materialsWithResponses]);

    if (isLoading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-brand-orange"></div>
            </div>
        );
    }

    return (
        <div>
            {/* View Mode Tabs */}
            <div className="mb-6 flex gap-2">
                {(['class', 'device'] as ViewMode[]).map(mode => (
                    <button
                        key={mode}
                        onClick={() => setViewMode(mode)}
                        className={`px-4 py-2 rounded-lg font-medium transition-colors ${viewMode === mode
                            ? 'bg-brand-orange text-white'
                            : 'bg-white text-gray-600 border border-gray-200 hover:border-brand-orange'
                            }`}
                    >
                        {mode === 'class' ? 'Class Overview' : 'By Device'}
                    </button>
                ))}
            </div>

            {/* Material/Device Selection */}
            <Card className="mb-6 border-t-4 border-t-brand-orange">
                <h4 className="font-sans font-semibold text-lg text-text-heading mb-4">
                    Selection
                </h4>

                <div className="space-y-4">
                    {/* Device Selection - Only in Device Mode */}
                    {viewMode === 'device' && (
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Select Device</label>
                            <select
                                value={selectedDeviceId}
                                onChange={(e) => setSelectedDeviceId(e.target.value)}
                                className="w-full p-3 bg-white text-text-body rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange outline-none"
                            >
                                <option value="">-- Select Device --</option>
                                {devicesWithResponses.map(d => (
                                    <option key={d.deviceId} value={d.deviceId}>{d.name}</option>
                                ))}
                            </select>
                        </div>
                    )}

                    {/* Material Selection - Always visible to ensure context is set */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Select Material</label>
                        <div className="flex flex-col sm:flex-row gap-4">
                            <select
                                value={selectedCollectionId}
                                onChange={(e) => { setSelectedCollectionId(e.target.value); setSelectedUnitId(''); setSelectedLessonId(''); setSelectedMaterialId(''); }}
                                className="flex-1 p-3 bg-white text-text-body rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange outline-none"
                            >
                                <option value="">-- Collection --</option>
                                {unitCollections.map(c => <option key={c.id} value={c.id}>{c.title}</option>)}
                            </select>
                            <select
                                value={selectedUnitId}
                                onChange={(e) => { setSelectedUnitId(e.target.value); setSelectedLessonId(''); setSelectedMaterialId(''); }}
                                disabled={!selectedCollectionId}
                                className="flex-1 p-3 bg-white text-text-body rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange outline-none disabled:opacity-50"
                            >
                                <option value="">-- Unit --</option>
                                {unitsForCollection.map(u => <option key={u.id} value={u.id}>{u.title}</option>)}
                            </select>
                            <select
                                value={selectedLessonId}
                                onChange={(e) => { setSelectedLessonId(e.target.value); setSelectedMaterialId(''); }}
                                disabled={!selectedUnitId}
                                className="flex-1 p-3 bg-white text-text-body rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange outline-none disabled:opacity-50"
                            >
                                <option value="">-- Lesson --</option>
                                {lessonsForUnit.map(l => <option key={l.id} value={l.id}>{l.title}</option>)}
                            </select>
                            <select
                                value={selectedMaterialId}
                                onChange={(e) => { setSelectedMaterialId(e.target.value); setSelectedQuestionId(''); }}
                                disabled={!selectedLessonId}
                                className="flex-1 p-3 bg-white text-text-body rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange outline-none disabled:opacity-50"
                            >
                                <option value="">-- Material --</option>
                                {materialsForLesson.map(m => <option key={m.id} value={m.id}>{m.title}</option>)}
                            </select>
                        </div>
                    </div>
                </div>
            </Card>

            {/* Question Selection for device mode only */}
            {viewMode === 'device' && selectedMaterialId && (
                <Card className="mb-6 border-t-4 border-t-brand-blue">
                    <h4 className="font-sans font-semibold text-lg text-text-heading mb-4">Select Question</h4>
                    <select
                        value={selectedQuestionId}
                        onChange={(e) => setSelectedQuestionId(e.target.value)}
                        className="w-full p-3 bg-white text-text-body rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange outline-none"
                    >
                        <option value="">-- Select Question --</option>
                        {questionsForMaterial.map((q, idx) => (
                            <option key={q.id} value={q.id}>
                                Q{idx + 1}: {q.questionText.substring(0, 60)}{q.questionText.length > 60 ? '...' : ''}
                            </option>
                        ))}
                    </select>
                </Card>
            )}

            {/* Class-level Responses Display per §6(4)(b) - shows all questions when material selected */}
            {viewMode === 'class' && selectedMaterialId && questionsForMaterial.length > 0 && (
                <Card className="border-t-4 border-t-brand-green">
                    <h4 className="font-sans font-semibold text-lg text-text-heading mb-6">Questions & Responses</h4>
                    <div className="space-y-8">
                        {questionsForMaterial.map((question, qIdx) => {
                            const questionResponses = responses.filter(r => r.questionId === question.id);

                            return (
                                <div key={question.id} className="border-b border-gray-200 pb-6 last:border-b-0 last:pb-0">
                                    <h5 className="font-medium text-text-heading mb-2">Q{qIdx + 1}: {question.questionText}</h5>
                                    {question.maxScore && <p className="text-sm text-gray-500 mb-3">Max Score: {question.maxScore}</p>}

                                    {questionResponses.length === 0 ? (
                                        <p className="text-gray-400 text-sm">No responses yet</p>
                                    ) : question.questionType === 'MULTIPLE_CHOICE' ? (
                                        // MC distribution per §6(4)(c)
                                        <div className="space-y-2">
                                            {(question.options ?? []).map((opt, idx) => {
                                                const count = questionResponses.filter(r => r.responseContent === String(idx)).length;
                                                const percentage = questionResponses.length > 0 ? Math.round((count / questionResponses.length) * 100) : 0;
                                                // Handle potential string/number mismatch for correctAnswer
                                                const isCorrect = String(question.correctAnswer) === String(idx);
                                                const deviceNames = questionResponses.filter(r => r.responseContent === String(idx)).map(r => getDeviceName(r.deviceId));

                                                return (
                                                    <div
                                                        key={idx}
                                                        onClick={() => setDevicePopup({
                                                            isOpen: true,
                                                            title: `Option: ${opt}`,
                                                            deviceNames
                                                        })}
                                                        className={`p-2 rounded border cursor-pointer transition-colors ${isCorrect ? 'border-green-400 bg-green-50 hover:bg-green-100' : 'border-gray-200 hover:bg-gray-50'}`}
                                                    >
                                                        <div className="flex justify-between items-center text-sm">
                                                            <span className={isCorrect ? 'text-green-700 font-medium' : 'text-gray-700'}>{opt} {isCorrect && '✓'}</span>
                                                            <span className="text-gray-500">{count} ({percentage}%)</span>
                                                        </div>
                                                        <div className="mt-1 h-1.5 bg-gray-100 rounded-full overflow-hidden">
                                                            <div className={`h-full ${isCorrect ? 'bg-green-400' : 'bg-brand-blue'}`} style={{ width: `${percentage}%` }} />
                                                        </div>
                                                    </div>
                                                );
                                            })}
                                        </div>
                                    ) : (
                                        // Written answer with feedback per §6(4)(d)
                                        <div className="space-y-4">
                                            {question.correctAnswer && (
                                                <div className="p-2 bg-green-50 rounded border border-green-200">
                                                    <p className="text-xs font-medium text-green-700">Correct Answer:</p>
                                                    <p className="text-xs text-green-600">{question.correctAnswer}</p>
                                                </div>
                                            )}
                                            {question.markScheme && (
                                                <div className="p-2 bg-blue-50 rounded border border-blue-200">
                                                    <p className="text-xs font-medium text-blue-700">Mark Scheme:</p>
                                                    <p className="text-xs text-blue-600">{question.markScheme}</p>
                                                </div>
                                            )}
                                            {questionResponses.map(response => (
                                                <div key={response.id} className="border border-gray-200 rounded-lg p-3">
                                                    <div className="flex justify-between items-start mb-2">
                                                        <span className="font-medium text-brand-blue text-sm">{getDeviceName(response.deviceId)}</span>
                                                        <span className="text-xs text-gray-400">{new Date(response.timestamp).toLocaleString()}</span>
                                                    </div>
                                                    <p className="text-gray-700 bg-gray-50 p-2 rounded text-sm">{response.responseContent}</p>
                                                    {question.correctAnswer ? (
                                                        <div className="mt-2">
                                                            <span className={`px-2 py-1 rounded text-xs font-semibold ${response.isCorrect ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                                                                {response.isCorrect ? 'Correct' : 'Incorrect'}
                                                            </span>
                                                        </div>
                                                    ) : (
                                                        <FeedbackInput
                                                            responseId={response.id}
                                                            feedback={getFeedbackForResponse(response.id)}
                                                            maxScore={question.maxScore}
                                                            hasFailed={(() => {
                                                                const fb = getFeedbackForResponse(response.id);
                                                                return fb ? failedFeedbackIds.has(fb.id) : false;
                                                            })()}
                                                            onSave={handleSaveFeedback}
                                                            onApprove={handleApproveFeedback}
                                                            onRetry={handleRetryFeedback}
                                                        />
                                                    )}
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            );
                        })}
                    </div>
                </Card>
            )}

            {/* Responses Display for device mode with question selected */}
            {viewMode === 'device' && selectedQuestionId && (
                <Card className="border-t-4 border-t-brand-green">
                    <h4 className="font-sans font-semibold text-lg text-text-heading mb-4">Responses</h4>

                    {(() => {
                        const question = questionsForMaterial.find(q => q.id === selectedQuestionId);
                        if (!question) return null;

                        const relevantResponses = viewMode === 'device' && selectedDeviceId
                            ? responsesForQuestion.filter(r => r.deviceId === selectedDeviceId)
                            : responsesForQuestion;

                        if (relevantResponses.length === 0) {
                            return <p className="text-gray-500 text-center py-8">No responses yet</p>;
                        }

                        // Multiple Choice Question per §6(5)(c)
                        if (question.questionType === 'MULTIPLE_CHOICE') {
                            const options = question.options ?? [];

                            // §6(6)(c): Single device view - improved UI (no percentages)
                            if (viewMode === 'device' && selectedDeviceId) {
                                const response = relevantResponses.find(r => r.deviceId === selectedDeviceId);
                                const studentAnswerIndex = response ? parseInt(response.responseContent) : -1;

                                return (
                                    <div className="space-y-4">
                                        <div className="mb-4">
                                            <p className="font-medium text-text-heading">{question.questionText}</p>
                                            {question.maxScore && <p className="text-sm text-gray-500">Max Score: {question.maxScore}</p>}
                                        </div>
                                        {/* Status banner */}
                                        {response && (
                                            <div className={`p-3 rounded-lg border flex items-center gap-2 mb-4 ${response.isCorrect ? 'bg-green-50 border-green-200 text-green-700' : 'bg-red-50 border-red-200 text-red-700'}`}>
                                                <span className="font-bold">{response.isCorrect ? 'Correct' : 'Incorrect'}</span>
                                            </div>
                                        )}
                                        <div className="space-y-2">
                                            {options.map((opt, idx) => {
                                                const isSelected = idx === studentAnswerIndex;
                                                // Check for correct answer match (handling potential string/number types)
                                                const isCorrectOption = String(question.correctAnswer) === String(idx);

                                                let containerClass = "border-gray-200 bg-white";
                                                let textClass = "text-gray-700";
                                                let badge = null;

                                                if (isSelected) {
                                                    if (isCorrectOption) {
                                                        containerClass = "border-green-500 bg-green-50";
                                                        textClass = "text-green-800 font-medium";
                                                        badge = <span className="text-xs font-semibold px-2 py-1 rounded bg-green-200 text-green-800">Selected</span>;
                                                    } else {
                                                        containerClass = "border-red-500 bg-red-50";
                                                        textClass = "text-red-800 font-medium";
                                                        badge = <span className="text-xs font-semibold px-2 py-1 rounded bg-red-200 text-red-800">Selected</span>;
                                                    }
                                                } else if (isCorrectOption) {
                                                    // Show correct answer if student got it wrong
                                                    containerClass = "border-green-500 bg-white border-dashed";
                                                    textClass = "text-green-700";
                                                    badge = <span className="text-xs font-semibold px-2 py-1 rounded bg-green-100 text-green-700">Correct Answer</span>;
                                                }

                                                return (
                                                    <div key={idx} className={`p-3 rounded-lg border ${containerClass} flex justify-between items-center transition-colors`}>
                                                        <div className="flex items-center gap-3">
                                                            <div className={`w-5 h-5 rounded-full border flex items-center justify-center ${isSelected ? (isCorrectOption ? 'border-green-600 bg-green-600' : 'border-red-600 bg-red-600') : 'border-gray-300'}`}>
                                                                {isSelected && <div className="w-2 h-2 bg-white rounded-full" />}
                                                            </div>
                                                            <span className={textClass}>{opt}</span>
                                                        </div>
                                                        {badge}
                                                    </div>
                                                );
                                            })}
                                        </div>
                                    </div>
                                );
                            }

                            const distribution = options.map((opt, idx) => ({
                                option: opt,
                                index: idx,
                                count: relevantResponses.filter(r => r.responseContent === String(idx)).length,
                                devices: relevantResponses.filter(r => r.responseContent === String(idx)).map(r => getDeviceName(r.deviceId))
                            }));
                            const total = relevantResponses.length;

                            return (
                                <div className="space-y-4">
                                    <div className="mb-4">
                                        <p className="font-medium text-text-heading">{question.questionText}</p>
                                        {question.maxScore && <p className="text-sm text-gray-500">Max Score: {question.maxScore}</p>}
                                    </div>
                                    {distribution.map(({ option, index, count, devices: deviceNames }) => {
                                        // Handle potential string/number mismatch for correctAnswer
                                        const isCorrect = String(question.correctAnswer) === String(index);
                                        const percentage = total > 0 ? Math.round((count / total) * 100) : 0;
                                        return (
                                            <div
                                                key={index}
                                                className={`p-3 rounded-lg border ${isCorrect ? 'border-green-400 bg-green-50' : 'border-gray-200 bg-white'}`}
                                                title={`Devices: ${deviceNames.length > 0 ? deviceNames.join(', ') : 'None'}`}
                                            >
                                                <div className="flex justify-between items-center">
                                                    <span className={isCorrect ? 'text-green-700 font-medium' : 'text-gray-700'}>
                                                        {option} {isCorrect && '✓'}
                                                    </span>
                                                    <span className="text-sm text-gray-500">{count} ({percentage}%)</span>
                                                </div>
                                                <div className="mt-2 h-2 bg-gray-100 rounded-full overflow-hidden">
                                                    <div
                                                        className={`h-full ${isCorrect ? 'bg-green-400' : 'bg-brand-blue'}`}
                                                        style={{ width: `${percentage}%` }}
                                                    />
                                                </div>
                                                {/* Device names removed from inline display per §6(4)(c)(iv) - now in tooltip */}
                                            </div>
                                        );
                                    })}
                                </div>
                            );
                        }

                        // Written Answer Question per §6(5)(d)
                        return (
                            <div className="space-y-6">
                                <div className="mb-4">
                                    <p className="font-medium text-text-heading">{question.questionText}</p>
                                    {question.maxScore && <p className="text-sm text-gray-500">Max Score: {question.maxScore}</p>}
                                    {question.correctAnswer && (
                                        <div className="mt-2 p-3 bg-green-50 rounded-lg border border-green-200">
                                            <p className="text-sm font-medium text-green-700">Correct Answer:</p>
                                            <p className="text-sm text-green-600 mt-1">{question.correctAnswer}</p>
                                        </div>
                                    )}
                                    {question.markScheme && (
                                        <div className="mt-2 p-3 bg-blue-50 rounded-lg border border-blue-200">
                                            <p className="text-sm font-medium text-blue-700">Mark Scheme:</p>
                                            <p className="text-sm text-blue-600 mt-1">{question.markScheme}</p>
                                        </div>
                                    )}
                                </div>
                                {relevantResponses.map(response => (
                                    <div key={response.id} className="border border-gray-200 rounded-lg p-4">
                                        <div className="flex justify-between items-start mb-2">
                                            <span className="font-medium text-brand-blue">{getDeviceName(response.deviceId)}</span>
                                            <span className="text-xs text-gray-400">{new Date(response.timestamp).toLocaleString()}</span>
                                        </div>
                                        <p className="text-gray-700 bg-gray-50 p-3 rounded">{response.responseContent}</p>
                                        {question.correctAnswer ? (
                                            <div className="mt-2">
                                                <span className={`px-2 py-1 rounded text-xs font-semibold ${response.isCorrect ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                                                    {response.isCorrect ? 'Correct' : 'Incorrect'}
                                                </span>
                                            </div>
                                        ) : (
                                            <FeedbackInput
                                                responseId={response.id}
                                                feedback={getFeedbackForResponse(response.id)}
                                                maxScore={question.maxScore}
                                                hasFailed={(() => {
                                                    const fb = getFeedbackForResponse(response.id);
                                                    return fb ? failedFeedbackIds.has(fb.id) : false;
                                                })()}
                                                onSave={handleSaveFeedback}
                                                onApprove={handleApproveFeedback}
                                                onRetry={handleRetryFeedback}
                                            />
                                        )}
                                    </div>
                                ))}
                            </div>
                        );
                    })()}
                </Card>
            )}

            {/* Empty state */}
            {responses.length === 0 && (
                <Card className="text-center py-16">
                    <p className="text-gray-500 text-lg">No responses received yet</p>
                    <p className="text-gray-400 text-sm mt-2">Responses will appear here when students submit answers</p>
                </Card>
            )}
            {/* Device List Popup per §6(4)(c)(iv) */}
            {devicePopup.isOpen && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" onClick={() => setDevicePopup(prev => ({ ...prev, isOpen: false }))}>
                    <div className="bg-white rounded-lg p-6 max-w-md w-full m-4 shadow-xl" onClick={e => e.stopPropagation()}>
                        <div className="flex justify-between items-center mb-4">
                            <h3 className="font-semibold text-lg text-text-heading">{devicePopup.title}</h3>
                            <button onClick={() => setDevicePopup(prev => ({ ...prev, isOpen: false }))} className="text-gray-400 hover:text-gray-600">
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                            </button>
                        </div>
                        <div className="max-h-60 overflow-y-auto">
                            {devicePopup.deviceNames.length > 0 ? (
                                <ul className="space-y-2">
                                    {devicePopup.deviceNames.map((name, i) => (
                                        <li key={i} className="flex items-center gap-2 p-2 rounded hover:bg-gray-50">
                                            <span className="w-2 h-2 rounded-full bg-brand-blue"></span>
                                            <span className="text-text-body">{name}</span>
                                        </li>
                                    ))}
                                </ul>
                            ) : (
                                <p className="text-gray-500 text-center py-4">No devices selected this option.</p>
                            )}
                        </div>
                        <div className="mt-6 flex justify-end">
                            <button
                                onClick={() => setDevicePopup(prev => ({ ...prev, isOpen: false }))}
                                className="px-4 py-2 bg-gray-100 text-gray-700 rounded hover:bg-gray-200 font-medium text-sm transition-colors"
                            >
                                Close
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};
