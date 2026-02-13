import * as signalR from "@microsoft/signalr";
import type {
    UnitCollectionEntity,
    UnitEntity,
    LessonEntity,
    MaterialEntity,
    QuestionEntity,
    AttachmentEntity,
    PairedDeviceEntity,
    DeviceStatusEntity,
    ResponseEntity,
    FeedbackEntity,
    InternalCreateUnitCollectionDto,
    InternalCreateUnitDto,
    InternalCreateLessonDto,
    InternalCreateMaterialDto,
    InternalCreateQuestionDto,
    InternalUpdateQuestionDto,
    InternalCreateAttachmentDto,
    InternalCreateFeedbackDto,
} from "../../models";

/**
 * Custom retry policy that retries indefinitely with capped exponential backoff.
 * Per FrontendWorkflowSpecifications §2(4)(a-b).
 */
class InfiniteRetryPolicy implements signalR.IRetryPolicy {
    private readonly retryDelays = [0, 2000, 10000, 30000]; // 0s, 2s, 10s, 30s

    nextRetryDelayInMilliseconds(retryContext: signalR.RetryContext): number | null {
        // Per §2(4)(b): Use delays 0s, 2s, 10s, 30s, then repeat 30s indefinitely
        if (retryContext.previousRetryCount < this.retryDelays.length) {
            return this.retryDelays[retryContext.previousRetryCount];
        }
        // Continue with 30s delay indefinitely
        return 30000;
    }
}

/**
 * Pending subscription entry for deferred handler registration.
 */
interface PendingSubscription {
    methodName: string;
    callback: (...args: unknown[]) => void;
}

/**
 * SignalR service for communication with Main backend.
 * Per NetworkingAPISpec §1(1).
 */
class SignalRService {
    private connection: signalR.HubConnection | null = null;
    private connectionStateCallbacks: Array<(state: signalR.HubConnectionState) => void> = [];
    private reconnectedCallbacks: Array<() => void> = [];
    private pendingSubscriptions: PendingSubscription[] = [];
    private initialized = false;

    /**
     * Initializes the SignalR connection with the dynamically determined backend port.
     * Per FrontendWorkflowSpecifications §2ZA(8)(c)(iv): Use port from IPC.
     */
    public async initialize(): Promise<void> {
        if (this.initialized && this.connection) {
            return;
        }

        // Get the active backend port from main process
        const port = await window.electronAPI.getBackendPort();
        const hubUrl = `http://localhost:${port}/TeacherPortalHub`;
        console.log(`SignalR initializing with hub URL: ${hubUrl}`);

        // Per FrontendWorkflowSpecifications §2(4)(a): Custom retry policy that retries indefinitely
        this.connection = new signalR.HubConnectionBuilder()
            .withUrl(hubUrl)
            .withAutomaticReconnect(new InfiniteRetryPolicy())
            .configureLogging(signalR.LogLevel.Information)
            .build();

        // Per §2(4)(c): Track connection state changes
        this.connection.onreconnecting((error) => {
            console.log("SignalR reconnecting:", error);
            this.notifyConnectionStateChange(signalR.HubConnectionState.Reconnecting);
        });

        this.connection.onreconnected((connectionId) => {
            console.log("SignalR reconnected:", connectionId);
            this.notifyConnectionStateChange(signalR.HubConnectionState.Connected);
            // Per §2(4)(c): Notify listeners to re-fetch all entities
            this.notifyReconnected();
        });

        this.connection.onclose((error) => {
            console.log("SignalR connection closed:", error);
            this.notifyConnectionStateChange(signalR.HubConnectionState.Disconnected);
        });

        // Apply any pending subscriptions that were registered before initialization
        this.applyPendingSubscriptions();

        this.initialized = true;
    }

    /**
     * Applies pending subscriptions to the connection.
     * Called after the connection is created.
     */
    private applyPendingSubscriptions(): void {
        if (!this.connection) return;
        
        for (const sub of this.pendingSubscriptions) {
            this.connection.on(sub.methodName, sub.callback);
        }
        console.log(`Applied ${this.pendingSubscriptions.length} pending SignalR subscriptions`);
    }

    /**
     * Subscribes to a hub method, deferring if connection not yet initialized.
     * Returns an unsubscribe function.
     */
    private subscribe<T extends (...args: unknown[]) => void>(methodName: string, callback: T): () => void {
        if (this.connection) {
            // Connection exists, subscribe directly
            this.connection.on(methodName, callback);
            return () => this.connection?.off(methodName, callback);
        } else {
            // Connection not yet initialized, queue the subscription
            const pending: PendingSubscription = { methodName, callback };
            this.pendingSubscriptions.push(pending);
            return () => {
                // Remove from pending if not yet applied
                const index = this.pendingSubscriptions.indexOf(pending);
                if (index > -1) {
                    this.pendingSubscriptions.splice(index, 1);
                }
                // Also remove from connection in case it was applied
                this.connection?.off(methodName, callback);
            };
        }
    }

