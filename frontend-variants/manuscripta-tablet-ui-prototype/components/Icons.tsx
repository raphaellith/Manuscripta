
import React from 'react';

export const AudioIcon: React.FC<{ className?: string }> = ({ className }) => (
  <svg className={className} viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
    <path d="M 20 35 L 35 35 L 55 20 L 55 80 L 35 65 L 20 65 Z M 65 30 Q 75 50 65 70 M 75 25 Q 90 50 75 75"
      fill="currentColor" stroke="currentColor" strokeWidth="4" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);
