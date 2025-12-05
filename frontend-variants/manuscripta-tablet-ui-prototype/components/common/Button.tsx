import React from 'react';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  children: React.ReactNode;
  variant?: 'large' | 'back';
}

const Button: React.FC<ButtonProps> = ({ children, variant = 'large', className, ...props }) => {
  const baseClasses = "flex items-center justify-center w-full border-4 border-eink-black cursor-pointer select-none text-eink-black bg-eink-light";
  
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