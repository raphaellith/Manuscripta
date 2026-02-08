/**
 * Centralized configuration for the Manuscripta Teacher Portal.
 * Per FrontendWorkflowSpecifications §2ZA(8).
 */

/**
 * The designated backend port.
 * Per FrontendWorkflowSpecifications §2ZA(8)(b).
 */
export const BACKEND_PORT = 5910;

/**
 * The backend base URL.
 */
export const BACKEND_URL = `http://localhost:${BACKEND_PORT}`;

/**
 * The backend health endpoint URL.
 * Per FrontendWorkflowSpecifications §2ZA(5)(a).
 */
export const BACKEND_HEALTH_URL = `${BACKEND_URL}/`;

/**
 * The SignalR hub URL.
 * Per FrontendWorkflowSpecifications §2(2)(d).
 */
export const SIGNALR_HUB_URL = `${BACKEND_URL}/hub`;

/**
 * The WebSocket URL for SignalR (used in CSP).
 */
export const BACKEND_WS_URL = `ws://localhost:${BACKEND_PORT}`;

/**
 * Backend executable name.
 */
export const BACKEND_EXECUTABLE_NAME = 'Manuscripta.Main.exe';

/**
 * Backend resource directory name (relative to resources).
 */
export const BACKEND_RESOURCE_DIR = 'backend';
