/**
 * Backend Process Manager
 * Per FrontendWorkflowSpecifications §2ZA.
 * 
 * Responsible for starting, monitoring, and terminating the backend process.
 */

import { spawn, ChildProcess } from 'child_process';
import * as path from 'path';
import * as http from 'http';
import { app } from 'electron';
import {
    DEFAULT_BACKEND_PORT,
    ALTERNATIVE_PORT_RANGE_MIN,
    ALTERNATIVE_PORT_RANGE_MAX,
    getBackendHealthUrl,
    BACKEND_EXECUTABLE_NAME,
    BACKEND_RESOURCE_DIR,
} from './config';

/**
 * Backend process states.
 */
export enum BackendState {
    STOPPED = 'stopped',
    STARTING = 'starting',
    RUNNING = 'running',
    RESTARTING = 'restarting',
    FAILED = 'failed',
}

/**
 * Manages the backend process lifecycle.
 * Per FrontendWorkflowSpecifications §2ZA.
 */
export class BackendProcessManager {
    private process: ChildProcess | null = null;
    private state: BackendState = BackendState.STOPPED;
    private currentPort: number = DEFAULT_BACKEND_PORT; // Per §2ZA(8)(c)(iv): Track active port
    private restartAttempts = 0;
    private restartDelay = 1000; // Initial delay: 1 second
    private lastSuccessfulStart: number | null = null;
    private restartWindowStart: number | null = null; // Per §2ZA(6)(d): Track 5-minute window
    private healthMonitorInterval: NodeJS.Timeout | null = null; // Health monitoring for external processes
    private onReadyCallbacks: Array<() => void> = [];
    private onErrorCallbacks: Array<(error: string) => void> = [];
    private onStateChangeCallbacks: Array<(state: BackendState) => void> = [];
    private isShuttingDown = false;

    // Configuration per §2ZA(5)(d) and §2ZA(6)(d)
    private readonly MAX_STARTUP_WAIT_MS = 30000; // 30 seconds
    private readonly HEALTH_POLL_INTERVAL_MS = 500; // 500 milliseconds
    private readonly HEALTH_MONITOR_INTERVAL_MS = 5000; // 5 seconds - for monitoring external processes
    private readonly MAX_RESTART_ATTEMPTS = 5;
    private readonly RESTART_WINDOW_MS = 300000; // 5 minutes
    private readonly STABLE_UPTIME_MS = 60000; // 60 seconds
    private readonly MAX_RESTART_DELAY_MS = 30000; // 30 seconds
    private readonly GRACEFUL_SHUTDOWN_TIMEOUT_MS = 5000; // 5 seconds

    /**
     * Resolves the backend executable path.
     * Per FrontendWorkflowSpecifications §2ZA(2)(c).
     */
    private getBackendExecutablePath(): string {
        if (app.isPackaged) {
            // Production mode: resolve relative to process.resourcesPath
            // Per §2ZA(2)(c)(i)
            return path.join(process.resourcesPath, BACKEND_RESOURCE_DIR, BACKEND_EXECUTABLE_NAME);
        } else {
            // Development mode: configurable fallback
            // Per §2ZA(2)(c)(ii)
            // Default to published output in resources/backend during dev
            return path.join(__dirname, '..', '..', 'resources', BACKEND_RESOURCE_DIR, BACKEND_EXECUTABLE_NAME);
        }
    }

    /**
     * Checks if the backend is already running by probing the health endpoint.
     * Per FrontendWorkflowSpecifications §2ZA(3)(a).
     * @param port - The port to check
     */
    private async isBackendAlreadyRunning(port: number): Promise<boolean> {
        return this.probeHealth(port);
    }

    /**
     * Probes the backend health endpoint.
     * Per FrontendWorkflowSpecifications §2ZA(5)(a-b).
     * @param port - The port to probe
     */
    private probeHealth(port: number): Promise<boolean> {
        return new Promise((resolve) => {
            const healthUrl = getBackendHealthUrl(port);
            const req = http.get(healthUrl, { timeout: 2000 }, (res) => {
                resolve(res.statusCode === 200);
            });
            req.on('error', () => resolve(false));
            req.on('timeout', () => {
                req.destroy();
                resolve(false);
            });
        });
    }

