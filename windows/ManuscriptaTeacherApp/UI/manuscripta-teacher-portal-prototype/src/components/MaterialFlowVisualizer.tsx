import React from 'react';
import QuillLogo from '../resources/Quill Logo.png';

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

// Computer icon component
const ComputerIcon: React.FC = () => (
  <svg viewBox="0 0 100 80" className="w-full h-full">
    {/* Monitor */}
    <rect x="10" y="5" width="80" height="50" rx="4" fill="var(--color-brand-green)" opacity="0.15" stroke="var(--color-brand-green)" strokeWidth="3"/>
    {/* Screen content - simple UI mockup */}
    <rect x="18" y="12" width="64" height="36" fill="white" opacity="0.5"/>
    <rect x="22" y="16" width="20" height="4" fill="var(--color-brand-green)" opacity="0.6"/>
    <rect x="22" y="24" width="56" height="2" fill="var(--color-text-body)" opacity="0.3"/>
    <rect x="22" y="30" width="56" height="2" fill="var(--color-text-body)" opacity="0.3"/>
    <rect x="22" y="36" width="40" height="2" fill="var(--color-text-body)" opacity="0.3"/>
    {/* Stand */}
    <path d="M40 55 L60 55 L55 65 L45 65 Z" fill="var(--color-brand-green)" opacity="0.8"/>
    {/* Base */}
    <rect x="30" y="65" width="40" height="6" rx="2" fill="var(--color-brand-green)" opacity="0.8"/>
  </svg>
);

// Simplified tablet rendering showing a lesson
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
        className="rounded-sm p-2 h-full flex flex-col"
        style={{ backgroundColor: '#f0ede6' }}
      >
        {/* Tab bar */}
        <div className="flex border-b border-gray-800 mb-1">
          <div className="flex-1 py-0.5 font-semibold text-center bg-gray-200 border border-b-0 border-gray-800 underline" style={{ fontSize: '8px' }}>Lesson</div>
          <div className="flex-1 py-0.5 font-semibold text-center border border-b-0 border-gray-800" style={{ fontSize: '8px' }}>Quiz</div>
          <div className="flex-1 py-0.5 font-semibold text-center border border-b-0 border-gray-800" style={{ fontSize: '8px' }}>Worksheet</div>
        </div>
        {/* Lesson content */}
        <div className="flex-1 overflow-hidden">
          <h3 className="font-serif font-medium border-b border-gray-800 pb-1 mb-1" style={{ fontSize: '10px' }}>
            The Battle of Hastings
          </h3>
          <p className="leading-snug text-gray-800" style={{ fontSize: '8px' }}>
            In 1066, a power struggle for the English throne culminated in the Battle of Hastings...
          </p>
        </div>
        {/* AI toolbar hint */}
        <div className="mt-auto pt-1">
          <div className="grid grid-cols-3 gap-0.5">
            <div className="text-center py-0.5 px-0.5 border border-gray-700 bg-gray-100" style={{ fontSize: '7px' }}>Simplify</div>
            <div className="text-center py-0.5 px-0.5 border border-gray-700 bg-gray-100" style={{ fontSize: '7px' }}>Expand</div>
            <div className="text-center py-0.5 px-0.5 border border-gray-700 bg-gray-100" style={{ fontSize: '7px' }}>Summarise</div>
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
            src={QuillLogo}
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
        
        {/* Computer - Left Side */}
        <div className="flex flex-col items-center">
          <div style={{ width: 180, height: 150 }}>
            <ComputerIcon />
          </div>
          <div className="mt-4 text-center">
            <div className="font-semibold text-lg" style={{ color: 'var(--color-brand-green)' }}>
              Teacher App
            </div>
            <div className="text-sm text-text-body opacity-70 mt-1 max-w-[150px]">
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
