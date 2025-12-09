import React, { useState } from 'react';
import { AITask } from '../types';
import characterImage from '../resources/char_swirlok.png';

interface CharacterHelperProps {
  onAIAssist: (task: AITask) => void;
  isVisible: boolean;
}

const CharacterHelper: React.FC<CharacterHelperProps> = ({ onAIAssist, isVisible }) => {
  const [isDialogueOpen, setIsDialogueOpen] = useState(false);
  const [isHovered, setIsHovered] = useState(false);

  if (!isVisible) return null;

  const handleCharacterClick = () => {
    setIsDialogueOpen(!isDialogueOpen);
  };

  const handleTaskClick = (task: AITask) => {
    setIsDialogueOpen(false);
    onAIAssist(task);
  };

  return (
    <div className="flex items-end gap-4">
      {/* Character */}
      <button
        onClick={handleCharacterClick}
        onMouseEnter={() => setIsHovered(true)}
        onMouseLeave={() => setIsHovered(false)}
        className="focus:outline-none transition-transform duration-200"
        style={{
          transform: isHovered ? 'scale(1.1) translateY(-4px)' : 'scale(1)',
        }}
        aria-label="Open AI assistant dialogue"
      >
        <img
          src={characterImage}
          alt="Loopa the AI assistant"
          className="w-24 h-24 object-contain"
        />
      </button>

      {/* Dialogue Box */}
      {isDialogueOpen && (
        <div className="bg-eink-light border-4 border-eink-black p-4 flex flex-col gap-3">
          <p className="text-xl font-bold text-eink-black text-center">How can I help?</p>
          <div className="flex gap-2">
            <button
              onClick={() => handleTaskClick(AITask.Simplify)}
              className="px-4 py-2 bg-eink-cream border-2 border-eink-black text-eink-black text-lg font-bold text-center hover:bg-eink-light transition-colors"
            >
              Simplify
            </button>
            <button
              onClick={() => handleTaskClick(AITask.Summarise)}
              className="px-4 py-2 bg-eink-cream border-2 border-eink-black text-eink-black text-lg font-bold text-center hover:bg-eink-light transition-colors"
            >
              Summarise
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default CharacterHelper;
