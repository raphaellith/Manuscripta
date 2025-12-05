import React from 'react';
import { AITask } from '../types';
import Button from './common/Button';

interface AIAssistViewProps {
  task: AITask | null;
  content: string;
  isLoading: boolean;
  onBack: () => void;
}

const LoadingIndicator: React.FC = () => (
    <div className="flex justify-center items-center h-full">
        <p className="text-3xl text-eink-dark font-medium">Loading...</p>
    </div>
);

const AIAssistView: React.FC<AIAssistViewProps> = ({ task, content, isLoading, onBack }) => {
  return (
    <div className="flex flex-col h-full">
      <h1 className="text-4xl font-serif font-medium text-eink-black mb-6 pb-4 border-b-4 border-eink-black">
        {task || 'AI Assistant'}
      </h1>
      <div className="bg-eink-light border-4 border-eink-black p-6 overflow-y-auto flex-1">
        {isLoading ? (
          <LoadingIndicator />
        ) : (
          <p className="text-3xl whitespace-pre-wrap leading-relaxed text-eink-black">
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