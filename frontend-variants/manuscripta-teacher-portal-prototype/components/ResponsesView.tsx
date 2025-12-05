
import React, { useState, useMemo } from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { Card } from './Card';
import type { ContentItem, Question, StudentResponse } from '../types';

interface ResponsesViewProps {
    contentItems: ContentItem[];
}

// Mock questions for quizzes
const mockQuestions: Question[] = [
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
    {
        id: 'q3',
        materialId: '1b',
        text: 'How was King Harold Godwinson famously depicted to have died in the Bayeux Tapestry?',
        type: 'MULTIPLE_CHOICE',
        options: ['From a sword wound', 'Falling from his horse', 'With an arrow to the eye'],
        correctAnswer: '2'
    },
];

// Generate mock responses from 28 devices
const generateMockResponses = (): StudentResponse[] => {
    const responses: StudentResponse[] = [];
    const correctAnswers: Record<string, string> = { q1: '1', q2: '1', q3: '2' };
    
    for (let deviceNum = 1; deviceNum <= 28; deviceNum++) {
        for (const question of mockQuestions) {
            // Simulate realistic response distribution
            let answer: string;
            const random = Math.random();
            
            if (random < 0.75) {
                // 75% get it right
                answer = correctAnswers[question.id];
            } else {
                // 25% get it wrong - pick a random wrong answer
                const options = question.options || [];
                const wrongAnswers = options.map((_, i) => String(i)).filter(i => i !== correctAnswers[question.id]);
                answer = wrongAnswers[Math.floor(Math.random() * wrongAnswers.length)];
            }
            
            responses.push({
                id: `r-${question.id}-${deviceNum}`,
                questionId: question.id,
                answer,
                isCorrect: answer === correctAnswers[question.id],
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
        const correct = responses.filter(r => manualMarks[r.id] ?? r.isCorrect).length;
        
        return {
            totalDevices: deviceIds.size,
            totalResponses: responses.length,
            correctResponses: correct,
            averageScore: Math.round((correct / responses.length) * 100),
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

    const toggleMark = (responseId: string, currentValue: boolean) => {
        setManualMarks(prev => ({
            ...prev,
            [responseId]: !currentValue,
        }));
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
                name: String.fromCharCode(65 + index), // A, B, C, etc.
                count,
                fullText: option,
                isCorrect: String(index) === question.correctAnswer,
            };
        });
    };

    const formatTimestamp = (ts: number) => {
        return new Date(ts).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
    };

    return (
        <div>
            {/* Header with material selector and export */}
            <Card className="mb-6">
                <div className="flex flex-wrap items-center justify-between gap-4">
                    <div className="flex-1 min-w-[200px]">
                        <label className="block font-sans font-medium text-text-heading text-sm mb-2">
                            Select Quiz or Poll
                        </label>
                        <select
                            value={selectedMaterialId}
                            onChange={(e) => setSelectedMaterialId(e.target.value)}
                            className="w-full p-3 bg-brand-gray text-text-body font-sans rounded-lg border-2 border-transparent focus:border-brand-orange focus:outline-none"
                        >
                            <option value="">-- Select Material --</option>
                            {quizzes.map(quiz => (
                                <option key={quiz.id} value={quiz.id}>
                                    {quiz.title} ({quiz.unit})
                                </option>
                            ))}
                        </select>
                    </div>
                    <button
                        onClick={handleExport}
                        disabled={!selectedMaterialId}
                        className="px-6 py-3 bg-brand-green text-white font-sans font-medium rounded-md hover:bg-green-800 transition-colors shadow-sm disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                        </svg>
                        Export
                    </button>
                </div>
            </Card>

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
                        const correctCount = questionResponses.filter(r => manualMarks[r.id] ?? r.isCorrect).length;
                        const correctPercent = questionResponses.length > 0 
                            ? Math.round((correctCount / questionResponses.length) * 100) 
                            : 0;

                        return (
                            <Card key={question.id}>
                                <div className="flex items-start gap-4 mb-4">
                                    <span className="flex-shrink-0 w-8 h-8 bg-brand-orange text-white rounded-full flex items-center justify-center font-sans font-semibold text-sm">
                                        {qIndex + 1}
                                    </span>
                                    <div className="flex-1">
                                        <h3 className="font-sans font-medium text-text-heading text-lg">{question.text}</h3>
                                        <p className="text-sm text-gray-500 mt-1">
                                            {questionResponses.length} responses | {correctPercent}% correct
                                        </p>
                                    </div>
                                </div>

                                {/* Bar chart */}
                                <div className="h-48 mt-4">
                                    <ResponsiveContainer width="100%" height="100%">
                                        <BarChart data={chartData} layout="vertical">
                                            <XAxis type="number" />
                                            <YAxis dataKey="name" type="category" width={30} />
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
                                            {String.fromCharCode(65 + parseInt(question.correctAnswer || '0'))}. {question.options?.[parseInt(question.correctAnswer || '0')]}
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
                                                            <tr key={response.id} className="border-b border-gray-50 hover:bg-gray-50">
                                                                <td className="py-2 px-3 font-sans text-text-body">
                                                                    {response.deviceId.replace('device-', 'Tablet ')}
                                                                </td>
                                                                <td className="py-2 px-3 font-sans text-text-body">
                                                                    <span className="font-medium">{String.fromCharCode(65 + answerIndex)}.</span>
                                                                    <span className="text-gray-500 ml-1 truncate inline-block max-w-[150px]" title={answerText}>
                                                                        {answerText.length > 25 ? answerText.substring(0, 25) + '...' : answerText}
                                                                    </span>
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
                                                                    <button
                                                                        onClick={() => toggleMark(response.id, isMarkedCorrect)}
                                                                        className="text-xs px-2 py-1 rounded border border-gray-200 hover:border-brand-orange hover:text-brand-orange transition-colors"
                                                                    >
                                                                        {isMarkedCorrect ? 'Mark Wrong' : 'Mark Correct'}
                                                                    </button>
                                                                </td>
                                                            </tr>
                                                        );
                                                    })}
                                                </tbody>
                                            </table>
                                        </div>
                                    )}
                                </div>
                            </Card>
                        );
                    })}
                </div>
            )}
        </div>
    );
};
