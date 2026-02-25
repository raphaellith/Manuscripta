/**
 * Z-Index Hierarchy for UI Layer Management
 * 
 * This file defines the z-index values used throughout the application to ensure
 * proper layering of UI elements, especially modals. Values are chosen to provide
 * clear separation between different UI layers and allow for future additions.
 * 
 * HIERARCHY (from back to front):
 * - z-0: Decorative background elements (blurred circles, etc.)
 * - z-10: Header wrapper container
 * - z-20: Header element itself
 * - z-[50]: Low priority modals (create/edit/view operations)
 * - z-[80]: Moderate priority modals (workflow-blocking, e.g. device pairing)
 * - z-[90]: High priority modals (system-critical, e.g. dependency installation)
 * 
 * USAGE NOTES:
 * - Low priority (create/edit/view): z-[50]
 * - Moderate priority (workflow-blocking): z-[80]
 * - High priority (system-critical): z-[90]
 * 
 * When multiple modals are open simultaneously, higher priority modals will
 * automatically appear above lower priority modals.
 */

export const Z_INDEX = {
  // Background/static elements
  BACKGROUND: 0,
  
  // Main layout elements
  HEADER_WRAPPER: 10,
  HEADER: 20,
  
  // Modals (by priority)
  MODAL_LOW: 50,        // create/edit/view operations
  MODAL_MODERATE: 80,   // workflow-blocking operations (device pairing)
  MODAL_HIGH: 90,       // system-critical operations (dependency installation)
} as const;
