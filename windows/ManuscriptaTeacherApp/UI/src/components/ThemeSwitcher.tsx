import React, { useEffect, useState } from 'react';
import { themes } from '../themes';

export const ThemeSwitcher: React.FC = () => {
  const [currentTheme, setCurrentTheme] = useState<string>('default');
  const [isOpen, setIsOpen] = useState<boolean>(false);

  useEffect(() => {
    const theme = themes[currentTheme];
    if (theme) {
      Object.entries(theme.colors).forEach(([key, value]) => {
        document.documentElement.style.setProperty(key, value);
      });
    }
  }, [currentTheme]);

  const getSwatchColors = (themeKey: string) => {
    const theme = themes[themeKey];
    return [
      theme.colors['--color-brand-cream'],
      theme.colors['--color-brand-orange'],
      theme.colors['--color-brand-yellow'],
      theme.colors['--color-brand-blue'],
      theme.colors['--color-brand-green'],
    ];
  };

  return (
    <div className="fixed bottom-4 right-4 z-50">
      {/* Collapsed state - just a button */}
      {!isOpen && (
        <button
          onClick={() => setIsOpen(true)}
          className="bg-white p-3 rounded-full shadow-lg border border-gray-200 hover:shadow-xl transition-all flex items-center gap-2"
          title="Theme Switcher"
        >
          <div className="flex -space-x-1">
            {getSwatchColors(currentTheme).slice(0, 4).map((color, i) => (
              <div
                key={i}
                className="w-4 h-4 rounded-full border border-white"
                style={{ backgroundColor: color }}
              />
            ))}
          </div>
          <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01" />
          </svg>
        </button>
      )}

      {/* Expanded state - full panel */}
      {isOpen && (
        <div className="bg-white rounded-lg shadow-xl border border-gray-200 w-72 max-h-96 overflow-hidden flex flex-col">
          {/* Header */}
          <div className="flex items-center justify-between p-3 border-b border-gray-100">
            <span className="text-sm font-medium text-gray-700">Theme Switcher</span>
            <button
              onClick={() => setIsOpen(false)}
              className="p-1 hover:bg-gray-100 rounded transition-colors"
            >
              <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          {/* Theme list */}
          <div className="overflow-y-auto flex-1 p-2">
            <div className="flex flex-col gap-1">
              {Object.entries(themes).map(([key, theme]) => (
                <button
                  key={key}
                  onClick={() => setCurrentTheme(key)}
                  className={`flex items-center gap-3 px-3 py-2 rounded-md text-left transition-all ${
                    currentTheme === key
                      ? 'bg-gray-100 ring-2 ring-gray-300'
                      : 'hover:bg-gray-50'
                  }`}
                >
                  {/* Color swatches */}
                  <div className="flex -space-x-1 flex-shrink-0">
                    {getSwatchColors(key).map((color, i) => (
                      <div
                        key={i}
                        className="w-5 h-5 rounded-full border-2 border-white shadow-sm"
                        style={{ backgroundColor: color }}
                      />
                    ))}
                  </div>
                  {/* Theme name */}
                  <span className={`text-sm truncate ${
                    currentTheme === key ? 'font-medium text-gray-900' : 'text-gray-600'
                  }`}>
                    {theme.name}
                  </span>
                </button>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