    /**
     * Ensures the connection is initialized before use.
     */
    private async ensureInitialized(): Promise<signalR.HubConnection> {
        if (!this.connection) {
            await this.initialize();
        }
        return this.connection!;
    }

    /**
     * Starts the SignalR connection with automatic retries.
     * Returns a promise that resolves when connection is established.
     */
    public async startConnection(maxRetries = 5, retryDelayMs = 2000): Promise<void> {
        const connection = await this.ensureInitialized();
        
        for (let attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                if (connection.state === signalR.HubConnectionState.Connected) {
                    return; // Already connected
                }
                if (connection.state !== signalR.HubConnectionState.Disconnected) {
                    // Wait a bit if in transitional state
                    await new Promise(resolve => setTimeout(resolve, 500));
                    continue;
                }
                await connection.start();
                console.log("SignalR Connection Started.");
                this.notifyConnectionStateChange(signalR.HubConnectionState.Connected);
                return; // Success
            } catch (err) {
                console.error(`SignalR connection attempt ${attempt}/${maxRetries} failed:`, err);
                if (attempt < maxRetries) {
                    await new Promise(resolve => setTimeout(resolve, retryDelayMs));
                }
            }
        }
        throw new Error("Failed to establish connection after multiple attempts.");
    }

    public stopConnection(): void {
        if (this.connection && this.connection.state !== signalR.HubConnectionState.Disconnected) {
            this.connection.stop().then(() => console.log("SignalR Connection Stopped."));
        }
    }

    public getConnectionState(): signalR.HubConnectionState {
        if (!this.connection) {
            return signalR.HubConnectionState.Disconnected;
        }
        return this.connection.state;
    }

    /**
     * Subscribe to connection state changes.
     * Per FrontendWorkflowSpecifications §2(4)(c).
     */
    public onConnectionStateChange(callback: (state: signalR.HubConnectionState) => void): () => void {
        this.connectionStateCallbacks.push(callback);
        return () => {
            const index = this.connectionStateCallbacks.indexOf(callback);
            if (index > -1) {
                this.connectionStateCallbacks.splice(index, 1);
            }
        };
    }

    private notifyConnectionStateChange(state: signalR.HubConnectionState): void {
        this.connectionStateCallbacks.forEach(cb => cb(state));
    }

    /**
     * Subscribe to reconnection events.
     * Per FrontendWorkflowSpecifications §2(4)(c): Re-fetch all entities when connection is restored.
     */
    public onReconnected(callback: () => void): () => void {
        this.reconnectedCallbacks.push(callback);
        return () => {
            const index = this.reconnectedCallbacks.indexOf(callback);
            if (index > -1) {
                this.reconnectedCallbacks.splice(index, 1);
            }
        };
    }

    private notifyReconnected(): void {
        console.log("SignalR reconnected - notifying listeners to refetch entities per §2(4)(c)");
        this.reconnectedCallbacks.forEach(cb => cb());
    }

    // Generic handler registration
    // Note: These methods assume startConnection() has been called
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    public on(methodName: string, newMethod: (...args: any[]) => void): void {
        if (!this.connection) {
            throw new Error("SignalR connection not initialized. Call startConnection() first.");
        }
        this.connection.on(methodName, newMethod);
    }

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    public off(methodName: string, method: (...args: any[]) => void): void {
        if (!this.connection) {
            return; // Silently ignore if not initialized
        }
        this.connection.off(methodName, method);
    }

    /**
     * Helper to get the connection for invoke calls.
     * Throws if not initialized.
     */
    private getConnection(): signalR.HubConnection {
        if (!this.connection) {
            throw new Error("SignalR connection not initialized. Call startConnection() first.");
        }
        return this.connection;
    }

    // ==========================================
    // Unit Collection CRUD - NetworkingAPISpec §1(1)(a)
    // ==========================================

    public async createUnitCollection(dto: InternalCreateUnitCollectionDto): Promise<UnitCollectionEntity> {
        return await this.getConnection().invoke<UnitCollectionEntity>("CreateUnitCollection", dto);
    }

    public async getAllUnitCollections(): Promise<UnitCollectionEntity[]> {
        return await this.getConnection().invoke<UnitCollectionEntity[]>("GetAllUnitCollections");
    }

    public async updateUnitCollection(entity: UnitCollectionEntity): Promise<void> {
        await this.getConnection().invoke("UpdateUnitCollection", entity);
    }

    public async deleteUnitCollection(id: string): Promise<void> {
        await this.getConnection().invoke("DeleteUnitCollection", id);
    }

    // ==========================================
    // Unit CRUD - NetworkingAPISpec §1(1)(b)
    // ==========================================

    public async createUnit(dto: InternalCreateUnitDto): Promise<UnitEntity> {
        return await this.getConnection().invoke<UnitEntity>("CreateUnit", dto);
    }

    public async getAllUnits(): Promise<UnitEntity[]> {
        return await this.getConnection().invoke<UnitEntity[]>("GetAllUnits");
    }

    public async updateUnit(entity: UnitEntity): Promise<void> {
        await this.getConnection().invoke("UpdateUnit", entity);
    }

    public async deleteUnit(id: string): Promise<void> {
        await this.getConnection().invoke("DeleteUnit", id);
    }

    // ==========================================
    // Lesson CRUD - NetworkingAPISpec §1(1)(c)
    // ==========================================

    public async createLesson(dto: InternalCreateLessonDto): Promise<LessonEntity> {
        return await this.getConnection().invoke<LessonEntity>("CreateLesson", dto);
    }

    public async getAllLessons(): Promise<LessonEntity[]> {
        return await this.getConnection().invoke<LessonEntity[]>("GetAllLessons");
    }

    public async updateLesson(entity: LessonEntity): Promise<void> {
        await this.getConnection().invoke("UpdateLesson", entity);
    }

    public async deleteLesson(id: string): Promise<void> {
        await this.getConnection().invoke("DeleteLesson", id);
    }

    // ==========================================
    // Material CRUD - NetworkingAPISpec §1(1)(d)
    // ==========================================

    public async createMaterial(dto: InternalCreateMaterialDto): Promise<MaterialEntity> {
        return await this.getConnection().invoke<MaterialEntity>("CreateMaterial", dto);
    }

    public async getAllMaterials(): Promise<MaterialEntity[]> {
        return await this.getConnection().invoke<MaterialEntity[]>("GetAllMaterials");
    }

    public async updateMaterial(entity: MaterialEntity): Promise<void> {
        await this.getConnection().invoke("UpdateMaterial", entity);
    }

    public async deleteMaterial(id: string): Promise<void> {
        await this.getConnection().invoke("DeleteMaterial", id);
    }

    /**
     * Generates a PDF document for the specified material.
     * Per NetworkingAPISpec §1(1)(m)(i).
     * @returns Decoded PDF file bytes as a Uint8Array.
     */
    public async generateMaterialPdf(materialId: string): Promise<Uint8Array> {
        // SignalR returns byte[] as base64 string
        const base64 = await this.connection.invoke<string>("GenerateMaterialPdf", materialId);
        // Decode base64 to Uint8Array
        const binaryString = atob(base64);
        const bytes = new Uint8Array(binaryString.length);
        for (let i = 0; i < binaryString.length; i++) {
            bytes[i] = binaryString.charCodeAt(i);
        }
        return bytes;
    }

    // ==========================================
    // Question CRUD - NetworkingAPISpec §1(1)(d1)
    // ==========================================

    /**
     * Creates a new question and returns its assigned ID.
     * Per NetworkingAPISpec §1(1)(d1)(i).
     */
    public async createQuestion(dto: InternalCreateQuestionDto): Promise<string> {
        return await this.getConnection().invoke<string>("CreateQuestion", dto);
    }

    /**
     * Retrieves all questions under a material.
     * Per NetworkingAPISpec §1(1)(d1)(ii).
     */
    public async getQuestionsUnderMaterial(materialId: string): Promise<QuestionEntity[]> {
        return await this.getConnection().invoke<QuestionEntity[]>("GetQuestionsUnderMaterial", materialId);
    }

    /**
     * Updates a question entity.
     * Per NetworkingAPISpec §1(1)(d1)(iii).
     */
    public async updateQuestion(dto: InternalUpdateQuestionDto): Promise<void> {
        await this.getConnection().invoke("UpdateQuestion", dto);
    }

    /**
     * Deletes a question by ID.
     * Per NetworkingAPISpec §1(1)(d1)(iv).
     */
    public async deleteQuestion(id: string): Promise<void> {
        await this.getConnection().invoke("DeleteQuestion", id);
    }

    // ==========================================
    // Classroom Methods - FrontendWorkflowSpec §5
    // ==========================================

    /**
     * Initiates device pairing by starting UDP broadcast and TCP listener.
     * Per FrontendWorkflowSpec §5A(2)(a).
     */
    public async pairDevices(): Promise<void> {
        await this.getConnection().invoke("PairDevices");
    }

    /**
     * Stops the pairing process.
     * Per FrontendWorkflowSpec §5A(4)(a).
     */
    public async stopPairing(): Promise<void> {
        await this.getConnection().invoke("StopPairing");
    }

    /**
     * Retrieves all paired devices.
     * Per FrontendWorkflowSpec §5(2)(i).
     */
    public async getAllPairedDevices(): Promise<PairedDeviceEntity[]> {
        return await this.getConnection().invoke<PairedDeviceEntity[]>("GetAllPairedDevices");
    }

    /**
     * Retrieves all device statuses.
     * Per FrontendWorkflowSpec §5(2)(ii).
     */
    public async getAllDeviceStatuses(): Promise<DeviceStatusEntity[]> {
        return await this.getConnection().invoke<DeviceStatusEntity[]>("GetAllDeviceStatuses");
    }

    /**
     * Locks selected devices.
     * Per FrontendWorkflowSpec §5C(2).
     */
    public async lockDevices(deviceIds: string[]): Promise<void> {
        await this.getConnection().invoke("LockDevices", deviceIds);
    }

    /**
     * Unlocks selected devices.
     * Per FrontendWorkflowSpec §5C(2).
     */
    public async unlockDevices(deviceIds: string[]): Promise<void> {
        await this.getConnection().invoke("UnlockDevices", deviceIds);
    }

    /**
     * Deploys material to selected devices.
     * Per FrontendWorkflowSpec §5C(3)(b).
     */
    public async deployMaterial(materialId: string, deviceIds: string[]): Promise<void> {
        await this.getConnection().invoke("DeployMaterial", materialId, deviceIds);
    }

    /**
     * Unpairs selected devices.
     * Per FrontendWorkflowSpec §5A(5).
     */
    public async unpairDevices(deviceIds: string[]): Promise<void> {
        await this.getConnection().invoke("UnpairDevices", deviceIds);
    }

    /**
     * Updates a paired device (e.g., rename).
     * Per FrontendWorkflowSpec §5B(4).
     */
    public async updatePairedDevice(entity: PairedDeviceEntity): Promise<void> {
        await this.getConnection().invoke("UpdatePairedDevice", entity);
    }

    // ==========================================
    // Client Handler Subscriptions - NetworkingAPISpec §2(1)
    // ==========================================

    /**
     * Subscribe to device status updates.
     * Per NetworkingAPISpec §2(1)(a).
     */
    public onDeviceStatusUpdate(callback: (status: DeviceStatusEntity) => void): () => void {
        return this.subscribe("UpdateDeviceStatus", callback as (...args: unknown[]) => void);
    }

    /**
     * Subscribe to new device paired events.
     * Per NetworkingAPISpec §2(1)(b).
     */
    public onDevicePaired(callback: (device: PairedDeviceEntity) => void): () => void {
        return this.subscribe("DevicePaired", callback as (...args: unknown[]) => void);
    }

    /**
     * Subscribe to hand raised events.
     * Per NetworkingAPISpec §2(1)(d)(i).
     */
    public onHandRaised(callback: (deviceId: string) => void): () => void {
        return this.subscribe("HandRaised", callback as (...args: unknown[]) => void);
    }

    /**
     * Subscribe to distribution failure events.
     * Per NetworkingAPISpec §2(1)(d)(ii).
     * Payload includes deviceId and materialId per API Contract §3.6.2.
     */
    public onDistributionFailed(callback: (deviceId: string) => void): () => void {
        return this.subscribe("DistributionFailed", callback as (...args: unknown[]) => void);
    }

    /**
     * Subscribe to remote control failure events.
     * Per NetworkingAPISpec §2(1)(d)(iii).
     */
    public onRemoteControlFailed(callback: (payload: { deviceId: string; command: string }) => void): () => void {
        return this.subscribe("RemoteControlFailed", callback as (...args: unknown[]) => void);
    }

    /**
     * Subscribe to feedback delivery failure events.
     * Per NetworkingAPISpec §2(1)(e)(v).
     * Payload includes deviceId and feedbackId per API Contract §3.6.2.
     */
    public onFeedbackDeliveryFailed(callback: (deviceId: string) => void): () => void {
        return this.subscribe("FeedbackDeliveryFailed", callback as (...args: unknown[]) => void);
    }

    // ==========================================
    // Attachment CRUD - NetworkingAPISpec §1(1)(l)
    // ==========================================

    /**
     * Creates a new attachment and returns its assigned ID.
     * Per NetworkingAPISpec §1(1)(l)(i).
     */
    public async createAttachment(dto: InternalCreateAttachmentDto): Promise<string> {
        return await this.getConnection().invoke<string>("CreateAttachment", dto);
    }

    /**
     * Retrieves all attachments under a material.
     * Per NetworkingAPISpec §1(1)(l)(ii).
     */
    public async getAttachmentsUnderMaterial(materialId: string): Promise<AttachmentEntity[]> {
        return await this.getConnection().invoke<AttachmentEntity[]>("GetAttachmentsUnderMaterial", materialId);
    }

    /**
     * Deletes an attachment by ID.
     * Per NetworkingAPISpec §1(1)(l)(iii).
     */
    public async deleteAttachment(id: string): Promise<void> {
        await this.getConnection().invoke("DeleteAttachment", id);
    }

    // ==========================================
    // Response Methods - NetworkingAPISpec §1(1)(i)
    // ==========================================

    /**
     * Retrieves all responses.
     * Per NetworkingAPISpec §1(1)(i)(i).
     */
    public async getAllResponses(): Promise<ResponseEntity[]> {
        return await this.getConnection().invoke<ResponseEntity[]>("GetAllResponses");
    }

    /**
     * Retrieves all responses under a question.
     * Per NetworkingAPISpec §1(1)(i)(ii).
     */
    public async getResponsesUnderQuestion(questionId: string): Promise<ResponseEntity[]> {
        return await this.getConnection().invoke<ResponseEntity[]>("GetResponsesUnderQuestion", questionId);
    }

    // ==========================================
    // Feedback Methods - NetworkingAPISpec §1(1)(h)
    // ==========================================

    /**
     * Retrieves all feedbacks.
     * Per NetworkingAPISpec §1(1)(h)(iv).
     */
    public async getAllFeedbacks(): Promise<FeedbackEntity[]> {
        return await this.getConnection().invoke<FeedbackEntity[]>("GetAllFeedbacks");
    }

    /**
     * Creates a new feedback entity.
     * Per NetworkingAPISpec §1(1)(h)(i).
     */
    public async createFeedback(dto: InternalCreateFeedbackDto): Promise<FeedbackEntity> {
        return await this.getConnection().invoke<FeedbackEntity>("CreateFeedback", dto);
    }

    /**
     * Updates an existing feedback entity.
     * Per NetworkingAPISpec §1(1)(h)(v).
     * Only PROVISIONAL feedback can be updated per §6A(7)(b)(ii).
     */
    public async updateFeedback(entity: FeedbackEntity): Promise<void> {
        await this.getConnection().invoke("UpdateFeedback", entity);
    }

    /**
     * Deletes an existing feedback entity.
     * Per NetworkingAPISpec §1(1)(h)(vi) and FrontendWorkflowSpecifications §6A(7)(a)(ii).
     * Invoked when teacher clears both Text and Marks on a PROVISIONAL feedback.
     */
    public async deleteFeedback(feedbackId: string): Promise<void> {
        await this.getConnection().invoke("DeleteFeedback", feedbackId);
    }

    /**
     * Approves feedback for dispatch.
     * Per NetworkingAPISpec §1(1)(h)(ii).
     */
    public async approveFeedback(feedbackId: string): Promise<void> {
        await this.getConnection().invoke("ApproveFeedback", feedbackId);
    }

    /**
     * Retries dispatch of feedback.
     * Per NetworkingAPISpec §1(1)(h)(iii).
     */
    public async retryFeedbackDispatch(feedbackId: string): Promise<void> {
        await this.getConnection().invoke("RetryFeedbackDispatch", feedbackId);
    }

    /**
     * Subscribe to response refresh events.
     * Per NetworkingAPISpec §2(1)(e)(i).
     */
    public onRefreshResponses(callback: () => void): () => void {
        return this.subscribe("RefreshResponses", callback as (...args: unknown[]) => void);
    }

    /**
     * Subscribe to feedback dispatch failure events.
     * Per NetworkingAPISpec §2(1)(c)(i).
     */
    public onFeedbackDispatchFailed(callback: (feedbackId: string, deviceId: string) => void): () => void {
        return this.subscribe("FeedbackDeliveryFailed", callback as (...args: unknown[]) => void);
    }
}

const signalRService = new SignalRService();
export default signalRService;

