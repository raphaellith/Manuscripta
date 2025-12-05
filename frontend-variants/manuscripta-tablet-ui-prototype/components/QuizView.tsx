import React from 'react';
import { QUIZ_QUESTIONS } from '../constants';
import Button from './common/Button';
import AudioButton from './common/AudioButton';

interface QuizViewProps {
  onBackToLesson: () => void;
  onSubmit: (isCorrect: boolean) => void;
  selectedAnswer: number | null;
  setSelectedAnswer: (index: number | null) => void;
}

const QuizView: React.FC<QuizViewProps> = ({ onBackToLesson, onSubmit, selectedAnswer, setSelectedAnswer }) => {
  const question = QUIZ_QUESTIONS[0]; // For this prototype, we only use the first question

  const handleSubmit = () => {
    if (selectedAnswer === null) {
      alert('Please select an answer first');
      return;
    }
    onSubmit(selectedAnswer === question.correctAnswerIndex);
  };
  
  const playAudio = () => {
    console.log('🔊 Reading question aloud...');
  };

  return (
    <div className="flex flex-col h-full">
      <Button variant="back" onClick={onBackToLesson}>
        ◄ Back to Lesson
      </Button>
      <div className="bg-[#e8e6e0] border-4 border-black p-8 mb-8 shadow-md">
        <p className="text-4xl font-bold leading-normal text-black">
          {question.question}
        </p>
      </div>
      <div className="space-y-5 overflow-y-auto">
        {question.options.map((option, index) => (
          <div
            key={index}
            onClick={() => setSelectedAnswer(index)}
            className={`
              bg-[#e8e6e0] border-4 border-black p-5 text-3xl min-h-[70px] flex items-center
              shadow-md cursor-pointer transition-all user-select-none text-black
              ${selectedAnswer === index
                ? 'bg-[#c8c6c0]'
                : 'hover:bg-[#d8d6d0]'
              }
            `}
          >
            {`${String.fromCharCode(65 + index)}) ${option}`}
          </div>
        ))}
      </div>
      <div className="mt-auto">
         <Button onClick={handleSubmit}>✓ Submit Answer</Button>
      </div>
      <AudioButton onClick={playAudio} title="Read question aloud" />
    </div>
  );
};

export default QuizView;