    /**
     * Starts health monitoring for externally-started backend processes.
     * Since we don't have a process handle for externally-started backends,
     * we must poll the health endpoint to detect when they go down.
     */
    private startHealthMonitoring(): void {
        this.stopHealthMonitoring(); // Clear any existing monitor
        
        this.healthMonitorInterval = setInterval(async () => {
            if (this.isShuttingDown || this.state !== BackendState.RUNNING) {
                return;
            }
            
            const healthy = await this.probeHealth(this.currentPort);
            if (!healthy && this.state === BackendState.RUNNING) {
                console.log('Health monitor detected backend is no longer responding');
                this.stopHealthMonitoring();
                this.handleUnexpectedExit();
            }
        }, this.HEALTH_MONITOR_INTERVAL_MS);
    }

    /**
     * Stops health monitoring.
     */
    private stopHealthMonitoring(): void {
        if (this.healthMonitorInterval) {
            clearInterval(this.healthMonitorInterval);
            this.healthMonitorInterval = null;
        }
    }

    /**
     * Waits for the backend to become ready.
     * Per FrontendWorkflowSpecifications §2ZA(5).
     * @param port - The port to check for readiness
     */
    private async waitForReady(port: number): Promise<boolean> {
        const startTime = Date.now();
        
        while (Date.now() - startTime < this.MAX_STARTUP_WAIT_MS) {
            if (await this.probeHealth(port)) {
                return true;
            }
            await this.sleep(this.HEALTH_POLL_INTERVAL_MS);
        }
        
        return false;
    }

    /**
     * Spawns the backend process.
     * Per FrontendWorkflowSpecifications §2ZA(3)(c).
     * @param port - The port to bind the backend to
     * @returns Promise that resolves to true if spawned successfully, false if port is unavailable
     */
    private spawnBackend(port: number): Promise<boolean> {
        return new Promise((resolve) => {
            const executablePath = this.getBackendExecutablePath();
            let addressInUseDetected = false;
            
            // Per §2ZA(3)(c)(ii): Start with --urls argument
            this.process = spawn(executablePath, [`--urls`, `http://localhost:${port}`], {
                stdio: ['ignore', 'pipe', 'pipe'],
                detached: false,
                windowsHide: true,
                cwd: path.dirname(executablePath),
            });

            // Per §2ZA(3)(c)(iii): Capture stdout and stderr for diagnostics
            this.process.stdout?.on('data', (data) => {
                console.log(`[Backend stdout]: ${data.toString().trim()}`);
            });

            this.process.stderr?.on('data', (data) => {
                const output = data.toString();
                console.error(`[Backend stderr]: ${output.trim()}`);
                // Per §2ZA(8)(c)(ii): Detect address-in-use error
                if (output.includes('EADDRINUSE') || output.includes('address already in use') || output.includes('Address already in use')) {
                    addressInUseDetected = true;
                }
            });

            // Per §2ZA(6)(a): Monitor for unexpected termination
            this.process.on('exit', (code, signal) => {
                console.log(`Backend process exited with code ${code}, signal ${signal}`);
                const proc = this.process;
                this.process = null;
                
                // If process exited very quickly with non-zero code, likely port issue
                // Per §2ZA(8)(c)(ii): Treat rapid exit as port unavailable
                if (addressInUseDetected || (code !== 0 && code !== null)) {
                    resolve(false);
                    return;
                }
                
                if (!this.isShuttingDown && proc !== null) {
                    this.handleUnexpectedExit();
                }
            });

            this.process.on('error', (err) => {
                console.error(`Backend process error: ${err.message}`);
                this.process = null;
                resolve(false);
            });

            // Give the process a moment to fail if port is in use
            // If it doesn't fail quickly, assume spawn was successful
            setTimeout(() => {
                if (this.process !== null) {
                    resolve(true);
                }
            }, 1000);
        });
    }

