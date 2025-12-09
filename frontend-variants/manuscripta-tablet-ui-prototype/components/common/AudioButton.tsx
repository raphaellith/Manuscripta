
import React from 'react';
import { AudioIcon } from '../Icons';

interface AudioButtonProps {
    onClick: () => void;
    title?: string;
}

const AudioButton: React.FC<AudioButtonProps> = ({ onClick, title = "Read aloud" }) => {
    return (
        <button 
            onClick={onClick} 
            title={title}
            className="w-20 h-20 bg-eink-light border-4 border-eink-black rounded-md flex items-center justify-center cursor-pointer select-none text-eink-black"
        >
            <AudioIcon className="w-10 h-10" />
        </button>
    );
}

export default AudioButton;
