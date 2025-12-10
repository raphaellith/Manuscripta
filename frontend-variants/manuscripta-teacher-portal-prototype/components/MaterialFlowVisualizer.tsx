import React from 'react';

interface FlowInfo {
  title: string;
  color: string;
  description: string;
}

const flowSteps: FlowInfo[] = [
  {
    title: 'Teacher App',
    color: 'var(--color-brand-green)',
    description: 'Teachers use the Manuscripta app to create, organise, and deploy educational materials with AI assistance.',
  },
  {
    title: 'Material Types',
    color: 'var(--color-brand-orange)',
    description: 'Content is generated in various formats: readings, quizzes, worksheets, and PDFs tailored to different learning needs.',
  },
  {
    title: 'Student Tablet',
    color: 'var(--color-brand-blue)',
    description: 'Interactive materials are rendered on e-ink tablets with AI-powered accessibility features for personalised learning.',
  },
];

const materialTypes = ['Reading', 'Quiz', 'Worksheet', 'PDF'];

// Teacher Portal Preview - miniature classroom control interface
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
      <span className="text-white font-semibold" style={{ fontSize: '7px' }}>Classroom Control</span>
    </div>
    
    {/* Content area */}
    <div className="p-2">
      {/* Launch lesson card preview */}
      <div className="bg-white rounded p-1.5 mb-2 border-t-2" style={{ borderTopColor: 'var(--color-brand-orange)' }}>
        <div className="font-semibold text-gray-700 mb-1" style={{ fontSize: '6px' }}>Launch a Lesson</div>
        <div className="flex gap-1">
          <div className="bg-gray-100 rounded px-1 py-0.5 flex-1" style={{ fontSize: '5px' }}>Unit 1</div>
          <div className="bg-brand-orange text-white rounded px-1 py-0.5" style={{ fontSize: '5px' }}>Launch</div>
        </div>
      </div>
      
      {/* Device grid preview */}
      <div className="grid grid-cols-4 gap-1">
        {[
          { status: 'ON_TASK', color: '#3b82f6' },
          { status: 'HAND_RAISED', color: 'var(--color-brand-orange)' },
          { status: 'ON_TASK', color: '#3b82f6' },
          { status: 'IDLE', color: 'var(--color-brand-green)' },
          { status: 'ON_TASK', color: '#3b82f6' },
          { status: 'ON_TASK', color: '#3b82f6' },
          { status: 'LOCKED', color: '#ca8a04' },
          { status: 'ON_TASK', color: '#3b82f6' },
        ].map((tablet, i) => (
          <div 
            key={i}
            className="rounded flex items-center justify-center"
            style={{ 
              backgroundColor: `${tablet.color}20`,
              height: 18,
            }}
          >
            <svg xmlns="http://www.w3.org/2000/svg" style={{ width: 8, height: 8, color: tablet.color }} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
            </svg>
          </div>
        ))}
      </div>
    </div>
  </div>
);

// Tablet preview matching the current tablet UI with character helper
const TabletPreview: React.FC = () => (
  <div className="relative">
    {/* Tablet outer frame */}
    <div 
      className="rounded-lg p-2"
      style={{ 
        backgroundColor: '#4a4a4a',
        width: 180,
        height: 240,
      }}
    >
      {/* Tablet screen */}
      <div 
        className="rounded-sm p-2 h-full flex flex-col relative"
        style={{ backgroundColor: '#f0ede6' }}
      >
        {/* Tab bar */}
        <div className="flex border-b-2 border-gray-800 mb-1">
          <div className="flex-1 py-0.5 font-bold text-center bg-gray-200 border-2 border-b-0 border-gray-800 underline" style={{ fontSize: '8px' }}>Reading</div>
          <div className="flex-1 py-0.5 font-bold text-center border-2 border-b-0 border-gray-800" style={{ fontSize: '8px' }}>Quiz</div>
          <div className="flex-1 py-0.5 font-bold text-center border-2 border-b-0 border-gray-800" style={{ fontSize: '8px' }}>Worksheet</div>
        </div>
        {/* Lesson content */}
        <div className="flex-1 overflow-hidden">
          <h3 className="font-serif font-medium border-b-2 border-gray-800 pb-1 mb-1" style={{ fontSize: '10px' }}>
            The Battle of Hastings
          </h3>
          <p className="leading-snug text-gray-800" style={{ fontSize: '7px' }}>
            In 1066, a power struggle for the English throne culminated in the Battle of Hastings...
          </p>
        </div>
        {/* Footer with character and audio button */}
        <div className="mt-auto pt-1 flex items-end justify-between">
          {/* Character helper */}
          <div className="relative">
            <img 
              src="/resources/char_swirlok.png" 
              alt="AI Helper" 
              className="object-contain"
              style={{ width: 32, height: 32 }}
            />
            {/* Speech bubble hint */}
            <div 
              className="absolute -top-4 left-8 bg-gray-100 border border-gray-800 rounded px-1"
              style={{ fontSize: '5px' }}
            >
              Need help?
            </div>
          </div>
          {/* Audio button */}
          <div 
            className="flex items-center justify-center border-2 border-gray-800 rounded bg-gray-100"
            style={{ width: 24, height: 24 }}
          >
            <svg 
              xmlns="http://www.w3.org/2000/svg" 
              fill="none" 
              viewBox="0 0 24 24" 
              strokeWidth={2} 
              stroke="currentColor"
              style={{ width: 14, height: 14 }}
            >
              <path strokeLinecap="round" strokeLinejoin="round" d="M19.114 5.636a9 9 0 010 12.728M16.463 8.288a5.25 5.25 0 010 7.424M6.75 8.25l4.72-4.72a.75.75 0 011.28.53v15.88a.75.75 0 01-1.28.53l-4.72-4.72H4.51c-.88 0-1.704-.507-1.938-1.354A9.01 9.01 0 012.25 12c0-.83.112-1.633.322-2.396C2.806 8.756 3.63 8.25 4.51 8.25H6.75z" />
            </svg>
          </div>
        </div>
      </div>
    </div>
  </div>
);

