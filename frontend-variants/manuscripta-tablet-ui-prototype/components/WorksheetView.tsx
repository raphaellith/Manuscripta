import React from 'react';
import { WORKSHEET_ITEMS } from '../constants';
import SubmitButton from './common/SubmitButton';

const WorksheetView: React.FC = () => {
  
  return (
    <div className="flex flex-col h-full">
      <h1 className="text-4xl font-serif font-medium text-eink-black mb-10 pb-4 border-b-4 border-eink-black">
        Worksheet
      </h1>
      <div className="space-y-10 overflow-y-auto">
        {WORKSHEET_ITEMS.map((item, index) => (
          <div key={index} className="flex items-center text-3xl leading-relaxed text-eink-black">
            <span>{index + 1}. </span>
            <p className="ml-4 flex-grow">
              {item.sentence.split('______')[0]}
              <input 
                type="text" 
                className="mx-2 px-2 pb-1 w-48 bg-transparent border-b-4 border-eink-black focus:outline-none focus:border-eink-dark text-center font-semibold" 
              />
              {item.sentence.split('______')[1]}
            </p>
          </div>
        ))}
      </div>
      <div className="mt-auto">
        <SubmitButton>
            Submit Answers
        </SubmitButton>
      </div>
    </div>
  );
};

export default WorksheetView;