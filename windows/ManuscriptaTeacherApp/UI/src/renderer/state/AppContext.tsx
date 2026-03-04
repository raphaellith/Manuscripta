/**
 * Application state context for SignalR data.
 * Per WindowsAppStructureSpec §2B(1)(d)(iv).
 */

import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import signalRService from '../services/signalr/SignalRService';
import type {
    UnitCollectionEntity,
    UnitEntity,
    LessonEntity,
    MaterialEntity,
    QuestionEntity,
    InternalCreateUnitCollectionDto,
    InternalCreateUnitDto,
    InternalCreateLessonDto,
    InternalCreateMaterialDto,
    GenerationRequest,
    GenerationResult,
} from '../models';

/**
 * Backend process state for lifecycle management.
 * Per FrontendWorkflowSpecifications §2ZA(6)(c).
 */
export type BackendProcessState = 'connected' | 'reconnecting';

interface AppState {
    isConnected: boolean;
    isLoading: boolean;
    error: string | null;
    backendState: BackendProcessState;
    unitCollections: UnitCollectionEntity[];
    units: UnitEntity[];
    lessons: LessonEntity[];
    materials: MaterialEntity[];
    questions: QuestionEntity[];
}

interface AppContextValue extends AppState {
    // CRUD operations
    createUnitCollection: (dto: InternalCreateUnitCollectionDto) => Promise<UnitCollectionEntity>;
    updateUnitCollection: (entity: UnitCollectionEntity) => Promise<void>;
    deleteUnitCollection: (id: string) => Promise<void>;

    createUnit: (dto: InternalCreateUnitDto) => Promise<UnitEntity>;
    updateUnit: (entity: UnitEntity) => Promise<void>;
    deleteUnit: (id: string) => Promise<void>;

    createLesson: (dto: InternalCreateLessonDto) => Promise<LessonEntity>;
    updateLesson: (entity: LessonEntity) => Promise<void>;
    deleteLesson: (id: string) => Promise<void>;

    createMaterial: (dto: InternalCreateMaterialDto) => Promise<MaterialEntity>;
    updateMaterial: (entity: MaterialEntity) => Promise<MaterialEntity>;
    deleteMaterial: (id: string) => Promise<void>;

    // AI Generation - NetworkingAPISpec §1(1)(i)(i-ii) and (x)
    // Server generates unique ID and sends via OnGenerationStarted event
    generateReading: (request: GenerationRequest) => Promise<GenerationResult>;
    generateWorksheet: (request: GenerationRequest) => Promise<GenerationResult>;
    cancelGeneration: (generationId: string) => Promise<boolean>;

    // Helpers
    getUnitsForCollection: (collectionId: string) => UnitEntity[];
    getLessonsForUnit: (unitId: string) => LessonEntity[];
    getMaterialsForLesson: (lessonId: string) => MaterialEntity[];
    getQuestionsForMaterial: (materialId: string) => QuestionEntity[];

    refreshData: () => Promise<void>;
}

const AppContext = createContext<AppContextValue | null>(null);

export const useAppContext = () => {
    const context = useContext(AppContext);
    if (!context) {
        throw new Error('useAppContext must be used within AppProvider');
    }
    return context;
};

interface AppProviderProps {
    children: ReactNode;
}

