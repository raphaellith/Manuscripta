import React from 'react';

// Document Icon
const DocIcon: React.FC<{ type: string; color: string; label: string }> = ({ type, color, label }) => (
  <div 
    className="w-16 h-20 rounded shadow-sm border flex flex-col items-center justify-center relative bg-white"
    style={{ borderColor: color }}
  >
    <div className="absolute top-0 right-0 w-4 h-4 bg-gray-100" style={{ borderBottomLeftRadius: 8 }}></div>
    <div className="text-2xl mb-1">{type === 'pdf' ? '📄' : '📘'}</div>
    <div className="text-[8px] font-bold px-1 text-center leading-tight">{label}</div>
  </div>
);

export const RAGPipelineDesign: React.FC = () => {
  return (
    <div className="min-h-screen bg-brand-cream p-8 flex flex-col items-center">
       <style>{`
        @keyframes slide-in {
          0% { transform: translateX(-20px); opacity: 0; }
          100% { transform: translateX(0); opacity: 1; }
        }
        @keyframes retrieval-pulse {
          0% { box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.4); }
          70% { box-shadow: 0 0 0 15px rgba(59, 130, 246, 0); }
          100% { box-shadow: 0 0 0 0 rgba(59, 130, 246, 0); }
        }
      `}</style>

      {/* Header */}
      <div className="text-center mb-16 mt-8">
         <h1 className="font-serif text-5xl text-text-heading mb-4">
            RAG Pipeline Design
          </h1>
         <div className="h-1 w-32 bg-brand-blue mx-auto rounded-full mb-8"></div>
      </div>

      {/* Main Visual */}
      <div className="flex items-center justify-center gap-8 mb-16 w-full max-w-4xl">
        
        {/* Source Documents */}
        <div className="flex flex-col items-center gap-4">
            <div className="flex -space-x-4 relative">
                 <div className="transform -rotate-12 translate-y-2"><DocIcon type="pdf" color="#ef4444" label="History.pdf" /></div>
                 <div className="z-10"><DocIcon type="textbook" color="#3b82f6" label="Textbook" /></div>
                 <div className="transform rotate-12 translate-y-2"><DocIcon type="pdf" color="#10b981" label="Notes" /></div>
            </div>
            <p className="font-bold text-gray-600">Source Material</p>
        </div>

        {/* Arrow */}
        <div className="text-brand-blue text-4xl opacity-50">➔</div>

        {/* Vector Store / Retrieval */}
        <div className="flex flex-col items-center gap-4">
            <div 
                className="w-32 h-32 bg-white rounded-xl border-4 border-brand-blue flex flex-col items-center justify-center shadow-lg"
                style={{ animation: 'retrieval-pulse 2s infinite' }}
            >
                <div className="text-4xl mb-2">🔍</div>
                <div className="text-xs font-bold text-center">Context<br/>Retrieval</div>
            </div>
             <p className="font-bold text-brand-blue">Anchoring</p>
        </div>

        {/* Arrow */}
        <div className="text-brand-blue text-4xl opacity-50">➔</div>

        {/* Output */}
        <div className="flex flex-col items-center gap-4">
             <div className="w-48 bg-white rounded-lg shadow-md border border-gray-200 p-4 relative overflow-hidden">
                <div className="absolute top-0 left-0 w-full h-1 bg-brand-orange"></div>
                <h3 className="font-bold text-sm mb-2 text-brand-orange">AI Generated Quiz</h3>
                <div className="space-y-2">
                    <div className="h-2 bg-gray-100 rounded w-full"></div>
                    <div className="h-2 bg-gray-100 rounded w-3/4"></div>
                    <div className="h-2 bg-gray-100 rounded w-5/6"></div>
                </div>
                <div className="mt-3 flex items-center gap-1 text-[8px] text-brand-blue bg-blue-50 p-1 rounded">
                    <span>🔗</span> Source: Page 42, Textbook
                </div>
             </div>
             <p className="font-bold text-gray-600">Verified Output</p>
        </div>

      </div>

    </div>
  );
};

export default RAGPipelineDesign;
