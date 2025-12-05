
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
                className="w-16 h-16 bg-eink-light border-4 border-eink-black rounded-md flex items-center justify-center cursor-pointer select-none text-eink-black"
            >
                <AudioIcon className="w-8 h-8" />
            </button>
        </div>
    );
}

export default AudioButton;
