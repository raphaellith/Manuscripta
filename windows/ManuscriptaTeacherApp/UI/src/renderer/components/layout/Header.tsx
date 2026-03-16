/**
 * Header component with navigation.
 * Copied from prototype with updated imports.
 */

import React from 'react';
import QuillLogo from '../../../resources/Quill Logo.png';

// For now, simplified view type until we have full routing
type View = 'lesson-library' | 'classroom-control' | 'responses' | 'settings';

interface HeaderProps {
    activeView: View;
    setActiveView: (view: View) => void;
}

const viewConfig: Record<View, { label: string, activeClass: string, textClass: string }> = {
    'lesson-library': { label: 'Library', activeClass: 'bg-brand-green/10 text-brand-green', textClass: 'hover:text-brand-green' },
    'classroom-control': { label: 'Classroom', activeClass: 'bg-brand-orange/10 text-brand-orange', textClass: 'hover:text-brand-orange' },
    responses: { label: 'Responses', activeClass: 'bg-purple-100 text-purple-800', textClass: 'hover:text-purple-600' },
    settings: { label: 'Settings', activeClass: 'bg-gray-100 text-gray-800', textClass: 'hover:text-gray-800' },
};

export const Header: React.FC<HeaderProps> = ({ activeView, setActiveView }) => {
    const navItems: View[] = [
        'lesson-library',
        'classroom-control',
        'responses',
        'settings',
    ];

    return (
        <div className="w-full flex justify-center p-6 pb-0 z-20">
            <header className="bg-white/95 backdrop-blur-xl border border-white/20 px-8 h-20 flex justify-between items-center shadow-soft w-full max-w-7xl rounded-2xl ring-1 ring-gray-900/5 transition-all pointer-events-auto">
                <div className="flex items-center gap-10 h-full">
                    <h2 className="text-2xl font-serif text-text-heading flex items-center gap-3 tracking-tight cursor-default select-none group">
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


            </header>
        </div>
    );
};
