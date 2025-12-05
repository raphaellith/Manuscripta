
import React, { useState, useCallback } from 'react';
import { View, AITask } from './types';
import Tablet from './components/Tablet';
import LessonView from './components/LessonView';
import QuizView from './components/QuizView';
import WorksheetView from './components/WorksheetView';
import AIAssistView from './components/AIAssistView';
import FeedbackView from './components/FeedbackView';
import { generateContent } from './services/geminiService';

const App: React.FC = () => {
  const [currentView, setCurrentView] = useState<View>(View.Lesson);
  const [previousView, setPreviousView] = useState<View>(View.Lesson);
  const [aiTask, setAiTask] = useState<AITask | null>(null);
  const [aiContent, setAiContent] = useState<string>('');
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [selectedAnswer, setSelectedAnswer] = useState<number | null>(null);

  const handleReset = () => {
    setCurrentView(View.Lesson);
    setSelectedAnswer(null);
  };

  const handleAIAssist = useCallback(async (task: AITask) => {
    setPreviousView(currentView);
    setAiTask(task);
    setIsLoading(true);
    setCurrentView(View.AIAssist);
    const content = await generateContent(task);
    setAiContent(content);
    setIsLoading(false);
  }, [currentView]);

  const handleBackFromAIAssist = () => {
    setCurrentView(previousView);
    setAiTask(null);
    setAiContent('');
  };

  const handleQuizSubmit = (isCorrect: boolean) => {
    if (isCorrect) {
      setCurrentView(View.FeedbackCorrect);
    } else {
      setCurrentView(View.FeedbackIncorrect);
    }
    setSelectedAnswer(null);
  };

  const renderView = () => {
    switch (currentView) {
      case View.Lesson:
        return <LessonView />;
      case View.Quiz:
        return (
          <QuizView
            onBackToLesson={() => setCurrentView(View.Lesson)}
            onSubmit={handleQuizSubmit}
            selectedAnswer={selectedAnswer}
            setSelectedAnswer={setSelectedAnswer}
          />
        );
      case View.Worksheet:
        return <WorksheetView />;
      case View.AIAssist:
        return (
          <AIAssistView
            task={aiTask}
            content={aiContent}
            isLoading={isLoading}
            onBack={handleBackFromAIAssist}
          />
        );
      case View.FeedbackCorrect:
        return <FeedbackView isCorrect={true} onNext={handleReset} />;
      case View.FeedbackIncorrect:
        return <FeedbackView isCorrect={false} onNext={() => setCurrentView(View.Quiz)} />;
      default:
        return <LessonView />;
    }
  };

  return (
    <div className="flex justify-center items-center min-h-screen p-5 font-sans">
      <Tablet>
        <div className="flex flex-col h-full">
            <nav className="flex-shrink-0 mb-4">
                <div className="flex border-b-4 border-black">
                    <TabButton title="Lesson" isActive={currentView === View.Lesson} onClick={() => setCurrentView(View.Lesson)} />
                    <TabButton title="Quiz" isActive={currentView === View.Quiz} onClick={() => setCurrentView(View.Quiz)} />
                    <TabButton title="Worksheet" isActive={currentView === View.Worksheet} onClick={() => setCurrentView(View.Worksheet)} />
                </div>
            </nav>
            <main className="flex-grow flex flex-col min-h-0">
                {renderView()}
            </main>
            <footer className="flex-shrink-0 mt-auto pt-4 space-y-2">
                 {currentView === View.Lesson && <AIAssistToolbar onAIAssist={handleAIAssist} />}
            </footer>
        </div>
      </Tablet>
    </div>
  );
};

interface TabButtonProps {
    title: string;
    isActive: boolean;
    onClick: () => void;
}

const TabButton: React.FC<TabButtonProps> = ({ title, isActive, onClick }) => {
    const activeClasses = 'bg-[#e8e6e0] text-black underline';
    const inactiveClasses = 'bg-[#d8d6d0] text-black hover:bg-[#c8c6c0]';
    return (
        <button 
            onClick={onClick}
            className={`flex-1 py-3 text-2xl font-bold border-4 border-b-0 border-black transition-colors ${isActive ? activeClasses : inactiveClasses}`}
        >
            {title}
        </button>
    );
};


interface AIAssistToolbarProps {
    onAIAssist: (task: AITask) => void;
}
const AIAssistToolbar: React.FC<AIAssistToolbarProps> = ({ onAIAssist }) => (
    <div className="bg-[#e8e6e0] border-4 border-black p-3 shadow-sm">
        <div className="grid grid-cols-3 gap-3">
            <AIAssistButton onClick={() => onAIAssist(AITask.Simplify)}>
                {AITask.Simplify}
            </AIAssistButton>
            <AIAssistButton onClick={() => onAIAssist(AITask.Expand)}>
                {AITask.Expand}
            </AIAssistButton>
            <AIAssistButton onClick={() => onAIAssist(AITask.Summarise)}>
                {AITask.Summarise}
            </AIAssistButton>
        </div>
    </div>
);

interface AIAssistButtonProps {
    onClick: () => void;
    children: React.ReactNode;
}
const AIAssistButton: React.FC<AIAssistButtonProps> = ({ onClick, children }) => (
    <button 
        onClick={onClick}
        className="px-4 py-2 bg-[#e8e6e0] border-2 border-black text-black text-lg font-semibold hover:bg-[#d8d6d0] active:bg-[#c8c6c0] transition-all shadow-sm w-full text-center"
    >
        {children}
    </button>
);


export default App;
