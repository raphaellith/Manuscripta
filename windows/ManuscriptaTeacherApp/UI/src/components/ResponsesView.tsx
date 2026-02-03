
import React, { useState, useMemo } from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { Card } from '../renderer/components/common/Card';
import type { ContentItem, Question, StudentResponse } from '../types';

interface ResponsesViewProps {
    contentItems: ContentItem[];
}

// Mock questions for quizzes - covering all three question types
const mockQuestions: Question[] = [
    // Multiple Choice questions
    {
        id: 'q1',
        materialId: '1b',
        text: 'What was the date of the Battle of Hastings?',
        type: 'MULTIPLE_CHOICE',
        options: ['25th December 1066', '14th October 1066', '4th July 1066'],
        correctAnswer: '1'
    },
    {
        id: 'q2',
        materialId: '1b',
        text: 'Where did the battle take place?',
        type: 'MULTIPLE_CHOICE',
        options: ['In London', 'Near the modern town of Battle, East Sussex', 'In Normandy, France'],
        correctAnswer: '1'
    },
    // True/False question
    {
        id: 'q3',
        materialId: '1b',
        text: 'Harold Godwinson was the last Anglo-Saxon King of England.',
        type: 'TRUE_FALSE',
        options: ['True', 'False'],
        correctAnswer: '0' // True (index 0)
    },
    // Written Answer question
    {
        id: 'q4',
        materialId: '1b',
        text: 'In your own words, explain why William believed he had a right to the English throne.',
        type: 'WRITTEN_ANSWER',
        correctAnswer: undefined // No single correct answer for written responses
    },
];

// Sample written answers for the written answer question
const sampleWrittenAnswers = [
    "William believed he had a right because Edward the Confessor had promised him the throne.",
    "He was related to the English royal family through his aunt Emma of Normandy.",
    "Edward promised William the throne and Harold swore an oath to support him.",
    "William claimed Edward had promised him succession and that Harold broke his sacred oath.",
    "Because Edward the Confessor was his cousin and promised him the crown.",
    "His aunt married King Ethelred so he thought he was next in line.",
    "Edward told William he would be king, but Harold took it instead.",
    "William said Edward promised him the throne when he visited Normandy.",
];

// Generate mock responses from 28 devices
const generateMockResponses = (): StudentResponse[] => {
    const responses: StudentResponse[] = [];
    const correctAnswers: Record<string, string> = { q1: '1', q2: '1', q3: '0' };
    
    for (let deviceNum = 1; deviceNum <= 28; deviceNum++) {
        for (const question of mockQuestions) {
            let answer: string;
            let isCorrect: boolean;
            const random = Math.random();
            
            if (question.type === 'WRITTEN_ANSWER') {
                // For written answers, pick a random sample answer
                answer = sampleWrittenAnswers[Math.floor(Math.random() * sampleWrittenAnswers.length)];
                isCorrect = false; // Needs manual marking
            } else if (question.type === 'TRUE_FALSE') {
                // 80% get true/false correct
                if (random < 0.80) {
                    answer = correctAnswers[question.id];
                    isCorrect = true;
                } else {
                    answer = correctAnswers[question.id] === '0' ? '1' : '0';
                    isCorrect = false;
                }
            } else {
                // Multiple choice - 75% correct
                if (random < 0.75) {
                    answer = correctAnswers[question.id];
                    isCorrect = true;
                } else {
                    const options = question.options || [];
                    const wrongAnswers = options.map((_, i) => String(i)).filter(i => i !== correctAnswers[question.id]);
                    answer = wrongAnswers[Math.floor(Math.random() * wrongAnswers.length)];
                    isCorrect = false;
                }
            }
            
            responses.push({
                id: `r-${question.id}-${deviceNum}`,
                questionId: question.id,
                answer,
                isCorrect,
                timestamp: Date.now() - Math.random() * 3600000,
                deviceId: `device-${deviceNum}`,
                synced: true,
            });
        }
    }
    
    return responses;
};

const mockResponses = generateMockResponses();

