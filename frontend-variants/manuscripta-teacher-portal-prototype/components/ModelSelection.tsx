import React from 'react';

// Chip Icon
const ChipIcon: React.FC<{ type: string; color: string; label: string }> = ({ type, color, label }) => (
    <div className="w-20 h-20 bg-gray-800 rounded-md border-2 border-gray-600 flex flex-col items-center justify-center p-2 relative shadow-inner">
        <div className="absolute top-1 left-1 w-1 h-1 bg-gray-500 rounded-full"></div>
        <div className="absolute top-1 right-1 w-1 h-1 bg-gray-500 rounded-full"></div>
        <div className="absolute bottom-1 left-1 w-1 h-1 bg-gray-500 rounded-full"></div>
        <div className="absolute bottom-1 right-1 w-1 h-1 bg-gray-500 rounded-full"></div>
        
        <div className="text-white font-mono text-xs mb-1 font-bold">{type}</div>
        <div 
            className="w-10 h-10 rounded-sm flex items-center justify-center font-bold text-xs bg-opacity-20"
            style={{ backgroundColor: color, color: color }}
        >
           NPU
        </div>
    </div>
);

export const ModelSelection: React.FC = () => {
  return (
    <div className="min-h-screen bg-brand-cream p-8 flex flex-col items-center">
       <style>{`
        @keyframes scan {
          0% { top: 0; opacity: 0; }
          20% { opacity: 1; }
          80% { opacity: 1; }
          100% { top: 100%; opacity: 0; }
        }
      `}</style>

      {/* Header */}
      <div className="text-center mb-16 mt-8">
         <h1 className="font-serif text-5xl text-text-heading mb-4">
            Model Selection
          </h1>
         <div className="h-1 w-32 bg-purple-600 mx-auto rounded-full mb-8"></div>
      </div>

      {/* Main Visual */}
      <div className="flex items-center justify-center gap-16 mb-16 w-full max-w-5xl">
        
        {/* Models */}
        <div className="flex flex-col items-center gap-4">
             <div className="bg-white px-6 py-3 rounded-full shadow border border-purple-200 font-mono text-purple-800 font-bold flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-purple-500"></div> Granite
             </div>
             <div className="bg-white px-6 py-3 rounded-full shadow border border-blue-200 font-mono text-blue-800 font-bold flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-blue-500"></div> Mistral
             </div>

             <div className="bg-white px-6 py-3 rounded-full shadow border border-teal-200 font-mono text-teal-800 font-bold flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-teal-500"></div> Qwen
             </div>
             <div className="text-gray-400 font-bold text-xl leading-none">...</div>
             <p className="text-center font-bold text-gray-500 mt-2">Candidates</p>
        </div>

        {/* Evaluation Funnel */}
        <div className="relative w-48 h-48 flex items-center justify-center">
             <div className="absolute inset-0 bg-gradient-to-r from-transparent via-purple-100 to-transparent opacity-50 blur-xl"></div>
             <div className="relative z-10 text-center">
                <div className="text-5xl mb-2">⚖️</div>
                <div className="font-bold text-sm text-gray-700">Size</div>
                <div className="h-0.5 w-full bg-gray-200 my-2 relative overflow-hidden">
                    <div className="absolute left-0 top-0 h-full w-1/3 bg-purple-500" style={{ animation: 'scan 2s infinite linear', width: '100%', height: '100%', top:0 }}></div>
                </div>
                <div className="font-bold text-sm text-gray-700">Latency</div>
             </div>
        </div>

        {/* Hardware Targets */}
        <div className="flex flex-col items-center gap-6">
             <div className="flex gap-4">
                <ChipIcon type="INTEL" color="#0068B5" label="Core Ultra" />
                <ChipIcon type="QUALCOMM" color="#3253DC" label="Snapdragon" />
             </div>
             <p className="font-bold text-gray-600">On-Device Targets</p>
        </div>

      </div>

    </div>
  );
};

export default ModelSelection;
