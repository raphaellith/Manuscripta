import * as signalR from "@microsoft/signalr";
import type {
    UnitCollectionEntity,
    UnitEntity,
    LessonEntity,
    MaterialEntity,
    InternalCreateUnitCollectionDto,
    InternalCreateUnitDto,
    InternalCreateLessonDto,
    InternalCreateMaterialDto,
} from "../../models";

/**
 * SignalR service for communication with Main backend.
 * Per NetworkingAPISpec §1(1).
 */
class SignalRService {
    private connection: signalR.HubConnection;

    constructor() {
        this.connection = new signalR.HubConnectionBuilder()
            .withUrl("http://localhost:5913/hub")
            .withAutomaticReconnect()
            .configureLogging(signalR.LogLevel.Information)
            .build();
    }

    public async startConnection(): Promise<void> {
        try {
            if (this.connection.state === signalR.HubConnectionState.Disconnected) {
                await this.connection.start();
                console.log("SignalR Connection Started.");
            }
        } catch (err) {
            console.error("Error while starting SignalR connection: " + err);
            setTimeout(() => this.startConnection(), 5000);
        }
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
}

const signalRService = new SignalRService();
export default signalRService;