export const ResponsesView: React.FC<ResponsesViewProps> = ({ contentItems }) => {
    const [selectedMaterialId, setSelectedMaterialId] = useState<string>('');
    const [expandedQuestions, setExpandedQuestions] = useState<Set<string>>(new Set());
    const [manualMarks, setManualMarks] = useState<Record<string, boolean>>({});
    const [expandedWrittenAnswer, setExpandedWrittenAnswer] = useState<string | null>(null);
    const [autoMarkEnabled, setAutoMarkEnabled] = useState<boolean>(false);

    // Get deployed quizzes only
    const quizzes = useMemo(() => {
        return contentItems.filter(item => item.type === 'Quiz' && item.status === 'Deployed');
    }, [contentItems]);

    // Get questions for selected material
    const questions = useMemo(() => {
        if (!selectedMaterialId) return [];
        return mockQuestions.filter(q => q.materialId === selectedMaterialId);
    }, [selectedMaterialId]);

    // Get responses for selected material
    const responses = useMemo(() => {
        if (!selectedMaterialId) return [];
        const questionIds = questions.map(q => q.id);
        return mockResponses.filter(r => questionIds.includes(r.questionId));
    }, [selectedMaterialId, questions]);

    // Calculate summary statistics
    const stats = useMemo(() => {
        if (responses.length === 0) return null;
        
        const deviceIds = new Set(responses.map(r => r.deviceId));
        // For written answers, use manual marks; for others, use isCorrect or manual override
        const correct = responses.filter(r => {
            const question = mockQuestions.find(q => q.id === r.questionId);
            if (question?.type === 'WRITTEN_ANSWER') {
                return manualMarks[r.id] === true;
            }
            return manualMarks[r.id] ?? r.isCorrect;
        }).length;
        
        // Exclude written answers from percentage calculation for now
        const gradableResponses = responses.filter(r => {
            const question = mockQuestions.find(q => q.id === r.questionId);
            return question?.type !== 'WRITTEN_ANSWER';
        });
        
        return {
            totalDevices: deviceIds.size,
            totalResponses: responses.length,
            correctResponses: correct,
            averageScore: gradableResponses.length > 0 
                ? Math.round((gradableResponses.filter(r => manualMarks[r.id] ?? r.isCorrect).length / gradableResponses.length) * 100) 
                : 0,
        };
    }, [responses, manualMarks]);


    const toggleQuestion = (questionId: string) => {
        setExpandedQuestions(prev => {
            const next = new Set(prev);
            if (next.has(questionId)) {
                next.delete(questionId);
            } else {
                next.add(questionId);
            }
            return next;
        });
    };

    // For written answers: set mark directly (true = correct, false = incorrect)
    const setMark = (responseId: string, value: boolean) => {
        setManualMarks(prev => ({
            ...prev,
            [responseId]: value,
        }));
    };

    // For MC/TF: toggle the current value
    const toggleMark = (responseId: string, currentValue: boolean) => {
        setManualMarks(prev => ({
            ...prev,
            [responseId]: !currentValue,
        }));
    };

    const handleAutoMark = () => {
        setAutoMarkEnabled(prev => !prev);
        // Placeholder: in production, this would trigger LLM-based marking
        if (!autoMarkEnabled) {
            alert('Auto-Mark enabled. In production, this would use AI to automatically grade written responses based on marking criteria.');
        }
    };

    const handleExport = () => {
        alert('Export functionality would download responses as CSV/JSON file.');
    };

    const getChartData = (question: Question) => {
        if (!question.options) return [];
        
        const questionResponses = responses.filter(r => r.questionId === question.id);
        
        return question.options.map((option, index) => {
            const count = questionResponses.filter(r => r.answer === String(index)).length;
            return {
                name: question.type === 'TRUE_FALSE' ? option : String.fromCharCode(65 + index),
                count,
                fullText: option,
                isCorrect: String(index) === question.correctAnswer,
            };
        });
    };

    const formatTimestamp = (ts: number) => {
        return new Date(ts).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
    };

    // Get question type badge
    const getQuestionTypeBadge = (type: string) => {
        switch (type) {
            case 'MULTIPLE_CHOICE':
                return <span className="px-2 py-0.5 text-xs font-medium bg-brand-blue/10 text-brand-blue rounded">Multiple Choice</span>;
            case 'TRUE_FALSE':
                return <span className="px-2 py-0.5 text-xs font-medium bg-purple-100 text-purple-700 rounded">True / False</span>;
            case 'WRITTEN_ANSWER':
                return <span className="px-2 py-0.5 text-xs font-medium bg-brand-orange/10 text-brand-orange rounded">Written Answer</span>;
            default:
                return null;
        }
    };

    return (
        <div>
            {/* Header with material selector and export - inline style like Library search */}
            <div className="mb-6 flex flex-col md:flex-row gap-4 sticky top-0 z-40 pb-4 bg-gradient-to-b from-brand-cream to-transparent">
                <div className="relative flex-grow">
                    <select
                        value={selectedMaterialId}
                        onChange={(e) => setSelectedMaterialId(e.target.value)}
                        className="w-full p-4 bg-white text-text-body font-sans rounded-xl border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none transition-all shadow-soft appearance-none cursor-pointer"
                    >
                        <option value="">Select a Quiz or Poll to view responses...</option>
                        {quizzes.map(quiz => (
                            <option key={quiz.id} value={quiz.id}>
                                {quiz.title} ({quiz.unit})
                            </option>
                        ))}
                    </select>
                    <span className="absolute inset-y-0 right-0 flex items-center pr-4 pointer-events-none">
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                        </svg>
                    </span>
                </div>
                <button
                    onClick={handleExport}
                    disabled={!selectedMaterialId}
                    className="px-8 py-4 bg-brand-green text-white font-sans font-medium rounded-xl hover:bg-green-800 transition-colors shadow-soft hover:shadow-md whitespace-nowrap flex-shrink-0 flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                    </svg>
                    Export
                </button>
            </div>

            {/* Summary statistics */}
            {stats && (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
                    <Card className="text-center">
                        <p className="text-3xl font-serif font-semibold text-brand-orange">{stats.totalDevices}</p>
                        <p className="text-sm text-gray-500 font-sans mt-1">Devices Responded</p>
                    </Card>
                    <Card className="text-center">
                        <p className="text-3xl font-serif font-semibold text-brand-blue">{stats.totalResponses}</p>
                        <p className="text-sm text-gray-500 font-sans mt-1">Total Responses</p>
                    </Card>
                    <Card className="text-center">
                        <p className="text-3xl font-serif font-semibold text-brand-green">{stats.correctResponses}</p>
                        <p className="text-sm text-gray-500 font-sans mt-1">Correct Answers</p>
                    </Card>
                    <Card className="text-center">
                        <p className="text-3xl font-serif font-semibold text-text-heading">{stats.averageScore}%</p>
                        <p className="text-sm text-gray-500 font-sans mt-1">Average Score</p>
                    </Card>
                </div>
            )}

            {/* Questions with charts */}
            {!selectedMaterialId ? (
                <Card className="text-center py-12">
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-16 w-16 mx-auto text-gray-300 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                    </svg>
                    <p className="text-gray-500 font-sans">Select a quiz or poll to view student responses</p>
                </Card>
            ) : (
                <div className="space-y-6">
                    {questions.map((question, qIndex) => {
                        const chartData = getChartData(question);
                        const questionResponses = responses.filter(r => r.questionId === question.id);
                        const isExpanded = expandedQuestions.has(question.id);
                        
                        // Calculate correct count based on question type
                        const correctCount = question.type === 'WRITTEN_ANSWER'
                            ? questionResponses.filter(r => manualMarks[r.id] === true).length
                            : questionResponses.filter(r => manualMarks[r.id] ?? r.isCorrect).length;
                        const markedCount = question.type === 'WRITTEN_ANSWER'
                            ? Object.keys(manualMarks).filter(id => questionResponses.some(r => r.id === id)).length
                            : 0;

                        return (
                            <Card key={question.id}>
                                <div className="flex items-start gap-4 mb-4">
                                    <span className="flex-shrink-0 w-8 h-8 bg-brand-orange text-white rounded-full flex items-center justify-center font-sans font-semibold text-sm">
                                        {qIndex + 1}
                                    </span>
                                    <div className="flex-1">
                                        <div className="flex items-center gap-2 mb-1">
                                            {getQuestionTypeBadge(question.type)}
                                        </div>
                                        <h3 className="font-sans font-medium text-text-heading text-lg">{question.text}</h3>
                                        <p className="text-sm text-gray-500 mt-1">
                                            {questionResponses.length} responses
                                            {question.type !== 'WRITTEN_ANSWER' && ` | ${questionResponses.length > 0 ? Math.round((correctCount / questionResponses.length) * 100) : 0}% correct`}
                                            {question.type === 'WRITTEN_ANSWER' && ` | ${markedCount} marked`}
                                        </p>
                                    </div>
                                </div>

                                {/* Render based on question type */}
                                {question.type === 'WRITTEN_ANSWER' ? (
                                    /* Written Answer - show response list directly */
                                    <div className="mt-4 p-4 bg-brand-orange/5 rounded-lg border border-brand-orange/10">
                                        <div className="flex items-center justify-between mb-3">
                                            <p className="text-sm text-gray-600">
                                                Written responses require manual review. Use the checkmark/X buttons to mark responses.
                                            </p>
                                            <button
                                                onClick={handleAutoMark}
                                                className={`flex items-center gap-2 px-3 py-1.5 text-sm font-medium rounded-lg transition-colors ${
                                                    autoMarkEnabled
                                                        ? 'bg-purple-600 text-white'
                                                        : 'bg-white border border-purple-300 text-purple-600 hover:bg-purple-50'
                                                }`}
                                            >
                                                <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                                                </svg>
                                                Auto-Mark
                                            </button>
                                        </div>
                                        <div className="space-y-2 max-h-[400px] overflow-y-auto">
                                            {questionResponses.sort((a, b) => a.deviceId.localeCompare(b.deviceId)).map(response => {
                                                const isMarked = manualMarks[response.id] !== undefined;
                                                const isMarkedCorrect = manualMarks[response.id] === true;
                                                const isThisExpanded = expandedWrittenAnswer === response.id;
                                                
                                                return (
                                                    <div
                                                        key={response.id}
                                                        className={`p-3 rounded-lg border-2 transition-all ${
                                                            isMarked 
                                                                ? isMarkedCorrect 
                                                                    ? 'border-brand-green bg-brand-green/10' 
                                                                    : 'border-red-400 bg-red-50'
                                                                : 'bg-white border-gray-200 hover:border-brand-orange/30'
                                                        }`}
                                                    >
                                                        <div className="flex items-start justify-between gap-3">
                                                            <div className="flex-1">
                                                                <div className="flex items-center gap-2 mb-1">
                                                                    <span className={`text-xs font-medium ${isMarked ? (isMarkedCorrect ? 'text-brand-green' : 'text-red-600') : 'text-gray-500'}`}>
                                                                        {response.deviceId.replace('device-', 'Tablet ')}
                                                                    </span>
                                                                    <span className={`text-xs ${isMarked ? (isMarkedCorrect ? 'text-brand-green/70' : 'text-red-500/70') : 'text-gray-400'}`}>
                                                                        {formatTimestamp(response.timestamp)}
                                                                    </span>
                                                                    {isMarked && (
                                                                        <span className={`inline-flex items-center gap-1 text-xs font-medium ${isMarkedCorrect ? 'text-brand-green' : 'text-red-600'}`}>
                                                                            {isMarkedCorrect ? (
                                                                                <svg xmlns="http://www.w3.org/2000/svg" className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                                                                </svg>
                                                                            ) : (
                                                                                <svg xmlns="http://www.w3.org/2000/svg" className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                                                                </svg>
                                                                            )}
                                                                            {isMarkedCorrect ? 'Correct' : 'Incorrect'}
                                                                        </span>
                                                                    )}
                                                                </div>
                                                                <p className={`text-sm text-text-body ${!isThisExpanded ? 'line-clamp-2' : ''}`}>
                                                                    {response.answer}
                                                                </p>
                                                                {response.answer.length > 100 && (
                                                                    <button
                                                                        onClick={() => setExpandedWrittenAnswer(isThisExpanded ? null : response.id)}
                                                                        className="text-xs text-brand-orange hover:underline mt-1"
                                                                    >
                                                                        {isThisExpanded ? 'Show less' : 'Show more'}
                                                                    </button>
                                                                )}
                                                            </div>
                                                            <div className="flex gap-1">
                                                                <button
                                                                    onClick={() => setMark(response.id, true)}
                                                                    className={`p-1.5 rounded transition-colors ${
                                                                        isMarkedCorrect 
                                                                            ? 'bg-brand-green text-white' 
                                                                            : 'bg-gray-100 text-gray-400 hover:bg-brand-green/10 hover:text-brand-green'
                                                                    }`}
                                                                    title="Mark Correct"
                                                                >
                                                                    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                                                    </svg>
                                                                </button>
                                                                <button
                                                                    onClick={() => setMark(response.id, false)}
                                                                    className={`p-1.5 rounded transition-colors ${
                                                                        isMarked && !isMarkedCorrect 
                                                                            ? 'bg-red-500 text-white' 
                                                                            : 'bg-gray-100 text-gray-400 hover:bg-red-100 hover:text-red-500'
                                                                    }`}
                                                                    title="Mark Incorrect"
                                                                >
                                                                    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                                                    </svg>
                                                                </button>
                                                            </div>
                                                        </div>
                                                    </div>
                                                );
                                            })}
                                        </div>
                                    </div>
                                ) : (
                                    /* Multiple Choice or True/False - show bar chart */
                                    <>
                                        <div className={question.type === 'TRUE_FALSE' ? 'h-32 mt-4' : 'h-48 mt-4'}>
                                            <ResponsiveContainer width="100%" height="100%">
                                                <BarChart data={chartData} layout="vertical">
                                                    <XAxis type="number" />
                                                    <YAxis 
                                                        dataKey="name" 
                                                        type="category" 
                                                        width={question.type === 'TRUE_FALSE' ? 50 : 30} 
                                                    />
                                                    <Tooltip 
                                                        content={({ active, payload }) => {
                                                            if (active && payload && payload.length) {
                                                                const data = payload[0].payload;
                                                                return (
                                                                    <div className="bg-white shadow-lg rounded p-3 border">
                                                                        <p className="font-medium text-sm">{data.fullText}</p>
                                                                        <p className="text-gray-600 text-sm">{data.count} responses</p>
                                                                        {data.isCorrect && (
                                                                            <p className="text-brand-green text-xs mt-1 font-medium">Correct Answer</p>
                                                                        )}
                                                                    </div>
                                                                );
                                                            }
                                                            return null;
                                                        }}
                                                    />
                                                    <Bar dataKey="count" radius={[0, 4, 4, 0]}>
                                                        {chartData.map((entry, index) => (
                                                            <Cell 
                                                                key={`cell-${index}`} 
                                                                fill={entry.isCorrect ? '#1E6F5C' : '#E5E7EB'} 
                                                            />
                                                        ))}
                                                    </Bar>
                                                </BarChart>
                                            </ResponsiveContainer>
                                        </div>

                                        {/* Correct answer indicator */}
                                        <div className="mt-4 p-3 bg-brand-green/10 rounded-lg border border-brand-green/20">
                                            <p className="text-sm font-sans">
                                                <span className="font-medium text-brand-green">Correct Answer: </span>
                                                <span className="text-text-body">
                                                    {question.type === 'TRUE_FALSE' 
                                                        ? question.options?.[parseInt(question.correctAnswer || '0')]
                                                        : `${String.fromCharCode(65 + parseInt(question.correctAnswer || '0'))}. ${question.options?.[parseInt(question.correctAnswer || '0')]}`
                                                    }
                                                </span>
                                            </p>
                                        </div>

                                        {/* Expandable individual responses */}
                                        <div className="mt-4 border-t border-gray-100 pt-4">
                                            <button
                                                onClick={() => toggleQuestion(question.id)}
                                                className="flex items-center gap-2 text-sm font-medium text-brand-orange hover:text-brand-orange-dark transition-colors"
                                            >
                                                <svg 
                                                    xmlns="http://www.w3.org/2000/svg" 
                                                    className={`h-4 w-4 transition-transform ${isExpanded ? 'rotate-90' : ''}`} 
                                                    fill="none" 
                                                    viewBox="0 0 24 24" 
                                                    stroke="currentColor"
                                                >
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                                                </svg>
                                                {isExpanded ? 'Hide' : 'View'} Individual Responses ({questionResponses.length})
                                            </button>

                                            {isExpanded && (
                                                <div className="mt-4 overflow-x-auto">
                                                    <table className="w-full text-sm">
                                                        <thead>
                                                            <tr className="border-b border-gray-200">
                                                                <th className="text-left py-2 px-3 font-sans font-medium text-gray-600">Tablet</th>
                                                                <th className="text-left py-2 px-3 font-sans font-medium text-gray-600">Answer</th>
                                                                <th className="text-left py-2 px-3 font-sans font-medium text-gray-600">Status</th>
                                                                <th className="text-left py-2 px-3 font-sans font-medium text-gray-600">Time</th>
                                                                <th className="text-left py-2 px-3 font-sans font-medium text-gray-600">Mark</th>
                                                            </tr>
                                                        </thead>
                                                        <tbody>
                                                            {questionResponses.sort((a, b) => a.deviceId.localeCompare(b.deviceId)).map(response => {
                                                                const answerIndex = parseInt(response.answer);
                                                                const answerText = question.options?.[answerIndex] || response.answer;
                                                                const isMarkedCorrect = manualMarks[response.id] ?? response.isCorrect;

                                                                return (
                                                                    <tr key={response.id} className={`border-b transition-all ${
                                                                        isMarkedCorrect 
                                                                            ? 'bg-brand-green/10 border-brand-green/20' 
                                                                            : 'bg-red-50 border-red-100'
                                                                    }`}>
                                                                        <td className="py-2 px-3 font-sans text-text-body">
                                                                            {response.deviceId.replace('device-', 'Tablet ')}
                                                                        </td>
                                                                        <td className="py-2 px-3 font-sans text-text-body">
                                                                            {question.type === 'TRUE_FALSE' ? (
                                                                                <span className="font-medium">{answerText}</span>
                                                                            ) : (
                                                                                <>
                                                                                    <span className="font-medium">{String.fromCharCode(65 + answerIndex)}.</span>
                                                                                    <span className="text-gray-500 ml-1 truncate inline-block max-w-[150px]" title={answerText}>
                                                                                        {answerText.length > 25 ? answerText.substring(0, 25) + '...' : answerText}
                                                                                    </span>
                                                                                </>
                                                                            )}
                                                                        </td>
                                                                        <td className="py-2 px-3">
                                                                            {isMarkedCorrect ? (
                                                                                <span className="inline-flex items-center gap-1 text-brand-green font-medium">
                                                                                    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                                                                    </svg>
                                                                                    Correct
                                                                                </span>
                                                                            ) : (
                                                                                <span className="inline-flex items-center gap-1 text-red-500 font-medium">
                                                                                    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                                                                    </svg>
                                                                                    Wrong
                                                                                </span>
                                                                            )}
                                                                        </td>
                                                                        <td className="py-2 px-3 font-sans text-gray-500">
                                                                            {formatTimestamp(response.timestamp)}
                                                                        </td>
                                                                        <td className="py-2 px-3">
                                                                            <div className="flex gap-1">
                                                                                <button
                                                                                    onClick={() => setMark(response.id, true)}
                                                                                    className={`p-1.5 rounded transition-colors ${
                                                                                        isMarkedCorrect 
                                                                                            ? 'bg-brand-green text-white' 
                                                                                            : 'bg-gray-100 text-gray-400 hover:bg-brand-green/10 hover:text-brand-green'
                                                                                    }`}
                                                                                    title="Mark Correct"
                                                                                >
                                                                                    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                                                                    </svg>
                                                                                </button>
                                                                                <button
                                                                                    onClick={() => setMark(response.id, false)}
                                                                                    className={`p-1.5 rounded transition-colors ${
                                                                                        manualMarks[response.id] === false
                                                                                            ? 'bg-red-500 text-white' 
                                                                                            : 'bg-gray-100 text-gray-400 hover:bg-red-100 hover:text-red-500'
                                                                                    }`}
                                                                                    title="Mark Wrong"
                                                                                >
                                                                                    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                                                                    </svg>
                                                                                </button>
                                                                            </div>
                                                                        </td>
                                                                    </tr>
                                                                );
                                                            })}
                                                        </tbody>
                                                    </table>
                                                </div>
                                            )}
                                        </div>
                                    </>
                                )}
                            </Card>
                        );
                    })}
                </div>
            )}
        </div>
    );
};