    /**
     * Handles unexpected backend termination.
     * Per FrontendWorkflowSpecifications §2ZA(6)(b).
     */
    private async handleUnexpectedExit(): Promise<void> {
        const now = Date.now();

        // Check if we've had stable uptime - reset backoff if so
        // Per §2ZA(6)(b)(iii)
        if (this.lastSuccessfulStart && 
            now - this.lastSuccessfulStart >= this.STABLE_UPTIME_MS) {
            this.restartAttempts = 0;
            this.restartDelay = 1000;
            this.restartWindowStart = null;
        }

        // Per §2ZA(6)(d): Track restart attempts within 5-minute window
        if (this.restartWindowStart === null) {
            this.restartWindowStart = now;
        } else if (now - this.restartWindowStart > this.RESTART_WINDOW_MS) {
            // Window expired, reset counter
            this.restartAttempts = 0;
            this.restartDelay = 1000;
            this.restartWindowStart = now;
        }

        this.restartAttempts++;

        // Per §2ZA(6)(d): Check if max attempts exceeded within window
        if (this.restartAttempts > this.MAX_RESTART_ATTEMPTS) {
            this.setState(BackendState.FAILED);
            this.notifyError('Application failed to start after multiple attempts. Please restart the application.');
            return;
        }

        // Per §2ZA(6)(c)(i): Display reconnecting indicator
        this.setState(BackendState.RESTARTING);

        // Per §2ZA(6)(b)(ii): Exponential backoff
        console.log(`Attempting backend restart in ${this.restartDelay}ms (attempt ${this.restartAttempts}/${this.MAX_RESTART_ATTEMPTS})`);
        await this.sleep(this.restartDelay);
        
        // Double the delay, cap at max
        this.restartDelay = Math.min(this.restartDelay * 2, this.MAX_RESTART_DELAY_MS);

        // Attempt restart
        try {
            await this.startInternal(false);
        } catch (err) {
            console.error('Restart failed:', err);
            // handleUnexpectedExit will be called again by the exit handler
        }
    }

    /**
     * Internal start logic.
     * Per FrontendWorkflowSpecifications §2ZA(3) and §2ZA(8)(c).
     * @param checkExisting Whether to check for existing backend process.
     */
    private async startInternal(checkExisting: boolean): Promise<void> {
        this.setState(BackendState.STARTING);

        // Per §2ZA(8)(b-c): Try default port first, then alternative range (skipping 5911-5913)
        const portsToTry = [DEFAULT_BACKEND_PORT];
        for (let port = ALTERNATIVE_PORT_RANGE_MIN; port <= ALTERNATIVE_PORT_RANGE_MAX; port++) {
            portsToTry.push(port);
        }

        for (const port of portsToTry) {
            console.log(`Trying port ${port}...`);

            // Per §2ZA(3)(a-b): Check if backend is already running on this port
            // Per §2ZA(3)(d): In deployment mode, never connect to a backend not spawned by itself
            if (checkExisting && !app.isPackaged && await this.isBackendAlreadyRunning(port)) {
                // Development mode only: connect to an existing backend
                // Per §2ZA(3)(b): If already running and responds, use it
                console.log(`[Dev mode] Backend already running on port ${port}, skipping spawn`);
                this.currentPort = port;
                this.lastSuccessfulStart = Date.now();
                this.setState(BackendState.RUNNING);
                // Start health monitoring since we don't have a process handle
                this.startHealthMonitoring();
                this.notifyReady();
                return;
            }

            // Per §2ZA(3)(c): Spawn the backend on this port
            // Per §2ZA(3)(c)(iv) and §2ZA(8)(c)(i-ii): Retry with next port if unavailable
            const spawned = await this.spawnBackend(port);
            if (!spawned) {
                console.log(`Port ${port} unavailable, trying next...`);
                continue;
            }

            // Per §2ZA(5): Wait for backend to become ready
            const ready = await this.waitForReady(port);

            if (ready) {
                // Per §2ZA(8)(c)(iv): Use this port for all subsequent communications
                this.currentPort = port;
                this.lastSuccessfulStart = Date.now();
                this.setState(BackendState.RUNNING);
                console.log(`Backend successfully started on port ${port}`);
                this.notifyReady();
                return;
            } else {
                // Per §2ZA(5)(d): Backend did not become ready in time
                this.killProcess();
                console.log(`Backend failed to become ready on port ${port}, trying next...`);
            }
        }

        // Per §2ZA(8)(c)(iii): All ports exhausted
        throw new Error(`No available port found. Tried ${DEFAULT_BACKEND_PORT} and ${ALTERNATIVE_PORT_RANGE_MIN}-${ALTERNATIVE_PORT_RANGE_MAX}. Please close other applications and try again.`);
    }

