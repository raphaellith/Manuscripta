import React from 'react';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  children: React.ReactNode;
  variant?: 'large' | 'back';
}

const Button: React.FC<ButtonProps> = ({ children, variant = 'large', className, ...props }) => {
  const baseClasses = "flex items-center justify-center w-full border-4 border-black shadow-md cursor-pointer transition-all user-select-none text-black bg-[#e8e6e0] hover:bg-[#d8d6d0] active:bg-[#c8c6c0]";
  
  const variantClasses = {
    large: 'min-h-[60px] text-3xl font-bold mt-12 p-4',
    back: 'min-h-[55px] text-xl mb-8 p-3',
  };

  return (
    <button className={`${baseClasses} ${variantClasses[variant]} ${className}`} {...props}>
      {children}
    </button>
  );
};

export default Button;