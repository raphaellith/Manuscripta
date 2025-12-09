import React from 'react';
import { LESSON_TITLE, LESSON_CONTENT } from '../constants';
import { AITask } from '../types';
import AudioButton from './common/AudioButton';
import Button from './common/Button';

interface LessonViewProps {
  showAIContent?: boolean;
  aiContent?: string;
  aiTask?: AITask | null;
  isLoading?: boolean;
  onBackFromAI?: () => void;
}

const LoadingIndicator: React.FC = () => (
  <div className="flex justify-center items-center h-full">
    <p className="text-3xl text-eink-dark font-medium">Loading...</p>
  </div>
);

const LessonView: React.FC<LessonViewProps> = ({
  showAIContent = false,
  aiContent = '',
  aiTask = null,
  isLoading = false,
  onBackFromAI,
}) => {
  const playAudio = () => {
    console.log('🔊 Reading lesson content aloud...');
    // In a real app, you would use SpeechSynthesisUtterance here
  };

  // Show AI content overlay when active
  if (showAIContent) {
    return (
      <div className="flex flex-col h-full">
        <h1 className="text-4xl font-serif font-medium text-eink-black mb-6 pb-4 border-b-4 border-eink-black">
          {aiTask || 'AI Assistant'}
        </h1>
        <div className="bg-eink-light border-4 border-eink-black p-6 overflow-y-auto flex-1">
          {isLoading ? (
            <LoadingIndicator />
          ) : (
            <p className="text-3xl whitespace-pre-wrap leading-relaxed text-eink-black">
              {aiContent}
            </p>
          )}
        </div>
        <div className="mt-4">
          <Button onClick={onBackFromAI}>◄ Back</Button>
        </div>
      </div>
    );
  }

  // Default lesson content
  return (
    <div className="flex flex-col h-full">
      <h1 className="text-4xl font-serif font-medium text-eink-black mb-10 pb-4 border-b-4 border-eink-black">
        {LESSON_TITLE}
      </h1>
      <div className="space-y-8 overflow-y-auto">
        {LESSON_CONTENT.map((paragraph, index) => (
          <p key={index} className="text-3xl leading-relaxed text-eink-black">
            {paragraph}
          </p>
        ))}
      </div>
    </div>
  );
};

export default LessonView;