export const AppProvider: React.FC<AppProviderProps> = ({ children }) => {
    const [state, setState] = useState<AppState>({
        isConnected: false,
        isLoading: true,
        error: null,
        backendState: 'connected',
        unitCollections: [],
        units: [],
        lessons: [],
        materials: [],
        questions: [],
    });

    const fetchAllData = useCallback(async () => {
        try {
            setState(prev => ({ ...prev, isLoading: true, error: null }));

            // Ensure connection is established before fetching
            await signalRService.startConnection();
            setState(prev => ({ ...prev, isConnected: true }));

            const [collections, units, lessons, materials] = await Promise.all([
                signalRService.getAllUnitCollections(),
                signalRService.getAllUnits(),
                signalRService.getAllLessons(),
                signalRService.getAllMaterials(),
            ]);

            // Fetch questions for all materials
            const questionsArrays = await Promise.all(
                materials.map(m => signalRService.getQuestionsUnderMaterial(m.id))
            );
            const questions = questionsArrays.flat();

            setState(prev => ({
                ...prev,
                isLoading: false,
                unitCollections: collections,
                units,
                lessons,
                materials,
                questions,
            }));
        } catch (err) {
            console.error('Failed to fetch data:', err);
            setState(prev => ({
                ...prev,
                isConnected: false,
                isLoading: false,
                error: 'Unable to load your library. Please try again.',
            }));
        }
    }, []);

    useEffect(() => {
        const init = async () => {
            try {
                await signalRService.startConnection();
                setState(prev => ({ ...prev, isConnected: true }));
                await fetchAllData();
            } catch (err) {
                console.error('Failed to initialize connection:', err);
                // Per §2ZA(4)(d): Avoid wording that implies frontend-backend separation
                setState(prev => ({
                    ...prev,
                    isConnected: false,
                    isLoading: false,
                    error: 'Unable to load your library. Please restart the application and try again.',
                }));
            }
        };
        init();
        return () => signalRService.stopConnection();
    }, [fetchAllData]);

    // Per §2ZA(6)(c)(i): Listen for backend state changes from main process
    useEffect(() => {
        if (typeof window !== 'undefined' && window.electronAPI?.onBackendStateChange) {
            const unsubscribe = window.electronAPI.onBackendStateChange((newState) => {
                console.log('Backend state changed:', newState);
                setState(prev => ({ ...prev, backendState: newState }));
            });
            return unsubscribe;
        }
    }, []);

    // CRUD operations
    const createUnitCollection = async (dto: InternalCreateUnitCollectionDto) => {
        const created = await signalRService.createUnitCollection(dto);
        setState(prev => ({ ...prev, unitCollections: [...prev.unitCollections, created] }));
        return created;
    };

    const updateUnitCollection = async (entity: UnitCollectionEntity) => {
        await signalRService.updateUnitCollection(entity);
        setState(prev => ({
            ...prev,
            unitCollections: prev.unitCollections.map(c => c.id === entity.id ? entity : c),
        }));
    };

    const deleteUnitCollection = async (id: string) => {
        await signalRService.deleteUnitCollection(id);
        setState(prev => ({
            ...prev,
            unitCollections: prev.unitCollections.filter(c => c.id !== id),
            units: prev.units.filter(u => u.unitCollectionId !== id),
        }));
    };

    const createUnit = async (dto: InternalCreateUnitDto) => {
        const created = await signalRService.createUnit(dto);
        setState(prev => ({ ...prev, units: [...prev.units, created] }));
        return created;
    };

    const updateUnit = async (entity: UnitEntity) => {
        await signalRService.updateUnit(entity);
        setState(prev => ({
            ...prev,
            units: prev.units.map(u => u.id === entity.id ? entity : u),
        }));
    };

    const deleteUnit = async (id: string) => {
        await signalRService.deleteUnit(id);
        setState(prev => ({
            ...prev,
            units: prev.units.filter(u => u.id !== id),
            lessons: prev.lessons.filter(l => l.unitId !== id),
        }));
    };

    const createLesson = async (dto: InternalCreateLessonDto) => {
        const created = await signalRService.createLesson(dto);
        setState(prev => ({ ...prev, lessons: [...prev.lessons, created] }));
        return created;
    };

    const updateLesson = async (entity: LessonEntity) => {
        await signalRService.updateLesson(entity);
        setState(prev => ({
            ...prev,
            lessons: prev.lessons.map(l => l.id === entity.id ? entity : l),
        }));
    };

    const deleteLesson = async (id: string) => {
        await signalRService.deleteLesson(id);
        setState(prev => ({
            ...prev,
            lessons: prev.lessons.filter(l => l.id !== id),
            materials: prev.materials.filter(m => m.lessonId !== id),
        }));
    };

    const createMaterial = async (dto: InternalCreateMaterialDto) => {
        const created = await signalRService.createMaterial(dto);
        setState(prev => ({ ...prev, materials: [...prev.materials, created] }));

        // Per FrontendWorkflowSpec §4A(2)(b)(ii): When material type is poll,
        // create a default multiple choice question
        if (dto.materialType === 'POLL') {
            try {
                const questionId = await signalRService.createQuestion({
                    materialId: created.id,
                    questionType: 'MULTIPLE_CHOICE',
                    questionText: 'Poll Question',
                    options: ['Option 1', 'Option 2'],
                    maxScore: 1,
                });
                // Update material content to include the question reference
                const updatedMaterial = {
                    ...created,
                    content: `!!! question id="${questionId}"\n`,
                };
                await signalRService.updateMaterial(updatedMaterial);
                setState(prev => ({
                    ...prev,
                    materials: prev.materials.map(m => m.id === created.id ? updatedMaterial : m),
                }));
            } catch (err) {
                console.error('Failed to create default poll question:', err);
                // Roll back material creation to avoid inconsistent poll state
                try {
                    await signalRService.deleteMaterial(created.id);
                } catch (deleteErr) {
                    console.error('Failed to roll back material after poll question failure:', deleteErr);
                }
                setState(prev => ({
                    ...prev,
                    materials: prev.materials.filter(m => m.id !== created.id),
                }));
                throw err;
            }
        }

        return created;
    };

    const updateMaterial = async (entity: MaterialEntity): Promise<MaterialEntity> => {
        const updated = await signalRService.updateMaterial(entity);
        setState(prev => ({
            ...prev,
            materials: prev.materials.map(m => m.id === updated.id ? updated : m),
        }));
        return updated;
    };

    const deleteMaterial = async (id: string) => {
        await signalRService.deleteMaterial(id);
        setState(prev => ({
            ...prev,
            materials: prev.materials.filter(m => m.id !== id),
        }));
    };

    // Helpers
    const getUnitsForCollection = (collectionId: string) =>
        state.units.filter(u => u.unitCollectionId === collectionId);

    const getLessonsForUnit = (unitId: string) =>
        state.lessons.filter(l => l.unitId === unitId);

    const getMaterialsForLesson = (lessonId: string) =>
        state.materials.filter(m => m.lessonId === lessonId);

    const getQuestionsForMaterial = (materialId: string) =>
        state.questions.filter(q => q.materialId === materialId);

    // AI Generation methods - Per NetworkingAPISpec §1(1)(i)(i-ii) and (x)
    // Server generates unique ID and sends via OnGenerationStarted event
    const generateReading = async (request: GenerationRequest): Promise<GenerationResult> => {
        return await signalRService.generateReading(request);
    };

    const generateWorksheet = async (request: GenerationRequest): Promise<GenerationResult> => {
        return await signalRService.generateWorksheet(request);
    };

    const cancelGeneration = async (generationId: string): Promise<boolean> => {
        return await signalRService.cancelGeneration(generationId);
    };

    const value: AppContextValue = {
        ...state,
        createUnitCollection,
        updateUnitCollection,
        deleteUnitCollection,
        createUnit,
        updateUnit,
        deleteUnit,
        createLesson,
        updateLesson,
        deleteLesson,
        createMaterial,
        updateMaterial,
        deleteMaterial,
        generateReading,
        generateWorksheet,
        cancelGeneration,
        getUnitsForCollection,
        getLessonsForUnit,
        getMaterialsForLesson,
        getQuestionsForMaterial,
        refreshData: fetchAllData,
    };

    return (
        <AppContext.Provider value={value}>
            {children}
        </AppContext.Provider>
    );
};
