import React from 'react';


// Teacher Portal Preview - miniature classroom control interface (Reused)
const TeacherPortalPreview: React.FC = () => (
  <div 
    className="rounded-lg shadow-lg overflow-hidden"
    style={{ 
      width: 220,
      height: 160,
      backgroundColor: 'var(--color-brand-cream)',
      border: '2px solid var(--color-brand-green)',
    }}
  >
    {/* Header bar */}
    <div 
      className="flex items-center px-2 py-1 gap-1"
      style={{ backgroundColor: 'var(--color-brand-green)' }}
    >
      <div className="w-2 h-2 rounded-full bg-white opacity-80" />
      <span className="text-white font-semibold" style={{ fontSize: '7px' }}>Manuscripta Teacher</span>
    </div>
    
    {/* Content area */}
    <div className="p-2 flex flex-col gap-2">
      {/* Action buttons */}
      <div className="bg-white rounded p-1.5 border-l-2" style={{ borderLeftColor: 'var(--color-brand-orange)' }}>
        <div className="font-semibold text-gray-700 mb-1" style={{ fontSize: '7px' }}>AI Actions</div>
        <div className="flex flex-col gap-1">
          <div className="bg-brand-orange text-white rounded px-1 py-0.5 text-center" style={{ fontSize: '6px' }}>Generate Worksheet</div>
          <div className="bg-gray-200 text-gray-700 rounded px-1 py-0.5 text-center" style={{ fontSize: '6px' }}>Create Quiz</div>
        </div>
      </div>
      
       <div className="bg-white rounded p-1.5 border-l-2" style={{ borderLeftColor: 'var(--color-brand-blue)' }}>
        <div className="font-semibold text-gray-700 mb-1" style={{ fontSize: '7px' }}>Settings</div>
        <div className="flex gap-1">
           <div className="border border-gray-300 rounded px-1 py-0.5 flex-1 text-center" style={{ fontSize: '5px' }}>Reading Age: 11</div>
        </div>
      </div>
    </div>
  </div>
);

// AI Engine Representation
const AIEngineWithTasks: React.FC = () => (
    <div className="relative">
      <div 
        className="rounded-full shadow-lg flex items-center justify-center border-4 border-double"
        style={{ 
          width: 160,
          height: 160,
          backgroundColor: '#fff',
          borderColor: 'var(--color-brand-orange)',
        }}
      >
        <div className="text-center">
            <div className="text-4xl mb-1">🤖</div>
            <div className="font-bold text-gray-800 text-xs">LLM Engine</div>
            <div className="text-[8px] text-gray-500 mt-1">Context Aware</div>
        </div>
      </div>
      
      {/* Floating Task Bubbles */}
      <div className="absolute -top-4 -right-10 bg-brand-yellow px-2 py-1 rounded shadow text-[10px] font-bold">Reading Materials</div>
      <div className="absolute top-1/2 -right-16 bg-brand-blue text-white px-2 py-1 rounded shadow text-[10px] font-bold">Quiz Questions</div>
      <div className="absolute -bottom-4 -right-10 bg-brand-green text-white px-2 py-1 rounded shadow text-[10px] font-bold">Level Adjustment</div>
      <div className="absolute top-10 -right-20 bg-purple-600 text-white px-2 py-1 rounded shadow text-[10px] font-bold">Auto Marking</div>
    </div>
);

export const LLMImplementationPlanning: React.FC = () => {
  return (
    <div className="min-h-screen bg-brand-cream p-8 flex flex-col items-center">
      {/* Animations */}
      <style>{`
        @keyframes pulse-flow {
          0% { transform: scale(0.95); opacity: 0.5; }
          50% { transform: scale(1.05); opacity: 1; }
          100% { transform: scale(0.95); opacity: 0.5; }
        }
         @keyframes float {
          0%, 100% { transform: translateY(0); }
          50% { transform: translateY(-10px); }
        }
      `}</style>
      
      {/* Header */}
      <div className="text-center mb-16 mt-8">
         <h1 className="font-serif text-5xl text-text-heading mb-4">
            LLM Implementation Planning
          </h1>
         <div className="h-1 w-32 bg-brand-orange mx-auto rounded-full mb-8"></div>
      </div>

      {/* Main Visual */}
      <div className="flex items-center justify-center gap-12 mb-16 px-12">
        
        {/* Input: Teacher */}
        <div className="flex flex-col items-center gap-4">
             <TeacherPortalPreview />
             <p className="font-bold text-brand-green">Teacher Request</p>
        </div>

        {/* Arrow Flow */}
        <div className="flex items-center gap-2">
            <div className="flex gap-1" style={{ animation: 'pulse-flow 2s infinite' }}>
                <div className="w-3 h-3 rounded-full bg-brand-orange opacity-40"></div>
                <div className="w-3 h-3 rounded-full bg-brand-orange opacity-60"></div>
                <div className="w-3 h-3 rounded-full bg-brand-orange opacity-80"></div>
                <div className="w-3 h-3 rounded-full bg-brand-orange"></div>
            </div>
            <div className="h-0.5 w-24 bg-brand-orange"></div>
             <div className="w-0 h-0 border-t-8 border-t-transparent border-l-[12px] border-l-brand-orange border-b-8 border-b-transparent"></div>
        </div>

        {/* Core: LLM */}
        <div className="flex flex-col items-center gap-4" style={{ animation: 'float 3s ease-in-out infinite' }}>
            <AIEngineWithTasks />
             <p className="font-bold text-brand-orange">AI Processing</p>
        </div>

      </div>

    </div>
  );
};

export default LLMImplementationPlanning;
