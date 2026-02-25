/**
 * Reusable modal overlay component that handles z-index layering.
 * 
 * Provides consistent backdrop styling and automatic z-index management
 * based on modal priority. Eliminates duplicate overlay markup.
 * 
 * Z-Index Priority Levels:
 * - 'low':      z-[50]  - Regular modals (create/edit/view operations)
 * - 'moderate': z-[80]  - Workflow-blocking modals (device pairing)
 * - 'high':     z-[90]  - System-critical modals (dependency installation)
 */

import React from 'react';

type ModalPriority = 'low' | 'moderate' | 'high';

interface ModalOverlayProps {
  /** Priority level determining z-index stacking order */
  priority?: ModalPriority;
  /** Click handler for backdrop (typically closes modal) */
  onClick?: (e: React.MouseEvent<HTMLDivElement>) => void;
  /** Modal content - typically a styled container div with form/content */
  children: React.ReactNode;
}

const zIndexMap: Record<ModalPriority, string> = {
  low: 'z-[50]',
  moderate: 'z-[80]',
  high: 'z-[90]',
};

/**
 * ModalOverlay component providing consistent modal backdrop styling.
 * 
 * @example
 * ```tsx
 * <ModalOverlay priority="low" onClick={() => onClose()}>
 *   <div className="bg-white rounded-lg p-8 shadow-2xl...">
 *     {modalContent}
 *   </div>
 * </ModalOverlay>
 * ```
 */
export const ModalOverlay: React.FC<ModalOverlayProps> = ({
  priority = 'low',
  onClick,
  children,
}) => {
  return (
    <div
      className={`fixed inset-0 bg-text-heading/20 backdrop-blur-sm flex items-center justify-center ${zIndexMap[priority]} p-4`}
      onClick={onClick}
    >
      {children}
    </div>
  );
};
