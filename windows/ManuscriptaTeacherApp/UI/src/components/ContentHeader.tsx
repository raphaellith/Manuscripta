
import React from 'react';

type HeaderVariant = 'green' | 'orange' | 'blue' | 'yellow' | 'cream';

interface ContentHeaderProps {
  title: string;
  description?: string; // Optional but not rendered based on new spec
  variant?: HeaderVariant;
  action?: React.ReactNode;
}

const variantStyles: Record<HeaderVariant, { container: string, title: string, decoration: string }> = {
  green: {
    container: 'bg-brand-green text-white',
    title: 'text-white',
    decoration: 'bg-white/10'
  },
  orange: {
    container: 'bg-brand-orange text-white',
    title: 'text-white',
    decoration: 'bg-white/20'
  },
  blue: {
    container: 'bg-brand-blue text-text-onBlue',
    title: 'text-text-onBlue',
    decoration: 'bg-white/40'
  },
  yellow: {
    container: 'bg-brand-yellow text-text-onYellow',
    title: 'text-text-onYellow',
    decoration: 'bg-white/40'
  },
  cream: {
    container: 'bg-white border border-gray-200 text-text-heading',
    title: 'text-text-heading',
    decoration: 'bg-brand-gray/50'
  }
};

export const ContentHeader: React.FC<ContentHeaderProps> = ({ title, variant = 'cream', action }) => {
  const styles = variantStyles[variant];

  return (
    <div className={`${styles.container} rounded-xl p-6 mb-6 shadow-md relative overflow-hidden transition-colors duration-300 flex flex-wrap justify-between items-center gap-4`}>
      {/* Decorative Blur */}
      <div className={`absolute top-0 right-0 w-48 h-48 rounded-full blur-3xl pointer-events-none -translate-y-1/2 translate-x-1/3 ${styles.decoration}`}></div>
      
      <div className="relative z-10">
        <h3 className={`text-2xl font-serif font-medium ${styles.title}`}>{title}</h3>
      </div>
      
      {action && (
        <div className="relative z-10 flex-shrink-0">
          {action}
        </div>
      )}
    </div>
  );
};
