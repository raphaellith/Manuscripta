/**
 * Reusable modal overlay component that handles z-index layering.
 * 
 * Provides consistent backdrop styling and automatic z-index management
 * based on modal priority. Eliminates duplicate overlay markup.
 * 
 * Z-Index Priority Levels:
 * - 'standard': z-[50]  - Regular modals (create/edit/view operations)
 * - 'pairing':  z-[80]  - Device pairing modal
 * - 'dependency': z-[90] - Runtime dependency installation (highest priority)
 */

import React from 'react';

type ModalPriority = 'standard' | 'pairing' | 'dependency';

interface ModalOverlayProps {
  /** Priority level determining z-index stacking order */
  priority?: ModalPriority;
  /** Click handler for backdrop (typically closes modal) */
  onClick?: (e: React.MouseEvent<HTMLDivElement>) => void;
  /** Modal content - typically a styled container div with form/content */
  children: React.ReactNode;
}

const zIndexMap: Record<ModalPriority, string> = {
  standard: 'z-[50]',
  pairing: 'z-[80]',
  dependency: 'z-[90]',
};

/**
 * ModalOverlay component providing consistent modal backdrop styling.
 * 
 * @example
 * ```tsx
 * <ModalOverlay priority="standard" onClick={() => onClose()}>
 *   <div className="bg-white rounded-lg p-8 shadow-2xl...">
 *     {modalContent}
 *   </div>
 * </ModalOverlay>
 * ```
 */
export const ModalOverlay: React.FC<ModalOverlayProps> = ({
  priority = 'standard',
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
