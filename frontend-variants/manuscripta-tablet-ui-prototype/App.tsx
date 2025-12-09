
import React, { useState, useCallback } from 'react';
import { View, AITask } from './types';
import Tablet from './components/Tablet';
import LessonView from './components/LessonView';
import QuizView from './components/QuizView';
import WorksheetView from './components/WorksheetView';
import FeedbackView from './components/FeedbackView';
import CharacterHelper from './components/CharacterHelper';
import AudioButton from './components/common/AudioButton';
import { generateContent } from './services/geminiService';

const App: React.FC = () => {
  const [currentView, setCurrentView] = useState<View>(View.Lesson);
  const [aiTask, setAiTask] = useState<AITask | null>(null);
  const [aiContent, setAiContent] = useState<string>('');
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [showAIContent, setShowAIContent] = useState<boolean>(false);
  const [selectedAnswer, setSelectedAnswer] = useState<number | null>(null);

  const handleReset = () => {
    setCurrentView(View.Lesson);
    setSelectedAnswer(null);
  };

  const handleAIAssist = useCallback(async (task: AITask) => {
    setAiTask(task);
    setIsLoading(true);
    setShowAIContent(true);
    const content = await generateContent(task);
    setAiContent(content);
    setIsLoading(false);
  }, []);

  const handleBackFromAIContent = () => {
    setShowAIContent(false);
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
        return (
          <LessonView
            showAIContent={showAIContent}
            aiContent={aiContent}
            aiTask={aiTask}
            isLoading={isLoading}
            onBackFromAI={handleBackFromAIContent}
          />
        );
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
                <div className="flex border-b-4 border-eink-black">
                    <TabButton title="Reading" isActive={currentView === View.Lesson} onClick={() => setCurrentView(View.Lesson)} />
                    <TabButton title="Quiz" isActive={currentView === View.Quiz} onClick={() => setCurrentView(View.Quiz)} />
                    <TabButton title="Worksheet" isActive={currentView === View.Worksheet} onClick={() => setCurrentView(View.Worksheet)} />
                </div>
            </nav>
            <main className="flex-grow flex flex-col min-h-0">
                {renderView()}
            </main>
            <footer className="flex-shrink-0 mt-auto pt-4">
                    <div className="flex justify-between items-end">
                        <div>
                            <CharacterHelper 
                              onAIAssist={handleAIAssist} 
                              isVisible={currentView === View.Lesson && !showAIContent} 
                            />
                        </div>
                        <div>
                            {(() => {
                              if (showAIContent) return null;
                              
                              let audioProps = null;
                              switch (currentView) {
                                case View.Lesson:
                                  audioProps = { onClick: () => console.log('🔊 Reading lesson content aloud...'), title: "Read lesson aloud" };
                                  break;
                                case View.Quiz:
                                  audioProps = { onClick: () => console.log('🔊 Reading question aloud...'), title: "Read question aloud" };
                                  break;
                                case View.Worksheet:
                                  audioProps = { onClick: () => console.log('🔊 Reading worksheet aloud...'), title: "Read worksheet aloud" };
                                  break;
                              }
                              
                              return audioProps ? <AudioButton {...audioProps} /> : null;
                            })()}
                        </div>
                    </div>
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
    return (
        <button 
            onClick={onClick}
            className={`flex-1 py-3 text-2xl font-bold border-4 border-b-0 border-eink-black text-eink-black ${isActive ? 'bg-eink-light underline' : 'bg-eink-cream'}`}
        >
            {title}
        </button>
    );
};

export default App;
