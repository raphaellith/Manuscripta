/**
 * Main App component.
 * Per WindowsAppStructureSpec §2B(1)(d).
 */

import React, { useState } from 'react';
import { AppProvider, useAppContext } from './state/AppContext';
import { Header } from './components/layout/Header';
import { LessonLibraryPage } from './components/pages/LessonLibraryPage';

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
