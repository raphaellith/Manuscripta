import React from 'react';
import { AITask } from '../types';
import Button from './common/Button';

interface AIAssistViewProps {
  task: AITask | null;
  content: string;
  isLoading: boolean;
  onBack: () => void;
}

const LoadingSpinner: React.FC = () => (
    <div className="flex justify-center items-center h-full">
        <div className="animate-spin rounded-full h-16 w-16 border-b-4 border-black"></div>
    </div>
);

const AIAssistView: React.FC<AIAssistViewProps> = ({ task, content, isLoading, onBack }) => {
  return (
    <div className="flex flex-col h-full">
      <h1 className="text-4xl font-bold text-black mb-6 pb-4 border-b-4 border-black">
        {task || 'AI Assistant'}
      </h1>
      <div className="bg-[#e8e6e0] border-4 border-black p-6 shadow-md overflow-y-auto">
        {isLoading ? (
          <LoadingSpinner />
        ) : (
          <p className="text-3xl whitespace-pre-wrap leading-relaxed text-black">
            {content}
          </p>
        )}
      </div>
      <div className="mt-auto">
        <Button onClick={onBack}>◄ Back</Button>
      </div>
    </div>
  );
};

export default AIAssistView;