    /**
     * Gets the currently active backend port.
     * Per FrontendWorkflowSpecifications §2ZA(8)(c)(iv).
     */
    public getActivePort(): number {
        return this.currentPort;
    }

    /**
     * Starts the backend process.
     * Per FrontendWorkflowSpecifications §2ZA(3).
     */
    public async start(): Promise<void> {
        this.isShuttingDown = false;
        this.restartAttempts = 0;
        this.restartDelay = 1000;
        this.currentPort = DEFAULT_BACKEND_PORT;
        
        await this.startInternal(true);
    }

    /**
     * Stops the backend process gracefully.
     * Per FrontendWorkflowSpecifications §2ZA(7).
     */
    public async stop(): Promise<void> {
        this.isShuttingDown = true;
        this.stopHealthMonitoring();
        
        if (!this.process) {
            this.setState(BackendState.STOPPED);
            return;
        }

        // Per §2ZA(7)(a)(i): Send termination signal
        const killed = this.process.kill('SIGTERM');
        
        if (!killed) {
            // Process may already be dead
            this.process = null;
            this.setState(BackendState.STOPPED);
            return;
        }

        // Per §2ZA(7)(a)(ii): Wait up to 5 seconds for graceful exit
        const exitPromise = new Promise<void>((resolve) => {
            const onExit = () => {
                this.process?.removeListener('exit', onExit);
                resolve();
            };
            this.process?.on('exit', onExit);
        });

        const timeoutPromise = this.sleep(this.GRACEFUL_SHUTDOWN_TIMEOUT_MS);

        const result = await Promise.race([
            exitPromise.then(() => 'exited'),
            timeoutPromise.then(() => 'timeout'),
        ]);

        // Per §2ZA(7)(a)(iii): Force kill if not exited
        if (result === 'timeout' && this.process) {
            console.log('Backend did not exit gracefully, force killing');
            this.killProcess();
        }

        this.process = null;
        this.setState(BackendState.STOPPED);
    }

    /**
     * Force kills the backend process.
     */
    private killProcess(): void {
        if (this.process) {
            try {
                this.process.kill('SIGKILL');
            } catch {
                // Process may already be dead
            }
            this.process = null;
        }
    }

    /**
     * Registers a callback for when the backend is ready.
     */
    public onReady(callback: () => void): void {
        this.onReadyCallbacks.push(callback);
    }

    /**
     * Registers a callback for backend errors.
     */
    public onError(callback: (error: string) => void): void {
        this.onErrorCallbacks.push(callback);
    }

    /**
     * Registers a callback for state changes.
     */
    public onStateChange(callback: (state: BackendState) => void): void {
        this.onStateChangeCallbacks.push(callback);
    }

    /**
     * Gets the current backend state.
     */
    public getState(): BackendState {
        return this.state;
    }

    private setState(state: BackendState): void {
        this.state = state;
        this.onStateChangeCallbacks.forEach(cb => cb(state));
    }

    private notifyReady(): void {
        this.onReadyCallbacks.forEach(cb => cb());
    }

    private notifyError(error: string): void {
        this.onErrorCallbacks.forEach(cb => cb(error));
    }

    private sleep(ms: number): Promise<void> {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}

// Singleton instance
export const backendProcessManager = new BackendProcessManager();
