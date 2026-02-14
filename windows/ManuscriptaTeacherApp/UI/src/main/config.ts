/**
 * Centralized configuration for the Manuscripta Teacher Portal.
 * Per FrontendWorkflowSpecifications §2ZA(8).
 */

/**
 * The default SignalR port.
 * Per FrontendWorkflowSpecifications §2ZA(8)(b).
 */
export const DEFAULT_BACKEND_PORT = 5910;

/**
 * The HTTP API port.
 * Per API Contract specification - REST API endpoints are served on this port.
 */
export const HTTP_API_PORT = 5911;

/**
 * Alternative port range for auto-retry (skipping 5911-5913 which are used for other purposes).
 * Per FrontendWorkflowSpecifications §2ZA(8)(c)(i).
 */
export const ALTERNATIVE_PORT_RANGE_MIN = 5914;
export const ALTERNATIVE_PORT_RANGE_MAX = 5919;

/**
 * Get the backend base URL for a given port.
 */
export const getBackendUrl = (port: number): string => `http://localhost:${port}`;

/**
 * Get the backend health endpoint URL for a given port.
 * Per FrontendWorkflowSpecifications §2ZA(5)(a).
 */
export const getBackendHealthUrl = (port: number): string => `${getBackendUrl(port)}/`;

/**
 * Get the SignalR hub URL for a given port.
 * Per FrontendWorkflowSpecifications §1(1)(a) and §2(2)(d).
 */
export const getSignalRHubUrl = (port: number): string => `${getBackendUrl(port)}/TeacherPortalHub`;

/**
 * Get the WebSocket URL for SignalR (used in CSP) for a given port.
 */
export const getBackendWsUrl = (port: number): string => `ws://localhost:${port}`;

/**
 * Backend executable name.
 */
export const BACKEND_EXECUTABLE_NAME = 'Manuscripta.Main.exe';

/**
 * Backend resource directory name (relative to resources).
 */
export const BACKEND_RESOURCE_DIR = 'backend';
