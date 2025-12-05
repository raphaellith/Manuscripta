
import React from 'react';
import type { View } from '../types';

interface SidebarProps {
  activeView: View;
  setActiveView: (view: View) => void;
}

const viewLabels: Record<View, string> = {
  dashboard: 'Class Dashboard',
  'lesson-library': 'Lesson Library',
  'ai-assistant': 'AI Assistant',
  'classroom-control': 'Classroom Control',
  settings: 'Settings',
  'lesson-creator': 'Lesson Creator',
};

export const Sidebar: React.FC<SidebarProps> = ({ activeView, setActiveView }) => {
  const sidebarItems: View[] = [
    'dashboard',
    'classroom-control',
    'lesson-library',
    'ai-assistant',
    'settings',
  ];

  return (
    <aside className="sidebar w-64 bg-brand-green text-white p-6 flex flex-col shadow-xl z-20">
      <nav className="flex-1">
        <ul className="space-y-2">
          {sidebarItems.map((view) => (
            <li key={view}>
              <button
                onClick={() => setActiveView(view)}
                className={`w-full text-left flex items-center px-4 py-3 rounded-md text-sm font-medium transition-all duration-200 ${
                  activeView === view
                    ? 'bg-brand-orange text-white shadow-md'
                    : 'text-gray-100 hover:bg-white/10 hover:text-white'
                }`}
              >
                {viewLabels[view]}
              </button>
            </li>
          ))}
        </ul>
      </nav>
      <div className="mt-auto pt-6 border-t border-white/10">
        <p className="text-xs text-white/50 text-center font-sans">v2.4.0 • Academic Modern</p>
      </div>
    </aside>
  );
};
