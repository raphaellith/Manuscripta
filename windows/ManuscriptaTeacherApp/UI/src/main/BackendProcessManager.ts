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
    BACKEND_PORT,
    BACKEND_HEALTH_URL,
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
    private restartAttempts = 0;
    private restartDelay = 1000; // Initial delay: 1 second
    private lastSuccessfulStart: number | null = null;
    private onReadyCallbacks: Array<() => void> = [];
    private onErrorCallbacks: Array<(error: string) => void> = [];
    private onStateChangeCallbacks: Array<(state: BackendState) => void> = [];
    private isShuttingDown = false;

    // Configuration per §2ZA(5)(d) and §2ZA(6)(d)
    private readonly MAX_STARTUP_WAIT_MS = 30000; // 30 seconds
    private readonly HEALTH_POLL_INTERVAL_MS = 500; // 500 milliseconds
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
     */
    private async isBackendAlreadyRunning(): Promise<boolean> {
        return this.probeHealth();
    }

    /**
     * Probes the backend health endpoint.
     * Per FrontendWorkflowSpecifications §2ZA(5)(a-b).
     */
    private probeHealth(): Promise<boolean> {
        return new Promise((resolve) => {
            const req = http.get(BACKEND_HEALTH_URL, { timeout: 2000 }, (res) => {
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
     * Waits for the backend to become ready.
     * Per FrontendWorkflowSpecifications §2ZA(5).
     */
    private async waitForReady(): Promise<boolean> {
        const startTime = Date.now();
        
        while (Date.now() - startTime < this.MAX_STARTUP_WAIT_MS) {
            if (await this.probeHealth()) {
                return true;
            }
            await this.sleep(this.HEALTH_POLL_INTERVAL_MS);
        }
        
        return false;
    }

    /**
     * Spawns the backend process.
     * Per FrontendWorkflowSpecifications §2ZA(3)(c).
     */
    private spawnBackend(): void {
        const executablePath = this.getBackendExecutablePath();
        
        // Per §2ZA(3)(c)(ii): Start with --urls argument
        this.process = spawn(executablePath, [`--urls`, `http://localhost:${BACKEND_PORT}`], {
            stdio: ['ignore', 'pipe', 'pipe'],
            detached: false,
            windowsHide: true,
        });

        // Per §2ZA(3)(c)(iii): Capture stdout and stderr for diagnostics
        this.process.stdout?.on('data', (data) => {
            console.log(`[Backend stdout]: ${data.toString().trim()}`);
        });

        this.process.stderr?.on('data', (data) => {
            console.error(`[Backend stderr]: ${data.toString().trim()}`);
        });

        // Per §2ZA(6)(a): Monitor for unexpected termination
        this.process.on('exit', (code, signal) => {
            console.log(`Backend process exited with code ${code}, signal ${signal}`);
            this.process = null;
            
            if (!this.isShuttingDown) {
                this.handleUnexpectedExit();
            }
        });

        this.process.on('error', (err) => {
            console.error(`Backend process error: ${err.message}`);
            this.process = null;
            
            if (!this.isShuttingDown) {
                this.handleUnexpectedExit();
            }
        });
    }

    /**
     * Handles unexpected backend termination.
     * Per FrontendWorkflowSpecifications §2ZA(6)(b).
     */
    private async handleUnexpectedExit(): Promise<void> {
        // Check if we've had stable uptime - reset backoff if so
        // Per §2ZA(6)(b)(iii)
        if (this.lastSuccessfulStart && 
            Date.now() - this.lastSuccessfulStart >= this.STABLE_UPTIME_MS) {
            this.restartAttempts = 0;
            this.restartDelay = 1000;
        }

        this.restartAttempts++;

        // Per §2ZA(6)(d): Check if max attempts exceeded within window
        if (this.restartAttempts > this.MAX_RESTART_ATTEMPTS) {
            this.setState(BackendState.FAILED);
            this.notifyError('Backend failed to start after multiple attempts. Please restart the application.');
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
     * @param checkExisting Whether to check for existing backend process.
     */
    private async startInternal(checkExisting: boolean): Promise<void> {
        this.setState(BackendState.STARTING);

        // Per §2ZA(3)(a-b): Check if backend is already running
        if (checkExisting && await this.isBackendAlreadyRunning()) {
            console.log('Backend already running, skipping spawn');
            this.lastSuccessfulStart = Date.now();
            this.setState(BackendState.RUNNING);
            this.notifyReady();
            return;
        }

        // Per §2ZA(3)(c): Spawn the backend
        this.spawnBackend();

        // Per §2ZA(5): Wait for backend to become ready
        const ready = await this.waitForReady();

        if (ready) {
            this.lastSuccessfulStart = Date.now();
            this.setState(BackendState.RUNNING);
            this.notifyReady();
        } else {
            // Per §2ZA(5)(d): Backend did not become ready in time
            this.killProcess();
            throw new Error('Backend did not become ready within timeout period');
        }
    }

    /**
     * Starts the backend process.
     * Per FrontendWorkflowSpecifications §2ZA(3).
     */
    public async start(): Promise<void> {
        this.isShuttingDown = false;
        this.restartAttempts = 0;
        this.restartDelay = 1000;
        
        await this.startInternal(true);
    }

    /**
     * Stops the backend process gracefully.
     * Per FrontendWorkflowSpecifications §2ZA(7).
     */
    public async stop(): Promise<void> {
        this.isShuttingDown = true;
        
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
