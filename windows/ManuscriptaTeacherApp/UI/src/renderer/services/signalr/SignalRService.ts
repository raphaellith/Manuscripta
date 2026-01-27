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
    InternalCreateUnitCollectionDto,
    InternalCreateUnitDto,
    InternalCreateLessonDto,
    InternalCreateMaterialDto,
    InternalCreateQuestionDto,
    InternalUpdateQuestionDto,
    InternalCreateAttachmentDto,
} from "../../models";

/**
 * SignalR service for communication with Main backend.
 * Per NetworkingAPISpec §1(1).
 */
class SignalRService {
    private connection: signalR.HubConnection;

    constructor() {
        this.connection = new signalR.HubConnectionBuilder()
            .withUrl("http://localhost:5910/hub")
            .withAutomaticReconnect()
            .configureLogging(signalR.LogLevel.Information)
            .build();
    }

    /**
     * Starts the SignalR connection with automatic retries.
     * Returns a promise that resolves when connection is established.
     */
    public async startConnection(maxRetries = 5, retryDelayMs = 2000): Promise<void> {
        for (let attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                if (this.connection.state === signalR.HubConnectionState.Connected) {
                    return; // Already connected
                }
                if (this.connection.state !== signalR.HubConnectionState.Disconnected) {
                    // Wait a bit if in transitional state
                    await new Promise(resolve => setTimeout(resolve, 500));
                    continue;
                }
                await this.connection.start();
                console.log("SignalR Connection Started.");
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
        if (this.connection.state !== signalR.HubConnectionState.Disconnected) {
            this.connection.stop().then(() => console.log("SignalR Connection Stopped."));
        }
    }

    public getConnectionState(): signalR.HubConnectionState {
        return this.connection.state;
    }

    // Generic handler registration
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    public on(methodName: string, newMethod: (...args: any[]) => void): void {
        this.connection.on(methodName, newMethod);
    }

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    public off(methodName: string, method: (...args: any[]) => void): void {
        this.connection.off(methodName, method);
    }

    // ==========================================
    // Unit Collection CRUD - NetworkingAPISpec §1(1)(a)
    // ==========================================

    public async createUnitCollection(dto: InternalCreateUnitCollectionDto): Promise<UnitCollectionEntity> {
        return await this.connection.invoke<UnitCollectionEntity>("CreateUnitCollection", dto);
    }

    public async getAllUnitCollections(): Promise<UnitCollectionEntity[]> {
        return await this.connection.invoke<UnitCollectionEntity[]>("GetAllUnitCollections");
    }

    public async updateUnitCollection(entity: UnitCollectionEntity): Promise<void> {
        await this.connection.invoke("UpdateUnitCollection", entity);
    }

    public async deleteUnitCollection(id: string): Promise<void> {
        await this.connection.invoke("DeleteUnitCollection", id);
    }

    // ==========================================
    // Unit CRUD - NetworkingAPISpec §1(1)(b)
    // ==========================================

    public async createUnit(dto: InternalCreateUnitDto): Promise<UnitEntity> {
        return await this.connection.invoke<UnitEntity>("CreateUnit", dto);
    }

    public async getAllUnits(): Promise<UnitEntity[]> {
        return await this.connection.invoke<UnitEntity[]>("GetAllUnits");
    }

    public async updateUnit(entity: UnitEntity): Promise<void> {
        await this.connection.invoke("UpdateUnit", entity);
    }

    public async deleteUnit(id: string): Promise<void> {
        await this.connection.invoke("DeleteUnit", id);
    }

    // ==========================================
    // Lesson CRUD - NetworkingAPISpec §1(1)(c)
    // ==========================================

    public async createLesson(dto: InternalCreateLessonDto): Promise<LessonEntity> {
        return await this.connection.invoke<LessonEntity>("CreateLesson", dto);
    }

    public async getAllLessons(): Promise<LessonEntity[]> {
        return await this.connection.invoke<LessonEntity[]>("GetAllLessons");
    }

    public async updateLesson(entity: LessonEntity): Promise<void> {
        await this.connection.invoke("UpdateLesson", entity);
    }

    public async deleteLesson(id: string): Promise<void> {
        await this.connection.invoke("DeleteLesson", id);
    }

    // ==========================================
    // Material CRUD - NetworkingAPISpec §1(1)(d)
    // ==========================================

    public async createMaterial(dto: InternalCreateMaterialDto): Promise<MaterialEntity> {
        return await this.connection.invoke<MaterialEntity>("CreateMaterial", dto);
    }

    public async getAllMaterials(): Promise<MaterialEntity[]> {
        return await this.connection.invoke<MaterialEntity[]>("GetAllMaterials");
    }

    public async updateMaterial(entity: MaterialEntity): Promise<void> {
        await this.connection.invoke("UpdateMaterial", entity);
    }

    public async deleteMaterial(id: string): Promise<void> {
        await this.connection.invoke("DeleteMaterial", id);
    }

    // ==========================================
    // Question CRUD - NetworkingAPISpec §1(1)(d1)
    // ==========================================

    /**
     * Creates a new question and returns its assigned ID.
     * Per NetworkingAPISpec §1(1)(d1)(i).
     */
    public async createQuestion(dto: InternalCreateQuestionDto): Promise<string> {
        return await this.connection.invoke<string>("CreateQuestion", dto);
    }

    /**
     * Retrieves all questions under a material.
     * Per NetworkingAPISpec §1(1)(d1)(ii).
     */
    public async getQuestionsUnderMaterial(materialId: string): Promise<QuestionEntity[]> {
        return await this.connection.invoke<QuestionEntity[]>("GetQuestionsUnderMaterial", materialId);
    }

    /**
     * Updates a question entity.
     * Per NetworkingAPISpec §1(1)(d1)(iii).
     */
    public async updateQuestion(dto: InternalUpdateQuestionDto): Promise<void> {
        await this.connection.invoke("UpdateQuestion", dto);
    }

    /**
     * Deletes a question by ID.
     * Per NetworkingAPISpec §1(1)(d1)(iv).
     */
    public async deleteQuestion(id: string): Promise<void> {
        await this.connection.invoke("DeleteQuestion", id);
    }

    // ==========================================
    // Classroom Methods - FrontendWorkflowSpec §5
    // ==========================================

    /**
     * Initiates device pairing by starting UDP broadcast and TCP listener.
     * Per FrontendWorkflowSpec §5A(2)(a).
     */
    public async pairDevices(): Promise<void> {
        await this.connection.invoke("PairDevices");
    }

    /**
     * Stops the pairing process.
     * Per FrontendWorkflowSpec §5A(4)(a).
     */
    public async stopPairing(): Promise<void> {
        await this.connection.invoke("StopPairing");
    }

    /**
     * Retrieves all paired devices.
     * Per FrontendWorkflowSpec §5(2)(i).
     */
    public async getAllPairedDevices(): Promise<PairedDeviceEntity[]> {
        return await this.connection.invoke<PairedDeviceEntity[]>("GetAllPairedDevices");
    }

    /**
     * Retrieves all device statuses.
     * Per FrontendWorkflowSpec §5(2)(ii).
     */
    public async getAllDeviceStatuses(): Promise<DeviceStatusEntity[]> {
        return await this.connection.invoke<DeviceStatusEntity[]>("GetAllDeviceStatuses");
    }

    /**
     * Locks selected devices.
     * Per FrontendWorkflowSpec §5C(2).
     */
    public async lockDevices(deviceIds: string[]): Promise<void> {
        await this.connection.invoke("LockDevices", deviceIds);
    }

    /**
     * Unlocks selected devices.
     * Per FrontendWorkflowSpec §5C(2).
     */
    public async unlockDevices(deviceIds: string[]): Promise<void> {
        await this.connection.invoke("UnlockDevices", deviceIds);
    }

    /**
     * Deploys material to selected devices.
     * Per FrontendWorkflowSpec §5C(3)(b).
     */
    public async deployMaterial(materialId: string, deviceIds: string[]): Promise<void> {
        await this.connection.invoke("DeployMaterial", materialId, deviceIds);
    }

    /**
     * Unpairs selected devices.
     * Per FrontendWorkflowSpec §5A(5).
     */
    public async unpairDevices(deviceIds: string[]): Promise<void> {
        await this.connection.invoke("UnpairDevices", deviceIds);
    }

    /**
     * Updates a paired device (e.g., rename).
     * Per FrontendWorkflowSpec §5B(4).
     */
    public async updatePairedDevice(entity: PairedDeviceEntity): Promise<void> {
        await this.connection.invoke("UpdatePairedDevice", entity);
    }

    // ==========================================
    // Client Handler Subscriptions - NetworkingAPISpec §2(1)
    // ==========================================

    /**
     * Subscribe to device status updates.
     * Per NetworkingAPISpec §2(1)(a).
     */
    public onDeviceStatusUpdate(callback: (status: DeviceStatusEntity) => void): () => void {
        this.connection.on("UpdateDeviceStatus", callback);
        return () => this.connection.off("UpdateDeviceStatus", callback);
    }

    /**
     * Subscribe to new device paired events.
     * Per NetworkingAPISpec §2(1)(b).
     */
    public onDevicePaired(callback: (device: PairedDeviceEntity) => void): () => void {
        this.connection.on("DevicePaired", callback);
        return () => this.connection.off("DevicePaired", callback);
    }

    /**
     * Subscribe to hand raised events.
     * Per NetworkingAPISpec §2(1)(d)(i).
     */
    public onHandRaised(callback: (deviceId: string) => void): () => void {
        this.connection.on("HandRaised", callback);
        return () => this.connection.off("HandRaised", callback);
    }

    /**
     * Subscribe to distribution failure events.
     * Per NetworkingAPISpec §2(1)(d)(ii).
     */
    public onDistributionFailed(callback: (deviceId: string) => void): () => void {
        this.connection.on("DistributionFailed", callback);
        return () => this.connection.off("DistributionFailed", callback);
    }

    /**
     * Subscribe to remote control failure events.
     * Per NetworkingAPISpec §2(1)(d)(iii).
     */
    public onRemoteControlFailed(callback: (payload: { deviceId: string; command: string }) => void): () => void {
        this.connection.on("RemoteControlFailed", callback);
        return () => this.connection.off("RemoteControlFailed", callback);
    }

    /**
     * Subscribe to feedback delivery failure events.
     * Per NetworkingAPISpec §2(1)(d)(v).
     */
    public onFeedbackDeliveryFailed(callback: (deviceId: string) => void): () => void {
        this.connection.on("FeedbackDeliveryFailed", callback);
        return () => this.connection.off("FeedbackDeliveryFailed", callback);
    }

    // ==========================================
    // Attachment CRUD - NetworkingAPISpec §1(1)(l)
    // ==========================================

    /**
     * Creates a new attachment and returns its assigned ID.
     * Per NetworkingAPISpec §1(1)(l)(i).
     */
    public async createAttachment(dto: InternalCreateAttachmentDto): Promise<string> {
        return await this.connection.invoke<string>("CreateAttachment", dto);
    }

    /**
     * Retrieves all attachments under a material.
     * Per NetworkingAPISpec §1(1)(l)(ii).
     */
    public async getAttachmentsUnderMaterial(materialId: string): Promise<AttachmentEntity[]> {
        return await this.connection.invoke<AttachmentEntity[]>("GetAttachmentsUnderMaterial", materialId);
    }

    /**
     * Deletes an attachment by ID.
     * Per NetworkingAPISpec §1(1)(l)(iii).
     */
    public async deleteAttachment(id: string): Promise<void> {
        await this.connection.invoke("DeleteAttachment", id);
    }
}

const signalRService = new SignalRService();
export default signalRService;
