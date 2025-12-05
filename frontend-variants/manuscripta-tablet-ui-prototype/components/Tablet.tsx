import React from 'react';

interface TabletProps {
  children: React.ReactNode;
}

const Tablet: React.FC<TabletProps> = ({ children }) => {
  return (
    <div className="w-[828px] bg-[#d4d4d4] border-[20px] border-solid border-[#8a8a8a] rounded-lg p-[30px] shadow-[0_0_0_2px_#7a7a7a,0_10px_40px_rgba(0,0,0,0.2)]">
      <div className="bg-[#e8e6e0] h-[1024px] p-[40px] shadow-inner flex flex-col bg-[repeating-linear-gradient(0deg,transparent,transparent_2px,rgba(0,0,0,0.01)_2px,rgba(0,0,0,0.01)_4px)]">
        {children}
      </div>
    </div>
  );
};

export default Tablet;