/**
 * Main App component.
 * Per WindowsAppStructureSpec §2B(1)(d).
 * 
 * This version combines the production LessonLibraryPage with prototype pages
 * for views that haven't been fully implemented yet.
 */

import React, { useState } from 'react';
import { AppProvider, useAppContext } from './state/AppContext';
import { Header } from './components/layout/Header';
import { LessonLibraryPage } from './components/pages/LessonLibraryPage';

// Import prototype pages for unimplemented views (disposable prototype)
import { ClassDashboard } from '../components/ClassDashboard';
import { ClassroomControl } from '../components/ClassroomControl';
import { ResponsesView } from '../components/ResponsesView';
import { AiAssistant } from '../components/AiAssistant';
import { Settings } from '../components/Settings';

// Import prototype types for mock data
import type { Unit, LessonFolder, ContentItem } from '../types';

// Mock data for prototype components
const mockUnits: Unit[] = [
    { id: 'u1', title: 'Norman Conquest', subject: 'History', ageRange: '11-12', description: 'The Battle of Hastings and Norman invasion' },
    { id: 'u2', title: 'Medieval Life', subject: 'History', ageRange: '11-12', description: 'Daily life in medieval England' },
];

const mockLessonFolders: LessonFolder[] = [
    { id: 'f1', unit: 'Norman Conquest', number: 1, title: 'The Battle of Hastings' },
    { id: 'f2', unit: 'Norman Conquest', number: 2, title: 'William the Conqueror' },
    { id: 'f3', unit: 'Medieval Life', number: 1, title: 'Lords and Peasants' },
];

const mockContentItems: ContentItem[] = [
    { id: '1a', title: 'The Battle of Hastings', subject: 'History', created: '2025-12-01', status: 'Deployed', unit: 'Norman Conquest', type: 'Reading', lessonNumber: 1 },
    { id: '1b', title: 'Battle of Hastings Quiz', subject: 'History', created: '2025-12-01', status: 'Deployed', unit: 'Norman Conquest', type: 'Quiz', lessonNumber: 1 },
    { id: '2a', title: 'William the Conqueror', subject: 'History', created: '2025-12-02', status: 'Deployed', unit: 'Norman Conquest', type: 'Reading', lessonNumber: 2 },
    { id: '3a', title: 'Lords and Peasants', subject: 'History', created: '2025-12-03', status: 'Draft', unit: 'Medieval Life', type: 'Reading', lessonNumber: 1 },
];

type View = 'dashboard' | 'lesson-library' | 'classroom-control' | 'responses' | 'ai-assistant' | 'settings';

const AppContent: React.FC = () => {
    const { isLoading, error, isConnected, refreshData } = useAppContext();
    const [activeView, setActiveView] = useState<View>('lesson-library');

    const renderView = () => {
        if (isLoading) {
            return (
                <div className="flex items-center justify-center h-64">
                    <div className="text-center">
                        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-brand-orange mx-auto mb-4"></div>
                        <p className="text-text-body">Loading your library...</p>
                    </div>
                </div>
            );
        }

        if (error) {
            return (
                <div className="flex items-center justify-center h-64">
                    <div className="text-center">
                        <p className="text-red-600 mb-4">{error}</p>
                        <button
                            onClick={refreshData}
                            className="px-6 py-2 bg-brand-orange text-white rounded-lg hover:bg-brand-orange-dark transition-colors"
                        >
                            Retry
                        </button>
                    </div>
                </div>
            );
        }

        switch (activeView) {
            case 'lesson-library':
                return <LessonLibraryPage />;
            case 'dashboard':
                return <ClassDashboard />;
            case 'classroom-control':
                return <ClassroomControl units={mockUnits} lessonFolders={mockLessonFolders} contentItems={mockContentItems} />;
            case 'responses':
                return <ResponsesView contentItems={mockContentItems} />;
            case 'ai-assistant':
                return <AiAssistant />;
            case 'settings':
                return <Settings />;
            default:
                return (
                    <div className="text-center py-12">
                        <p className="text-text-body">View "{activeView}" is not yet implemented.</p>
                    </div>
                );
        }
    };

    return (
        <div className="h-screen bg-brand-cream text-text-body font-sans selection:bg-brand-orange-light selection:text-brand-orange relative overflow-hidden">
            {/* Connection status indicator */}
            <div className={`absolute top-2 right-2 z-50 px-2 py-1 rounded text-xs ${isConnected ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                {isConnected ? '● Connected' : '○ Disconnected'}
            </div>

            {/* Floating Header Wrapper */}
            <div className="absolute top-0 left-0 w-full z-40 pointer-events-none">
                <Header activeView={activeView} setActiveView={setActiveView} />
            </div>

            <main className="h-full overflow-y-auto bg-brand-cream scroll-smooth pt-28">
                <div className="max-w-7xl mx-auto p-8 w-full">
                    {/* Decorative Backgrounds */}
                    <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-brand-yellow rounded-full blur-3xl opacity-20 pointer-events-none -z-0 translate-x-1/3 -translate-y-1/3 mix-blend-multiply"></div>
                    <div className="absolute bottom-0 left-0 w-[600px] h-[600px] bg-brand-blue rounded-full blur-3xl opacity-20 pointer-events-none -z-0 -translate-x-1/3 translate-y-1/3 mix-blend-multiply"></div>

                    <div className="relative z-10">
                        {renderView()}
                    </div>
                </div>
            </main>
        </div>
    );
};

const App: React.FC = () => {
    return (
        <AppProvider>
            <AppContent />
        </AppProvider>
    );
};

export default App;
