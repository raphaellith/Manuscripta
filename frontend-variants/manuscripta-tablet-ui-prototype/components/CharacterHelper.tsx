import React, { useState } from 'react';
import { AITask } from '../types';
import characterImage from '../resources/char_swirlok.png';
import { CONTENT_TEXT_SIZE } from '../constants';

interface CharacterHelperProps {
  onAIAssist: (task: AITask) => void;
  isVisible: boolean;
  aiContent?: string;
  aiTask?: AITask | null;
  isLoading?: boolean;
  onClose: () => void;
}

const CharacterHelper: React.FC<CharacterHelperProps> = ({ 
  onAIAssist, 
  isVisible,
  aiContent,
  aiTask,
  isLoading,
  onClose
}) => {
  const [isDialogueOpen, setIsDialogueOpen] = useState(false);
  const [isHovered, setIsHovered] = useState(false);

  // If we have content or are loading, strictly show the dialogue
  const showResponse = !!aiContent || isLoading;
  
  if (!isVisible) return null;

  const handleCharacterClick = () => {
    // If showing a response, clicking character toggles visibility of the whole bubble
    // or we might want to just close it? Let's just toggle 'isDialogueOpen' if it's the menu.
    // However, if we are in "response mode", we should probably just let the close button handle it,
    // or clicking the character resets/closes it.
    if (showResponse) {
       onClose();
    } else {
       setIsDialogueOpen(!isDialogueOpen);
    }
  };

  const handleTaskClick = (task: AITask) => {
    setIsDialogueOpen(false); // Close the menu, state will switch to loading via props
    onAIAssist(task);
  };

  return (
    <>
      {/* Response Box - positioned relative to footer, not character */}
      {showResponse && (
        <div className="absolute bottom-full left-0 right-0 mb-4 bg-eink-light border-4 border-eink-black p-8 max-h-[70vh] overflow-y-auto z-20 shadow-2xl flex flex-col">
            <div className="flex justify-between items-center mb-6 border-b-4 border-eink-black pb-3">
                <h3 className={`${CONTENT_TEXT_SIZE} font-bold text-eink-black`}>{aiTask || 'Thinking...'}</h3>
                <button 
                    onClick={onClose}
                    className="text-3xl font-bold text-eink-black hover:text-gray-600 px-2 leading-none"
                >
                    ✕
                </button>
            </div>
            
            {isLoading ? (
                <div className="flex justify-center items-center py-12">
                    <p className={`${CONTENT_TEXT_SIZE} text-eink-black animate-pulse`}>Thinking...</p>
                </div>
            ) : (
                <p className={`${CONTENT_TEXT_SIZE} leading-relaxed text-eink-black whitespace-pre-wrap`}>
                    {aiContent}
                </p>
            )}
        </div>
      )}

      {/* Character and Menu */}
      <div className="flex items-end gap-4 relative">
        {/* Character */}
        <button
          onClick={handleCharacterClick}
          onMouseEnter={() => setIsHovered(true)}
          onMouseLeave={() => setIsHovered(false)}
          className="focus:outline-none transition-transform duration-200 z-10"
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

        {/* Dialogue Box (Menu) */}
        {isDialogueOpen && !showResponse && (
          <div className="absolute bottom-full left-28 mb-4 bg-eink-light border-4 border-eink-black p-4 flex flex-col gap-3 min-w-[200px] z-20 shadow-lg">
             {/* Speech bubble tail could be added here with pseudo-elements if desired, but simple box matches style */}
            <p className="text-xl font-bold text-eink-black text-center">How can I help?</p>
            <div className="flex gap-2 justify-center">
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
    </>
  );
};

export default CharacterHelper;
