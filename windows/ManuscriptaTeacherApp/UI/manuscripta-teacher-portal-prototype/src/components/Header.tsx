
import React from 'react';
import type { View } from '../types';
import QuillLogo from '../resources/Quill Logo.png';

interface HeaderProps {
  activeView: View;
  setActiveView: (view: View) => void;
}

const viewConfig: Record<View, { label: string, activeClass: string, textClass: string }> = {
  dashboard: { label: 'Dashboard', activeClass: 'bg-brand-blue/20 text-blue-800', textClass: 'hover:text-blue-600' },
  'lesson-library': { label: 'Library', activeClass: 'bg-brand-green/10 text-brand-green', textClass: 'hover:text-brand-green' },
  'lesson-creator': { label: 'Creator', activeClass: 'bg-brand-yellow/30 text-yellow-900', textClass: 'hover:text-yellow-700' },
  'classroom-control': { label: 'Classroom', activeClass: 'bg-brand-orange/10 text-brand-orange', textClass: 'hover:text-brand-orange' },
  responses: { label: 'Responses', activeClass: 'bg-purple-100 text-purple-800', textClass: 'hover:text-purple-600' },
  'ai-assistant': { label: 'AI Assistant', activeClass: 'bg-brand-yellow/50 text-yellow-900', textClass: 'hover:text-yellow-700' },
  settings: { label: 'Settings', activeClass: 'bg-gray-100 text-gray-800', textClass: 'hover:text-gray-800' },
};

export const Header: React.FC<HeaderProps> = ({ activeView, setActiveView }) => {
  const navItems: View[] = [
    'dashboard',
    'lesson-library',
    'classroom-control',
    'responses',
    'ai-assistant',
    'settings',
  ];

  return (
    <div className="w-full flex justify-center p-6 pb-0 z-50">
        <header className="bg-white/95 backdrop-blur-xl border border-white/20 px-8 h-20 flex justify-between items-center shadow-soft w-full max-w-7xl rounded-2xl ring-1 ring-gray-900/5 transition-all pointer-events-auto">
          <div className="flex items-center gap-10 h-full">
              <h2 className="text-2xl font-serif font-medium text-text-heading flex items-center gap-3 tracking-tight cursor-default select-none group">
                <img src={QuillLogo} alt="Manuscripta Logo" className="h-[37px] w-auto" />
                Manuscripta
              </h2>
              
              <nav className="hidden md:block h-full">
                <ul className="flex h-full items-center space-x-1">
                    {navItems.map((view) => {
                        const config = viewConfig[view];
                        const isActive = activeView === view;
                        return (
                            <li key={view} className="flex items-center">
                                <button
                                    onClick={() => setActiveView(view)}
                                    className={`relative px-4 py-2 text-sm font-medium transition-all duration-200 rounded-lg
                                        ${isActive 
                                            ? config.activeClass 
                                            : `text-text-body/60 hover:bg-gray-50 ${config.textClass}`
                                        }`}
                                >
                                    {config.label}
                                </button>
                            </li>
                        );
                    })}
                </ul>
              </nav>
          </div>

          <div className="user-info flex items-center gap-6">
            <div className="hidden lg:block text-right">
                <p className="font-sans text-sm font-semibold text-text-heading">Jon Kirk</p>
                <p className="font-sans text-xs text-text-body/60">History Dept.</p>
            </div>
            <div className="user-avatar w-10 h-10 bg-gradient-to-br from-brand-green to-emerald-800 text-white rounded-full flex items-center justify-center font-sans font-bold shadow-sm ring-2 ring-white cursor-pointer hover:ring-brand-orange/20 transition-all">
              JK
            </div>
          </div>
        </header>
    </div>
  );
};
