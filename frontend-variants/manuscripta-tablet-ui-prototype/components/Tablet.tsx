import React from 'react';

interface TabletProps {
  children: React.ReactNode;
}

const Tablet: React.FC<TabletProps> = ({ children }) => {
  return (
    <div className="w-[828px] bg-eink-mid border-[20px] border-solid border-eink-dark rounded-lg p-[30px]">
      <div className="bg-eink-light h-[1024px] p-[40px] shadow-inner flex flex-col">
        {children}
      </div>
    </div>
  );
};

export default Tablet;