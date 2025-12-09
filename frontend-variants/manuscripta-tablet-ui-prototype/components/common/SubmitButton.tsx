import React from 'react';
import Button from './Button';

interface SubmitButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  children: React.ReactNode;
}

const SubmitButton: React.FC<SubmitButtonProps> = ({ children, onClick, ...props }) => {
  return (
    <Button onClick={onClick} {...props}>
      {children}
    </Button>
  );
};

export default SubmitButton;
