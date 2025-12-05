
import React from 'react';
import { QUIZ_QUESTIONS } from '../constants';
import Button from './common/Button';
import AudioButton from './common/AudioButton';

interface FeedbackViewProps {
  isCorrect: boolean;
  onNext: () => void;
}

const FeedbackView: React.FC<FeedbackViewProps> = ({ isCorrect, onNext }) => {
  const playAudio = () => {
    console.log('🔊 Reading feedback aloud...');
  };

  return (
    <div className="flex flex-col h-full text-center">
      {isCorrect ? (
        <>
          <div className="text-[160px] leading-none text-eink-black">✓</div>
          <div className="text-5xl font-bold my-10 text-eink-black">Correct!</div>
          <div className="text-3xl leading-relaxed text-center mb-10 p-8 bg-eink-light border-4 border-eink-black text-eink-black">
            William won the battle.
          </div>
          <div className="flex-grow"></div>
          <Button onClick={onNext}>Next Question ►</Button>
        </>
      ) : (
        <>
          <div className="text-[160px] leading-none text-eink-black">✗</div>
          <div className="text-5xl font-bold my-10 text-eink-black">Not quite right.</div>
          <div className="text-left text-3xl leading-relaxed p-6 bg-eink-light border-4 border-eink-black mb-10 text-eink-black">
            <div className="font-bold text-2xl mb-4">The correct answer is:</div>
            <div>{QUIZ_QUESTIONS[0].options[QUIZ_QUESTIONS[0].correctAnswerIndex]}</div>
          </div>
          <div className="flex-grow"></div>
          <Button onClick={onNext}>Try Again</Button>
        </>
      )}
      <AudioButton onClick={playAudio} title="Read feedback aloud" />
    </div>
  );
};

export default FeedbackView;
