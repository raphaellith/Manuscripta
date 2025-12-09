import React from 'react';
import { QUIZ_QUESTIONS } from '../constants';
import Button from './common/Button';

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
  
  return (
    <div className="flex flex-col h-full">
      <Button variant="back" onClick={onBackToLesson}>
        ◄ Back to Lesson
      </Button>
      <div className="bg-eink-light border-4 border-eink-black p-8 mb-8">
        <p className="text-4xl font-bold leading-normal text-eink-black">
          {question.question}
        </p>
      </div>
      <div className="space-y-5 overflow-y-auto">
        {question.options.map((option, index) => (
          <div
            key={index}
            onClick={() => setSelectedAnswer(index)}
            className={`
              bg-eink-light border-4 border-eink-black p-5 text-3xl min-h-[70px] flex items-center
              cursor-pointer select-none text-eink-black
              ${selectedAnswer === index ? 'bg-eink-mid' : ''}
            `}
          >
            {`${String.fromCharCode(65 + index)}) ${option}`}
          </div>
        ))}
      </div>
      <div className="mt-auto">
         <Button onClick={handleSubmit}>✓ Submit Answer</Button>
      </div>
    </div>
  );
};

export default QuizView;