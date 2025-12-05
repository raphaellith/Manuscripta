import React from 'react';
import { LESSON_TITLE, LESSON_CONTENT } from '../constants';
import AudioButton from './common/AudioButton';

const LessonView: React.FC = () => {
  const playAudio = () => {
    console.log('🔊 Reading lesson content aloud...');
    // In a real app, you would use SpeechSynthesisUtterance here
  };

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
       <AudioButton onClick={playAudio} title="Read lesson aloud"/>
    </div>
  );
};

export default LessonView;