import React from 'react';
import { WORKSHEET_ITEMS } from '../constants';
import AudioButton from './common/AudioButton';

const WorksheetView: React.FC = () => {
  const playAudio = () => {
    console.log('🔊 Reading worksheet aloud...');
  };
  
  return (
    <div className="flex flex-col h-full">
      <h1 className="text-4xl font-bold text-black mb-10 pb-4 border-b-4 border-black">
        Worksheet
      </h1>
      <div className="space-y-10 overflow-y-auto">
        {WORKSHEET_ITEMS.map((item, index) => (
          <div key={index} className="flex items-center text-3xl leading-relaxed text-black">
            <span>{index + 1}. </span>
            <p className="ml-4 flex-grow">
              {item.sentence.split('______')[0]}
              <input 
                type="text" 
                className="mx-2 px-2 pb-1 w-48 bg-transparent border-b-4 border-black focus:outline-none focus:border-gray-500 text-center font-semibold" 
              />
              {item.sentence.split('______')[1]}
            </p>
          </div>
        ))}
      </div>
      <div className="mt-auto">
        <button className="flex items-center justify-center w-full min-h-[60px] text-3xl font-bold mt-12 p-4 border-4 border-black shadow-md cursor-pointer transition-all user-select-none text-black bg-[#e8e6e0] hover:bg-[#d8d6d0] active:bg-black active:text-[#e8e6e0]">
            Check Answers
        </button>
      </div>
      <AudioButton onClick={playAudio} title="Read worksheet aloud" />
    </div>
  );
};

export default WorksheetView;