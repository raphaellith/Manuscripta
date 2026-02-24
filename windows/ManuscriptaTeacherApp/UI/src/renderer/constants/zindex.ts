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
 * - z-[50]: Standard modals (create/edit/view operations)
 * - z-[80]: Device pairing modal (mid-priority)
 * - z-[90]: Dependency installation modal (highest priority - must always be on top)
 * 
 * USAGE NOTES:
 * - Standard modals (create collection, lesson, material, etc.): z-[50]
 * - ReMarkablePairingModal: z-[80] (blocking operation)
 * - RuntimeDependencyInstallModal: z-[90] (critical path blocker)
 * 
 * When both the ReMarkablePairingModal and RuntimeDependencyInstallModal are open,
 * the RuntimeDependencyInstallModal will appear above it because it has a higher
 * z-index value.
 */

export const Z_INDEX = {
  // Background/static elements
  BACKGROUND: 0,
  
  // Main layout elements
  HEADER_WRAPPER: 10,
  HEADER: 20,
  
  // Modals (by priority)
  STANDARD_MODAL: 50,          // create/edit/view operations
  DEVICE_PAIRING: 80,          // ReMarkablePairingModal
  DEPENDENCY_INSTALL: 90,      // RuntimeDependencyInstallModal (highest priority)
} as const;
