
import React from 'react';

interface CardProps {
  children: React.ReactNode;
  className?: string;
}

export const Card: React.FC<CardProps> = ({ children, className = '' }) => {
  return (
    <div className={`bg-white rounded-lg p-8 shadow-soft border border-gray-100 transition-shadow hover:shadow-md ${className}`}>
      {children}
    </div>
  );
};
