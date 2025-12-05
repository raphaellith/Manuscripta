
import React from 'react';
import { AudioIcon } from '../Icons';

interface AudioButtonProps {
    onClick: () => void;
    title?: string;
}

const AudioButton: React.FC<AudioButtonProps> = ({ onClick, title = "Read aloud" }) => {
    return (
        <div className="flex justify-center mt-auto pt-4">
             <button 
                onClick={onClick} 
                title={title}
                className="w-16 h-16 bg-[#e8e6e0] border-4 border-black rounded-md flex items-center justify-center cursor-pointer transition-all user-select-none shadow-md text-black hover:bg-[#d8d6d0] active:bg-black active:text-[#e8e6e0]"
            >
                <AudioIcon className="w-8 h-8" />
            </button>
        </div>
    );
}

export default AudioButton;
