import React from 'react';

interface LayerInfo {
  title: string;
  color: string;
  description: string;
}

const layers: LayerInfo[] = [
  {
    title: 'Unit',
    color: 'var(--color-brand-green)',
    description: 'Units contain the context and source materials that inform all generated content.',
  },
  {
    title: 'Lesson',
    color: 'var(--color-brand-blue)',
    description: 'Lessons are organisational containers that group related materials together.',
  },
  {
    title: 'Material',
    color: 'var(--color-brand-orange)',
    description: 'Materials are the content pieces generated based on the unit context.',
  },
];

export const HierarchyVisualizer: React.FC = () => {
  return (
    <div className="min-h-screen bg-brand-cream p-8 flex flex-col items-center">
      {/* Header */}
      <div className="text-center mb-12">
        <h1 className="font-serif text-4xl font-medium text-text-heading mb-4">
          Library Hierarchy
        </h1>
        <p className="text-lg text-text-body opacity-80">
          Understanding how Units, Lessons, and Materials work together
        </p>
      </div>

      {/* One-to-Many Diagram */}
      <div className="relative mb-16" style={{ width: 700, height: 600 }}>
        
        {/* Unit - Large outer circle */}
        <div
          className="absolute rounded-full"
          style={{
            width: 680,
            height: 580,
            top: 10,
            left: 10,
            border: '3px solid var(--color-brand-green)',
          }}
        />
        <div
          className="absolute text-center"
          style={{ top: 30, left: '50%', transform: 'translateX(-50%)' }}
        >
          <div className="font-semibold text-xl" style={{ color: 'var(--color-brand-green)' }}>
            UNIT
          </div>
          <div className="text-xs text-text-body opacity-70 mt-1">
            Textbooks • Teacher Notes • Curriculum
          </div>
        </div>

        {/* Lesson 1 - Left */}
        <div
          className="absolute rounded-full"
          style={{
            width: 280,
            height: 280,
            top: 180,
            left: 40,
            border: '3px solid var(--color-brand-blue)',
          }}
        />
        <div
          className="absolute text-center"
          style={{ top: 200, left: 180, transform: 'translateX(-50%)' }}
        >
          <div className="font-semibold text-lg" style={{ color: 'var(--color-brand-blue)' }}>
            LESSON 1
          </div>
        </div>

        {/* Lesson 1 Materials */}
        <div
          className="absolute rounded-full flex items-center justify-center"
          style={{
            width: 70,
            height: 70,
            top: 250,
            left: 60,
            border: '2px solid var(--color-brand-orange)',
          }}
        >
          <span className="text-xs font-medium" style={{ color: 'var(--color-brand-orange)' }}>Reading</span>
        </div>
        <div
          className="absolute rounded-full flex items-center justify-center"
          style={{
            width: 70,
            height: 70,
            top: 330,
            left: 110,
            border: '2px solid var(--color-brand-orange)',
          }}
        >
          <span className="text-xs font-medium" style={{ color: 'var(--color-brand-orange)' }}>Quiz</span>
        </div>
        <div
          className="absolute rounded-full flex items-center justify-center"
          style={{
            width: 70,
            height: 70,
            top: 270,
            left: 190,
            border: '2px solid var(--color-brand-orange)',
          }}
        >
          <span className="text-xs font-medium" style={{ color: 'var(--color-brand-orange)' }}>Worksheet</span>
        </div>

        {/* Lesson 2 - Right */}
        <div
          className="absolute rounded-full"
          style={{
            width: 280,
            height: 280,
            top: 180,
            left: 380,
            border: '3px solid var(--color-brand-blue)',
          }}
        />
        <div
          className="absolute text-center"
          style={{ top: 200, left: 520, transform: 'translateX(-50%)' }}
        >
          <div className="font-semibold text-lg" style={{ color: 'var(--color-brand-blue)' }}>
            LESSON 2
          </div>
        </div>

        {/* Lesson 2 Materials */}
        <div
          className="absolute rounded-full flex items-center justify-center"
          style={{
            width: 70,
            height: 70,
            top: 250,
            left: 420,
            border: '2px solid var(--color-brand-orange)',
          }}
        >
          <span className="text-xs font-medium" style={{ color: 'var(--color-brand-orange)' }}>Reading</span>
        </div>
        <div
          className="absolute rounded-full flex items-center justify-center"
          style={{
            width: 70,
            height: 70,
            top: 330,
            left: 470,
            border: '2px solid var(--color-brand-orange)',
          }}
        >
          <span className="text-xs font-medium" style={{ color: 'var(--color-brand-orange)' }}>PDF</span>
        </div>
        <div
          className="absolute rounded-full flex items-center justify-center"
          style={{
            width: 70,
            height: 70,
            top: 270,
            left: 550,
            border: '2px solid var(--color-brand-orange)',
          }}
        >
          <span className="text-xs font-medium" style={{ color: 'var(--color-brand-orange)' }}>Quiz</span>
        </div>
        <div
          className="absolute rounded-full flex items-center justify-center"
          style={{
            width: 70,
            height: 70,
            top: 370,
            left: 540,
            border: '2px solid var(--color-brand-orange)',
          }}
        >
          <span className="text-xs font-medium" style={{ color: 'var(--color-brand-orange)' }}>Worksheet</span>
        </div>

      </div>

      {/* Legend Cards */}
      <div className="max-w-4xl w-full grid grid-cols-1 md:grid-cols-3 gap-6">
        {layers.map((layer) => (
          <div 
            key={layer.title}
            className="bg-white rounded-xl p-5 shadow-sm"
          >
            <div className="flex items-center gap-3 mb-3">
              <div 
                className="w-4 h-4 rounded-full"
                style={{ backgroundColor: layer.color }}
              />
              <h3 className="font-semibold text-text-heading">{layer.title}</h3>
            </div>
            <p className="text-sm text-text-body opacity-80">
              {layer.description}
            </p>
          </div>
        ))}
      </div>
    </div>
  );
};

export default HierarchyVisualizer;
