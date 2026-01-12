import * as signalR from "@microsoft/signalr";
import type {
    UnitCollectionEntity,
    UnitEntity,
    LessonEntity,
    MaterialEntity,
    QuestionEntity,
    AttachmentEntity,
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
    // Classroom Methods - Frontend Workflow Spec §5
    // (Stub implementations - backend not yet implemented)
    // ==========================================

    public async pairDevices(): Promise<void> {
        // TODO: Implement when backend PairDevices method is available
        console.warn('pairDevices: Not yet implemented');
        await this.connection.invoke("PairDevices");
    }

    public async getAllPairedDevices(): Promise<PairedDeviceEntity[]> {
        // TODO: Implement when backend is available
        console.warn('getAllPairedDevices: Not yet implemented');
        return await this.connection.invoke<PairedDeviceEntity[]>("GetAllPairedDevices");
    }

    public async getAllDeviceStatuses(): Promise<DeviceStatusEntity[]> {
        // TODO: Implement when backend is available
        console.warn('getAllDeviceStatuses: Not yet implemented');
        return await this.connection.invoke<DeviceStatusEntity[]>("GetAllDeviceStatuses");
    }

    public async lockDevices(deviceIds: string[]): Promise<void> {
        // TODO: Implement when backend is available
        console.warn('lockDevices: Not yet implemented');
        await this.connection.invoke("LockDevices", deviceIds);
    }

    public async unlockDevices(deviceIds: string[]): Promise<void> {
        // TODO: Implement when backend is available
        console.warn('unlockDevices: Not yet implemented');
        await this.connection.invoke("UnlockDevices", deviceIds);
    }

    public async deployMaterial(materialId: string): Promise<void> {
        // TODO: Implement when backend is available
        console.warn('deployMaterial: Not yet implemented');
        await this.connection.invoke("DeployMaterial", materialId);
    }

    public async finishMaterial(materialId: string): Promise<void> {
        // TODO: Implement when backend is available
        console.warn('finishMaterial: Not yet implemented');
        await this.connection.invoke("FinishMaterial", materialId);
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

// Stub types for classroom functionality (to be moved to models when implemented)
interface PairedDeviceEntity {
    id: string;
    deviceId: string;
    deviceName?: string;
}

interface DeviceStatusEntity {
    deviceId: string;
    status: 'ON_TASK' | 'IDLE' | 'LOCKED' | 'DISCONNECTED';
    batteryLevel?: number;
}

const signalRService = new SignalRService();
export default signalRService;