// Material bubble component with animation
const MaterialBubble: React.FC<{ label: string; index: number }> = ({ label, index }) => {
  const colors = [
    'var(--color-brand-orange)',
    'var(--color-brand-blue)',
    'var(--color-brand-green)',
    'var(--color-brand-yellow)',
  ];
  
  return (
    <div
      className="absolute rounded-full flex items-center justify-center px-3 py-2 shadow-md"
      style={{
        backgroundColor: `${colors[index % colors.length]}`,
        animation: `float-${index} 3s ease-in-out infinite`,
        animationDelay: `${index * 0.4}s`,
      }}
    >
      <span className="text-sm font-semibold text-white drop-shadow-sm">{label}</span>
    </div>
  );
};

export const MaterialFlowVisualizer: React.FC = () => {
  return (
    <div className="min-h-screen bg-brand-cream p-8 flex flex-col items-center">
      {/* Inline keyframe animations */}
      <style>{`
        @keyframes float-0 {
          0%, 100% { transform: translate(0, 0); }
          50% { transform: translate(10px, -15px); }
        }
        @keyframes float-1 {
          0%, 100% { transform: translate(0, 0); }
          50% { transform: translate(-5px, -20px); }
        }
        @keyframes float-2 {
          0%, 100% { transform: translate(0, 0); }
          50% { transform: translate(8px, 10px); }
        }
        @keyframes float-3 {
          0%, 100% { transform: translate(0, 0); }
          50% { transform: translate(-8px, -12px); }
        }
        @keyframes pulse-arrow {
          0%, 100% { opacity: 0.7; }
          50% { opacity: 1; }
        }
        @keyframes flow-right {
          0% { transform: translateX(-10px); opacity: 0.5; }
          50% { opacity: 1; }
          100% { transform: translateX(10px); opacity: 0.5; }
        }
      `}</style>

      {/* Header */}
      <div className="text-center mb-12">
        <div className="flex items-center justify-center gap-4 mb-4">
          <img 
            src="/resources/Quill Logo.png" 
            alt="Manuscripta Logo" 
            className="w-16 h-16 object-contain"
          />
          <h1 className="font-serif text-5xl font-medium text-text-heading">
            Manuscripta
          </h1>
        </div>
        <p className="text-lg text-text-body opacity-80 max-w-2xl mx-auto">
          Generate and deploy learning materials directly to student tablets
        </p>
      </div>

      {/* Main Flow Diagram */}
      <div className="relative flex items-center justify-center gap-8 mb-16" style={{ width: 900, height: 350 }}>
        
        {/* Teacher Portal - Left Side */}
        <div className="flex flex-col items-center">
          <TeacherPortalPreview />
          <div className="mt-4 text-center">
            <div className="font-semibold text-lg" style={{ color: 'var(--color-brand-green)' }}>
              Teacher Portal
            </div>
            <div className="text-sm text-text-body opacity-70 mt-1 max-w-[180px]">
              Generate & deploy materials
            </div>
          </div>
        </div>

        {/* Arrow with Material Bubbles - Centre */}
        <div className="relative flex items-center" style={{ width: 350, height: 200 }}>
          {/* Arrow shaft */}
          <div 
            className="absolute top-1/2 left-0 right-16 h-3 rounded-full"
            style={{ 
              backgroundColor: 'var(--color-brand-orange)',
              transform: 'translateY(-50%)',
              opacity: 0.3,
            }}
          />
          {/* Animated arrow segments */}
          {[0, 1, 2, 3, 4].map((i) => (
            <div
              key={i}
              className="absolute top-1/2 h-3 w-12 rounded-full"
              style={{
                backgroundColor: 'var(--color-brand-orange)',
                left: `${i * 50 + 20}px`,
                transform: 'translateY(-50%)',
                animation: 'flow-right 1.5s ease-in-out infinite',
                animationDelay: `${i * 0.2}s`,
              }}
            />
          ))}
          {/* Arrow head */}
          <div 
            className="absolute right-0 top-1/2"
            style={{ 
              transform: 'translateY(-50%)',
              width: 0,
              height: 0,
              borderTop: '20px solid transparent',
              borderBottom: '20px solid transparent',
              borderLeft: '30px solid var(--color-brand-orange)',
              animation: 'pulse-arrow 2s ease-in-out infinite',
            }}
          />
          
          {/* Material Bubbles floating above/below arrow */}
          <div className="absolute" style={{ top: 20, left: 30 }}>
            <MaterialBubble label="Reading" index={0} />
          </div>
          <div className="absolute" style={{ bottom: 20, left: 90 }}>
            <MaterialBubble label="Quiz" index={1} />
          </div>
          <div className="absolute" style={{ top: 30, left: 160 }}>
            <MaterialBubble label="Worksheet" index={2} />
          </div>
          <div className="absolute" style={{ bottom: 30, left: 230 }}>
            <MaterialBubble label="PDF" index={3} />
          </div>
        </div>

        {/* Tablet - Right Side */}
        <div className="flex flex-col items-center">
          <TabletPreview />
          <div className="mt-4 text-center">
            <div className="font-semibold text-lg" style={{ color: 'var(--color-brand-blue)' }}>
              Student Tablet
            </div>
            <div className="text-sm text-text-body opacity-70 mt-1 max-w-[150px]">
              Interactive e-ink learning
            </div>
          </div>
        </div>
      </div>

      {/* Legend Cards */}

    </div>
  );
};

export default MaterialFlowVisualizer;
